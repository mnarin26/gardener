package com.mustafa.gardener.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mustafa.gardener.modules.camera.RealCameraManager
import com.mustafa.gardener.modules.camera.SimCameraManager
import com.mustafa.gardener.modules.vision.CocoLabels
import com.mustafa.gardener.modules.vision.DefaultVisionManager
import com.mustafa.gardener.modules.vision.DetectTrialSettings
import com.mustafa.gardener.modules.vision.DetectionOverlay
import com.mustafa.gardener.modules.vision.SegmentationOverlay
import com.mustafa.gardener.modules.vision.SimVisionManager
import com.mustafa.gardener.modules.vision.TfliteSegVisionEngine
import com.mustafa.gardener.modules.vision.TfliteYoloDetectVisionEngine
import com.mustafa.gardener.modules.vision.VisionBackend
import com.mustafa.gardener.modules.vision.VisionConfig
import com.mustafa.gardener.modules.vision.VisionEngineFactory
import com.mustafa.gardener.modules.vision.VisionEngineState
import kotlinx.coroutines.delay
import kotlin.math.max

private enum class CameraSource {
    SIM,
    REAL,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraPreviewScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var source by remember { mutableStateOf(CameraSource.REAL) }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var resolutionText by remember { mutableStateOf("target 640×480") }
    var visionStatusTr by remember { mutableStateOf("Model yok — sadece önizleme") }
    var analysisNote by remember { mutableStateOf("ImageAnalysis kapalı") }
    var detections by remember { mutableStateOf<DetectionOverlay?>(null) }
    var segOverlay by remember { mutableStateOf<SegmentationOverlay?>(null) }
    var hitText by remember { mutableStateOf("") }
    var fpsText by remember { mutableStateOf("—") }
    var lastDetTimestampNs by remember { mutableStateOf(0L) }
    var rollingFps by remember { mutableFloatStateOf(0f) }

    var confThreshold by remember {
        mutableFloatStateOf(DetectTrialSettings.loadConf(context))
    }
    var classFilter by remember {
        mutableStateOf(DetectTrialSettings.loadClassFilter(context))
    }
    var checklistSelected by remember {
        mutableStateOf(
            when (val f = DetectTrialSettings.loadClassFilter(context)) {
                DetectTrialSettings.ClassFilter.All -> DetectTrialSettings.CHECKLIST.toSet()
                DetectTrialSettings.ClassFilter.Outdoor -> DetectTrialSettings.OUTDOOR_PRESET
                is DetectTrialSettings.ClassFilter.Custom -> f.names
            }
        )
    }
    var showControls by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val realCamera = remember { RealCameraManager(context) }
    val simCamera = remember { SimCameraManager() }
    val realVision = remember {
        DefaultVisionManager(VisionEngineFactory.forRealPath(context))
    }
    val simVision = remember { SimVisionManager() }

    fun applyDetectControls() {
        val engine = realVision.engine() as? TfliteYoloDetectVisionEngine ?: return
        engine.setConfThreshold(confThreshold)
        engine.setAllowedClassIndices(classFilter.allowedIndices())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }

    DisposableEffect(Unit) {
        realCamera.attachVisionManager(realVision)
        realVision.start()
        applyDetectControls()
        onDispose {
            realCamera.attachVisionManager(null)
            realCamera.unbindPreview()
            realCamera.stop()
            VisionEngineFactory.closeQuietly(realVision.engine())
            realVision.stop()
            simCamera.stop()
            simVision.stop()
        }
    }

    LaunchedEffect(confThreshold, classFilter, source) {
        if (source == CameraSource.REAL) {
            applyDetectControls()
        }
    }

