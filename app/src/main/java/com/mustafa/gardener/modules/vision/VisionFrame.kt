package com.mustafa.gardener.modules.vision

/**
 * Lightweight analysis-frame metadata passed from CameraX ImageAnalysis toward vision.
 *
 * Intentionally carries no pixel buffer yet — avoids allocation / copy while no model runs.
 * A future TFLite/LiteRT engine can extend this or accept ImageProxy directly.
 */
data class VisionFrame(
    val width: Int,
    val height: Int,
    val timestampNs: Long,
    val rotationDegrees: Int = 0,
)
