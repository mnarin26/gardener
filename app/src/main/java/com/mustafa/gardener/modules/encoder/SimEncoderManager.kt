package com.mustafa.gardener.modules.encoder

import com.mustafa.gardener.core.BaseSimDriver

class SimEncoderManager : BaseSimDriver("EncoderManager"), EncoderManager {
    override fun tickCount(): Long = 0L

    override fun detail(): String = "SIM — encoder ticks = 0"
}
