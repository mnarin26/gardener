package com.mustafa.gardener.modules.vision

import android.content.Context
import java.io.File

/**
 * Placeholder locations for a future on-device model file.
 *
 * No model is packaged or loaded yet. After `best.pt` → TFLite/ONNX/LiteRT export,
 * drop the file under assets and/or copy into [filesDirModelFile].
 *
 * TODO: pick TFLite / Ultralytics-Android / LiteRT runtime and wire load here.
 */
object VisionModelPaths {
    /** Packaged asset relative path (under `app/src/main/assets/`). */
    const val ASSET_MODEL_RELATIVE: String = "models/outdoor_nav_seg.tflite"

    /** Suggested filename when copying into app filesDir. */
    const val FILES_DIR_MODEL_NAME: String = "outdoor_nav_seg.tflite"

    fun filesDirModelFile(context: Context): File =
        File(context.applicationContext.filesDir, "models/$FILES_DIR_MODEL_NAME")

    fun assetPresent(context: Context): Boolean =
        try {
            context.applicationContext.assets.open(ASSET_MODEL_RELATIVE).use { true }
        } catch (_: Exception) {
            false
        }

    fun filesDirPresent(context: Context): Boolean = filesDirModelFile(context).isFile

    /**
     * True if any configured placeholder location has a file.
     * Presence alone does not mean inference is wired — runtime load is still TODO.
     */
    fun anyModelFilePresent(context: Context): Boolean =
        assetPresent(context) || filesDirPresent(context)

    fun describeExpectedLocations(): String =
        "assets/$ASSET_MODEL_RELATIVE or filesDir/models/$FILES_DIR_MODEL_NAME"
}
