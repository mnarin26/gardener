package com.mustafa.gardener.modules.camera

import com.mustafa.gardener.core.SensorDriver

/**
 * Camera capture interface.
 * SIM path: [SimCameraManager]. REAL preview path: [RealCameraManager] (CameraX).
 */
interface CameraManager : SensorDriver {
    /** True only when a live preview/analysis stream is actually bound. */
    fun hasLiveFrames(): Boolean

    /** Human-readable selected/bound resolution, or a target/status label. */
    fun previewResolutionText(): String
}
