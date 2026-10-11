package com.mustafa.gardener.modules.esp32

import com.mustafa.gardener.core.BaseSimDriver

class SimEsp32Communication : BaseSimDriver("Esp32Communication"), Esp32Communication {
    override fun isLinked(): Boolean = false

    override fun detail(): String = "SIM — ESP32 not connected"
}
