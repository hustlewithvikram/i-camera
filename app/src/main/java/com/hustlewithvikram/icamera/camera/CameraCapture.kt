package com.hustlewithvikram.icamera.camera

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer

class CameraCapture(private val context: Context) {
    fun capture(
        imageCapture: ImageCapture,
        onResult: (android.net.Uri?) -> Unit
    ) {
        if (imageCapture.outputFormat == ImageCapture.OUTPUT_FORMAT_RAW_JPEG) {
            captureRawJpeg(imageCapture, onResult)
            return
        }

        val raw = imageCapture.outputFormat == ImageCapture.OUTPUT_FORMAT_RAW
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "iCamera_" + System.currentTimeMillis() + if (raw) ".dng" else ".jpg")
            put(MediaStore.Images.Media.MIME_TYPE, if (raw) "image/x-adobe-dng" else "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/iCamera")
            }
        }

        val output = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ).build()

        imageCapture.takePicture(
            output,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onResult(outputFileResults.savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    onResult(null)
                }
            }
        )
    }

    private fun captureRawJpeg(
        imageCapture: ImageCapture,
        onResult: (android.net.Uri?) -> Unit
    ) {
        val stamp = System.currentTimeMillis()
        val rawValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "iCamera_$stamp.dng")
            put(MediaStore.Images.Media.MIME_TYPE, "image/x-adobe-dng")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/iCamera")
        }
        val jpegValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "iCamera_$stamp.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/iCamera")
        }
        val rawOutput = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            rawValues
        ).build()
        val jpegOutput = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            jpegValues
        ).build()

        imageCapture.takePicture(
            rawOutput,
            jpegOutput,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    if (outputFileResults.savedUri != null) onResult(outputFileResults.savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    onResult(null)
                }
            }
        )
    }

    fun startVideo(
        videoCapture: VideoCapture<Recorder>,
        withAudio: Boolean,
        onStarted: () -> Unit,
        onFinished: () -> Unit
    ): Recording {
        val values = ContentValues().apply {
            put(
                MediaStore.Video.Media.DISPLAY_NAME,
                "iCamera_" + System.currentTimeMillis() + ".mp4"
            )
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/iCamera")
            }
        }

        val output = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(values).build()

        var pending = videoCapture.output.prepareRecording(context, output)
        if (withAudio) {
            pending = pending.withAudioEnabled()
        }

        return pending.start(
            ContextCompat.getMainExecutor(context),
            Consumer<VideoRecordEvent> { event ->
                when (event) {
                    is VideoRecordEvent.Start -> onStarted()
                    is VideoRecordEvent.Finalize -> onFinished()
                }
            }
        )
    }
}
