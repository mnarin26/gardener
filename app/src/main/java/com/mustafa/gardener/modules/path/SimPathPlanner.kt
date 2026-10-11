package com.mustafa.gardener.modules.path

import com.mustafa.gardener.core.BaseSimDriver

class SimPathPlanner : BaseSimDriver("PathPlanner"), PathPlanner {
    override fun hasActivePath(): Boolean = false

    override fun detail(): String = "SIM — no path"
}
