package com.mustafa.gardener.modules.objectmgr

import com.mustafa.gardener.core.SensorDriver

/**
 * Tracks detected objects / garden classes. Stub only.
 */
interface ObjectManager : SensorDriver {
    fun trackedObjectCount(): Int
}
