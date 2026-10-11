package com.mustafa.gardener.modules.vision

import com.mustafa.gardener.core.SensorDriver

/**
 * Pluggable vision (detection / segmentation) facade.
 *
 * Backed by a [VisionEngine]. Skeleton: SIM stub or unloaded real engine —
 * no on-device inference claimed until a model file is exported and a runtime is wired.
 */
interface VisionManager : SensorDriver {
    fun engine(): VisionEngine

    fun modelLabel(): String

    fun engineState(): VisionEngineState

    /** Turkish status for camera/vision UI. */
    fun uiStatusTr(): String

    /**
     * Whether the camera should bind ImageAnalysis toward this manager.
     * False while unloaded / SIM — preview-only must stay cheap.
     */
    fun wantsFrameAnalysis(): Boolean

    fun classLabels(): List<String>

    /** Optional CameraX analysis hook; engines no-op until READY. */
    fun onAnalysisFrame(frame: VisionFrame)

    /** Latest segmentation overlay from the active engine, if any. */
    fun latestOverlay(): SegmentationOverlay? = engine().latestOverlay()

    /** Latest detection boxes from the active engine, if any. */
    fun latestDetections(): DetectionOverlay? = engine().latestDetections()
}
