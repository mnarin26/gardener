package com.mustafa.gardener.modules.vision

import android.content.Context
import java.io.File

/**
 * On-device model locations.
 *
 * Default trial: Ultralytics YOLOv8n COCO float32 TFLite (detection).
 * Optional: Qualcomm FFNet Cityscapes seg (behind [VisionBackend.FFNET_SEG]).
 */
object VisionModelPaths {
    /** Default YOLO detect asset. */
    const val ASSET_YOLO_RELATIVE: String = "models/yolov8n_float32.tflite"

    const val ASSET_YOLO_LABELS_RELATIVE: String = "models/yolov8n_coco_labels.txt"

    /** Optional filesDir override for detect. */
    const val FILES_DIR_YOLO_NAME: String = "yolov8n_detect.tflite"

    /** Bundled FFNet seg asset (not default). */
    const val ASSET_FFNET_RELATIVE: String = "models/ffnet_122ns_lowres_w8a8.tflite"

    const val ASSET_FFNET_LABELS_RELATIVE: String = "models/ffnet_122ns_lowres_labels.txt"

    /** Suggested filename when copying a fine-tuned seg model into app filesDir. */
    const val FILES_DIR_SEG_NAME: String = "outdoor_nav_seg.tflite"

    /** Legacy / alternate seg asset name still recognized if present. */
    const val ASSET_LEGACY_SEG_RELATIVE: String = "models/outdoor_nav_seg.tflite"

    // --- Backward-compatible aliases used by TfliteSegVisionEngine ---
    const val ASSET_MODEL_RELATIVE: String = ASSET_FFNET_RELATIVE
    const val ASSET_LABELS_RELATIVE: String = ASSET_FFNET_LABELS_RELATIVE
    const val FILES_DIR_MODEL_NAME: String = FILES_DIR_SEG_NAME
    const val ASSET_LEGACY_MODEL_RELATIVE: String = ASSET_LEGACY_SEG_RELATIVE

    fun filesDirYoloFile(context: Context): File =
        File(context.applicationContext.filesDir, "models/$FILES_DIR_YOLO_NAME")

    fun filesDirModelFile(context: Context): File =
        File(context.applicationContext.filesDir, "models/$FILES_DIR_SEG_NAME")

    fun yoloAssetPresent(context: Context): Boolean =
        assetExists(context, ASSET_YOLO_RELATIVE)

    fun yoloFilesDirPresent(context: Context): Boolean = filesDirYoloFile(context).isFile

    fun anyYoloModelPresent(context: Context): Boolean =
        yoloAssetPresent(context) || yoloFilesDirPresent(context)

    fun assetPresent(context: Context): Boolean =
        assetExists(context, ASSET_FFNET_RELATIVE) ||
            assetExists(context, ASSET_LEGACY_SEG_RELATIVE)

    fun filesDirPresent(context: Context): Boolean = filesDirModelFile(context).isFile

    fun anySegModelFilePresent(context: Context): Boolean =
        assetPresent(context) || filesDirPresent(context)

    /**
     * True if the configured default (or requested) backend has a model file.
     */
    fun anyModelFilePresent(
        context: Context,
        backend: VisionBackend = VisionConfig.DEFAULT_BACKEND,
    ): Boolean = when (backend) {
        VisionBackend.YOLO_DETECT -> anyYoloModelPresent(context)
        VisionBackend.FFNET_SEG -> anySegModelFilePresent(context)
    }

    fun describeExpectedLocations(
        backend: VisionBackend = VisionConfig.DEFAULT_BACKEND,
    ): String = when (backend) {
        VisionBackend.YOLO_DETECT ->
            "assets/$ASSET_YOLO_RELATIVE or filesDir/models/$FILES_DIR_YOLO_NAME"
        VisionBackend.FFNET_SEG ->
            "assets/$ASSET_FFNET_RELATIVE (or $ASSET_LEGACY_SEG_RELATIVE) " +
                "or filesDir/models/$FILES_DIR_SEG_NAME"
    }

    fun resolveYoloAssetRelative(context: Context): String? =
        if (assetExists(context, ASSET_YOLO_RELATIVE)) ASSET_YOLO_RELATIVE else null

    fun resolveAssetModelRelative(context: Context): String? = when {
        assetExists(context, ASSET_FFNET_RELATIVE) -> ASSET_FFNET_RELATIVE
        assetExists(context, ASSET_LEGACY_SEG_RELATIVE) -> ASSET_LEGACY_SEG_RELATIVE
        else -> null
    }

    private fun assetExists(context: Context, relative: String): Boolean =
        try {
            context.applicationContext.assets.open(relative).use { true }
        } catch (_: Exception) {
            false
        }
}
