package com.mustafa.gardener.modules.esp32

import com.mustafa.gardener.core.SensorDriver

/**
 * USB bridge to ESP32 (motors, safety, sensor timestamps).
 * No serial/USB session in this skeleton.
 */
interface Esp32Communication : SensorDriver {
    fun isLinked(): Boolean
}
