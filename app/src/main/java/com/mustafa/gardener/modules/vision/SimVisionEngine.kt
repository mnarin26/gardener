package com.mustafa.gardener.modules.vision

/**
 * Simulation vision backend — labels only, no frames, no inference.
 */
class SimVisionEngine : VisionEngine {
    override fun state(): VisionEngineState = VisionEngineState.SIM

    override fun modelLabel(): String = "none (SIM stub)"

    override fun statusDetail(): String =
        "SIM — no model / no inference; Phase1 labels: ${VisionLabels.phase1Summary()}"

    override fun uiStatusTr(): String = "SIM — model yok (simülasyon)"

    override fun wantsFrameAnalysis(): Boolean = false

    override fun classLabels(): List<String> = VisionLabels.PHASE1
}
