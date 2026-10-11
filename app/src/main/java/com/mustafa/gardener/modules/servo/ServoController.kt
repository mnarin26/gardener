package com.mustafa.gardener.modules.servo

import com.mustafa.gardener.core.SensorDriver

/**
 * LiDAR tilt / other servo control. No actuator in skeleton.
 */
interface ServoController : SensorDriver {
    fun commandedAngleDegrees(): Float
}