    LaunchedEffect(source, permissionGranted) {
        when (source) {
            CameraSource.SIM -> {
                realCamera.unbindPreview()
                simCamera.start()
                simVision.start()
                resolutionText = simCamera.previewResolutionText()
                visionStatusTr = simVision.uiStatusTr()
                analysisNote = "SIM — analiz yok"
                detections = null
                segOverlay = null
                hitText = ""
                fpsText = "—"
                lastDetTimestampNs = 0L
                rollingFps = 0f
            }
            CameraSource.REAL -> {
                simCamera.stop()
                simVision.stop()
                realVision.start()
                val previous = realVision.engine()
                val readyYolo = previous is TfliteYoloDetectVisionEngine &&
                    previous.state() == VisionEngineState.READY
                val readySeg = previous is TfliteSegVisionEngine &&
                    previous.state() == VisionEngineState.READY
                val wantYolo = VisionConfig.DEFAULT_BACKEND == VisionBackend.YOLO_DETECT
                if (!(wantYolo && readyYolo) && !(!wantYolo && readySeg)) {
                    VisionEngineFactory.closeQuietly(previous)
                    realVision.setEngine(VisionEngineFactory.forRealPath(context))
                }
                applyDetectControls()
                realCamera.attachVisionManager(realVision)
                visionStatusTr = realVision.uiStatusTr()
                analysisNote = if (realVision.wantsFrameAnalysis()) {
                    when (VisionConfig.DEFAULT_BACKEND) {
                        VisionBackend.YOLO_DETECT ->
                            "ImageAnalysis açık (≤5 fps) — YOLOv8n tespit"
                        VisionBackend.FFNET_SEG ->
                            "ImageAnalysis açık (≤5 fps) — FFNet segmentasyon"
                    }
                } else {
                    "ImageAnalysis kapalı (model yok / yüklenemedi)"
                }
                if (!permissionGranted) {
                    realCamera.unbindPreview()
                    resolutionText = "REAL — CAMERA permission required"
                } else {
                    repeat(20) {
                        resolutionText = realCamera.previewResolutionText()
                        visionStatusTr = realVision.uiStatusTr()
                        if (realCamera.hasLiveFrames()) return@LaunchedEffect
                        delay(250)
                    }
                    resolutionText = realCamera.previewResolutionText()
                    visionStatusTr = realVision.uiStatusTr()
                }
            }
        }
    }

