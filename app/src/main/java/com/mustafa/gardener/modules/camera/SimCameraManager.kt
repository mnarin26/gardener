package com.mustafa.gardener.modules.camera

import com.mustafa.gardener.core.BaseSimDriver

class SimCameraManager : BaseSimDriver("CameraManager"), CameraManager {
    override fun hasLiveFrames(): Boolean = false

    override fun detail(): String = "SIM — no camera frames"
}
