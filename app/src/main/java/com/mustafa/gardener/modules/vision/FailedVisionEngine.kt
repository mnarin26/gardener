package com.mustafa.gardener.modules.vision

/**
 * Real-path engine after a model load / Interpreter failure.
 * Keeps SIM / unloaded paths distinct; no fake detections.
 */
class FailedVisionEngine(
    private val reason: String,
) : VisionEngine {
    override fun state(): VisionEngineState = VisionEngineState.FAILED

    override fun modelLabel(): String = "load failed"

    override fun statusDetail(): String =
        "model load failed — $reason; not running inference"

    override fun uiStatusTr(): String = "Model yüklenemedi — sadece önizleme"

    override fun wantsFrameAnalysis(): Boolean = false

    override fun classLabels(): List<String> =
        if (VisionConfig.DEFAULT_BACKEND == VisionBackend.YOLO_DETECT) {
            CocoLabels.ALL
        } else {
            VisionLabels.PHASE1
        }

    override fun onFrame(frame: VisionFrame) {
        // no-op
    }
}
