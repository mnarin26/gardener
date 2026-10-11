package com.mustafa.gardener.modules.vision

import android.content.Context
import android.util.Log

/**
 * Builds the real-path [VisionEngine]: TFLite when the asset/filesDir model loads,
 * otherwise unloaded / failed placeholders.
 */
object VisionEngineFactory {
    private const val TAG = "VisionEngineFactory"

    fun forRealPath(context: Context): VisionEngine {
        if (!VisionModelPaths.anyModelFilePresent(context)) {
            return UnloadedVisionEngine()
        }
        return try {
            TfliteSegVisionEngine.load(context)
        } catch (e: Exception) {
            Log.e(TAG, "TFLite vision engine load failed", e)
            FailedVisionEngine(e.message ?: e.javaClass.simpleName)
        }
    }
}
