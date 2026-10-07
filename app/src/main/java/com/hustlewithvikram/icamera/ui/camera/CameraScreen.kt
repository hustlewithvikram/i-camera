package com.hustlewithvikram.icamera.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Exposure
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Grid3x3
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hustlewithvikram.icamera.camera.CameraCapabilities
import com.hustlewithvikram.icamera.camera.CameraCapture
import com.hustlewithvikram.icamera.camera.CameraController
import com.hustlewithvikram.icamera.camera.PhotoMode
import com.hustlewithvikram.icamera.camera.VideoTransform
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class CaptureMode(
    val label: String,
    val photoMode: PhotoMode? = null
) {
    PHOTO("PHOTO", PhotoMode.PHOTO),
    VIDEO("VIDEO"),
    PORTRAIT("PORTRAIT", PhotoMode.PORTRAIT),
    NIGHT("NIGHT", PhotoMode.NIGHT),
    MACRO("MACRO", PhotoMode.MACRO),
    PRO("PRO"),
    RAW("RAW", PhotoMode.RAW),
    HDR("HDR", PhotoMode.HDR),
    RETOUCH("RETOUCH", PhotoMode.RETOUCH),
    AUTO("AUTO", PhotoMode.AUTO),
    PANORAMA("PANORAMA"),
    DOCUMENT("DOCUMENT"),
    SLOW_MOTION("SLOW MOTION"),
    TIMELAPSE("TIMELAPSE"),
    DUAL("DUAL", null)
}

