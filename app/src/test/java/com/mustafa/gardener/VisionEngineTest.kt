package com.mustafa.gardener

import com.mustafa.gardener.modules.vision.DefaultVisionManager
import com.mustafa.gardener.modules.vision.SimVisionEngine
import com.mustafa.gardener.modules.vision.SimVisionManager
import com.mustafa.gardener.modules.vision.UnloadedVisionEngine
import com.mustafa.gardener.modules.vision.VisionEngineState
import com.mustafa.gardener.modules.vision.VisionFrame
import com.mustafa.gardener.modules.vision.VisionLabels
import com.mustafa.gardener.runtime.RobotRuntime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisionEngineTest {

    @Test
    fun unloadedEngine_reportsModelMissing_noAnalysis() {
        val engine = UnloadedVisionEngine()
        assertEquals(VisionEngineState.UNLOADED, engine.state())
        assertEquals("none (unloaded)", engine.modelLabel())
        assertTrue(engine.statusDetail().contains("model file not present — not running inference"))
        assertEquals("Model yok — sadece önizleme", engine.uiStatusTr())
        assertFalse(engine.wantsFrameAnalysis())
        assertEquals(VisionLabels.PHASE1, engine.classLabels())
        // Must not throw / invent detections.
        engine.onFrame(VisionFrame(width = 640, height = 480, timestampNs = 1L))
    }

    @Test
    fun notConfiguredEngine_distinctState_sameUi() {
        val engine = UnloadedVisionEngine.notConfigured()
        assertEquals(VisionEngineState.NOT_CONFIGURED, engine.state())
        assertEquals("Model yok — sadece önizleme", engine.uiStatusTr())
        assertFalse(engine.wantsFrameAnalysis())
        assertTrue(engine.statusDetail().contains("not running inference"))
    }

    @Test
    fun simEngine_turkishSimStatus() {
        val engine = SimVisionEngine()
        assertEquals(VisionEngineState.SIM, engine.state())
        assertEquals("none (SIM stub)", engine.modelLabel())
        assertEquals("SIM — model yok (simülasyon)", engine.uiStatusTr())
        assertFalse(engine.wantsFrameAnalysis())
    }

    @Test
    fun defaultVisionManager_delegatesToUnloadedEngine() {
        val manager = DefaultVisionManager(UnloadedVisionEngine())
        manager.start()
        assertEquals(VisionEngineState.UNLOADED, manager.engineState())
        assertEquals("Model yok — sadece önizleme", manager.uiStatusTr())
        assertFalse(manager.wantsFrameAnalysis())
        assertTrue(manager.status().detail.contains("model file not present"))
        manager.stop()
        assertFalse(manager.isRunning())
    }

    @Test
    fun phase1Labels_excludeGrate() {
        assertEquals(listOf("wall", "sidewalk", "road", "fence"), VisionLabels.PHASE1)
        assertFalse(VisionLabels.PHASE1.contains(VisionLabels.PHASE2_GRATE))
    }

    @Test
    fun robotRuntime_simVisionStillWires() {
        val runtime = RobotRuntime.createSimulation()
        assertTrue(runtime.visionManager is SimVisionManager)
        assertEquals("none (SIM stub)", runtime.visionManager.modelLabel())
        assertEquals(VisionEngineState.SIM, runtime.visionManager.engineState())
        assertEquals("SIM — model yok (simülasyon)", runtime.visionManager.uiStatusTr())
        runtime.stopSimulation()
    }
}
