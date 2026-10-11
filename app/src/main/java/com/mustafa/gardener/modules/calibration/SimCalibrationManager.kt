package com.mustafa.gardener.modules.calibration

import com.mustafa.gardener.core.BaseSimDriver

class SimCalibrationManager : BaseSimDriver("CalibrationManager"), CalibrationManager {
    override fun isCalibrated(): Boolean = false

    override fun detail(): String = "SIM — not calibrated"
}
