package com.hustlewithvikram.icamera.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

class CameraController(private val context: Context) {
    fun bind(
        preview: Preview,
        imageCapture: ImageCapture,
        cameraSelector: CameraSelector,
        lifecycleOwner: LifecycleOwner
    ) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
        }, ContextCompat.getMainExecutor(context))
    }
}