    LaunchedEffect(source, permissionGranted) {
        if (source != CameraSource.REAL || !permissionGranted) return@LaunchedEffect
        while (true) {
            val latestDet = realVision.latestDetections()
            if (latestDet != null && latestDet !== detections) {
                if (lastDetTimestampNs > 0L &&
                    latestDet.timestampNs > lastDetTimestampNs
                ) {
                    val dtSec =
                        (latestDet.timestampNs - lastDetTimestampNs) / 1_000_000_000.0
                    if (dtSec > 0.001) {
                        val instant = (1.0 / dtSec).toFloat()
                        rollingFps = if (rollingFps <= 0f) {
                            instant
                        } else {
                            rollingFps * 0.65f + instant * 0.35f
                        }
                    }
                }
                lastDetTimestampNs = latestDet.timestampNs
                detections = latestDet
                fpsText = if (rollingFps > 0f) {
                    "%.1f".format(rollingFps)
                } else if (latestDet.inferenceMs > 0L) {
                    "%.1f".format(1000f / latestDet.inferenceMs.coerceAtLeast(1L))
                } else {
                    "—"
                }
                hitText = latestDet.boxes
                    .take(6)
                    .joinToString(" · ") {
                        "${it.label}:${"%.0f".format(it.score * 100)}%"
                    }.ifEmpty { "tespit yok" } +
                    "  (${latestDet.inferenceMs} ms)"
            }
            val latestSeg = realVision.latestOverlay()
            if (latestSeg != null && latestSeg !== segOverlay) {
                segOverlay = latestSeg
                hitText = latestSeg.classPixelCounts.entries
                    .sortedByDescending { it.value }
                    .joinToString(" · ") { "${it.key}:${it.value}" }
            }
            visionStatusTr = realVision.uiStatusTr()
            delay(200)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE4EFE6),
                        Color(0xFFF3F6F2),
                        Color(0xFFDCE6DF),
                    )
                )
            )
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Gardener",
            style = MaterialTheme.typography.displayLarge,
            color = Color(0xFF1B2A20)
        )
        Text(
            text = "Kamera / vision",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF3A5560)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (source == CameraSource.SIM) {
                Button(onClick = { source = CameraSource.SIM }) { Text("SIM") }
                OutlinedButton(onClick = { source = CameraSource.REAL }) { Text("REAL") }
            } else {
                OutlinedButton(onClick = { source = CameraSource.SIM }) { Text("SIM") }
                Button(onClick = { source = CameraSource.REAL }) { Text("REAL") }
            }
            if (VisionConfig.DEFAULT_BACKEND == VisionBackend.YOLO_DETECT) {
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { showControls = true }) {
                    Text("Kontroller")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Source: ${source.name}",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF1B2A20),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Resolution: $resolutionText",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF3A5560),
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "Vision: $visionStatusTr",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1B2A20),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = when (VisionConfig.DEFAULT_BACKEND) {
                VisionBackend.YOLO_DETECT ->
                    "Deneme: YOLOv8n COCO (person/car/… kutular)"
                VisionBackend.FFNET_SEG ->
                    "Deneme: FFNet Cityscapes (yol/kaldırım/duvar/çit)"
            },
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF3A5560),
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = analysisNote,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF6B5344),
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )
        if (hitText.isNotEmpty()) {
            Text(
                text = hitText,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF1B2A20),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .background(Color(0xFF1B2A20)),
            contentAlignment = Alignment.Center
        ) {
            when (source) {
                CameraSource.SIM -> {
                    Text(
                        text = "SIM — no camera frames",
                        color = Color(0xFFD0E0D4),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                CameraSource.REAL -> {
                    if (!permissionGranted) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "REAL path needs CAMERA permission",
                                color = Color(0xFFD0E0D4),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            ) {
                                Text("Grant camera permission")
                            }
                        }
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                    implementationMode =
                                        PreviewView.ImplementationMode.PERFORMANCE
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            update = { previewView ->
                                realCamera.bindPreview(lifecycleOwner, previewView)
                            }
                        )
                        val currentDet = detections
                        if (currentDet != null && currentDet.boxes.isNotEmpty()) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val imgW = currentDet.imageWidth.toFloat().coerceAtLeast(1f)
                                val imgH = currentDet.imageHeight.toFloat().coerceAtLeast(1f)
                                // PreviewView FILL_CENTER: scale to fill, center crop.
                                val scale = max(size.width / imgW, size.height / imgH)
                                val drawnW = imgW * scale
                                val drawnH = imgH * scale
                                val originX = (size.width - drawnW) / 2f
                                val originY = (size.height - drawnH) / 2f
                                val paint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    textSize = 28f
                                    color = android.graphics.Color.WHITE
                                }
                                for (box in currentDet.boxes) {
                                    val left = originX + box.left * drawnW
                                    val top = originY + box.top * drawnH
                                    val right = originX + box.right * drawnW
                                    val bottom = originY + box.bottom * drawnH
                                    val stroke = Color(CocoLabels.colorArgb(box.classIndex))
                                    drawRect(
                                        color = stroke,
                                        topLeft = Offset(left, top),
                                        size = Size(
                                            (right - left).coerceAtLeast(1f),
                                            (bottom - top).coerceAtLeast(1f),
                                        ),
                                        style = Stroke(width = 3.5f),
                                    )
                                    val label = "${box.label} ${"%.0f".format(box.score * 100)}"
                                    drawContext.canvas.nativeCanvas.drawText(
                                        label,
                                        left + 4f,
                                        (top - 8f).coerceAtLeast(28f),
                                        paint,
                                    )
                                }
                            }
                        }
                        val currentSeg = segOverlay
                        val overlayImage = remember(currentSeg?.timestampNs) {
                            currentSeg?.bitmap
                                ?.takeIf { !it.isRecycled }
                                ?.asImageBitmap()
                        }
                        if (currentSeg != null && overlayImage != null && currentDet == null) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawImage(
                                    image = overlayImage,
                                    srcOffset = IntOffset.Zero,
                                    srcSize = IntSize(
                                        currentSeg.maskWidth,
                                        currentSeg.maskHeight,
                                    ),
                                    dstOffset = IntOffset.Zero,
                                    dstSize = IntSize(
                                        size.width.toInt(),
                                        size.height.toInt(),
                                    ),
                                    alpha = 1f,
                                )
                            }
                        }
                        if (VisionConfig.DEFAULT_BACKEND == VisionBackend.YOLO_DETECT &&
                            realVision.wantsFrameAnalysis()
                        ) {
                            val ms = currentDet?.inferenceMs
                            Text(
                                text = if (ms != null) {
                                    "FPS $fpsText · ${ms} ms"
                                } else {
                                    "FPS $fpsText"
                                },
                                color = Color(0xFFF5F7F4),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .background(Color(0x99000000))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "UNVERIFIED on device: box alignment + NPU. Build wires CameraX + " +
                "YOLOv8n TFLite; confirm boxes on S24 Ultra.",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF8A4B2F)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = onBack) {
            Text("Back to robot skeleton")
        }
    }

    if (showControls && VisionConfig.DEFAULT_BACKEND == VisionBackend.YOLO_DETECT) {
        ModalBottomSheet(
            onDismissRequest = { showControls = false },
            sheetState = sheetState,
        ) {
            DetectControlsSheet(
                confThreshold = confThreshold,
                onConfChange = { value ->
                    confThreshold = DetectTrialSettings.clampConf(value)
                    DetectTrialSettings.saveConf(context, confThreshold)
                    applyDetectControls()
                },
                classFilter = classFilter,
                checklistSelected = checklistSelected,
                onSelectAll = {
                    classFilter = DetectTrialSettings.ClassFilter.All
                    checklistSelected = DetectTrialSettings.CHECKLIST.toSet()
                    DetectTrialSettings.saveClassFilter(context, classFilter)
                    applyDetectControls()
                },
                onSelectOutdoor = {
                    classFilter = DetectTrialSettings.ClassFilter.Outdoor
                    checklistSelected = DetectTrialSettings.OUTDOOR_PRESET
                    DetectTrialSettings.saveClassFilter(context, classFilter)
                    applyDetectControls()
                },
                onToggleClass = { name, checked ->
                    val next = if (checked) {
                        checklistSelected + name
                    } else {
                        checklistSelected - name
                    }
                    checklistSelected = next
                    classFilter = if (next == DetectTrialSettings.OUTDOOR_PRESET) {
                        DetectTrialSettings.ClassFilter.Outdoor
                    } else {
                        DetectTrialSettings.ClassFilter.Custom(next)
                    }
                    DetectTrialSettings.saveClassFilter(context, classFilter)
                    applyDetectControls()
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetectControlsSheet(
    confThreshold: Float,
    onConfChange: (Float) -> Unit,
    classFilter: DetectTrialSettings.ClassFilter,
    checklistSelected: Set<String>,
    onSelectAll: () -> Unit,
    onSelectOutdoor: () -> Unit,
    onToggleClass: (String, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 28.dp)
    ) {
        Text(
            text = "Tespit kontrolleri",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B2A20)
        )
        Text(
            text = "Eşik: ${"%.2f".format(confThreshold)}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF3A5560),
            modifier = Modifier.padding(top = 12.dp)
        )
        Slider(
            value = confThreshold,
            onValueChange = onConfChange,
            valueRange = DetectTrialSettings.MIN_CONF..DetectTrialSettings.MAX_CONF,
            steps = 10,
        )
        Text(
            text = "Bulunacak şeyler",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1B2A20),
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (classFilter is DetectTrialSettings.ClassFilter.All) {
                Button(onClick = onSelectAll) { Text("Hepsi") }
            } else {
                OutlinedButton(onClick = onSelectAll) { Text("Hepsi") }
            }
            if (classFilter is DetectTrialSettings.ClassFilter.Outdoor) {
                Button(onClick = onSelectOutdoor) { Text("Dışarı seti") }
            } else {
                OutlinedButton(onClick = onSelectOutdoor) { Text("Dışarı seti") }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            for (name in DetectTrialSettings.CHECKLIST) {
                val checked = classFilter is DetectTrialSettings.ClassFilter.All ||
                    name in checklistSelected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onToggleClass(name, it) },
                        enabled = classFilter !is DetectTrialSettings.ClassFilter.All,
                    )
                    Text(
                        text = DetectTrialSettings.turkishChecklistLabel(name),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF1B2A20),
                    )
                }
            }
        }
        if (classFilter is DetectTrialSettings.ClassFilter.All) {
            Text(
                text = "Hepsi: 80 COCO sınıfı gösterilir. Listeyi daraltmak için " +
                    "«Dışarı seti» veya tek tek seç.",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF6B5344),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
