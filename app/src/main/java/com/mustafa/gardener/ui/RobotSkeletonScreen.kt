package com.mustafa.gardener.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mustafa.gardener.R
import com.mustafa.gardener.core.DriverMode
import com.mustafa.gardener.core.ModuleStatus
import com.mustafa.gardener.modules.mission.MissionState
import com.mustafa.gardener.runtime.RobotRuntime
import com.mustafa.gardener.ui.theme.GardenerTheme

@Composable
fun RobotSkeletonScreen(runtime: RobotRuntime) {
    var tick by remember { mutableIntStateOf(0) }
    // Read after tick so Compose recomposes on SIM stub advances.
    @Suppress("UNUSED_EXPRESSION")
    tick

    val missionState = runtime.missionManager.currentState()
    val simActive = runtime.simulationManager.isSimulationActive()
    val modules = runtime.moduleStatuses()

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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.gardener_logo),
                contentDescription = "Gardener logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Column {
                Text(
                    text = "Gardener",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color(0xFF1B2A20)
                )
                Text(
                    text = "Robot app skeleton — architecture only",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF3A5560)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        StatusBlock(
            simActive = simActive,
            missionState = missionState,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                runtime.missionManager.advanceSimStub()
                tick += 1
            },
            enabled = simActive
        ) {
            Text("Advance mission (SIM stub)")
        }

        Text(
            text = "Button cycles MissionState with fake events. Not a real mow.",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF6B5344),
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
        )

        Text(
            text = "Wired modules (${modules.size})",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF1B2A20)
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(modules, key = { it.moduleName }) { status ->
                ModuleRow(status)
                HorizontalDivider(color = Color(0x33000000))
            }
        }
    }
}

@Composable
private fun StatusBlock(simActive: Boolean, missionState: MissionState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xCC1B2A20))
            .padding(16.dp)
    ) {
        Text(
            text = if (simActive) "SIMULATION mode ACTIVE" else "SIMULATION mode OFF",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFE8F0EA),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Mission state: ${missionState.name}",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFD0E0D4)
        )
        Text(
            text = "No camera / LiDAR / ESP32 / YOLO attached.",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFB8C8BC),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ModuleRow(status: ModuleStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = status.moduleName,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1B2A20)
            )
            Text(
                text = status.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF3A5560)
            )
        }
        Text(
            text = status.mode.name,
            style = MaterialTheme.typography.labelLarge,
            color = if (status.mode == DriverMode.SIMULATION) {
                Color(0xFF6B5344)
            } else {
                Color(0xFF3F6B4B)
            },
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RobotSkeletonScreenPreview() {
    GardenerTheme {
        RobotSkeletonScreen(runtime = RobotRuntime.createSimulation())
    }
}
