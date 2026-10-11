package com.mustafa.gardener.modules.gnss

import com.mustafa.gardener.core.BaseSimDriver

class SimGNSSManager : BaseSimDriver("GNSSManager"), GNSSManager {
    override fun hasFix(): Boolean = false

    override fun detail(): String = "SIM — no GNSS fix"
}
