package com.mustafa.gardener.modules.localization

import com.mustafa.gardener.core.BaseSimDriver

class SimLocalization : BaseSimDriver("Localization"), Localization {
    override fun hasPose(): Boolean = false

    override fun detail(): String = "SIM — no pose"
}
