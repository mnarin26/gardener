package com.mustafa.gardener.modules.lidar

import com.mustafa.gardener.core.SensorDriver

/**
 * LiDAR scan interface (e.g. future Slamtec C1). No device link in skeleton.
 */
interface LiDARManager : SensorDriver {
    fun lastScanPointCount(): Int
}
