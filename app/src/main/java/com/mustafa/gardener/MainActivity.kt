package com.mustafa.gardener

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.mustafa.gardener.runtime.RobotRuntime
import com.mustafa.gardener.ui.CameraPreviewScreen
import com.mustafa.gardener.ui.RobotSkeletonScreen
import com.mustafa.gardener.ui.theme.GardenerTheme

class MainActivity : ComponentActivity() {
    private lateinit var runtime: RobotRuntime

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtime = RobotRuntime.createSimulation()
        enableEdgeToEdge()
        setContent {
            GardenerTheme {
                var showCameraPreview by rememberSaveable { mutableStateOf(false) }
                if (showCameraPreview) {
                    CameraPreviewScreen(onBack = { showCameraPreview = false })
                } else {
                    RobotSkeletonScreen(
                        runtime = runtime,
                        onOpenCameraPreview = { showCameraPreview = true },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        if (::runtime.isInitialized) {
            runtime.stopSimulation()
        }
        super.onDestroy()
    }
}
