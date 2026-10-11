package com.mustafa.gardener.modules.vision

/**
 * Analysis frame from CameraX ImageAnalysis toward vision.
 *
 * When [argbPixels] is non-null it holds row-major ARGB_8888 samples of size
 * [width] * [height] (already rotation-normalized by the camera path when possible).
 * Metadata-only frames (null pixels) are ignored by the TFLite engine.
 */
data class VisionFrame(
    val width: Int,
    val height: Int,
    val timestampNs: Long,
    val rotationDegrees: Int = 0,
    val argbPixels: IntArray? = null,
)
