package com.mustafa.gardener.modules.localization

import com.mustafa.gardener.core.SensorDriver

/**
 * Pose estimate in map frame. Stub.
 */
interface Localization : SensorDriver {
    fun hasPose(): Boolean
}
