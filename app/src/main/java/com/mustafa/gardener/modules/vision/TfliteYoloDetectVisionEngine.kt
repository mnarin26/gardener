package com.mustafa.gardener.modules.vision

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Real TFLite YOLOv8n COCO object detection engine (smoke-trial default).
 *
 * Prefers NNAPI delegate; falls back to CPU. Does **not** claim NPU verified
 * on device — only that the delegate was requested.
 *
 * Input: float32 NHWC 640×640 (letterboxed). Output: [1,84,8400] (cx,cy,w,h + 80 cls).
 * Box coords may be normalized 0–1 or pixels in input space; [decodeAndNms] auto-detects.
 */
class TfliteYoloDetectVisionEngine private constructor(
    private val interpreter: Interpreter,
    private val nnApiDelegate: NnApiDelegate?,
    private val delegateLabel: String,
    private val modelFileLabel: String,
    private val inputSize: Int,
    private val numAnchors: Int,
    private val numClasses: Int,
) : VisionEngine {

    private val closed = AtomicBoolean(false)
    private val busy = AtomicBoolean(false)
    private val latest = AtomicReference<DetectionOverlay?>(null)
    private val confThreshold = AtomicReference(DetectTrialSettings.DEFAULT_CONF)
    /** null = all COCO classes; otherwise keep only these class indices after NMS. */
    private val allowedClassIndices = AtomicReference<Set<Int>?>(null)

    private val inputFloat = FloatArray(1 * inputSize * inputSize * 3)
    private val inputBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(4 * inputFloat.size).order(ByteOrder.nativeOrder())
    private val outputBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(4 * 1 * (4 + numClasses) * numAnchors)
            .order(ByteOrder.nativeOrder())
    private val outputFloat = FloatArray((4 + numClasses) * numAnchors)

    override fun state(): VisionEngineState = VisionEngineState.READY

    override fun modelLabel(): String = modelFileLabel

    override fun statusDetail(): String =
        "READY — $modelFileLabel; delegate=$delegateLabel (device verify pending); " +
            "input=${inputSize}x$inputSize; anchors=$numAnchors; classes=$numClasses COCO"

    override fun uiStatusTr(): String =
        "Model yüklendi — YOLOv8n COCO tespit (kişi/araba/…)"

    override fun wantsFrameAnalysis(): Boolean = !closed.get()

    override fun classLabels(): List<String> = CocoLabels.ALL

    override fun latestOverlay(): SegmentationOverlay? = null

    override fun latestDetections(): DetectionOverlay? = latest.get()

    fun confThreshold(): Float = confThreshold.get()

    fun setConfThreshold(value: Float) {
        confThreshold.set(DetectTrialSettings.clampConf(value))
    }

    fun setAllowedClassIndices(indices: Set<Int>?) {
        allowedClassIndices.set(indices)
    }

    override fun onFrame(frame: VisionFrame) {
        if (closed.get()) return
        val src = frame.argbPixels ?: return
        if (src.size < frame.width * frame.height) return
        if (!busy.compareAndSet(false, true)) return
        try {
            runInference(src, frame.width, frame.height, frame.rotationDegrees, frame.timestampNs)
        } catch (e: Exception) {
            Log.w(TAG, "inference failed: ${e.message}")
        } finally {
            busy.set(false)
        }
    }

    private fun runInference(
        srcArgb: IntArray,
        srcW: Int,
        srcH: Int,
        rotationDegrees: Int,
        timestampNs: Long,
    ) {
        val oriented = FrameOrientation.rotateArgb(srcArgb, srcW, srcH, rotationDegrees)
        val meta = FrameOrientation.letterboxRgbFloat(
            oriented.pixels,
            oriented.width,
            oriented.height,
            inputFloat,
            inputSize,
            inputSize,
        )

        inputBuffer.rewind()
        inputBuffer.asFloatBuffer().put(inputFloat)
        inputBuffer.rewind()
        outputBuffer.rewind()

        val t0 = System.nanoTime()
        interpreter.run(inputBuffer, outputBuffer)
        val inferenceMs = (System.nanoTime() - t0) / 1_000_000L
        Log.i(TAG, "inferenceMs=$inferenceMs delegate=$delegateLabel")

        outputBuffer.rewind()
        outputBuffer.asFloatBuffer().get(outputFloat)

        val boxes = decodeAndNms(
            raw = outputFloat,
            meta = meta,
            inputSize = inputSize,
            numAnchors = numAnchors,
            numClasses = numClasses,
            confThreshold = confThreshold.get(),
        )
        val allowed = allowedClassIndices.get()
        val filtered = if (allowed == null) {
            boxes
        } else {
            boxes.filter { it.classIndex in allowed }
        }
        Log.i(
            TAG,
            "detections=${filtered.size} after NMS" +
                (if (allowed != null) " (class filter)" else "") +
                " conf=${"%.2f".format(confThreshold.get())}",
        )
        latest.set(
            DetectionOverlay(
                boxes = filtered,
                imageWidth = oriented.width,
                imageHeight = oriented.height,
                timestampNs = timestampNs,
                inferenceMs = inferenceMs,
            )
        )
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        try {
            interpreter.close()
        } catch (_: Exception) {
        }
        try {
            nnApiDelegate?.close()
        } catch (_: Exception) {
        }
        latest.set(null)
    }

    companion object {
        private const val TAG = "GardenerVision"
        private const val DEFAULT_INPUT = 640
        private const val DEFAULT_ANCHORS = 8400
        private const val DEFAULT_CLASSES = 80
        private const val DEFAULT_CONF_THRESHOLD = DetectTrialSettings.DEFAULT_CONF
        private const val IOU_THRESHOLD = 0.45f
        private const val MAX_DETECTIONS = 25
        /** If all box coords among candidates are ≤ this, treat as normalized 0–1. */
        private const val NORMALIZED_COORD_MAX = 1.5f

        /**
         * Decode YOLOv8 [1,84,N] channel-major output + NMS.
         *
         * Ultralytics float32 TFLite often emits **normalized** cx/cy/w/h in ~0–1;
         * some exports emit pixels in inputSize space. Auto-detect: if max(|cx|,|cy|,|w|,|h|)
         * among conf-passers ≤ [NORMALIZED_COORD_MAX], scale by [inputSize] before
         * letterbox unpad.
         */
        internal fun decodeAndNms(
            raw: FloatArray,
            meta: FrameOrientation.LetterboxMeta,
            inputSize: Int,
            numAnchors: Int = DEFAULT_ANCHORS,
            numClasses: Int = DEFAULT_CLASSES,
            confThreshold: Float = DEFAULT_CONF_THRESHOLD,
        ): List<DetectionBox> {
            // Layout [1, 84, N] flattened as channel-major: raw[c * N + a]
            val n = numAnchors
            val minConf = DetectTrialSettings.clampConf(confThreshold)
            val scored = ArrayList<ScoredAnchor>(64)
            var maxBoxCoord = 0f

            for (a in 0 until n) {
                var bestCls = 0
                var bestScore = 0f
                for (c in 0 until numClasses) {
                    val s = raw[(4 + c) * n + a]
                    if (s > bestScore) {
                        bestScore = s
                        bestCls = c
                    }
                }
                if (bestScore < minConf) continue

                val cx = raw[0 * n + a]
                val cy = raw[1 * n + a]
                val w = raw[2 * n + a]
                val h = raw[3 * n + a]
                maxBoxCoord = max(
                    maxBoxCoord,
                    max(max(abs(cx), abs(cy)), max(abs(w), abs(h))),
                )
                scored.add(ScoredAnchor(cx, cy, w, h, bestCls, bestScore))
            }

            val toPixels = maxBoxCoord <= NORMALIZED_COORD_MAX
            val coordScale = if (toPixels) inputSize.toFloat() else 1f
            val candidates = ArrayList<Candidate>(scored.size)

            for (s in scored) {
                val cx = s.cx * coordScale
                val cy = s.cy * coordScale
                val w = s.w * coordScale
                val h = s.h * coordScale

                // Undo letterbox → upright image pixels
                val x1 = ((cx - w / 2f) - meta.padX) / meta.scale
                val y1 = ((cy - h / 2f) - meta.padY) / meta.scale
                val x2 = ((cx + w / 2f) - meta.padX) / meta.scale
                val y2 = ((cy + h / 2f) - meta.padY) / meta.scale

                val left = (x1 / meta.srcW).coerceIn(0f, 1f)
                val top = (y1 / meta.srcH).coerceIn(0f, 1f)
                val right = (x2 / meta.srcW).coerceIn(0f, 1f)
                val bottom = (y2 / meta.srcH).coerceIn(0f, 1f)
                if (right <= left || bottom <= top) continue

                candidates.add(
                    Candidate(
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom,
                        classIndex = s.classIndex,
                        score = s.score,
                    )
                )
            }

            candidates.sortByDescending { it.score }
            val kept = ArrayList<DetectionBox>(MAX_DETECTIONS)
            val suppressed = BooleanArray(candidates.size)
            for (i in candidates.indices) {
                if (suppressed[i]) continue
                val a = candidates[i]
                val label = CocoLabels.ALL.getOrElse(a.classIndex) { "cls${a.classIndex}" }
                kept.add(
                    DetectionBox(
                        left = a.left,
                        top = a.top,
                        right = a.right,
                        bottom = a.bottom,
                        label = CocoLabels.displayLabel(label),
                        classIndex = a.classIndex,
                        score = a.score,
                    )
                )
                if (kept.size >= MAX_DETECTIONS) break
                for (j in i + 1 until candidates.size) {
                    if (suppressed[j]) continue
                    val b = candidates[j]
                    if (a.classIndex != b.classIndex) continue
                    if (iou(a, b) >= IOU_THRESHOLD) suppressed[j] = true
                }
            }
            return kept
        }

        private data class ScoredAnchor(
            val cx: Float,
            val cy: Float,
            val w: Float,
            val h: Float,
            val classIndex: Int,
            val score: Float,
        )

        private data class Candidate(
            val left: Float,
            val top: Float,
            val right: Float,
            val bottom: Float,
            val classIndex: Int,
            val score: Float,
        )

        fun load(context: Context): TfliteYoloDetectVisionEngine {
            val app = context.applicationContext
            val modelBuffer = loadModelBuffer(app)
            val modelLabel = resolveModelLabel(app)

            var nnApi: NnApiDelegate? = null
            var delegateLabel = "CPU"
            val interpreter = try {
                nnApi = NnApiDelegate()
                val options = Interpreter.Options().apply {
                    addDelegate(nnApi)
                    setNumThreads(2)
                }
                Interpreter(modelBuffer, options).also { delegateLabel = "NNAPI" }
            } catch (e: Exception) {
                Log.w(TAG, "NNAPI delegate unavailable, falling back to CPU: ${e.message}")
                try {
                    nnApi?.close()
                } catch (_: Exception) {
                }
                nnApi = null
                Interpreter(
                    modelBuffer,
                    Interpreter.Options().apply { setNumThreads(4) },
                ).also { delegateLabel = "CPU" }
            }

            val inShape = interpreter.getInputTensor(0).shape()
            val outShape = interpreter.getOutputTensor(0).shape()
            val inputSize = when {
                inShape.size >= 3 && inShape[1] == inShape[2] -> inShape[1]
                inShape.size >= 4 -> inShape[2]
                else -> DEFAULT_INPUT
            }
            // Expect [1, 84, N] or [1, N, 84]
            val (numClasses, numAnchors) = when {
                outShape.size == 3 && outShape[1] == 4 + DEFAULT_CLASSES ->
                    DEFAULT_CLASSES to outShape[2]
                outShape.size == 3 && outShape[2] == 4 + DEFAULT_CLASSES ->
                    DEFAULT_CLASSES to outShape[1]
                outShape.size == 3 && outShape[1] > outShape[2] ->
                    (outShape[1] - 4).coerceAtLeast(1) to outShape[2]
                outShape.size == 3 ->
                    (outShape[2] - 4).coerceAtLeast(1) to outShape[1]
                else -> DEFAULT_CLASSES to DEFAULT_ANCHORS
            }

            Log.i(
                TAG,
                "Loaded $modelLabel delegate=$delegateLabel in=${inputSize}x$inputSize " +
                    "out=${outShape.contentToString()} classes=$numClasses anchors=$numAnchors",
            )

            return TfliteYoloDetectVisionEngine(
                interpreter = interpreter,
                nnApiDelegate = nnApi,
                delegateLabel = delegateLabel,
                modelFileLabel = modelLabel,
                inputSize = inputSize,
                numAnchors = numAnchors,
                numClasses = numClasses,
            )
        }

        private fun resolveModelLabel(context: Context): String {
            if (VisionModelPaths.yoloFilesDirPresent(context)) {
                return VisionModelPaths.FILES_DIR_YOLO_NAME
            }
            return VisionModelPaths.resolveYoloAssetRelative(context)
                ?.substringAfterLast('/')
                ?: VisionModelPaths.ASSET_YOLO_RELATIVE.substringAfterLast('/')
        }

        private fun loadModelBuffer(context: Context): MappedByteBuffer {
            val filesModel = VisionModelPaths.filesDirYoloFile(context)
            if (filesModel.isFile) {
                FileInputStream(filesModel).use { fis ->
                    return fis.channel.map(FileChannel.MapMode.READ_ONLY, 0, filesModel.length())
                }
            }
            val relative = VisionModelPaths.resolveYoloAssetRelative(context)
                ?: error("no YOLO model asset")
            context.assets.openFd(relative).use { fd ->
                FileInputStream(fd.fileDescriptor).use { fis ->
                    return fis.channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        fd.startOffset,
                        fd.declaredLength,
                    )
                }
            }
        }

        private fun iou(a: Candidate, b: Candidate): Float {
            val ix1 = max(a.left, b.left)
            val iy1 = max(a.top, b.top)
            val ix2 = min(a.right, b.right)
            val iy2 = min(a.bottom, b.bottom)
            val iw = max(0f, ix2 - ix1)
            val ih = max(0f, iy2 - iy1)
            val inter = iw * ih
            val union = (a.right - a.left) * (a.bottom - a.top) +
                (b.right - b.left) * (b.bottom - b.top) - inter
            return if (union <= 0f) 0f else inter / union
        }
    }
}
