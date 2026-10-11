package com.mustafa.gardener.modules.calibration

import com.mustafa.gardener.core.SensorDriver

/**
 * Service / auto calibration orchestration. Stub.
 */
interface CalibrationManager : SensorDriver {
    fun isCalibrated(): Boolean
}
