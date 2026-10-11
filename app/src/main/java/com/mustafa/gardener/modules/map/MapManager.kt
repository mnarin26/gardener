package com.mustafa.gardener.modules.map

import com.mustafa.gardener.core.SensorDriver

/**
 * Garden map storage / mowable regions. No real map in skeleton.
 */
interface MapManager : SensorDriver {
    fun hasApprovedMap(): Boolean
}
