package com.mustafa.gardener.modules.map

import com.mustafa.gardener.core.BaseSimDriver

class SimMapManager : BaseSimDriver("MapManager"), MapManager {
    override fun hasApprovedMap(): Boolean = false

    override fun detail(): String = "SIM — no map / no user approval"
}
