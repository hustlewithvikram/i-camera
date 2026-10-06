package com.hustlewithvikram.icamera.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ICameraColors = darkColorScheme(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainer = Color(0xFF151515),
    surfaceContainerHigh = Color(0xFF202020),
    surfaceContainerHighest = Color(0xFF2A2A2A),
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFE7E7E7),
    onPrimaryContainer = Color.Black
)

@Composable
fun ICameraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ICameraColors,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(32.dp)
        ),
        content = content
    )
}
