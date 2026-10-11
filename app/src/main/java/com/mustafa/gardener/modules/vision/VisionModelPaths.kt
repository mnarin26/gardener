package com.mustafa.gardener.modules.vision

import android.content.Context
import java.io.File

/**
 * On-device model locations for the ready-seg trial (and future fine-tuned export).
 *
 * Bundled trial asset: Qualcomm FFNet-122NS-LowRes w8a8 TFLite (Cityscapes).
 * Fine-tune export may later overwrite / replace via filesDir.
 */
object VisionModelPaths {
    /** Packaged asset relative path (under `app/src/main/assets/`). */
    const val ASSET_MODEL_RELATIVE: String = "models/ffnet_122ns_lowres_w8a8.tflite"

    /** Cityscapes label list shipped next to the model. */
    const val ASSET_LABELS_RELATIVE: String = "models/ffnet_122ns_lowres_labels.txt"

    /** Suggested filename when copying a fine-tuned model into app filesDir. */
    const val FILES_DIR_MODEL_NAME: String = "outdoor_nav_seg.tflite"

    /** Legacy / alternate asset name still recognized if present. */
    const val ASSET_LEGACY_MODEL_RELATIVE: String = "models/outdoor_nav_seg.tflite"

    fun filesDirModelFile(context: Context): File =
        File(context.applicationContext.filesDir, "models/$FILES_DIR_MODEL_NAME")

    fun assetPresent(context: Context): Boolean =
        assetExists(context, ASSET_MODEL_RELATIVE) ||
            assetExists(context, ASSET_LEGACY_MODEL_RELATIVE)

    fun filesDirPresent(context: Context): Boolean = filesDirModelFile(context).isFile

    /**
     * True if any configured model location has a file.
     */
    fun anyModelFilePresent(context: Context): Boolean =
        assetPresent(context) || filesDirPresent(context)

    fun describeExpectedLocations(): String =
        "assets/$ASSET_MODEL_RELATIVE (or $ASSET_LEGACY_MODEL_RELATIVE) " +
            "or filesDir/models/$FILES_DIR_MODEL_NAME"

    fun resolveAssetModelRelative(context: Context): String? = when {
        assetExists(context, ASSET_MODEL_RELATIVE) -> ASSET_MODEL_RELATIVE
        assetExists(context, ASSET_LEGACY_MODEL_RELATIVE) -> ASSET_LEGACY_MODEL_RELATIVE
        else -> null
    }

    private fun assetExists(context: Context, relative: String): Boolean =
        try {
            context.applicationContext.assets.open(relative).use { true }
        } catch (_: Exception) {
            false
        }
}
