package com.mustafa.gardener.modules.vision

import com.mustafa.gardener.core.SensorDriver

/**
 * Pluggable vision (detection / segmentation). No ML model loaded in skeleton.
 */
interface VisionManager : SensorDriver {
    fun modelLabel(): String
}
