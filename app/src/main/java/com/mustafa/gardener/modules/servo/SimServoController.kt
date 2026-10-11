package com.mustafa.gardener.modules.servo

import com.mustafa.gardener.core.BaseSimDriver

class SimServoController : BaseSimDriver("ServoController"), ServoController {
    override fun commandedAngleDegrees(): Float = 0f

    override fun detail(): String = "SIM — servo held at 0° (not real)"
}
