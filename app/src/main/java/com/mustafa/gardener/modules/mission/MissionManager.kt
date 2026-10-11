package com.mustafa.gardener.modules.mission

import com.mustafa.gardener.core.DriverMode
import com.mustafa.gardener.core.ModuleStatus

/**
 * High-level mission state machine.
 * Skeleton transitions are SIM stub events only.
 */
interface MissionManager {
    val moduleName: String
    val mode: DriverMode

    fun currentState(): MissionState
    fun start()
    fun stop()
    fun isRunning(): Boolean
    fun status(): ModuleStatus

    /** SIM-only smoke transition. Must not be treated as real mission progress. */
    fun advanceSimStub()
}
