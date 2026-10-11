package com.mustafa.gardener.modules.vision

/**
 * One detection in normalized upright-image coordinates (0..1).
 * "Upright" = after applying CameraX [rotationDegrees] to the analysis buffer.
 */
data class DetectionBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val label: String,
    val classIndex: Int,
    val score: Float,
)

/**
 * Latest detections for Compose overlay. Boxes are relative to the upright
 * analysis image ([imageWidth]×[imageHeight]); UI maps them through
 * PreviewView FILL_CENTER (scale-to-fill + center crop).
 */
data class DetectionOverlay(
    val boxes: List<DetectionBox>,
    val imageWidth: Int,
    val imageHeight: Int,
    val timestampNs: Long,
    val inferenceMs: Long,
)
