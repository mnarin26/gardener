package com.mustafa.gardener.modules.encoder

import com.mustafa.gardener.core.SensorDriver

/**
 * Wheel encoder / odometry ticks. Expects ESP32 bridge later.
 */
interface EncoderManager : SensorDriver {
    fun tickCount(): Long
}
