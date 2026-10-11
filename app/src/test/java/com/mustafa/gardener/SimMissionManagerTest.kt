package com.mustafa.gardener

import com.mustafa.gardener.core.DriverMode
import com.mustafa.gardener.modules.mission.MissionState
import com.mustafa.gardener.modules.mission.SimMissionManager
import com.mustafa.gardener.runtime.RobotRuntime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimMissionManagerTest {
    @Test
    fun advanceSimStub_cyclesFromIdle() {
        val mission = SimMissionManager()
        mission.start()
        assertEquals(MissionState.IDLE, mission.currentState())
        mission.advanceSimStub()
        assertEquals(MissionState.GO_TO_GARDEN, mission.currentState())
        assertEquals(DriverMode.SIMULATION, mission.mode)
    }

    @Test
    fun robotRuntime_wiresSimulationModules() {
        val runtime = RobotRuntime.createSimulation()
        assertTrue(runtime.simulationManager.isSimulationActive())
        assertFalse(runtime.esp32Communication.isLinked())
        assertEquals("none (SIM stub)", runtime.visionManager.modelLabel())
        val names = runtime.moduleStatuses().map { it.moduleName }.toSet()
        assertTrue(names.contains("Esp32Communication"))
        assertTrue(names.contains("MissionManager"))
        assertTrue(names.contains("SimulationManager"))
        assertEquals(18, names.size)
        runtime.stopSimulation()
        assertFalse(runtime.simulationManager.isSimulationActive())
    }
}
