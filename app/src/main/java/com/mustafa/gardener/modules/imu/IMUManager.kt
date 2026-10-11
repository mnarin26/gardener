package com.mustafa.gardener.modules.imu

import com.mustafa.gardener.core.SensorDriver

/**
 * IMU readings. Real Android sensor path not wired.
 */
interface IMUManager : SensorDriver {
    fun hasSample(): Boolean
}
