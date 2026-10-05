package com.hustlewithvikram.icamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hustlewithvikram.icamera.ui.camera.CameraScreen
import com.hustlewithvikram.icamera.ui.theme.ICameraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ICameraTheme { CameraScreen() } }
    }
}
