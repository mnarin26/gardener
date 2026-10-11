package com.mustafa.gardener.runtime

import com.mustafa.gardener.core.ModuleStatus
import com.mustafa.gardener.core.SensorDriver
import com.mustafa.gardener.modules.calibration.CalibrationManager
import com.mustafa.gardener.modules.calibration.SimCalibrationManager
import com.mustafa.gardener.modules.camera.CameraManager
import com.mustafa.gardener.modules.camera.SimCameraManager
import com.mustafa.gardener.modules.encoder.EncoderManager
import com.mustafa.gardener.modules.encoder.SimEncoderManager
import com.mustafa.gardener.modules.esp32.Esp32Communication
import com.mustafa.gardener.modules.esp32.SimEsp32Communication
import com.mustafa.gardener.modules.fusion.SensorFusion
import com.mustafa.gardener.modules.fusion.SimSensorFusion
import com.mustafa.gardener.modules.gnss.GNSSManager
import com.mustafa.gardener.modules.gnss.SimGNSSManager
import com.mustafa.gardener.modules.imu.IMUManager
import com.mustafa.gardener.modules.imu.SimIMUManager
import com.mustafa.gardener.modules.landmark.LandmarkManager
import com.mustafa.gardener.modules.landmark.SimLandmarkManager
import com.mustafa.gardener.modules.lidar.LiDARManager
import com.mustafa.gardener.modules.lidar.SimLiDARManager
import com.mustafa.gardener.modules.localization.Localization
import com.mustafa.gardener.modules.localization.SimLocalization
import com.mustafa.gardener.modules.map.MapManager
import com.mustafa.gardener.modules.map.SimMapManager
import com.mustafa.gardener.modules.mission.MissionManager
import com.mustafa.gardener.modules.mission.SimMissionManager
import com.mustafa.gardener.modules.objectmgr.ObjectManager
import com.mustafa.gardener.modules.objectmgr.SimObjectManager
import com.mustafa.gardener.modules.obstacle.ObstacleAvoidance
import com.mustafa.gardener.modules.obstacle.SimObstacleAvoidance
import com.mustafa.gardener.modules.path.PathPlanner
import com.mustafa.gardener.modules.path.SimPathPlanner
import com.mustafa.gardener.modules.servo.ServoController
import com.mustafa.gardener.modules.servo.SimServoController
import com.mustafa.gardener.modules.simulation.DefaultSimulationManager
import com.mustafa.gardener.modules.simulation.SimulationManager
import com.mustafa.gardener.modules.vision.SimVisionManager
import com.mustafa.gardener.modules.vision.VisionManager

/**
 * Wires all robot modules with SIMULATION implementations.
 * This is an architecture skeleton — not a working mower.
 */
class RobotRuntime private constructor(
    val simulationManager: SimulationManager,
    val cameraManager: CameraManager,
    val visionManager: VisionManager,
    val objectManager: ObjectManager,
    val lidarManager: LiDARManager,
    val servoController: ServoController,
    val gnssManager: GNSSManager,
    val imuManager: IMUManager,
    val encoderManager: EncoderManager,
    val landmarkManager: LandmarkManager,
    val sensorFusion: SensorFusion,
    val localization: Localization,
    val mapManager: MapManager,
    val pathPlanner: PathPlanner,
    val obstacleAvoidance: ObstacleAvoidance,
    val missionManager: MissionManager,
    val esp32Communication: Esp32Communication,
    val calibrationManager: CalibrationManager,
) {
    private val sensorDrivers: List<SensorDriver> = listOf(
        cameraManager,
        visionManager,
        objectManager,
        lidarManager,
        servoController,
        gnssManager,
        imuManager,
        encoderManager,
        landmarkManager,
        sensorFusion,
        localization,
        mapManager,
        pathPlanner,
        obstacleAvoidance,
        esp32Communication,
        calibrationManager,
    )

    fun startSimulation() {
        simulationManager.startAll(sensorDrivers)
        missionManager.start()
    }

    fun stopSimulation() {
        missionManager.stop()
        simulationManager.stopAll(sensorDrivers)
    }

    fun moduleStatuses(): List<ModuleStatus> {
        val mission = missionManager.status()
        val sim = simulationManager.status()
        return listOf(sim, mission) + sensorDrivers.map { it.status() }
    }

    companion object {
        fun createSimulation(): RobotRuntime {
            val runtime = RobotRuntime(
                simulationManager = DefaultSimulationManager(),
                cameraManager = SimCameraManager(),
                visionManager = SimVisionManager(),
                objectManager = SimObjectManager(),
                lidarManager = SimLiDARManager(),
                servoController = SimServoController(),
                gnssManager = SimGNSSManager(),
                imuManager = SimIMUManager(),
                encoderManager = SimEncoderManager(),
                landmarkManager = SimLandmarkManager(),
                sensorFusion = SimSensorFusion(),
                localization = SimLocalization(),
                mapManager = SimMapManager(),
                pathPlanner = SimPathPlanner(),
                obstacleAvoidance = SimObstacleAvoidance(),
                missionManager = SimMissionManager(),
                esp32Communication = SimEsp32Communication(),
                calibrationManager = SimCalibrationManager(),
            )
            runtime.startSimulation()
            return runtime
        }
    }
}
