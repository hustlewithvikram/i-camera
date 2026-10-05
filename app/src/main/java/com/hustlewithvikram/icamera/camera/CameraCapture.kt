package com.hustlewithvikram.icamera.camera

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat

class CameraCapture(private val context: Context) {
    fun capture(imageCapture: ImageCapture, onResult: (Boolean) -> Unit) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "iCamera_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/iCamera")
            }
        }
        val output = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ).build()
        imageCapture.takePicture(output, ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) = onResult(true)
                override fun onError(exception: ImageCaptureException) = onResult(false)
            })
    }
}
