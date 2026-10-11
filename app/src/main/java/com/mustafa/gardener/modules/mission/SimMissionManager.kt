package com.mustafa.gardener.modules.mission

import com.mustafa.gardener.core.DriverMode
import com.mustafa.gardener.core.ModuleStatus

class SimMissionManager : MissionManager {
    override val moduleName: String = "MissionManager"
    override val mode: DriverMode = DriverMode.SIMULATION

    @Volatile
    private var running: Boolean = false

    @Volatile
    private var state: MissionState = MissionState.IDLE

    private val simCycle = listOf(
        MissionState.IDLE,
        MissionState.GO_TO_GARDEN,
        MissionState.ENTER_GARDEN,
        MissionState.MAP_GARDEN,
        MissionState.MOW,
        MissionState.RETURN_TO_GATE,
        MissionState.RETURN_TO_STATION,
        MissionState.CHARGE,
        MissionState.WAITING,
    )

    override fun currentState(): MissionState = state

    override fun start() {
        running = true
    }

    override fun stop() {
        running = false
        state = MissionState.IDLE
    }

    override fun isRunning(): Boolean = running

    override fun status(): ModuleStatus = ModuleStatus(
        moduleName = moduleName,
        mode = mode,
        running = running,
        detail = "SIM state=${state.name}",
    )

    override fun advanceSimStub() {
        if (!running) return
        val index = simCycle.indexOf(state).let { if (it < 0) 0 else it }
        state = simCycle[(index + 1) % simCycle.size]
    }
}
