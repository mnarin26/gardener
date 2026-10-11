package com.mustafa.gardener.modules.vision

import com.mustafa.gardener.core.DriverMode
import com.mustafa.gardener.core.ModuleStatus
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Real-path [VisionManager] that delegates to a swappable [VisionEngine].
 *
 * Default engine is [UnloadedVisionEngine] — model not present, no inference.
 */
class DefaultVisionManager(
    initialEngine: VisionEngine = UnloadedVisionEngine(),
) : VisionManager {
    override val moduleName: String = "VisionManager"
    override val mode: DriverMode = DriverMode.REAL

    private val running = AtomicBoolean(false)
    private val engineRef = AtomicReference(initialEngine)

    override fun engine(): VisionEngine = engineRef.get()

    /** Swap backend later (e.g. after dropping an exported model). */
    fun setEngine(engine: VisionEngine) {
        engineRef.set(engine)
    }

    override fun modelLabel(): String = engine().modelLabel()

    override fun engineState(): VisionEngineState = engine().state()

    override fun uiStatusTr(): String = engine().uiStatusTr()

    override fun wantsFrameAnalysis(): Boolean = engine().wantsFrameAnalysis()

    override fun classLabels(): List<String> = engine().classLabels()

    override fun onAnalysisFrame(frame: VisionFrame) {
        engine().onFrame(frame)
    }

    override fun start() {
        running.set(true)
    }

    override fun stop() {
        running.set(false)
    }

    override fun isRunning(): Boolean = running.get()

    override fun status(): ModuleStatus = ModuleStatus(
        moduleName = moduleName,
        mode = mode,
        running = isRunning(),
        detail = "REAL — ${engine().statusDetail()}",
    )
}
