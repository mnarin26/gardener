package com.mustafa.gardener.modules.gnss

import com.mustafa.gardener.core.SensorDriver

/**
 * GNSS / phone location. Real provider not wired.
 */
interface GNSSManager : SensorDriver {
    fun hasFix(): Boolean
}
