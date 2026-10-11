package com.mustafa.gardener.modules.landmark

import com.mustafa.gardener.core.SensorDriver

/**
 * Landmark / station target tracking. Stub.
 */
interface LandmarkManager : SensorDriver {
    fun knownLandmarkCount(): Int
}
