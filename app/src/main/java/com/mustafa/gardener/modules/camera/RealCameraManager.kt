package com.mustafa.gardener.modules.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.mustafa.gardener.core.DriverMode
import com.mustafa.gardener.core.ModuleStatus
import com.mustafa.gardener.modules.vision.VisionFrame
import com.mustafa.gardener.modules.vision.VisionManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * CameraX-backed camera manager for continuous low-load preview.
 *
 * Stated assumption: target ~640×480 (VGA / 4:3) via ResolutionSelector;
 * device may bind the closest supported Size. No ImageCapture / high-res
 * still path — Preview use case by default.
 *
 * Optional [ImageAnalysis] is **disabled until** a [VisionManager] reports
 * [VisionManager.wantsFrameAnalysis] == true (model loaded). Preview-only
 * must not keep an analysis pipeline alive (battery).
 *
 * Device camera success is NOT claimed until hardware testing.
 * On-device ML inference is NOT claimed — analysis hook only forwards metadata.
 */
class RealCameraManager(
    context: Context,
) : CameraManager {
    override val moduleName: String = "CameraManager"
    override val mode: DriverMode = DriverMode.REAL

    private val appContext = context.applicationContext
    private val running = AtomicBoolean(false)
    private val previewBound = AtomicBoolean(false)
    private val analysisBound = AtomicBoolean(false)

    @Volatile
    private var cameraProvider: ProcessCameraProvider? = null

    @Volatile
    private var boundPreviewView: PreviewView? = null

    @Volatile
    private var boundLifecycleOwner: LifecycleOwner? = null

    @Volatile
    private var boundResolutionText: String = "target 640×480 (not bound)"

    @Volatile
    private var lastError: String? = null

    private val visionManagerRef = AtomicReference<VisionManager?>(null)
    private val lastAnalysisNs = AtomicLong(0L)
    private val analysisExecutor: ExecutorService =
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "gardener-camera-analysis").apply { isDaemon = true }
        }

    override fun start() {
        running.set(true)
    }

    override fun stop() {
        unbindPreview()
        running.set(false)
    }

    override fun isRunning(): Boolean = running.get()

    override fun hasLiveFrames(): Boolean = previewBound.get() && lastError == null

    override fun previewResolutionText(): String = boundResolutionText

    /** True only when ImageAnalysis is actually bound (model-driven; usually false). */
    fun isAnalysisBound(): Boolean = analysisBound.get()

    /**
     * Attach a [VisionManager] that may later request analysis frames.
     * Rebinds if preview is already up so enablement can change after model drop-in.
     */
    fun attachVisionManager(visionManager: VisionManager?) {
        visionManagerRef.set(visionManager)
        val owner = boundLifecycleOwner
        val view = boundPreviewView
        if (owner != null && view != null && previewBound.get()) {
            bindPreview(owner, view)
        }
    }

    /**
     * Bind a lifecycle-aware CameraX Preview to [previewView].
     * Call from the main thread after CAMERA permission is granted.
     * Idempotent for the same [previewView] while already bound with same analysis flag.
     */
    fun bindPreview(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val wantAnalysis = visionManagerRef.get()?.wantsFrameAnalysis() == true
        if (
            previewBound.get() &&
            boundPreviewView === previewView &&
            lastError == null &&
            analysisBound.get() == wantAnalysis
        ) {
            return
        }

        running.set(true)
        lastError = null
        boundResolutionText = "binding… target 640×480"
        boundLifecycleOwner = lifecycleOwner

        val future = ProcessCameraProvider.getInstance(appContext)
        future.addListener(
            {
                try {
                    val stillWantAnalysis =
                        visionManagerRef.get()?.wantsFrameAnalysis() == true
                    if (
                        boundPreviewView === previewView &&
                        previewBound.get() &&
                        lastError == null &&
                        analysisBound.get() == stillWantAnalysis
                    ) {
                        return@addListener
                    }
                    val provider = future.get()
                    cameraProvider = provider
                    provider.unbindAll()

                    val resolutionSelector = ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(TARGET_WIDTH, TARGET_HEIGHT),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                            )
                        )
                        .build()

                    val preview = Preview.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build()
                        .also { it.surfaceProvider = previewView.surfaceProvider }

                    val analysis = if (stillWantAnalysis) {
                        ImageAnalysis.Builder()
                            .setResolutionSelector(resolutionSelector)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { useCase ->
                                useCase.setAnalyzer(analysisExecutor) { imageProxy ->
                                    try {
                                        deliverThrottledFrame(imageProxy.width, imageProxy.height, imageProxy.imageInfo.timestamp, imageProxy.imageInfo.rotationDegrees)
                                    } finally {
                                        imageProxy.close()
                                    }
                                }
                            }
                    } else {
                        null
                    }

                    if (analysis != null) {
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                        analysisBound.set(true)
                    } else {
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                        )
                        analysisBound.set(false)
                    }

                    val resolution = preview.resolutionInfo?.resolution
                    val analysisNote = if (analysisBound.get()) {
                        "analysis on (≤${ANALYSIS_MAX_FPS} fps)"
                    } else {
                        "analysis off (preview only)"
                    }
                    boundResolutionText = if (resolution != null) {
                        "${resolution.width}×${resolution.height} (target ${TARGET_WIDTH}×${TARGET_HEIGHT}); $analysisNote"
                    } else {
                        "bound — resolution unknown (target ${TARGET_WIDTH}×${TARGET_HEIGHT}); $analysisNote"
                    }
                    boundPreviewView = previewView
                    previewBound.set(true)
                    lastError = null
                } catch (e: Exception) {
                    previewBound.set(false)
                    analysisBound.set(false)
                    boundPreviewView = null
                    lastError = e.message ?: e.javaClass.simpleName
                    boundResolutionText = "error — $lastError"
                }
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    private fun deliverThrottledFrame(
        width: Int,
        height: Int,
        timestampNs: Long,
        rotationDegrees: Int,
    ) {
        val minIntervalNs = 1_000_000_000L / ANALYSIS_MAX_FPS
        val previous = lastAnalysisNs.get()
        if (timestampNs > 0L && previous > 0L && timestampNs - previous < minIntervalNs) {
            return
        }
        if (timestampNs > 0L) {
            lastAnalysisNs.set(timestampNs)
        } else {
            val now = System.nanoTime()
            val prevWall = lastAnalysisNs.get()
            if (prevWall > 0L && now - prevWall < minIntervalNs) return
            lastAnalysisNs.set(now)
        }

        val vision = visionManagerRef.get() ?: return
        if (!vision.wantsFrameAnalysis()) return
        vision.onAnalysisFrame(
            VisionFrame(
                width = width,
                height = height,
                timestampNs = timestampNs,
                rotationDegrees = rotationDegrees,
            )
        )
    }

    fun unbindPreview() {
        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {
            // Provider may already be shut down with the process.
        }
        boundPreviewView = null
        boundLifecycleOwner = null
        previewBound.set(false)
        analysisBound.set(false)
        if (lastError == null) {
            boundResolutionText = "target 640×480 (unbound)"
        }
    }

    override fun status(): ModuleStatus {
        val detail = when {
            lastError != null -> "REAL — CameraX error: $lastError"
            previewBound.get() -> "REAL — CameraX preview bound; $boundResolutionText"
            running.get() -> "REAL — CameraX ready; $boundResolutionText"
            else -> "REAL — CameraX idle; $boundResolutionText"
        }
        return ModuleStatus(
            moduleName = moduleName,
            mode = mode,
            running = isRunning(),
            detail = detail,
        )
    }

    companion object {
        const val TARGET_WIDTH = 640
        const val TARGET_HEIGHT = 480

        /** Analysis budget when (and only when) a loaded engine requests frames. */
        const val ANALYSIS_MAX_FPS = 5
    }
}
