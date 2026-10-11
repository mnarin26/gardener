package com.mustafa.gardener.modules.imu

import com.mustafa.gardener.core.BaseSimDriver

class SimIMUManager : BaseSimDriver("IMUManager"), IMUManager {
    override fun hasSample(): Boolean = false

    override fun detail(): String = "SIM — no IMU samples"
}
