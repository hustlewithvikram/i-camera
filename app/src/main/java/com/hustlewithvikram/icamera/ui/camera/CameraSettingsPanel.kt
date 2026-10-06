package com.hustlewithvikram.icamera.ui.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.hustlewithvikram.icamera.camera.CameraCapabilities

@Composable
fun CameraSettingsPanel(
    capabilities: CameraCapabilities?,
    lowLightBoost: Boolean,
    onLowLightBoostChange: (Boolean) -> Unit,
    torchStrength: Float,
    onTorchStrengthChange: (Float) -> Unit
) {
    Surface(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xE6161616)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Camera settings", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("HARDWARE", color = Color(0xFF9E9E9E), style = MaterialTheme.typography.labelSmall)
            }
            if (capabilities?.supportsLowLightBoost == true) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Low Light Boost", color = Color.White)
                    Text(if (lowLightBoost) "ON" else "OFF", color = if (lowLightBoost) Color(0xFFFFC107) else Color.White)
                }
            }
            if (capabilities?.supportsTorchStrength == true && capabilities.maxTorchStrengthLevel > 1) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Torch intensity", color = Color.White)
                    Text("${torchStrength.toInt()}/${capabilities.maxTorchStrengthLevel}", color = Color.White)
                }
                Slider(value = torchStrength, onValueChange = onTorchStrengthChange, valueRange = 1f..capabilities.maxTorchStrengthLevel.toFloat())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (capabilities?.supportsUltraHdr == true) Badge("Ultra HDR")
                if (capabilities?.supportsRaw == true) Badge("RAW")
                if (capabilities?.supportsRawJpeg == true) Badge("RAW+JPEG")
                if (capabilities?.supportsLogicalMultiCamera == true) Badge("Multi-camera")
            }
        }
    }
}

@Composable
private fun Badge(label: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0x33222222)) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), color = Color(0xFFD6D6D6), style = MaterialTheme.typography.labelSmall)
    }
}
