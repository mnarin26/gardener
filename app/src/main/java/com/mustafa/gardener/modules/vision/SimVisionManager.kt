package com.mustafa.gardener.modules.vision

import com.mustafa.gardener.core.BaseSimDriver

class SimVisionManager : BaseSimDriver("VisionManager"), VisionManager {
    override fun modelLabel(): String = "none (SIM stub)"

    override fun detail(): String = "SIM — no model / no inference"
}
