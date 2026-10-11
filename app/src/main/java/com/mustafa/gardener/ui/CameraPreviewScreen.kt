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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import com.mustafa.gardener.modules.vision.DefaultVisionManager
import com.mustafa.gardener.modules.vision.SegmentationOverlay
import com.mustafa.gardener.modules.vision.SimVisionManager
import com.mustafa.gardener.modules.vision.TfliteSegVisionEngine
import com.mustafa.gardener.modules.vision.VisionEngineFactory
import com.mustafa.gardener.modules.vision.VisionEngineState
import com.mustafa.gardener.modules.vision.VisionLabels
import kotlinx.coroutines.delay

private enum class CameraSource {
    SIM,
    REAL,
}

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
    var overlay by remember { mutableStateOf<SegmentationOverlay?>(null) }
    var classHitText by remember { mutableStateOf("") }

    val realCamera = remember { RealCameraManager(context) }
    val simCamera = remember { SimCameraManager() }
    val realVision = remember {
        DefaultVisionManager(VisionEngineFactory.forRealPath(context))
    }
    val simVision = remember { SimVisionManager() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }

    DisposableEffect(Unit) {
        realCamera.attachVisionManager(realVision)
        realVision.start()
        onDispose {
            realCamera.attachVisionManager(null)
            realCamera.unbindPreview()
            realCamera.stop()
            (realVision.engine() as? TfliteSegVisionEngine)?.close()
            realVision.stop()
            simCamera.stop()
            simVision.stop()
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
                overlay = null
                classHitText = ""
            }
            CameraSource.REAL -> {
                simCamera.stop()
                simVision.stop()
                realVision.start()
                // Reload engine once when entering REAL (asset may exist).
                val previous = realVision.engine()
                if (previous !is TfliteSegVisionEngine ||
                    previous.state() != VisionEngineState.READY
                ) {
                    (previous as? TfliteSegVisionEngine)?.close()
                    realVision.setEngine(VisionEngineFactory.forRealPath(context))
                }
                realCamera.attachVisionManager(realVision)
                visionStatusTr = realVision.uiStatusTr()
                analysisNote = if (realVision.wantsFrameAnalysis()) {
                    "ImageAnalysis açık (≤5 fps) — gerçek segmentasyon"
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

    // Poll overlay while REAL + READY (engine updates from analysis thread).
    LaunchedEffect(source, permissionGranted) {
        if (source != CameraSource.REAL || !permissionGranted) return@LaunchedEffect
        while (true) {
            val latest = realVision.latestOverlay()
            if (latest != null && latest !== overlay) {
                overlay = latest
                classHitText = latest.classPixelCounts.entries
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
            text = "Hedef sınıflar: ${VisionLabels.phase1Summary()} (Cityscapes FFNet)",
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
        if (classHitText.isNotEmpty()) {
            Text(
                text = "Maske pikselleri: $classHitText",
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
                        val currentOverlay = overlay
                        val overlayImage = remember(currentOverlay?.timestampNs) {
                            currentOverlay?.bitmap
                                ?.takeIf { !it.isRecycled }
                                ?.asImageBitmap()
                        }
                        if (currentOverlay != null && overlayImage != null) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawImage(
                                    image = overlayImage,
                                    srcOffset = IntOffset.Zero,
                                    srcSize = IntSize(
                                        currentOverlay.maskWidth,
                                        currentOverlay.maskHeight,
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
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "UNVERIFIED on device: seg quality + NPU. Build wires CameraX + TFLite; " +
                "confirm overlay on S24 Ultra.",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF8A4B2F)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = onBack) {
            Text("Back to robot skeleton")
        }
    }
}
