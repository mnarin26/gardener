package com.mustafa.gardener.modules.fusion

import com.mustafa.gardener.core.SensorDriver

/**
 * Fuses camera / LiDAR / IMU / odometry into a common frame. Stub.
 */
interface SensorFusion : SensorDriver {
    fun isEstimateAvailable(): Boolean
}
