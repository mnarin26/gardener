package com.mustafa.gardener.modules.lidar

import com.mustafa.gardener.core.BaseSimDriver

class SimLiDARManager : BaseSimDriver("LiDARManager"), LiDARManager {
    override fun lastScanPointCount(): Int = 0

    override fun detail(): String = "SIM — no LiDAR scan"
}
