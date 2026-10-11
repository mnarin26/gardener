package com.mustafa.gardener.modules.path

import com.mustafa.gardener.core.SensorDriver

/**
 * Mowing / navigation path planning. Stub.
 */
interface PathPlanner : SensorDriver {
    fun hasActivePath(): Boolean
}
