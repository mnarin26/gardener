package com.mustafa.gardener.modules.obstacle

import com.mustafa.gardener.core.BaseSimDriver

class SimObstacleAvoidance : BaseSimDriver("ObstacleAvoidance"), ObstacleAvoidance {
    override fun isClear(): Boolean = true // SIM assumption only

    override fun detail(): String = "SIM — reports clear (not real sensing)"
}