private enum class FlashMode {
    AUTO,
    ON,
    OFF
}

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { CameraController(context) }
    val capture = remember { CameraCapture(context) }
    val secondaryPreviewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var mode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var previewScale by remember { mutableFloatStateOf(1f) }
    var previewAlpha by remember { mutableFloatStateOf(1f) }
    var showSettings by remember { mutableStateOf(false) }
    var lowLightBoost by remember { mutableStateOf(false) }
    var torchStrength by remember { mutableFloatStateOf(1f) }
    var iso by remember { mutableFloatStateOf(100f) }
    var shutter by remember { mutableFloatStateOf(0.01f) }
    var frontCamera by remember { mutableStateOf(false) }
    var capabilities by remember { mutableStateOf<CameraCapabilities?>(null) }
    var flashMode by remember { mutableStateOf(FlashMode.AUTO) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var ultraWide by remember { mutableStateOf(false) }
    var stableCaptureModes by remember { mutableStateOf<List<CaptureMode>?>(null) }
    var exposure by remember { mutableIntStateOf(0) }
    var isRecording by remember { mutableStateOf(false) }
    var focusPoint by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var lastPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showExposure by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(false) }
    var showModeSheet by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val microphoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && mode.isVideoCaptureMode()) {
            if (mode == CaptureMode.DUAL) {
                controller.startDualRecording(
                    capture = capture,
                    withAudio = true,
                    onStarted = { isRecording = true },
                    onFinished = { isRecording = false }
                )
            } else {
                controller.startRecording(
                    capture = capture,
                    withAudio = true,
                    transform = mode.videoTransform(),
                    onStarted = { isRecording = true },
                    onFinished = { isRecording = false }
                )
            }
        }
    }

    val documentScannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                ?.getPages()
                ?.firstOrNull()
                ?.let { lastPhotoUri = it.imageUri }
        }
        mode = CaptureMode.PHOTO
    }

    fun bindCamera() {
        if (mode == CaptureMode.DUAL) {
            error = null
            controller.bindConcurrent(
                primaryPreviewView = previewView,
                secondaryPreviewView = secondaryPreviewView,
                lifecycleOwner = lifecycleOwner,
                onReady = {
                    capabilities = capabilities?.copy(supportsConcurrentCamera = true)
                },
                onError = { error = "Dual camera is not supported by this device configuration." }
            )
            return
        }
        val mainSelector = if (frontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        val selector = if (!frontCamera && ultraWide) {
            controller.getUltraWideSelector() ?: mainSelector
        } else {
            mainSelector
        }

        error = null
        controller.bind(
            previewView = previewView,
            lifecycleOwner = lifecycleOwner,
            selector = selector,
            videoMode = mode == CaptureMode.VIDEO || mode == CaptureMode.DUAL,
            photoMode = mode.photoMode ?: PhotoMode.PHOTO,
            capabilitySelector = mainSelector,
            onReady = { caps ->
                // Freeze the mode rail from the first successful capability scan for
                // this physical camera. VIDEO must never replace the PHOTO capability
                // set with a different list (which previously made MACRO disappear
                // and PRO appear).
                val modesForCamera = stableCaptureModes ?: availableCaptureModes(caps).also {
                    stableCaptureModes = it
                }
                capabilities = caps.copy(
                    supportedPhotoModes = caps.supportedPhotoModes
                )

                // If switching front/back makes the previous mode unavailable, fall
                // back to PHOTO instead of letting the rail point at a missing item.
                if (mode !in modesForCamera) {
                    mode = CaptureMode.PHOTO
                }

                zoom = if (!frontCamera && ultraWide && caps.supportsUltraWide) {
                    caps.ultraWideZoomRatio
                } else {
                    zoom.coerceIn(1f, caps.maxZoomRatio)
                }
                exposure = exposure.coerceIn(caps.exposureMin, caps.exposureMax)
                iso = iso.coerceIn(caps.isoMin.toFloat(), caps.isoMax.toFloat())
                shutter = shutter.coerceIn(caps.exposureTimeMinNs / 1_000_000_000f, caps.exposureTimeMaxNs / 1_000_000_000f)
                if (!caps.hasFlash) {
                    flashMode = FlashMode.OFF
                }
            },
            onError = {
                error = "This camera configuration is not supported on this device."
            }
        )
    }

    LaunchedEffect(mode) {
        if (mode == CaptureMode.DOCUMENT) {
            val activity = context as? androidx.activity.ComponentActivity
            if (activity == null) {
                mode = CaptureMode.PHOTO
            } else {
                val options = GmsDocumentScannerOptions.Builder()
                    .setGalleryImportAllowed(false)
                    .setPageLimit(12)
                    .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
                    .setScannerMode(SCANNER_MODE_FULL)
                    .build()
                val scanner = GmsDocumentScanning.getClient(options)
                scanner.getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        documentScannerLauncher.launch(
                            IntentSenderRequest.Builder(intentSender).build()
                        )
                    }
                    .addOnFailureListener {
                        error = "Document scanning is unavailable on this device."
                        mode = CaptureMode.PHOTO
                    }
            }
        }
    }

    LaunchedEffect(mode, frontCamera, ultraWide) {
        previewScale = 0.965f
        previewAlpha = 0.72f
        kotlinx.coroutines.delay(45)
        previewScale = 1f
        previewAlpha = 1f
        if (isRecording) {
            controller.stopRecordingIfNeeded()
            isRecording = false
        }
        bindCamera()
    }

    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            kotlinx.coroutines.delay(900)
            focusPoint = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = previewScale
                    scaleY = previewScale
                    alpha = previewAlpha
                }
                .pointerInput(capabilities?.maxZoomRatio) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        val maxZoom = capabilities?.maxZoomRatio ?: 1f
                        val minZoom = if (ultraWide) {
                            capabilities?.hardwareUltraWideRatios?.minOrNull()
                                ?: capabilities?.ultraWideZoomRatio
                                ?: 0.5f
                        } else {
                            1f
                        }
                        if (maxZoom > 1f || ultraWide) {
                            val next = (zoom * zoomChange).coerceIn(minZoom, maxZoom)
                            zoom = next
                            controller.setZoom(if (ultraWide) {
                                val wideRatio = capabilities?.ultraWideZoomRatio ?: 0.5f
                                (next / wideRatio).coerceIn(1f, maxZoom)
                            } else {
                                next
                            })
                        }
                    }
                },
            factory = {
                previewView.setOnTouchListener { view, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        focusPoint = event.x to event.y
                        controller.focusAt(
                            previewView,
                            event.x,
                            event.y
                        )
                    }
                    true
                }
                previewView
            }
        )

        if (mode == CaptureMode.DUAL) {
            AndroidView(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 92.dp, end = 14.dp)
                    .size(width = 128.dp, height = 180.dp)
                    .clip(RoundedCornerShape(20.dp)),
                factory = { secondaryPreviewView }
            )
        }

        if (showGrid) {
            GridOverlay(Modifier.fillMaxSize())
        }

        focusPoint?.let { point ->
            FocusRing(
                x = point.first,
                y = point.second,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            TopControls(
                capabilities = capabilities,
                flashMode = flashMode,
                mode = mode,
                showExposure = showExposure,
                showGrid = showGrid,
                onFlashClick = {
                    flashMode = when (flashMode) {
                        FlashMode.AUTO -> FlashMode.ON
                        FlashMode.ON -> FlashMode.OFF
                        FlashMode.OFF -> FlashMode.AUTO
                    }

                    if (mode.isVideoCaptureMode()) {
                        controller.setTorch(flashMode == FlashMode.ON)
                    } else {
                        controller.imageCapture?.flashMode = flashMode.toImageFlashMode()
                    }
                },
                onExposureClick = { showExposure = !showExposure },
                onGridClick = { showGrid = !showGrid },
                onSettingsClick = { showSettings = !showSettings }
            )

            AnimatedVisibility(
                visible = showSettings,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 3 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 3 })
            ) {
                CameraSettingsPanel(
                    capabilities = capabilities,
                    lowLightBoost = lowLightBoost,
                    onLowLightBoostChange = {
                        lowLightBoost = it
                        controller.setLowLightBoost(it)
                    },
                    torchStrength = torchStrength,
                    onTorchStrengthChange = {
                        torchStrength = it
                        controller.setTorchStrength(it.toInt().coerceAtLeast(1))
                    },
                    proMode = mode == CaptureMode.PRO,
                    iso = iso,
                    onIsoChange = {
                        iso = it
                        controller.setManualExposure(iso.toInt(), (shutter * 1_000_000_000L).toLong())
                    },
                    shutter = shutter,
                    onShutterChange = {
                        shutter = it
                        controller.setManualExposure(iso.toInt(), (shutter * 1_000_000_000L).toLong())
                    }
                )
            }

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(
                visible = showExposure && capabilities?.exposureSupported == true,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
            ) {
                ExposureControl(
                    min = capabilities?.exposureMin ?: -6,
                    max = capabilities?.exposureMax ?: 6,
                    value = exposure,
                    onValueChange = {
                        exposure = it
                        controller.setExposure(it)
                    }
                )
            }

            ZoomControl(
                maxZoom = capabilities?.maxZoomRatio ?: 1f,
                hardwareUltraWideRatios = capabilities?.hardwareUltraWideRatios ?: emptyList(),
                supportsUltraWide = capabilities?.supportsUltraWide == true && !frontCamera,
                ultraWideRatio = capabilities?.ultraWideZoomRatio ?: 0.5f,
                value = zoom,
                onValueChange = { ratio ->
                    val wideRatio = capabilities?.ultraWideZoomRatio ?: 0.5f
                    if (ratio < 1f && capabilities?.supportsUltraWide == true && !frontCamera) {
                        ultraWide = true
                        zoom = ratio.coerceIn(wideRatio, 0.98f)
                    } else {
                        ultraWide = false
                        zoom = ratio.coerceIn(1f, capabilities?.maxZoomRatio ?: 1f)
                        controller.setSmoothZoom(
                            ratio = zoom,
                            minZoomRatio = 1f,
                            maxZoomRatio = capabilities?.maxZoomRatio ?: 1f
                        )
                    }
                }
            )

            Spacer(Modifier.size(6.dp))

            BottomControls(
                mode = mode,
                canVideo = capabilities?.hasVideo == true,
                hasFrontCamera = capabilities?.hasFrontCamera == true,
                isRecording = isRecording,
                lastPhotoUri = lastPhotoUri,
                onModeChange = { nextMode ->
                    if (!isRecording && (nextMode != CaptureMode.DUAL || capabilities?.supportsConcurrentCamera == true)) {
                        mode = nextMode
                    }
                },
                onCapture = {
                    if (mode == CaptureMode.DUAL) {
                        if (isRecording) {
                            controller.stopRecordingIfNeeded()
                            isRecording = false
                        } else {
                            controller.captureDualPhoto(capture) { uri ->
                                if (uri != null) lastPhotoUri = uri
                            }
                        }
                    } else if (mode == CaptureMode.PANORAMA) {
                        controller.imageCapture?.let { image ->
                            image.flashMode = ImageCapture.FLASH_MODE_OFF
                            capture.capturePanorama(image) { uri ->
                                if (uri != null) lastPhotoUri = uri
                            }
                        }
                    } else if (mode.isVideoCaptureMode()) {
                        if (isRecording) {
                            controller.stopRecordingIfNeeded()
                            isRecording = false
                        } else {
                            val micGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (micGranted) {
                                if (mode == CaptureMode.DUAL) {
                                    controller.startDualRecording(
                                        capture = capture,
                                        withAudio = true,
                                        onStarted = { isRecording = true },
                                        onFinished = { isRecording = false }
                                    )
                                } else controller.startRecording(
                                    capture = capture,
                                    withAudio = true,
                                    transform = mode.videoTransform(),
                                    onStarted = { isRecording = true },
                                    onFinished = { isRecording = false }
                                )
                            } else {
                                microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    } else if (mode != CaptureMode.DOCUMENT) {
                        controller.imageCapture?.let { image ->
                            image.flashMode = flashMode.toImageFlashMode()
                            capture.capture(image) { uri ->
                                if (uri != null) lastPhotoUri = uri
                            }
                        }
                    }
                },
                onLongCapture = {
                    if (mode == CaptureMode.DUAL && !isRecording) {
                        val micGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (micGranted) {
                            controller.startDualRecording(
                                capture = capture,
                                withAudio = true,
                                onStarted = { isRecording = true },
                                onFinished = { isRecording = false }
                            )
                        } else {
                            microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                onSwitchCamera = {
                    if (!isRecording) {
                        frontCamera = !frontCamera
                        ultraWide = false
                        stableCaptureModes = null
                        flashMode = FlashMode.AUTO
                    }
                }
            )

            Spacer(Modifier.size(6.dp))

            val captureModes = stableCaptureModes
                ?: availableCaptureModes(capabilities)

            CameraModeRail(
                selected = mode,
                modes = captureModes,
                onSelected = { next ->
                    if (!isRecording) mode = next
                },
                onShowAllModes = {
                    if (!isRecording) showModeSheet = true
                }
            )


        }

        ModePickerSheet(
            visible = showModeSheet,
            selected = mode,
            modes = stableCaptureModes ?: availableCaptureModes(capabilities),
            onDismiss = { showModeSheet = false },
            onSelected = { next ->
                if (!isRecording) {
                    mode = next
                    showModeSheet = false
                }
            }
        )

        error?.let { message ->
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xCC171717)
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(20.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

private fun CaptureMode.videoTransform(): VideoTransform = when (this) {
    CaptureMode.SLOW_MOTION -> VideoTransform.SLOW_MOTION
    CaptureMode.TIMELAPSE -> VideoTransform.TIMELAPSE
    else -> VideoTransform.NONE
}

private fun CaptureMode.isVideoCaptureMode(): Boolean = when (this) {
    CaptureMode.VIDEO,
    CaptureMode.SLOW_MOTION,
    CaptureMode.TIMELAPSE,
    CaptureMode.DUAL -> true
    else -> false
}

private fun FlashMode.toImageFlashMode(): Int = when (this) {
    FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
    FlashMode.ON -> ImageCapture.FLASH_MODE_ON
    FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
}

@Composable
private fun TopControls(
    capabilities: CameraCapabilities?,
    flashMode: FlashMode,
    mode: CaptureMode,
    showExposure: Boolean,
    showGrid: Boolean,
    onFlashClick: () -> Unit,
    onExposureClick: () -> Unit,
    onGridClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (capabilities?.supportsUltraHdr == true) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0x55000000)
            ) {
                Text(
                    "HDR",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        } else {
            Spacer(Modifier.size(1.dp))
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0x660F0F0F),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (capabilities?.hasFlash == true) {
                    CameraIconButton(
                        onClick = onFlashClick,
                        selected = flashMode != FlashMode.OFF,
                        size = 40.dp
                    ) {
                        Icon(
                            imageVector = when {
                                mode.isVideoCaptureMode() && flashMode == FlashMode.ON -> Icons.Rounded.FlashOn
                                mode.isVideoCaptureMode() -> Icons.Rounded.FlashOff
                                flashMode == FlashMode.AUTO -> Icons.Rounded.FlashAuto
                                flashMode == FlashMode.ON -> Icons.Rounded.FlashOn
                                else -> Icons.Rounded.FlashOff
                            },
                            contentDescription = "Flash",
                            tint = Color.White
                        )
                    }
                }

                if (capabilities?.exposureSupported == true) {
                    CameraIconButton(
                        onClick = onExposureClick,
                        selected = showExposure,
                        size = 40.dp
                    ) {
                        Icon(Icons.Rounded.Exposure, contentDescription = "Exposure", tint = Color.White)
                    }
                }

                CameraIconButton(
                    onClick = onGridClick,
                    selected = showGrid,
                    size = 40.dp
                ) {
                    Icon(Icons.Rounded.Grid3x3, contentDescription = "Grid", tint = Color.White)
                }

                CameraIconButton(
                    onClick = onSettingsClick,
                    selected = false,
                    size = 40.dp
                ) {
                    Text("•••", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun PhotoModeSelector(
    modes: List<PhotoMode>,
    selected: PhotoMode,
    onSelected: (PhotoMode) -> Unit
) {
    AnimatedContent(
        targetState = modes,
        transitionSpec = {
            (fadeIn() + slideInVertically(initialOffsetY = { it / 3 })) togetherWith
                (fadeOut() + slideOutVertically(targetOffsetY = { -it / 3 }))
        },
        label = "photo_modes"
    ) { available ->
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
        ) {
            items(available, key = { it.name }) { item ->
                val active = item == selected
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(18.dp)).clickable { onSelected(item) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (active) Color(0xD9FFFFFF) else Color(0x66111111)
                ) {
                    Text(item.label, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp), color = if (active) Color.Black else Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
private fun linearZoomFromRatio(
    ratio: Float,
    minZoomRatio: Float,
    maxZoomRatio: Float
): Float {
    if (maxZoomRatio <= minZoomRatio) return 0f
    if (ratio <= minZoomRatio) return 0f
    if (ratio >= maxZoomRatio) return 1f
    val cropAtMin = 1f / minZoomRatio
    val cropAtMax = 1f / maxZoomRatio
    val cropAtRatio = 1f / ratio
    return ((cropAtMin - cropAtRatio) / (cropAtMin - cropAtMax)).coerceIn(0f, 1f)
}

private fun zoomRatioFromLinearZoom(
    linearZoom: Float,
    minZoomRatio: Float,
    maxZoomRatio: Float
): Float {
    if (maxZoomRatio <= minZoomRatio) return minZoomRatio
    val progress = linearZoom.coerceIn(0f, 1f)
    if (progress <= 0f) return minZoomRatio
    if (progress >= 1f) return maxZoomRatio
    val cropAtMin = 1f / minZoomRatio
    val cropAtMax = 1f / maxZoomRatio
    val crop = cropAtMin + (cropAtMax - cropAtMin) * progress
    return (1f / crop).coerceIn(minZoomRatio, maxZoomRatio)
}

@Composable
private fun ZoomControl(
    maxZoom: Float,
    hardwareUltraWideRatios: List<Float>,
    supportsUltraWide: Boolean,
    ultraWideRatio: Float,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    if (maxZoom <= 1.01f && !supportsUltraWide) return

    val scrubMax = maxZoom.coerceAtLeast(1f)
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    fun formatZoom(ratio: Float): String {
        val rounded = (ratio * 10f).roundToInt() / 10f
        return if (abs(rounded - rounded.toInt()) < 0.001f) {
            rounded.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", rounded)
        }
    }

    // Idle chooser order is deliberately stable:
    // detected sub-1x lenses -> 1x -> 2x -> 3x -> exact hardware max.
    // No fake zoom-out or fake maximum is shown.
    val idleStops = remember(
        maxZoom,
        hardwareUltraWideRatios,
        supportsUltraWide,
        ultraWideRatio
    ) {
        buildList {
            if (supportsUltraWide) {
                hardwareUltraWideRatios
                    .filter { it > 0.05f && it < 1f }
                    .sorted()
                    .forEach { add(it) }

                if (isEmpty()) {
                    add(ultraWideRatio.coerceIn(0.1f, 0.98f))
                }
            }

            add(1f)

            if (scrubMax >= 2f) add(2f)
            if (scrubMax >= 3f) add(3f)

            // Keep the familiar 1x / 2x / 3x rail. The final stop is
            // always the exact maximum reported by CameraX.
            if (scrubMax > 3.05f) add(scrubMax)
            else if (scrubMax > 1.05f && scrubMax < 3f) add(scrubMax)
        }
            .filter { it >= 0.05f && it <= scrubMax + 0.01f }
            .distinctBy { (it * 20f).roundToInt() }
            .sorted()
    }

    // The precise wheel uses 0.1x detents, plus the exact hardware maximum.
    val scrubMin = if (supportsUltraWide) {
        hardwareUltraWideRatios
            .minOrNull()
            ?.coerceIn(0.1f, 0.99f)
            ?: ultraWideRatio.coerceIn(0.1f, 0.99f)
    } else {
        1f
    }

    val scrubStops = remember(scrubMin, scrubMax) {
        buildList {
            var ratio = (scrubMin * 10f).roundToInt() / 10f
            while (ratio < scrubMax - 0.051f) {
                add(ratio)
                ratio += 0.1f
            }
            add(scrubMax)
        }.distinctBy { (it * 10f).roundToInt() }
    }

    var scrubbing by remember { mutableStateOf(false) }
    var scrubZoom by remember {
        mutableFloatStateOf(value.coerceIn(scrubMin, scrubMax))
    }

    LaunchedEffect(value, scrubbing, scrubMax) {
        if (!scrubbing) {
            scrubZoom = value.coerceIn(scrubMin, scrubMax)
        }
    }

    fun applyZoom(next: Float) {
        val clamped = next.coerceIn(scrubMin, scrubMax)
        scrubZoom = clamped
        onValueChange(clamped)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(bottom = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        val presetSlot = 48.dp
        val presetSlotPx = with(density) { presetSlot.toPx() }
        val presetStartPx = with(density) {
            ((maxWidth - (presetSlot * idleStops.size)) / 2f)
                .coerceAtLeast(0.dp)
                .toPx()
        }

        // One gesture recognizer owns the complete zoom chooser. This is
        // intentional: a tap selects a preset; a long press transforms that
        // exact preset into the iPhone-style precision wheel, and the same
        // finger continues directly into the drag.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(idleStops, scrubMax) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val presetIndex = (
                            (down.position.x - presetStartPx) / presetSlotPx
                        ).toInt().coerceIn(0, idleStops.lastIndex)

                        val longPress = awaitLongPressOrCancellation(down.id)

                        if (longPress == null) {
                            if (!scrubbing && presetIndex in idleStops.indices) {
                                applyZoom(idleStops[presetIndex])
                            }
                        } else {
                            scrubbing = true
                            val startingZoom = idleStops[presetIndex]
                                .coerceIn(scrubMin, scrubMax)
                            val startX = longPress.position.x
                            scrubZoom = startingZoom
                            applyZoom(startingZoom)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: event.changes.firstOrNull()
                                    if (change == null) break

                                    // The finger position is mapped to the complete
                                    // available zoom range from the exact press point.
                                    // This removes the old fixed "1/52x per pixel"
                                    // sensitivity, which could never reach the real
                                    // maximum on a normal-width screen. It also makes
                                    // every 0.1x detent equally predictable.
                                    val x = change.position.x.coerceIn(0f, size.width.toFloat())
                                    // Map the finger to CameraX's linear/FOV zoom space.
                                    val startLinear = linearZoomFromRatio(
                                        ratio = startingZoom,
                                        minZoomRatio = scrubMin,
                                        maxZoomRatio = scrubMax
                                    )
                                    val nextLinear = if (x >= startX) {
                                        val travel = (size.width - startX).coerceAtLeast(1f)
                                        startLinear +
                                            ((x - startX) / travel) * (1f - startLinear)
                                    } else {
                                        val travel = startX.coerceAtLeast(1f)
                                        startLinear -
                                            ((startX - x) / travel) * startLinear
                                    }
                                    val next = zoomRatioFromLinearZoom(
                                        linearZoom = nextLinear,
                                        minZoomRatio = scrubMin,
                                        maxZoomRatio = scrubMax
                                    )

                                    if (abs(x - startX) > 0f) {
                                        change.consume()
                                        scrubZoom = next.coerceIn(scrubMin, scrubMax)
                                        onValueChange(scrubZoom)
                                    }

                                    if (!change.pressed) break
                                }
                            } finally {
                                scrubbing = false
                            }
                        }
                    }
                }
        ) {
            AnimatedContent(
                targetState = scrubbing,
                transitionSpec = {
                    fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(130)
                    ) togetherWith fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(90)
                    )
                },
                label = "iphoneZoomChooser"
            ) { expanded ->
                if (!expanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        idleStops.forEach { ratio ->
                            val active = abs(value - ratio) < 0.08f
                            Box(
                                modifier = Modifier
                                    .size(presetSlot)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (active) {
                                    Surface(
                                        modifier = Modifier.size(42.dp),
                                        shape = CircleShape,
                                        color = Color(0x995C5B45)
                                    ) {}
                                }

                                Text(
                                    text = formatZoom(ratio) + "x",
                                    color = if (active) {
                                        Color(0xFFFFD60A)
                                    } else {
                                        Color.White.copy(alpha = 0.92f)
                                    },
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                } else {
                    ZoomPrecisionWheel(
                        scrubZoom = scrubZoom,
                        scrubMax = scrubMax,
                        stops = scrubStops,
                        formatZoom = ::formatZoom
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomPrecisionWheel(
    scrubZoom: Float,
    scrubMax: Float,
    stops: List<Float>,
    formatZoom: (Float) -> String
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val itemWidth = 52.dp
    val density = LocalDensity.current

    val activeIndex = remember(scrubZoom, stops) {
        stops.indices.minByOrNull { index ->
            abs(stops[index] - scrubZoom)
        } ?: 0
    }

    LaunchedEffect(activeIndex) {
        if (stops.isNotEmpty()) {
            listState.scrollToItem(activeIndex)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
    ) {
        val sidePadding = (maxWidth / 2f - itemWidth / 2f).coerceAtLeast(0.dp)

        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = sidePadding
            )
        ) {
            items(stops.size, key = { index -> index }) { index ->
                val stop = stops[index]
                val distance by remember(index) {
                    derivedStateOf {
                        val info = listState.layoutInfo.visibleItemsInfo
                            .firstOrNull { it.index == index }
                        if (info == null) {
                            99f
                        } else {
                            val center = (
                                listState.layoutInfo.viewportStartOffset +
                                    listState.layoutInfo.viewportEndOffset
                            ) / 2f
                            abs((info.offset + info.size / 2f) - center) /
                                with(density) { itemWidth.toPx() }
                        }
                    }
                }

                val normalized = (1f - distance / 3f).coerceIn(0f, 1f)
                val active = index == activeIndex

                Box(
                    modifier = Modifier
                        .size(itemWidth, 70.dp)
                        .graphicsLayer {
                            scaleX = 0.78f + normalized * 0.22f
                            scaleY = 0.78f + normalized * 0.22f
                            alpha = 0.22f + normalized * 0.78f
                            translationY = distance.coerceAtMost(3f) * 4f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(if (active) 2.dp else 1.dp, if (active) 15.dp else 8.dp)
                        ) {
                            drawRoundRect(
                                color = if (active) {
                                    Color(0xFFFFD60A)
                                } else {
                                    Color.White.copy(alpha = 0.55f)
                                },
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                    size.minDimension / 2f
                                )
                            )
                        }

                        Text(
                            text = formatZoom(stop) + "x",
                            color = if (active) {
                                Color(0xFFFFD60A)
                            } else {
                                Color.White.copy(alpha = 0.74f)
                            },
                            style = if (active) {
                                MaterialTheme.typography.labelLarge
                            } else {
                                MaterialTheme.typography.labelSmall
                            },
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Small center marker: the zoom value under the finger is the selected
        // value, rather than a moving scrollbar thumb.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 2.dp)
                .size(width = 22.dp, height = 2.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFFFD60A))
        )
    }
}

@Composable
private fun ExposureControl(
    min: Int,
    max: Int,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xB8000000)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Exposure", color = Color.White)
                Text(
                    String.format(Locale.US, "%+.1f", value / 3f),
                    color = Color.White
                )
            }

            Slider(
                value = value.toFloat(),
                onValueChange = {
                    onValueChange(it.roundToInt().coerceIn(min, max))
                },
                valueRange = min.toFloat()..max.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun availableCaptureModes(capabilities: CameraCapabilities?): List<CaptureMode> {
    if (capabilities == null) return listOf(CaptureMode.PHOTO)
    return buildList {
        add(CaptureMode.PHOTO)
        if (capabilities.hasVideo) add(CaptureMode.VIDEO)
        if (capabilities.supportedPhotoModes.contains(PhotoMode.PORTRAIT)) add(CaptureMode.PORTRAIT)
        if (capabilities.supportedPhotoModes.contains(PhotoMode.NIGHT)) add(CaptureMode.NIGHT)
        if (capabilities.supportedPhotoModes.contains(PhotoMode.MACRO)) add(CaptureMode.MACRO)
        if (capabilities.isoMax > capabilities.isoMin &&
            capabilities.exposureTimeMaxNs > capabilities.exposureTimeMinNs
        ) add(CaptureMode.PRO)
        if (capabilities.supportedPhotoModes.contains(PhotoMode.RAW)) add(CaptureMode.RAW)
        if (capabilities.supportedPhotoModes.contains(PhotoMode.HDR)) add(CaptureMode.HDR)
        if (capabilities.supportedPhotoModes.contains(PhotoMode.RETOUCH)) add(CaptureMode.RETOUCH)
        if (capabilities.supportedPhotoModes.contains(PhotoMode.AUTO)) add(CaptureMode.AUTO)
        if (capabilities.supportsPanorama) add(CaptureMode.PANORAMA)
        if (capabilities.supportsDocumentScan) add(CaptureMode.DOCUMENT)
        if (capabilities.supportsSlowMotion) add(CaptureMode.SLOW_MOTION)
        if (capabilities.supportsTimelapse) add(CaptureMode.TIMELAPSE)
        if (capabilities.supportsDualPhotoVideo && capabilities.hasFrontCamera) add(CaptureMode.DUAL)
    }
}

@Composable
private fun CameraModeRail(
    selected: CaptureMode,
    modes: List<CaptureMode>,
    onSelected: (CaptureMode) -> Unit,
    onShowAllModes: () -> Unit
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // This flag is set before a programmatic selection changes the parent state.
    // That prevents the settled-scroll observer from immediately selecting the
    // mode that happened to be under the center while our own animation is running.
    var programmaticScroll by remember { mutableStateOf(false) }
    val latestSelected = rememberUpdatedState(selected)
    val latestOnSelected = rememberUpdatedState(onSelected)

    val flingBehavior = rememberSnapFlingBehavior(
        lazyListState = listState,
        snapPosition = SnapPosition.Center
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 8.dp)
    ) {
        val itemWidth = 74.dp
        val sidePadding = (maxWidth / 2f).coerceAtLeast(0.dp)

        fun centeredIndex(): Int? {
            val visible = listState.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) return null

            val viewportCenter = (
                listState.layoutInfo.viewportStartOffset +
                    listState.layoutInfo.viewportEndOffset
                ) / 2f

            return visible.minByOrNull { item ->
                abs((item.offset + item.size / 2f) - viewportCenter)
            }?.index
        }

        suspend fun centerMode(index: Int) {
            if (index !in modes.indices) return

            // First make the target item visible. Then measure its real position
            // and animate by the exact delta required to put it at viewport center.
            listState.animateScrollToItem(index)

            val item = listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == index }
                ?: return

            val viewportCenter = (
                listState.layoutInfo.viewportStartOffset +
                    listState.layoutInfo.viewportEndOffset
                ) / 2f
            val itemCenter = item.offset + item.size / 2f
            val correction = itemCenter - viewportCenter

            if (abs(correction) > 0.5f) {
                listState.animateScrollBy(correction)
            }
        }

        // Initial layout and capability changes must place the externally selected
        // mode in the center. If the mode is already centered, do nothing.
        LaunchedEffect(modes, selected) {
            if (programmaticScroll) return@LaunchedEffect

            val index = modes.indexOf(selected)
            if (index < 0) return@LaunchedEffect

            if (centeredIndex() != index) {
                programmaticScroll = true
                try {
                    centerMode(index)
                } finally {
                    programmaticScroll = false
                }
            }
        }

        // A mode changes because of a horizontal user gesture only after the rail
        // has completely settled. Never infer selection during an in-flight scroll.
        LaunchedEffect(listState, modes) {
            androidx.compose.runtime.snapshotFlow {
                if (listState.isScrollInProgress) {
                    null
                } else {
                    centeredIndex()
                }
            }.collect { index ->
                if (
                    index != null &&
                    index in modes.indices &&
                    !programmaticScroll &&
                    modes[index] != latestSelected.value
                ) {
                    latestOnSelected.value(modes[index])
                }
            }
        }

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            flingBehavior = flingBehavior,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = sidePadding
            )
        ) {
            items(modes, key = { it.name }) { item ->
                val itemIndex = modes.indexOf(item)
                val active = item == selected
                var verticalDrag by remember { mutableFloatStateOf(0f) }

                val progress by remember(itemIndex, modes) {
                    derivedStateOf {
                        val info = listState.layoutInfo.visibleItemsInfo
                            .firstOrNull { it.index == itemIndex }
                        val viewportCenter = (
                            listState.layoutInfo.viewportStartOffset +
                                listState.layoutInfo.viewportEndOffset
                            ) / 2f
                        val itemCenter = info?.let {
                            it.offset + it.size / 2f
                        } ?: viewportCenter

                        (
                            1f - (
                                abs(itemCenter - viewportCenter) /
                                    with(density) { itemWidth.toPx() * 2.5f }
                                )
                            ).coerceIn(0f, 1f)
                    }
                }

                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .wrapContentWidth()
                        .graphicsLayer {
                            val p = if (active) 1f else progress
                            scaleX = 0.84f + p * 0.16f
                            scaleY = 0.84f + p * 0.16f
                            alpha = 0.28f + p * 0.72f
                        }
                        // Only the active item listens for the upward mode-picker
                        // gesture. Inactive items leave all horizontal touch handling
                        // to LazyRow, so swiping the rail cannot be stolen by them.
                        .then(
                            if (active) {
                                Modifier.pointerInput(item) {
                                    detectVerticalDragGestures(
                                        onVerticalDrag = { _, dragAmount ->
                                            verticalDrag += dragAmount
                                        },
                                        onDragEnd = {
                                            if (verticalDrag < -36f) {
                                                onShowAllModes()
                                            }
                                            verticalDrag = 0f
                                        },
                                        onDragCancel = {
                                            verticalDrag = 0f
                                        }
                                    )
                                }
                            } else {
                                Modifier
                            }
                        )
                        .clip(RoundedCornerShape(19.dp))
                        .then(
                            if (active) {
                                Modifier
                                    .background(
                                        Color(0x661F1F1F),
                                        RoundedCornerShape(19.dp)
                                    )
                                    .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(19.dp)
                                    )
                            } else {
                                Modifier
                            }
                        )
                        .clickable {
                            if (active) {
                                onShowAllModes()
                            } else {
                                // Mark this as programmatic BEFORE changing the parent
                                // selection. This closes the race that caused taps to
                                // jump back to the previous/under-center mode.
                                programmaticScroll = true
                                onSelected(item)

                                scope.launch {
                                    try {
                                        centerMode(itemIndex)
                                    } finally {
                                        programmaticScroll = false
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.label,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp),
                        color = if (active) {
                            Color(0xFFFFD60A)
                        } else {
                            Color.White.copy(alpha = 0.78f)
                        },
                        style = if (active) {
                            MaterialTheme.typography.labelLarge
                        } else {
                            MaterialTheme.typography.labelMedium
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModePickerSheet(
    visible: Boolean,
    selected: CaptureMode,
    modes: List<CaptureMode>,
    onDismiss: () -> Unit,
    onSelected: (CaptureMode) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMediumLow
            )
        ),
        exit = fadeOut() + slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(
                dampingRatio = 0.9f,
                stiffness = Spring.StiffnessMedium
            )
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .clickable { },
                shape = RoundedCornerShape(
                    topStart = 30.dp,
                    topEnd = 30.dp,
                    bottomStart = 26.dp,
                    bottomEnd = 26.dp
                ),
                color = Color(0xD91A1A1A),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 38.dp, height = 4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.28f))
                    )

                    Spacer(Modifier.size(12.dp))

                    Text(
                        text = "Camera modes",
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelMedium
                    )

                    Spacer(Modifier.size(14.dp))

                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        userScrollEnabled = false
                    ) {
                        items(modes.size) { index ->
                            val item = modes[index]
                            val active = item == selected
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .size(width = 1.dp, height = 58.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onSelected(item) },
                                shape = RoundedCornerShape(18.dp),
                                color = if (active) Color(0xE6FFFFFF) else Color(0x33111111),
                                border = if (active) {
                                    androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Color.White.copy(alpha = 0.72f)
                                    )
                                } else null
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = item.label,
                                        color = if (active) Color.Black else Color.White,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.size(6.dp))
                }
            }
        }
    }
}

@Composable
private fun BottomControls(
    mode: CaptureMode,
    canVideo: Boolean,
    hasFrontCamera: Boolean,
    isRecording: Boolean,
    lastPhotoUri: Uri?,
    onModeChange: (CaptureMode) -> Unit,
    onCapture: () -> Unit,
    onLongCapture: () -> Unit,
    onSwitchCamera: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LatestThumbnail(lastPhotoUri)

        ShutterButton(
            isRecording = isRecording,
            onClick = onCapture,
            onLongClick = onLongCapture
        )

        if (hasFrontCamera) {
            CameraIconButton(
                onClick = onSwitchCamera,
                selected = false,
                size = 52.dp
            ) {
                Icon(
                    Icons.Rounded.Cameraswitch,
                    contentDescription = "Switch camera",
                    tint = Color.White
                )
            }
        } else {
            Spacer(Modifier.size(52.dp))
        }
    }
}

@Composable
private fun ModeLabel(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = if (selected) Color(0xFFFFC107) else Color.White,
        style = MaterialTheme.typography.labelLarge
    )
}

@Composable
private fun CameraIconButton(
    onClick: () -> Unit,
    selected: Boolean,
    size: Dp = 48.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) Color(0x99FFFFFF) else Color(0x66111111)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun ShutterButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .size(82.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .combinedClickable(
                onLongClick = onLongClick
            ) {
                scope.launch {
                    scale.animateTo(
                        0.88f,
                        spring(stiffness = Spring.StiffnessMedium)
                    )
                    scale.animateTo(
                        1f,
                        spring(stiffness = Spring.StiffnessLow)
                    )
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(82.dp),
            shape = CircleShape,
            color = Color.White
        ) {}

        Surface(
            modifier = Modifier
                .size(if (isRecording) 38.dp else 68.dp),
            shape = if (isRecording) RoundedCornerShape(9.dp) else CircleShape,
            color = if (isRecording) Color(0xFFE53935) else Color.Transparent
        ) {}
    }
}

@Composable
private fun LatestThumbnail(uri: Uri?) {
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    LaunchedEffect(uri) {
        bitmap = uri?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.loadThumbnail(
                            it,
                            android.util.Size(120, 120),
                            null
                        )
                    }.getOrNull()
                }
            } else {
                null
            }
        }
    }

    Surface(
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        color = Color(0x66111111)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Latest photo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.PhotoLibrary,
                    contentDescription = "Photos",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun GridOverlay(modifier: Modifier) {
    Canvas(modifier) {
        val thirdW = size.width / 3f
        val thirdH = size.height / 3f
        val lineColor = Color.White.copy(alpha = 0.22f)

        drawLine(lineColor, Offset(thirdW, 0f), Offset(thirdW, size.height))
        drawLine(lineColor, Offset(thirdW * 2f, 0f), Offset(thirdW * 2f, size.height))
        drawLine(lineColor, Offset(0f, thirdH), Offset(size.width, thirdH))
        drawLine(lineColor, Offset(0f, thirdH * 2f), Offset(size.width, thirdH * 2f))
    }
}

@Composable
private fun FocusRing(
    x: Float,
    y: Float,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (x - 32f).roundToInt(),
                        (y - 32f).roundToInt()
                    )
                }
                .size(64.dp)
        ) {
            drawCircle(
                color = Color(0xFFFFC107),
                radius = size.minDimension / 2f - 2.dp.toPx(),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
