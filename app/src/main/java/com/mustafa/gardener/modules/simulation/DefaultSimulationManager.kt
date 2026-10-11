package com.mustafa.gardener.modules.simulation

import com.mustafa.gardener.core.DriverMode
import com.mustafa.gardener.core.ModuleStatus
import com.mustafa.gardener.core.SensorDriver

class DefaultSimulationManager : SimulationManager {
    override val moduleName: String = "SimulationManager"
    override val mode: DriverMode = DriverMode.SIMULATION

    @Volatile
    private var active: Boolean = false

    override fun isSimulationActive(): Boolean = active

    override fun startAll(drivers: List<SensorDriver>) {
        active = true
        drivers.forEach { it.start() }
    }

    override fun stopAll(drivers: List<SensorDriver>) {
        drivers.forEach { it.stop() }
        active = false
    }

    override fun status(): ModuleStatus = ModuleStatus(
        moduleName = moduleName,
        mode = mode,
        running = active,
        detail = if (active) "SIMULATION mode ACTIVE" else "SIMULATION mode idle",
    )
}
