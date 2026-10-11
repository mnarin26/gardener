package com.mustafa.gardener

import com.mustafa.gardener.modules.vision.CityscapesLabels
import com.mustafa.gardener.modules.vision.CocoLabels
import com.mustafa.gardener.modules.vision.DefaultVisionManager
import com.mustafa.gardener.modules.vision.FailedVisionEngine
import com.mustafa.gardener.modules.vision.FrameOrientation
import com.mustafa.gardener.modules.vision.SimVisionEngine
import com.mustafa.gardener.modules.vision.SimVisionManager
import com.mustafa.gardener.modules.vision.TfliteYoloDetectVisionEngine
import com.mustafa.gardener.modules.vision.UnloadedVisionEngine
import com.mustafa.gardener.modules.vision.VisionBackend
import com.mustafa.gardener.modules.vision.VisionConfig
import com.mustafa.gardener.modules.vision.VisionEngineState
import com.mustafa.gardener.modules.vision.VisionFrame
import com.mustafa.gardener.modules.vision.VisionLabels
import com.mustafa.gardener.runtime.RobotRuntime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class VisionEngineTest {

    @Test
    fun unloadedEngine_reportsModelMissing_noAnalysis() {
        val engine = UnloadedVisionEngine()
        assertEquals(VisionEngineState.UNLOADED, engine.state())
        assertEquals("none (unloaded)", engine.modelLabel())
        assertTrue(engine.statusDetail().contains("model file not present — not running inference"))
        assertEquals("Model yok — sadece önizleme", engine.uiStatusTr())
        assertFalse(engine.wantsFrameAnalysis())
        assertEquals(VisionLabels.PHASE1, engine.classLabels())
        assertNull(engine.latestOverlay())
        engine.onFrame(VisionFrame(width = 640, height = 480, timestampNs = 1L))
    }

    @Test
    fun notConfiguredEngine_distinctState_sameUi() {
        val engine = UnloadedVisionEngine.notConfigured()
        assertEquals(VisionEngineState.NOT_CONFIGURED, engine.state())
        assertEquals("Model yok — sadece önizleme", engine.uiStatusTr())
        assertFalse(engine.wantsFrameAnalysis())
        assertTrue(engine.statusDetail().contains("not running inference"))
    }

    @Test
    fun failedEngine_turkishFailedStatus() {
        val engine = FailedVisionEngine("unit-test")
        assertEquals(VisionEngineState.FAILED, engine.state())
        assertEquals("Model yüklenemedi — sadece önizleme", engine.uiStatusTr())
        assertFalse(engine.wantsFrameAnalysis())
        assertNull(engine.latestOverlay())
    }

    @Test
    fun simEngine_turkishSimStatus() {
        val engine = SimVisionEngine()
        assertEquals(VisionEngineState.SIM, engine.state())
        assertEquals("none (SIM stub)", engine.modelLabel())
        assertEquals("SIM — model yok (simülasyon)", engine.uiStatusTr())
        assertFalse(engine.wantsFrameAnalysis())
    }

    @Test
    fun defaultVisionManager_delegatesToUnloadedEngine() {
        val manager = DefaultVisionManager(UnloadedVisionEngine())
        manager.start()
        assertEquals(VisionEngineState.UNLOADED, manager.engineState())
        assertEquals("Model yok — sadece önizleme", manager.uiStatusTr())
        assertFalse(manager.wantsFrameAnalysis())
        assertTrue(manager.status().detail.contains("model file not present"))
        manager.stop()
        assertFalse(manager.isRunning())
    }

    @Test
    fun phase1Labels_excludeGrate() {
        assertEquals(listOf("wall", "sidewalk", "road", "fence"), VisionLabels.PHASE1)
        assertFalse(VisionLabels.PHASE1.contains(VisionLabels.PHASE2_GRATE))
    }

    @Test
    fun cityscapesLabels_includePhase1Targets() {
        assertTrue(CityscapesLabels.ALL.containsAll(listOf("road", "sidewalk", "wall", "fence")))
        assertEquals(19, CityscapesLabels.ALL.size)
        assertEquals(4, CityscapesLabels.HIGHLIGHT_INDICES.size)
    }

    @Test
    fun robotRuntime_simVisionStillWires() {
        val runtime = RobotRuntime.createSimulation()
        assertTrue(runtime.visionManager is SimVisionManager)
        assertEquals("none (SIM stub)", runtime.visionManager.modelLabel())
        assertEquals(VisionEngineState.SIM, runtime.visionManager.engineState())
        assertEquals("SIM — model yok (simülasyon)", runtime.visionManager.uiStatusTr())
        runtime.stopSimulation()
    }

    @Test
    fun defaultBackend_isYoloDetect() {
        assertEquals(VisionBackend.YOLO_DETECT, VisionConfig.DEFAULT_BACKEND)
        assertEquals(80, CocoLabels.ALL.size)
        assertTrue(CocoLabels.ALL.containsAll(listOf("person", "car", "dog")))
    }

    @Test
    fun frameOrientation_rotate90_swapsDimensions() {
        val src = intArrayOf(
            1, 2, 3,
            4, 5, 6,
        )
        val out = FrameOrientation.rotateArgb(src, width = 3, height = 2, rotationDegrees = 90)
        assertEquals(2, out.width)
        assertEquals(3, out.height)
        // (0,0)=1 -> (1,0); (2,0)=3 -> (1,2); (0,1)=4 -> (0,0)
        assertEquals(4, out.pixels[0])
        assertEquals(1, out.pixels[1])
    }

    /**
     * Repro for S24 empty-UI bug: yolov8n_float32.tflite emits normalized 0–1
     * boxes; letterbox padY≈80 without scaling → all boxes dropped.
     */
    @Test
    fun decodeAndNms_normalizedCoords_yieldsBoxesAfterLetterbox() {
        val inputSize = 640
        val numAnchors = 4
        val numClasses = 80
        val chairIdx = CocoLabels.ALL.indexOf("chair")
        assertTrue(chairIdx >= 0)

        // 640×480 letterboxed into 640×640 → scale=1, padX=0, padY=80
        val meta = FrameOrientation.LetterboxMeta(
            scale = 1f,
            padX = 0f,
            padY = 80f,
            srcW = 640,
            srcH = 480,
        )

        val raw = FloatArray((4 + numClasses) * numAnchors)
        val a = 0
        // Normalized cx,cy,w,h ( Ultralytics TFLite style )
        raw[0 * numAnchors + a] = 0.5f
        raw[1 * numAnchors + a] = 0.5f
        raw[2 * numAnchors + a] = 0.25f
        raw[3 * numAnchors + a] = 0.4f
        raw[(4 + chairIdx) * numAnchors + a] = 0.80f

        val boxes = TfliteYoloDetectVisionEngine.decodeAndNms(
            raw = raw,
            meta = meta,
            inputSize = inputSize,
            numAnchors = numAnchors,
            numClasses = numClasses,
        )

        assertEquals(1, boxes.size)
        val box = boxes[0]
        assertEquals(chairIdx, box.classIndex)
        assertTrue(box.label.contains("chair"))
        assertEquals(0.80f, box.score, 1e-4f)
        // Expected after ×640 then unpad: left=0.375, top≈0.233, right=0.625, bottom≈0.767
        assertEquals(0.375f, box.left, 1e-3f)
        assertEquals(112f / 480f, box.top, 1e-3f)
        assertEquals(0.625f, box.right, 1e-3f)
        assertEquals(368f / 480f, box.bottom, 1e-3f)
        assertTrue(box.right > box.left)
        assertTrue(box.bottom > box.top)
    }

    @Test
    fun decodeAndNms_pixelCoords_stillWorks() {
        val inputSize = 640
        val numAnchors = 2
        val numClasses = 80
        val personIdx = CocoLabels.ALL.indexOf("person")

        val meta = FrameOrientation.LetterboxMeta(
            scale = 1f,
            padX = 0f,
            padY = 80f,
            srcW = 640,
            srcH = 480,
        )

        val raw = FloatArray((4 + numClasses) * numAnchors)
        // Pixel-space box in 640 input (max ≫ 1.5 → no normalize scale)
        raw[0] = 320f
        raw[1 * numAnchors] = 320f
        raw[2 * numAnchors] = 160f
        raw[3 * numAnchors] = 256f
        raw[(4 + personIdx) * numAnchors] = 0.9f

        val boxes = TfliteYoloDetectVisionEngine.decodeAndNms(
            raw = raw,
            meta = meta,
            inputSize = inputSize,
            numAnchors = numAnchors,
            numClasses = numClasses,
        )

        assertEquals(1, boxes.size)
        assertEquals(personIdx, boxes[0].classIndex)
        assertEquals(0.375f, boxes[0].left, 1e-3f)
        assertTrue(abs(boxes[0].right - boxes[0].left) > 0.1f)
    }
}
