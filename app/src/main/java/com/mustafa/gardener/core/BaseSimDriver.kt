package com.mustafa.gardener.core

/**
 * Common simulation driver. Labels itself as SIMULATION; produces no real sensor data.
 */
abstract class BaseSimDriver(
    final override val moduleName: String,
) : SensorDriver {
    final override val mode: DriverMode = DriverMode.SIMULATION

    @Volatile
    private var running: Boolean = false

    override fun start() {
        running = true
    }

    override fun stop() {
        running = false
    }

    override fun isRunning(): Boolean = running

    protected open fun detail(): String = "SIM stub — no hardware"

    override fun status(): ModuleStatus = ModuleStatus(
        moduleName = moduleName,
        mode = mode,
        running = isRunning(),
        detail = detail(),
    )
}
