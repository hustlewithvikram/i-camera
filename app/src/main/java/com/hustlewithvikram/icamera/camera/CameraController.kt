package com.hustlewithvikram.icamera.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.DynamicRange
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.TimeUnit

data class CameraCapabilities(
    val hasFlash: Boolean,
    val hasFrontCamera: Boolean,
    val hasVideo: Boolean,
    val maxZoomRatio: Float,
    val exposureSupported: Boolean,
    val exposureMin: Int,
    val exposureMax: Int,
    val supportedPhotoModes: Set<PhotoMode>,
    val supportsUltraHdr: Boolean,
    val supportsRaw: Boolean,
    val supportsRawJpeg: Boolean,
    val supportsZeroShutterLag: Boolean,
    val supportsConcurrentCamera: Boolean,
    val macroMinFocusDistance: Float
)

enum class PhotoMode(val label: String, val extensionMode: Int?) {
    PHOTO("PHOTO", null),
    NIGHT("NIGHT", ExtensionMode.NIGHT),
    HDR("HDR", ExtensionMode.HDR),
    PORTRAIT("PORTRAIT", ExtensionMode.BOKEH),
    RETOUCH("RETOUCH", ExtensionMode.FACE_RETOUCH),
    AUTO("AUTO", ExtensionMode.AUTO)
}

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class CameraController(private val context: Context) {
    private var provider: ProcessCameraProvider? = null

    var camera: Camera? = null
        private set
    var imageCapture: ImageCapture? = null
        private set
    var videoCapture: VideoCapture<Recorder>? = null
        private set

    private var recording: Recording? = null

    fun bind(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        selector: CameraSelector,
        videoMode: Boolean,
        photoMode: PhotoMode = PhotoMode.PHOTO,
        onReady: (CameraCapabilities) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val cameraProvider = future.get()
                provider = cameraProvider
                cameraProvider.unbindAll()

                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val useCases = mutableListOf<androidx.camera.core.UseCase>(preview)

                val image = if (!videoMode) {
                    ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                        .build()
                } else {
                    null
                }

                val video = if (videoMode) {
                    val cameraInfo = cameraProvider.getCameraInfo(selector)
                    val supported = Recorder.getVideoCapabilities(cameraInfo)
                        .getSupportedQualities(DynamicRange.SDR)

                    if (supported.isEmpty()) {
                        null
                    } else {
                        val selectorQuality = QualitySelector.from(
                            Quality.FHD,
                            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                        )
                        val recorder = Recorder.Builder()
                            .setQualitySelector(selectorQuality)
                            .build()
                        VideoCapture.withOutput(recorder)
                    }
                } else {
                    null
                }

                if (image != null) useCases += image
                if (video != null) useCases += video

                if (videoMode && video == null) {
                    throw IllegalStateException("Video capture is not supported.")
                }

                val extensionConfig = if (!videoMode && photoMode.extensionMode != null) {
                    val extensionsManager = ExtensionsManager.getInstanceAsync(context, cameraProvider).get()
                    if (extensionsManager.isExtensionAvailable(selector, photoMode.extensionMode)) {
                        androidx.camera.extensions.ExtensionSessionConfig.Builder(
                            photoMode.extensionMode,
                            extensionsManager
                        ).addUseCase(preview).addUseCase(image!!).build()
                    } else {
                        null
                    }
                } else {
                    null
                }

                camera = if (extensionConfig != null) {
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, extensionConfig)
                } else {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        *useCases.toTypedArray()
                    )
                }
                imageCapture = image
                videoCapture = video

                val activeCamera = camera ?: return@addListener
                val zoomState = activeCamera.cameraInfo.zoomState.value
                val exposure = activeCamera.cameraInfo.exposureState
                val supportedPhotoModes = mutableSetOf(PhotoMode.PHOTO)
                val supportedFormats = runCatching {
                    ImageCapture.getImageCaptureCapabilities(activeCamera.cameraInfo).supportedOutputFormats
                }.getOrDefault(setOf(ImageCapture.OUTPUT_FORMAT_JPEG))

                if (!videoMode) {
                    val extensionsManager = ExtensionsManager.getInstanceAsync(context, cameraProvider).get()
                    listOf(PhotoMode.NIGHT, PhotoMode.HDR, PhotoMode.PORTRAIT, PhotoMode.RETOUCH, PhotoMode.AUTO).forEach { item ->
                        if (item.extensionMode != null && runCatching {
                                extensionsManager.isExtensionAvailable(selector, item.extensionMode)
                            }.getOrDefault(false)) {
                            supportedPhotoModes += item
                        }
                    }
                }

                onReady(
                    CameraCapabilities(
                        hasFlash = activeCamera.cameraInfo.hasFlashUnit(),
                        hasFrontCamera = runCatching {
                            cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                        }.getOrDefault(false),
                        hasVideo = runCatching {
                            Recorder.getVideoCapabilities(
                                cameraProvider.getCameraInfo(selector)
                            ).getSupportedQualities(DynamicRange.SDR).isNotEmpty()
                        }.getOrDefault(false),
                        maxZoomRatio = zoomState?.maxZoomRatio ?: 1f,
                        exposureSupported = exposure.isExposureCompensationSupported,
                        exposureMin = exposure.exposureCompensationRange.lower,
                        exposureMax = exposure.exposureCompensationRange.upper,
                        supportedPhotoModes = supportedPhotoModes,
                        supportsUltraHdr = supportedFormats.contains(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR),
                        supportsRaw = supportedFormats.contains(ImageCapture.OUTPUT_FORMAT_RAW),
                        supportsRawJpeg = supportedFormats.contains(ImageCapture.OUTPUT_FORMAT_RAW_JPEG),
                        supportsZeroShutterLag = false,
                        supportsConcurrentCamera = cameraProvider.availableConcurrentCameraInfos.isNotEmpty(),
                        macroMinFocusDistance = runCatching {
                            androidx.camera.camera2.interop.Camera2CameraInfo.from(activeCamera.cameraInfo)
                                .getCameraCharacteristic(android.hardware.camera2.CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
                        }.getOrDefault(0f)
                    )
                )
            } catch (t: Throwable) {
                onError(t)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun startRecording(
        capture: CameraCapture,
        withAudio: Boolean,
        onStarted: () -> Unit,
        onFinished: () -> Unit
    ) {
        val video = videoCapture ?: return
        recording = capture.startVideo(
            videoCapture = video,
            withAudio = withAudio,
            onStarted = onStarted,
            onFinished = {
                recording = null
                onFinished()
            }
        )
    }

    fun stopRecordingIfNeeded() {
        recording?.stop()
        recording = null
    }

    fun setZoom(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
    }

    fun setExposure(index: Int) {
        camera?.cameraControl?.setExposureCompensationIndex(index)
    }

    fun setTorch(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }

    fun focusAt(previewView: PreviewView, x: Float, y: Float) {
        val activeCamera = camera ?: return
        val point = previewView.meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        activeCamera.cameraControl.startFocusAndMetering(action)
    }

    fun unbind() {
        stopRecordingIfNeeded()
        provider?.unbindAll()
        camera = null
        imageCapture = null
        videoCapture = null
    }
}
