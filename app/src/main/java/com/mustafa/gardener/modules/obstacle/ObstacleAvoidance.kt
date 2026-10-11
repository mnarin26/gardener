package com.mustafa.gardener.modules.obstacle

import com.mustafa.gardener.core.SensorDriver

/**
 * Near-field obstacle reaction. Stub — not a safety claim.
 */
interface ObstacleAvoidance : SensorDriver {
    fun isClear(): Boolean
}
