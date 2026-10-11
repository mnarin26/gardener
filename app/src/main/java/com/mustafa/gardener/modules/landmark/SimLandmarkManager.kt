package com.mustafa.gardener.modules.landmark

import com.mustafa.gardener.core.BaseSimDriver

class SimLandmarkManager : BaseSimDriver("LandmarkManager"), LandmarkManager {
    override fun knownLandmarkCount(): Int = 0

    override fun detail(): String = "SIM — no landmarks"
}
