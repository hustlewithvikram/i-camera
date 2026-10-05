package com.hustlewithvikram.icamera.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hustlewithvikram.icamera.camera.CameraCapture
import com.hustlewithvikram.icamera.camera.CameraController
import com.hustlewithvikram.icamera.ui.components.ShutterButton

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }

    if (!granted) {
        LaunchedEffect(Unit) { launcher.launch(Manifest.permission.CAMERA) }
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { CameraController(context) }
    val capture = remember { CameraCapture(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    val preview = Preview.Builder().build()
                    val image = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = image
                    controller.bind(
                        preview,
                        image,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        lifecycleOwner
                    )
                    preview.setSurfaceProvider(surfaceProvider)
                }
            }
        )

        ShutterButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            onClick = { imageCapture?.let { capture.capture(it) {} } }
        )
    }
}
