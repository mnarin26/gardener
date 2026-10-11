package com.mustafa.gardener.modules.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.mustafa.gardener.core.DriverMode
import com.mustafa.gardener.core.ModuleStatus
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CameraX-backed camera manager for continuous low-load preview.
 *
 * Stated assumption: target ~640×480 (VGA / 4:3) via ResolutionSelector;
 * device may bind the closest supported Size. No ImageCapture / high-res
 * still path — Preview use case only for continuous run.
 *
 * Device camera success is NOT claimed until hardware testing.
 */
class RealCameraManager(
    context: Context,
) : CameraManager {
    override val moduleName: String = "CameraManager"
    override val mode: DriverMode = DriverMode.REAL

    private val appContext = context.applicationContext
    private val running = AtomicBoolean(false)
    private val previewBound = AtomicBoolean(false)

    @Volatile
    private var cameraProvider: ProcessCameraProvider? = null

    @Volatile
    private var boundPreviewView: PreviewView? = null

    @Volatile
    private var boundResolutionText: String = "target 640×480 (not bound)"

    @Volatile
    private var lastError: String? = null

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

    /**
     * Bind a lifecycle-aware CameraX Preview to [previewView].
     * Call from the main thread after CAMERA permission is granted.
     * Idempotent for the same [previewView] while already bound.
     */
    fun bindPreview(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        if (previewBound.get() && boundPreviewView === previewView && lastError == null) {
            return
        }

        running.set(true)
        lastError = null
        boundResolutionText = "binding… target 640×480"

        val future = ProcessCameraProvider.getInstance(appContext)
        future.addListener(
            {
                try {
                    if (boundPreviewView === previewView && previewBound.get() && lastError == null) {
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

                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                    )

                    val resolution = preview.resolutionInfo?.resolution
                    boundResolutionText = if (resolution != null) {
                        "${resolution.width}×${resolution.height} (target ${TARGET_WIDTH}×${TARGET_HEIGHT})"
                    } else {
                        "bound — resolution unknown (target ${TARGET_WIDTH}×${TARGET_HEIGHT})"
                    }
                    boundPreviewView = previewView
                    previewBound.set(true)
                    lastError = null
                } catch (e: Exception) {
                    previewBound.set(false)
                    boundPreviewView = null
                    lastError = e.message ?: e.javaClass.simpleName
                    boundResolutionText = "error — $lastError"
                }
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    fun unbindPreview() {
        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {
            // Provider may already be shut down with the process.
        }
        boundPreviewView = null
        previewBound.set(false)
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
    }
}
