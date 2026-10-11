package com.mustafa.gardener.modules.vision

import android.content.Context
import android.graphics.Bitmap
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

/**
 * Real TFLite (LiteRT Interpreter) semantic segmentation engine.
 *
 * Default bundled model: Qualcomm FFNet-122NS-LowRes w8a8 (Cityscapes, 512×1024 input).
 * Prefer NNAPI delegate when available; fall back to CPU (XNNPACK). Device NPU success
 * is NOT claimed — only that the delegate was requested in Options.
 */
class TfliteSegVisionEngine private constructor(
    private val interpreter: Interpreter,
    private val nnApiDelegate: NnApiDelegate?,
    private val delegateLabel: String,
    private val modelFileLabel: String,
    private val inputHeight: Int,
    private val inputWidth: Int,
    private val maskHeight: Int,
    private val maskWidth: Int,
    private val numClasses: Int,
    @Suppress("unused") private val inputScale: Float,
    @Suppress("unused") private val inputZeroPoint: Int,
    private val outputScale: Float,
    private val outputZeroPoint: Int,
) : VisionEngine {

    private val closed = AtomicBoolean(false)
    private val busy = AtomicBoolean(false)
    private val latestOverlay = AtomicReference<SegmentationOverlay?>(null)
    private val inputBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(1 * inputHeight * inputWidth * 3).order(ByteOrder.nativeOrder())
    private val outputBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(1 * maskHeight * maskWidth * numClasses)
            .order(ByteOrder.nativeOrder())
    private val overlayPixels = IntArray(maskWidth * maskHeight)
    private val reusedScaled = IntArray(inputWidth * inputHeight)

    override fun state(): VisionEngineState = VisionEngineState.READY

    override fun modelLabel(): String = modelFileLabel

    override fun statusDetail(): String =
        "READY — $modelFileLabel; delegate=$delegateLabel (device verify pending); " +
            "input=${inputWidth}x$inputHeight; mask=${maskWidth}x$maskHeight; " +
            "highlight=${VisionLabels.phase1Summary()}"

    override fun uiStatusTr(): String =
        "Model yüklendi — FFNet Cityscapes (yol/kaldırım/duvar/çit)"

    override fun wantsFrameAnalysis(): Boolean = !closed.get()

    override fun classLabels(): List<String> = CityscapesLabels.ALL

    override fun latestOverlay(): SegmentationOverlay? = latestOverlay.get()

    override fun onFrame(frame: VisionFrame) {
        if (closed.get()) return
        val src = frame.argbPixels ?: return
        if (src.size < frame.width * frame.height) return
        if (!busy.compareAndSet(false, true)) return
        try {
            runInference(src, frame.width, frame.height, frame.timestampNs)
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
        timestampNs: Long,
    ) {
        scaleNearestArgb(srcArgb, srcW, srcH, reusedScaled, inputWidth, inputHeight)
        inputBuffer.rewind()
        for (i in 0 until inputWidth * inputHeight) {
            val c = reusedScaled[i]
            // Model expects quantized uint8 with scale≈1/255, zp=0 → raw 0..255 RGB.
            inputBuffer.put(((c shr 16) and 0xFF).toByte())
            inputBuffer.put(((c shr 8) and 0xFF).toByte())
            inputBuffer.put((c and 0xFF).toByte())
        }
        inputBuffer.rewind()
        outputBuffer.rewind()
        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()

        val counts = HashMap<String, Int>(4)
        for (y in 0 until maskHeight) {
            for (x in 0 until maskWidth) {
                var bestIdx = 0
                var bestScore = Float.NEGATIVE_INFINITY
                val base = (y * maskWidth + x) * numClasses
                for (c in 0 until numClasses) {
                    val q = outputBuffer.get(base + c).toInt() and 0xFF
                    val score = (q - outputZeroPoint) * outputScale
                    if (score > bestScore) {
                        bestScore = score
                        bestIdx = c
                    }
                }
                val color = if (CityscapesLabels.isHighlight(bestIdx)) {
                    val name = CityscapesLabels.ALL[bestIdx]
                    counts[name] = (counts[name] ?: 0) + 1
                    CityscapesLabels.overlayColorArgb(bestIdx)
                } else {
                    0
                }
                overlayPixels[y * maskWidth + x] = color
            }
        }

        val bmp = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888)
        bmp.setPixels(overlayPixels, 0, maskWidth, 0, 0, maskWidth, maskHeight)
        latestOverlay.getAndSet(
            SegmentationOverlay(
                bitmap = bmp,
                maskWidth = maskWidth,
                maskHeight = maskHeight,
                timestampNs = timestampNs,
                classPixelCounts = counts,
            )
        )?.bitmap?.recycle()
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
        latestOverlay.getAndSet(null)?.bitmap?.recycle()
    }

    companion object {
        private const val TAG = "TfliteSegVisionEngine"

        /** Default FFNet-122NS-LowRes shapes from Hub metadata. */
        private const val DEFAULT_IN_H = 512
        private const val DEFAULT_IN_W = 1024
        private const val DEFAULT_MASK_H = 128
        private const val DEFAULT_MASK_W = 256
        private const val DEFAULT_CLASSES = 19

        fun load(context: Context): TfliteSegVisionEngine {
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

            val inDetails = interpreter.getInputTensor(0)
            val outDetails = interpreter.getOutputTensor(0)
            val inShape = inDetails.shape() // [1,H,W,3]
            val outShape = outDetails.shape() // [1,h,w,C] or [1,h,w]
            val inH = if (inShape.size >= 3) inShape[1] else DEFAULT_IN_H
            val inW = if (inShape.size >= 3) inShape[2] else DEFAULT_IN_W
            val maskH: Int
            val maskW: Int
            val numClasses: Int
            if (outShape.size == 4) {
                maskH = outShape[1]
                maskW = outShape[2]
                numClasses = outShape[3]
            } else {
                maskH = outShape.getOrElse(1) { DEFAULT_MASK_H }
                maskW = outShape.getOrElse(2) { DEFAULT_MASK_W }
                numClasses = DEFAULT_CLASSES
            }

            val inQ = inDetails.quantizationParams()
            val outQ = outDetails.quantizationParams()

            Log.i(
                TAG,
                "Loaded $modelLabel delegate=$delegateLabel in=${inW}x$inH out=${maskW}x$maskH x$numClasses",
            )

            return TfliteSegVisionEngine(
                interpreter = interpreter,
                nnApiDelegate = nnApi,
                delegateLabel = delegateLabel,
                modelFileLabel = modelLabel,
                inputHeight = inH,
                inputWidth = inW,
                maskHeight = maskH,
                maskWidth = maskW,
                numClasses = numClasses.coerceAtLeast(1),
                inputScale = inQ.scale.takeIf { it > 0f } ?: (1f / 255f),
                inputZeroPoint = inQ.zeroPoint,
                outputScale = outQ.scale.takeIf { it > 0f } ?: 1f,
                outputZeroPoint = outQ.zeroPoint,
            )
        }

        private fun resolveModelLabel(context: Context): String {
            if (VisionModelPaths.filesDirPresent(context)) {
                return VisionModelPaths.FILES_DIR_MODEL_NAME
            }
            return VisionModelPaths.resolveAssetModelRelative(context)
                ?.substringAfterLast('/')
                ?: VisionModelPaths.ASSET_MODEL_RELATIVE.substringAfterLast('/')
        }

        private fun loadModelBuffer(context: Context): MappedByteBuffer {
            val filesModel = VisionModelPaths.filesDirModelFile(context)
            if (filesModel.isFile) {
                FileInputStream(filesModel).use { fis ->
                    return fis.channel.map(FileChannel.MapMode.READ_ONLY, 0, filesModel.length())
                }
            }
            val relative = VisionModelPaths.resolveAssetModelRelative(context)
                ?: error("no model asset")
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

        private fun scaleNearestArgb(
            src: IntArray,
            srcW: Int,
            srcH: Int,
            dst: IntArray,
            dstW: Int,
            dstH: Int,
        ) {
            for (y in 0 until dstH) {
                val sy = y * srcH / dstH
                for (x in 0 until dstW) {
                    val sx = x * srcW / dstW
                    dst[y * dstW + x] = src[sy * srcW + sx]
                }
            }
        }
    }
}
