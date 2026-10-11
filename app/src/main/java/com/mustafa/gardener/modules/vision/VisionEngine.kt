package com.mustafa.gardener.modules.vision

/**
 * Pluggable detection/segmentation backend behind [VisionManager].
 *
 * Report [VisionEngineState.READY] only after a successful model load.
 * SIM / unloaded / failed engines must not invent detections.
 */
interface VisionEngine {
    fun state(): VisionEngineState

    fun modelLabel(): String

    /** Short technical English detail for logs / module status. */
    fun statusDetail(): String

    /** Short Turkish UI line for the camera/vision screen. */
    fun uiStatusTr(): String

    /**
     * Whether CameraX ImageAnalysis should be bound.
     * Must stay false until a real model is loaded — preview-only must not burn battery.
     */
    fun wantsFrameAnalysis(): Boolean

    fun classLabels(): List<String>

    /**
     * Optional frame hook. Default engines no-op; never run heavy ML here without a model.
     */
    fun onFrame(frame: VisionFrame) {
        // no-op
    }

    /** Latest segmentation overlay bitmap, if this engine produces one. */
    fun latestOverlay(): SegmentationOverlay? = null

    /** Latest detection boxes, if this engine produces them. */
    fun latestDetections(): DetectionOverlay? = null
}
