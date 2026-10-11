package com.mustafa.gardener.modules.camera

import com.mustafa.gardener.core.SensorDriver

/**
 * Camera capture interface. Real CameraX / hardware path not implemented.
 */
interface CameraManager : SensorDriver {
    /** Preview/frame pipeline not available in skeleton. */
    fun hasLiveFrames(): Boolean
}
