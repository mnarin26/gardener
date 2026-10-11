package com.mustafa.gardener.modules.vision

import com.mustafa.gardener.core.BaseSimDriver

class SimVisionManager(
    private val visionEngine: VisionEngine = SimVisionEngine(),
) : BaseSimDriver("VisionManager"), VisionManager {

    override fun engine(): VisionEngine = visionEngine

    override fun modelLabel(): String = visionEngine.modelLabel()

    override fun engineState(): VisionEngineState = visionEngine.state()

    override fun uiStatusTr(): String = visionEngine.uiStatusTr()

    override fun wantsFrameAnalysis(): Boolean = visionEngine.wantsFrameAnalysis()

    override fun classLabels(): List<String> = visionEngine.classLabels()

    override fun onAnalysisFrame(frame: VisionFrame) {
        visionEngine.onFrame(frame)
    }

    override fun detail(): String = visionEngine.statusDetail()
}
