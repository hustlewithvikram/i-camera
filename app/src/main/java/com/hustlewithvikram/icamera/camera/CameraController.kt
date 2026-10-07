package com.hustlewithvikram.icamera.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Range
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ConcurrentCamera
import androidx.camera.core.UseCaseGroup
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
import kotlin.math.roundToInt

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
    val macroMinFocusDistance: Float,
    val supportsLowLightBoost: Boolean,
    val supportsTorchStrength: Boolean,
    val maxTorchStrengthLevel: Int,
    val supportsLogicalMultiCamera: Boolean,
    val isoMin: Int,
    val isoMax: Int,
    val exposureTimeMinNs: Long,
    val exposureTimeMaxNs: Long,
    val supportsUltraWide: Boolean = false,
    val ultraWideZoomRatio: Float = 0.5f,
    val supportsPanorama: Boolean = false,
    val supportsDocumentScan: Boolean = false,
    val supportsSlowMotion: Boolean = false,
    val supportsTimelapse: Boolean = false,
    val supportsDualPhotoVideo: Boolean = false,
    val hardwareZoomRatios: List<Float> = emptyList(),
    val hardwareUltraWideRatios: List<Float> = emptyList(),
    val supportedVideoFps: List<Int> = emptyList(),
    val supportsExtensionStrength: Boolean = false,
    val supportsAeAfLock: Boolean = false,
    val supportsOpticalStabilization: Boolean = false,
    val supportsElectronicStabilization: Boolean = false,
    val supportsFaceDetection: Boolean = false
)

