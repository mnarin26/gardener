package com.mustafa.gardener.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mustafa.gardener.modules.camera.RealCameraManager
import com.mustafa.gardener.modules.camera.SimCameraManager
import com.mustafa.gardener.modules.vision.DefaultVisionManager
import com.mustafa.gardener.modules.vision.SimVisionManager
import com.mustafa.gardener.modules.vision.UnloadedVisionEngine
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

    val realCamera = remember { RealCameraManager(context) }
    val simCamera = remember { SimCameraManager() }
    val realVision = remember {
        DefaultVisionManager(UnloadedVisionEngine.forContext(context))
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
            }
            CameraSource.REAL -> {
                simCamera.stop()
                simVision.stop()
                realVision.start()
                // Keep Unloaded/NotConfigured engine in sync with placeholder paths.
                realVision.setEngine(UnloadedVisionEngine.forContext(context))
                realCamera.attachVisionManager(realVision)
                visionStatusTr = realVision.uiStatusTr()
                if (!permissionGranted) {
                    realCamera.unbindPreview()
                    resolutionText = "REAL — CAMERA permission required"
                } else {
                    // Poll until CameraX reports a bound size (async bind).
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
            text = "Faz 1 sınıflar: ${VisionLabels.phase1Summary()} (ızgara = Faz 2)",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF3A5560),
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = "Continuous preview — keep resolution low to reduce phone load / battery use. " +
                "ImageAnalysis kapalı (model yok).",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF6B5344),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

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
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "UNVERIFIED on device: build wires CameraX; confirm preview on the phone. " +
                "On-device inference: not running (no model).",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF8A4B2F)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = onBack) {
            Text("Back to robot skeleton")
        }
    }
}
