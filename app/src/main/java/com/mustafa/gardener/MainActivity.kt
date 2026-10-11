package com.mustafa.gardener

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mustafa.gardener.runtime.RobotRuntime
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
                RobotSkeletonScreen(runtime = runtime)
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
