package com.hustlewithvikram.icamera.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.toBitmap
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer

enum class VideoTransform {
    NONE,
    SLOW_MOTION,
    TIMELAPSE
}

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


    fun captureDualPhoto(
        primary: ImageCapture,
        secondary: ImageCapture,
        onFinished: (Uri?) -> Unit
    ) {
        var first: Uri? = null
        var second: Uri? = null
        var completed = 0

        fun finish(uri: Uri?) {
            if (uri != null && first == null) first = uri else if (uri != null && second == null) second = uri
            completed++
            if (completed == 2) onFinished(first ?: second)
        }

        capture(primary) { uri -> finish(uri) }
        capture(secondary) { uri -> finish(uri) }
    }

    fun capturePanorama(
        imageCapture: ImageCapture,
        onResult: (Uri?) -> Unit
    ) {
        val frames = mutableListOf<Bitmap>()
        val frameCount = 6

        fun captureNext(index: Int) {
            if (index >= frameCount) {
                val first = frames.firstOrNull()
                if (first == null) {
                    onResult(null)
                    return
                }

                val stripWidth = (first.width * 0.42f).toInt().coerceAtLeast(1)
                val outputWidth = stripWidth * frameCount
                val stitched = Bitmap.createBitmap(
                    outputWidth,
                    first.height,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(stitched)

                frames.forEachIndexed { frameIndex, bitmap ->
                    val sourceLeft = ((bitmap.width - stripWidth) / 2).coerceAtLeast(0)
                    val source = android.graphics.Rect(
                        sourceLeft,
                        0,
                        (sourceLeft + stripWidth).coerceAtMost(bitmap.width),
                        bitmap.height
                    )
                    val destination = android.graphics.Rect(
                        frameIndex * stripWidth,
                        0,
                        (frameIndex + 1) * stripWidth,
                        first.height
                    )
                    canvas.drawBitmap(bitmap, source, destination, null)
                    if (bitmap !== first) bitmap.recycle()
                }
                first.recycle()

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "iCamera_Panorama_" + System.currentTimeMillis() + ".jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/iCamera")
                    }
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )
                if (uri == null) {
                    stitched.recycle()
                    onResult(null)
                    return
                }

                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stitched.compress(Bitmap.CompressFormat.JPEG, 92, stream)
                    } == true
                }.getOrDefault(false)
                stitched.recycle()

                if (saved) {
                    onResult(uri)
                } else {
                    context.contentResolver.delete(uri, null, null)
                    onResult(null)
                }
                return
            }

            imageCapture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                        val bitmap = runCatching {
                            var result = image.toBitmap()
                            val rotation = image.imageInfo.rotationDegrees
                            if (rotation != 0) {
                                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                result = Bitmap.createBitmap(
                                    result,
                                    0,
                                    0,
                                    result.width,
                                    result.height,
                                    matrix,
                                    true
                                )
                            }
                            result
                        }.getOrNull()
                        image.close()

                        if (bitmap == null) {
                            frames.forEach { it.recycle() }
                            frames.clear()
                            onResult(null)
                            return
                        }

                        frames += bitmap
                        if (index + 1 < frameCount) {
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                                { captureNext(index + 1) },
                                220L
                            )
                        } else {
                            captureNext(index + 1)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        frames.forEach { it.recycle() }
                        frames.clear()
                        onResult(null)
                    }
                }
            )
        }

        captureNext(0)
    }

    fun startVideo(
        videoCapture: VideoCapture<Recorder>,
        withAudio: Boolean,
        transform: VideoTransform = VideoTransform.NONE,
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
                    is VideoRecordEvent.Finalize -> {
                        val uri = event.outputResults.outputUri
                        if (event.error == VideoRecordEvent.Finalize.ERROR_NONE &&
                            transform != VideoTransform.NONE &&
                            uri != Uri.EMPTY
                        ) {
                            transformVideo(uri, transform) {
                                ContextCompat.getMainExecutor(context).execute(onFinished)
                            }
                        } else {
                            onFinished()
                        }
                    }
                }
            }
        )
    }

    private fun transformVideo(
        sourceUri: Uri,
        transform: VideoTransform,
        onFinished: () -> Unit
    ) {
        Thread {
            var destinationUri: Uri? = null
            var sourcePfd: android.os.ParcelFileDescriptor? = null
            var destinationPfd: android.os.ParcelFileDescriptor? = null
            var extractor: MediaExtractor? = null
            var muxer: MediaMuxer? = null
            try {
                extractor = MediaExtractor()
                extractor.setDataSource(context, sourceUri, null)
                val videoTrack = (0 until extractor.trackCount).firstOrNull { index ->
                    extractor.getTrackFormat(index)
                        .getString(android.media.MediaFormat.KEY_MIME)
                        ?.startsWith("video/") == true
                } ?: throw IllegalStateException("Recorded video track is missing.")

                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, "iCamera_" + transform.name.lowercase() + "_" + System.currentTimeMillis() + ".mp4")
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/iCamera")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                }
                destinationUri = context.contentResolver.insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    values
                ) ?: throw IllegalStateException("Unable to create processed video.")

                sourcePfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
                    ?: throw IllegalStateException("Unable to read recorded video.")
                destinationPfd = context.contentResolver.openFileDescriptor(destinationUri, "w")
                    ?: throw IllegalStateException("Unable to open processed video.")

                muxer = MediaMuxer(
                    destinationPfd!!.fileDescriptor,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                )
                val outputTrack = muxer.addTrack(extractor.getTrackFormat(videoTrack))
                muxer.start()
                extractor.selectTrack(videoTrack)

                val bufferSize = 4 * 1024 * 1024
                val buffer = java.nio.ByteBuffer.allocate(bufferSize)
                val info = MediaCodec.BufferInfo()
                var frameIndex = 0
                var lastPts = -1L

                while (true) {
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) break

                    val originalPts = extractor.sampleTime
                    if (originalPts >= 0L) {
                        val keep = when (transform) {
                            VideoTransform.SLOW_MOTION -> true
                            VideoTransform.TIMELAPSE -> frameIndex++ % 6 == 0
                            VideoTransform.NONE -> true
                        }
                        if (keep) {
                            val transformedPts = when (transform) {
                                VideoTransform.SLOW_MOTION -> originalPts * 2L
                                VideoTransform.TIMELAPSE -> originalPts / 6L
                                VideoTransform.NONE -> originalPts
                            }
                            info.offset = 0
                            info.size = size
                            info.flags = extractor.sampleFlags
                            info.presentationTimeUs = transformedPts.coerceAtLeast(lastPts + 1L)
                            muxer.writeSampleData(outputTrack, buffer, info)
                            lastPts = info.presentationTimeUs
                        }
                    }
                    buffer.clear()
                    extractor.advance()
                }

                muxer.stop()
                muxer.release()
                muxer = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val ready = ContentValues().apply {
                        put(MediaStore.Video.Media.IS_PENDING, 0)
                    }
                    context.contentResolver.update(destinationUri, ready, null, null)
                }

                context.contentResolver.delete(sourceUri, null, null)
                ContextCompat.getMainExecutor(context).execute(onFinished)
            } catch (_: Throwable) {
                runCatching { muxer?.stop() }
                runCatching { muxer?.release() }
                destinationUri?.let { context.contentResolver.delete(it, null, null) }
                ContextCompat.getMainExecutor(context).execute(onFinished)
            } finally {
                runCatching { extractor?.release() }
                runCatching { sourcePfd?.close() }
                runCatching { destinationPfd?.close() }
            }
        }.start()
    }

}