enum class PhotoMode(val label: String, val extensionMode: Int?) {
    PHOTO("PHOTO", null),
    NIGHT("NIGHT", ExtensionMode.NIGHT),
    HDR("HDR", ExtensionMode.HDR),
    PORTRAIT("PORTRAIT", ExtensionMode.BOKEH),
    RETOUCH("RETOUCH", ExtensionMode.FACE_RETOUCH),
    AUTO("AUTO", ExtensionMode.AUTO),
    MACRO("MACRO", null),
    RAW("RAW", null)
}

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class CameraController(private val context: Context) {
    private var provider: ProcessCameraProvider? = null
    private var bindGeneration = 0L
    private var extensionsManager: ExtensionsManager? = null

    var camera: Camera? = null
        private set
    var imageCapture: ImageCapture? = null
        private set
    var videoCapture: VideoCapture<Recorder>? = null
        private set

    private var recording: Recording? = null
    private var secondaryRecording: Recording? = null
    var secondaryVideoCapture: VideoCapture<Recorder>? = null
        private set
    var secondaryImageCapture: ImageCapture? = null
        private set

    private var ultraWideSelector: CameraSelector? = null
    private var ultraWideZoomRatio: Float = 0.5f

    fun getUltraWideSelector(): CameraSelector? = ultraWideSelector
    fun getUltraWideZoomRatio(): Float = ultraWideZoomRatio

    fun bind(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        selector: CameraSelector,
        videoMode: Boolean,
        photoMode: PhotoMode = PhotoMode.PHOTO,
        capabilitySelector: CameraSelector = selector,
        targetFps: Int? = null,
        onReady: (CameraCapabilities) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val requestGeneration = ++bindGeneration
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                if (requestGeneration != bindGeneration) return@addListener
                val cameraProvider = future.get()
                provider = cameraProvider
                if (requestGeneration != bindGeneration) return@addListener
                cameraProvider.unbindAll()

                val previewBuilder = Preview.Builder()
                targetFps?.let { fps -> previewBuilder.setTargetFrameRate(Range(fps, fps)) }
                val preview = previewBuilder.build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val useCases = mutableListOf<androidx.camera.core.UseCase>(preview)

                val image = if (!videoMode) {
                    val builder = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                    val formats = runCatching {
                        ImageCapture.getImageCaptureCapabilities(cameraProvider.getCameraInfo(selector)).supportedOutputFormats
                    }.getOrDefault(emptySet())
                    if (photoMode == PhotoMode.RAW && formats.contains(ImageCapture.OUTPUT_FORMAT_RAW_JPEG)) {
                        builder.setOutputFormat(ImageCapture.OUTPUT_FORMAT_RAW_JPEG)
                    } else if (photoMode == PhotoMode.RAW && formats.contains(ImageCapture.OUTPUT_FORMAT_RAW)) {
                        builder.setOutputFormat(ImageCapture.OUTPUT_FORMAT_RAW)
                    } else if (formats.contains(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)) {
                        builder.setOutputFormat(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
                    }
                    builder.build()
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
                        val selectorQuality = QualitySelector.from(Quality.HIGHEST)
                        val recorder = Recorder.Builder()
                            .setQualitySelector(selectorQuality)
                            .build()
                        val videoBuilder = VideoCapture.Builder(recorder)
                        targetFps?.let { fps -> videoBuilder.setTargetFrameRate(Range(fps, fps)) }
                        videoBuilder.build()
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
                    val extensionProvider = extensionsManager
                        ?: ExtensionsManager.getInstanceAsync(context, cameraProvider).get().also {
                            extensionsManager = it
                        }
                    if (extensionProvider.isExtensionAvailable(selector, photoMode.extensionMode)) {
                        androidx.camera.extensions.ExtensionSessionConfig.Builder(
                            photoMode.extensionMode,
                            extensionProvider
                        ).addUseCase(preview).addUseCase(image!!).build()
                    } else {
                        null
                    }
                } else {
                    null
                }

                if (requestGeneration != bindGeneration) return@addListener

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
                // Camera modes are capabilities of the underlying camera, not of the
                // currently selected capture mode. Always calculate them from the stable
                // logical selector so switching PHOTO <-> VIDEO cannot reorder the rail.
                val capabilityInfo = runCatching {
                    cameraProvider.getCameraInfo(capabilitySelector)
                }.getOrNull() ?: activeCamera.cameraInfo

                val supportedPhotoModes = mutableSetOf(PhotoMode.PHOTO)
                val supportedVideoFps = runCatching {
                    capabilityInfo.getSupportedFrameRateRanges()
                        .flatMap { range -> listOf(range.lower, range.upper) }
                        .filter { it in 1..240 }
                        .distinct()
                        .sorted()
                }.getOrDefault(emptyList())
                val supportsVideoFromCapability = runCatching {
                    Recorder.getVideoCapabilities(capabilityInfo)
                        .getSupportedQualities(DynamicRange.SDR)
                        .isNotEmpty()
                }.getOrDefault(false)
                val supportsHighSpeedVideo = runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val characteristics = androidx.camera.camera2.interop.Camera2CameraInfo
                            .from(capabilityInfo)
                        val highSpeedKey = CameraCharacteristics.Key(
                            "android.control.availableHighSpeedVideoConfigurations",
                            Array<IntArray>::class.java
                        )
                        val configs: Array<IntArray>? = characteristics.getCameraCharacteristic(highSpeedKey)
                        !configs.isNullOrEmpty()
                    } else {
                        false
                    }
                }.getOrDefault(false)
                val supportedFormats = runCatching {
                    ImageCapture.getImageCaptureCapabilities(capabilityInfo).supportedOutputFormats
                }.getOrDefault(setOf(ImageCapture.OUTPUT_FORMAT_JPEG))

                val extensionProvider = extensionsManager
                    ?: ExtensionsManager.getInstanceAsync(context, cameraProvider).get().also {
                        extensionsManager = it
                    }
                listOf(PhotoMode.NIGHT, PhotoMode.HDR, PhotoMode.PORTRAIT, PhotoMode.RETOUCH, PhotoMode.AUTO).forEach { item ->
                    if (item.extensionMode != null && runCatching {
                            extensionProvider.isExtensionAvailable(capabilitySelector, item.extensionMode)
                        }.getOrDefault(false)) {
                        supportedPhotoModes += item
                    }
                }

                val capabilityMacroDistance = runCatching {
                    androidx.camera.camera2.interop.Camera2CameraInfo.from(capabilityInfo)
                        .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
                }.getOrDefault(0f)
                val activeMacroDistance = runCatching {
                    androidx.camera.camera2.interop.Camera2CameraInfo.from(activeCamera.cameraInfo)
                        .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
                }.getOrDefault(0f)
                if (capabilityMacroDistance > 0f) supportedPhotoModes += PhotoMode.MACRO
                if (supportedFormats.contains(ImageCapture.OUTPUT_FORMAT_RAW) || supportedFormats.contains(ImageCapture.OUTPUT_FORMAT_RAW_JPEG)) {
                    supportedPhotoModes += PhotoMode.RAW
                }

                // Discover the real back-camera lens set exposed by CameraX.
                // CameraInfo intrinsicZoomRatio is defined relative to the default
                // back camera: < 1.0 is ultra-wide, 1.0 is the main camera, and
                // > 1.0 is telephoto. Use the actual CameraInfo selectors so a
                // detected ultra-wide can be rebound instead of faking a digital
                // sub-1x crop.
                val backCameraInfos = runCatching {
                    cameraProvider.availableCameraInfos
                        .filter { it.lensFacing == CameraSelector.LENS_FACING_BACK }
                }.getOrDefault(emptyList())

                val discoveredUltraWide = backCameraInfos
                    .filter {
                        val ratio = it.intrinsicZoomRatio
                        ratio.isFinite() && ratio > 0.05f && ratio < 0.98f
                    }
                    .minByOrNull { it.intrinsicZoomRatio }

                val discoveredUltraWideSelector = discoveredUltraWide?.cameraSelector
                val discoveredUltraWideRatio = discoveredUltraWide
                    ?.intrinsicZoomRatio
                    ?.coerceIn(0.1f, 0.98f)
                    ?: 0.5f

                ultraWideSelector = discoveredUltraWideSelector
                ultraWideZoomRatio = discoveredUltraWideRatio

                // These are real optical/physical lens ratios. The UI will add the
                // standard 2x/3x presets when the camera can reach them, and the
                // CameraX max zoom remains the final exact Nx stop.
                val hardwareZoomRatios = backCameraInfos
                    .map { it.intrinsicZoomRatio }
                    .filter { it.isFinite() && it > 1.01f }
                    .map { (it * 10f).roundToInt() / 10f }
                    .distinct()
                    .sorted()

                val hardwareUltraWideRatios = backCameraInfos
                    .map { it.intrinsicZoomRatio }
                    .filter { it.isFinite() && it > 0.05f && it < 0.98f }
                    .map { (it * 10f).roundToInt() / 10f }
                    .distinct()
                    .sorted()
                if (photoMode == PhotoMode.MACRO) setMacro(true, activeMacroDistance) else setMacro(false, 0f)

                if (requestGeneration != bindGeneration) return@addListener

                onReady(
                    val supportsAeAfLock = runCatching {
                        val c2 = androidx.camera.camera2.interop.Camera2CameraInfo.from(activeCamera.cameraInfo)
                        val aeModes = c2.getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES).orEmpty()
                        val afModes = c2.getCameraCharacteristic(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES).orEmpty()
                        aeModes.isNotEmpty() && afModes.isNotEmpty()
                    }.getOrDefault(false)
                    val supportsOis = runCatching {
                        val modes = androidx.camera.camera2.interop.Camera2CameraInfo.from(activeCamera.cameraInfo)
                            .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                        modes?.contains(CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON) == true
                    }.getOrDefault(false)
                    val supportsEis = runCatching {
                        val modes = androidx.camera.camera2.interop.Camera2CameraInfo.from(activeCamera.cameraInfo)
                            .getCameraCharacteristic(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
                        modes?.contains(CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON) == true
                    }.getOrDefault(false)
                    val supportsFaceDetection = runCatching {
                        val modes = androidx.camera.camera2.interop.Camera2CameraInfo.from(activeCamera.cameraInfo)
                            .getCameraCharacteristic(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES)
                        !modes.isNullOrEmpty()
                    }.getOrDefault(false)

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
                        supportsConcurrentCamera = cameraProvider.availableConcurrentCameraInfos.any { infos ->
                            infos.any { it.lensFacing == CameraSelector.LENS_FACING_BACK } &&
                                infos.any { it.lensFacing == CameraSelector.LENS_FACING_FRONT }
                        },
                        macroMinFocusDistance = activeMacroDistance,
                        supportsLowLightBoost = activeCamera.cameraInfo.isLowLightBoostSupported,
                        supportsTorchStrength = activeCamera.cameraInfo.isTorchStrengthSupported,
                        maxTorchStrengthLevel = activeCamera.cameraInfo.maxTorchStrengthLevel,
                        supportsLogicalMultiCamera = activeCamera.cameraInfo.isLogicalMultiCameraSupported,
                        isoMin = runCatching { androidx.camera.camera2.interop.Camera2CameraInfo.from(activeCamera.cameraInfo).getCameraCharacteristic(android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.lower ?: 100 }.getOrDefault(100),
                        isoMax = runCatching { androidx.camera.camera2.interop.Camera2CameraInfo.from(activeCamera.cameraInfo).getCameraCharacteristic(android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.upper ?: 800 }.getOrDefault(800),
                        exposureTimeMinNs = runCatching { androidx.camera.camera2.interop.Camera2CameraInfo.from(activeCamera.cameraInfo).getCameraCharacteristic(android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.lower ?: 1_000_000L }.getOrDefault(1_000_000L),
                        exposureTimeMaxNs = runCatching { androidx.camera.camera2.interop.Camera2CameraInfo.from(activeCamera.cameraInfo).getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.upper ?: 100_000_000L }.getOrDefault(100_000_000L),
                        supportsUltraWide = discoveredUltraWideSelector != null,
                        ultraWideZoomRatio = discoveredUltraWideRatio,
                        // Panorama and document scanning are software-assisted modes.
                        // They are only exposed on a camera that can capture stills.
                        supportsPanorama = capabilityInfo.lensFacing == CameraSelector.LENS_FACING_BACK,
                        supportsDocumentScan = capabilityInfo.lensFacing == CameraSelector.LENS_FACING_BACK,
                        // Slow motion is exposed only when Camera2 reports a
                        // constrained high-speed video capability.
                        supportsSlowMotion = supportsHighSpeedVideo && supportsVideoFromCapability,
                        supportsTimelapse = supportsVideoFromCapability,
                        supportsDualPhotoVideo = cameraProvider.availableConcurrentCameraInfos.any { infos ->
                            infos.any { it.lensFacing == CameraSelector.LENS_FACING_BACK } &&
                                infos.any { it.lensFacing == CameraSelector.LENS_FACING_FRONT }
                        },
                        hardwareZoomRatios = hardwareZoomRatios,
                        hardwareUltraWideRatios = hardwareUltraWideRatios,
                        supportedVideoFps = supportedVideoFps,
                        supportsExtensionStrength = extensionProvider.getCameraExtensionsInfo(activeCamera.cameraInfo).isExtensionStrengthAvailable(),
                    )
                )
            } catch (t: Throwable) {
                onError(t)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun bindConcurrent(
        primaryPreviewView: PreviewView,
        secondaryPreviewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        onReady: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val requestGeneration = ++bindGeneration
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                if (requestGeneration != bindGeneration) return@addListener
                val cameraProvider = future.get()
                provider = cameraProvider
                if (requestGeneration != bindGeneration) return@addListener
                cameraProvider.unbindAll()
                val pair = cameraProvider.availableConcurrentCameraInfos.firstOrNull { infos ->
                    infos.any { it.lensFacing == CameraSelector.LENS_FACING_BACK } &&
                        infos.any { it.lensFacing == CameraSelector.LENS_FACING_FRONT }
                } ?: throw IllegalStateException("Dual camera is not supported.")
                val backInfo = pair.first { it.lensFacing == CameraSelector.LENS_FACING_BACK }
                val frontInfo = pair.first { it.lensFacing == CameraSelector.LENS_FACING_FRONT }
                val backPreview = Preview.Builder().build().also { it.setSurfaceProvider(primaryPreviewView.surfaceProvider) }
                val frontPreview = Preview.Builder().build().also { it.setSurfaceProvider(secondaryPreviewView.surfaceProvider) }
                val backImage = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()
                val frontImage = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()
                val backRecorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
                val frontRecorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
                val backVideo = VideoCapture.withOutput(backRecorder)
                val frontVideo = VideoCapture.withOutput(frontRecorder)
                val backGroup = UseCaseGroup.Builder()
                    .addUseCase(backPreview)
                    .addUseCase(backImage)
                    .addUseCase(backVideo)
                    .build()
                val frontGroup = UseCaseGroup.Builder()
                    .addUseCase(frontPreview)
                    .addUseCase(frontImage)
                    .addUseCase(frontVideo)
                    .build()
                val configs = listOf(
                    ConcurrentCamera.SingleCameraConfig(backInfo.cameraSelector, backGroup, lifecycleOwner),
                    ConcurrentCamera.SingleCameraConfig(frontInfo.cameraSelector, frontGroup, lifecycleOwner)
                )
                if (requestGeneration != bindGeneration) return@addListener
                val concurrent = cameraProvider.bindToLifecycle(configs)
                if (concurrent.cameras.size != 2) throw IllegalStateException("Dual camera binding failed.")
                camera = concurrent.cameras[0]
                videoCapture = backVideo
                secondaryVideoCapture = frontVideo
                imageCapture = backImage
                secondaryImageCapture = frontImage
                onReady()
            } catch (t: Throwable) {
                secondaryVideoCapture = null
                onError(t)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun captureDualPhoto(
        capture: CameraCapture,
        onFinished: (android.net.Uri?) -> Unit
    ) {
        val primary = imageCapture
        val secondary = secondaryImageCapture
        if (primary == null || secondary == null) {
            onFinished(null)
            return
        }
        capture.captureDualPhoto(primary, secondary, onFinished)
    }

    fun startDualRecording(
        capture: CameraCapture,
        withAudio: Boolean,
        onStarted: () -> Unit,
        onFinished: () -> Unit
    ) {
        val primary = videoCapture ?: return
        val secondary = secondaryVideoCapture ?: return
        var primaryFinished = false
        var secondaryFinished = false
        fun finishIfBoth() {
            if (primaryFinished && secondaryFinished) onFinished()
        }
        recording = capture.startVideo(
            videoCapture = primary,
            withAudio = withAudio,
            onStarted = onStarted
        ) {
            primaryFinished = true
            recording = null
            finishIfBoth()
        }
        secondaryRecording = capture.startVideo(
            videoCapture = secondary,
            withAudio = false,
            onStarted = {},
            onFinished = {
            secondaryFinished = true
                secondaryRecording = null
                finishIfBoth()
            }
        )
    }

    fun startRecording(
        capture: CameraCapture,
        withAudio: Boolean,
        transform: VideoTransform = VideoTransform.NONE,
        onStarted: () -> Unit,
        onFinished: () -> Unit
    ) {
        val video = videoCapture ?: return
        recording = capture.startVideo(
            videoCapture = video,
            withAudio = withAudio,
            transform = transform,
            onStarted = onStarted,
            onFinished = {
                recording = null
                onFinished()
            }
        )
    }

    fun stopRecordingIfNeeded() {
        recording?.stop()
        secondaryRecording?.stop()
        recording = null
        secondaryRecording = null
    }

    fun setZoom(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
    }

    fun setSmoothZoom(
        ratio: Float,
        minZoomRatio: Float,
        maxZoomRatio: Float
    ) {
        val cameraControl = camera?.cameraControl ?: return
        if (maxZoomRatio <= minZoomRatio) return
        val clampedRatio = ratio.coerceIn(minZoomRatio, maxZoomRatio)
        if (clampedRatio <= minZoomRatio) {
            cameraControl.setLinearZoom(0f)
            return
        }
        if (clampedRatio >= maxZoomRatio) {
            cameraControl.setLinearZoom(1f)
            return
        }

        val cropAtMin = 1f / minZoomRatio
        val cropAtMax = 1f / maxZoomRatio
        val cropAtRatio = 1f / clampedRatio
        val linearZoom = (
            (cropAtMin - cropAtRatio) /
                (cropAtMin - cropAtMax)
            ).coerceIn(0f, 1f)

        cameraControl.setLinearZoom(linearZoom)
    }

    fun setExposure(index: Int) {
        camera?.cameraControl?.setExposureCompensationIndex(index)
    }

    @androidx.camera.camera2.interop.ExperimentalCamera2Interop
    fun setMacro(enabled: Boolean, minimumFocusDistance: Float) {
        val activeCamera = camera ?: return
        val camera2Control = androidx.camera.camera2.interop.Camera2CameraControl.from(activeCamera.cameraControl)
        val options = androidx.camera.camera2.interop.CaptureRequestOptions.Builder()
        if (enabled && minimumFocusDistance > 0f) {
            options.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF
            )
            options.setCaptureRequestOption(
                CaptureRequest.LENS_FOCUS_DISTANCE,
                minimumFocusDistance
            )
        } else {
            options.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            options.clearCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE)
        }
        camera2Control.setCaptureRequestOptions(options.build())
    }

    @androidx.camera.camera2.interop.ExperimentalCamera2Interop
    fun setExtensionStrength(strength: Int) {
        val activeCamera = camera ?: return
        extensionsManager?.getCameraExtensionsControl(activeCamera.cameraControl)
            ?.setExtensionStrength(strength.coerceIn(0, 100))
    }

    fun setWhiteBalance(mode: Int) {
        val activeCamera = camera ?: return
        val control = androidx.camera.camera2.interop.Camera2CameraControl.from(activeCamera.cameraControl)
        val options = androidx.camera.camera2.interop.CaptureRequestOptions.Builder()
        options.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, mode)
        control.setCaptureRequestOptions(options.build())
    }

    fun setManualFocus(distance: Float?) {
        val activeCamera = camera ?: return
        val control = androidx.camera.camera2.interop.Camera2CameraControl.from(activeCamera.cameraControl)
        val options = androidx.camera.camera2.interop.CaptureRequestOptions.Builder()
        if (distance == null) {
            options.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            options.clearCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE)
        } else {
            options.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            options.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, distance.coerceAtLeast(0f))
        }
        control.setCaptureRequestOptions(options.build())
    }

    @androidx.camera.camera2.interop.ExperimentalCamera2Interop
    fun setManualExposure(iso: Int, exposureTimeNs: Long) {
        val activeCamera = camera ?: return
        val control = androidx.camera.camera2.interop.Camera2CameraControl.from(activeCamera.cameraControl)
        val options = androidx.camera.camera2.interop.CaptureRequestOptions.Builder()
        options.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        options.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
        options.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTimeNs)
        control.setCaptureRequestOptions(options.build())
    }

    fun setLowLightBoost(enabled: Boolean) {
        camera?.cameraControl?.enableLowLightBoostAsync(enabled)
    }

    fun setTorchStrength(level: Int) {
        camera?.cameraControl?.setTorchStrengthLevel(level)
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
        secondaryVideoCapture = null
        secondaryImageCapture = null
    }
}
