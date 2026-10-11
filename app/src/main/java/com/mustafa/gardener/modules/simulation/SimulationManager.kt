package com.mustafa.gardener.modules.simulation

import com.mustafa.gardener.core.DriverMode
import com.mustafa.gardener.core.ModuleStatus
import com.mustafa.gardener.core.SensorDriver

/**
 * Owns simulation mode flag and can start/stop sim drivers together.
 */
interface SimulationManager {
    val moduleName: String
    val mode: DriverMode

    fun isSimulationActive(): Boolean
    fun startAll(drivers: List<SensorDriver>)
    fun stopAll(drivers: List<SensorDriver>)
    fun status(): ModuleStatus
}
