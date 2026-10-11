package com.mustafa.gardener.core

/**
 * Shared lifecycle API for sensor-like modules.
 * Simulation and (future) real drivers implement the same contract.
 */
interface SensorDriver {
    val moduleName: String
    val mode: DriverMode

    fun start()
    fun stop()
    fun isRunning(): Boolean
    fun status(): ModuleStatus
}
