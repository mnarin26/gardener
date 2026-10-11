package com.mustafa.gardener.modules.vision

/**
 * Selects the REAL-path vision backend.
 *
 * Default for this smoke trial: YOLOv8n COCO detection (clear boxes).
 * FFNet Cityscapes segmentation remains available but is not the default —
 * its indoor overlay was misaligned / hard to verify on S24 Ultra.
 */
enum class VisionBackend {
    YOLO_DETECT,
    FFNET_SEG,
}

object VisionConfig {
    /** Flip to [VisionBackend.FFNET_SEG] only when intentionally re-testing seg. */
    val DEFAULT_BACKEND: VisionBackend = VisionBackend.YOLO_DETECT
}
