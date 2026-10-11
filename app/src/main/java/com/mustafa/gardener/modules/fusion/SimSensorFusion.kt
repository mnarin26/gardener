package com.mustafa.gardener.modules.fusion

import com.mustafa.gardener.core.BaseSimDriver

class SimSensorFusion : BaseSimDriver("SensorFusion"), SensorFusion {
    override fun isEstimateAvailable(): Boolean = false

    override fun detail(): String = "SIM — no fused estimate"
}
