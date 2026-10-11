package com.mustafa.gardener.modules.vision

import android.content.Context
import android.util.Log

/**
 * Builds the real-path [VisionEngine].
 *
 * Default: YOLOv8n COCO detection. FFNet seg remains loadable via [VisionBackend.FFNET_SEG].
 */
object VisionEngineFactory {
    private const val TAG = "VisionEngineFactory"

    fun forRealPath(
        context: Context,
        backend: VisionBackend = VisionConfig.DEFAULT_BACKEND,
    ): VisionEngine {
        if (!VisionModelPaths.anyModelFilePresent(context, backend)) {
            return UnloadedVisionEngine()
        }
        return try {
            when (backend) {
                VisionBackend.YOLO_DETECT -> TfliteYoloDetectVisionEngine.load(context)
                VisionBackend.FFNET_SEG -> TfliteSegVisionEngine.load(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "TFLite vision engine load failed ($backend)", e)
            FailedVisionEngine(e.message ?: e.javaClass.simpleName)
        }
    }

    fun closeQuietly(engine: VisionEngine?) {
        when (engine) {
            is TfliteYoloDetectVisionEngine -> engine.close()
            is TfliteSegVisionEngine -> engine.close()
        }
    }
}
