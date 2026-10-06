package com.hustlewithvikram.icamera.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
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
        if (granted && mode == CaptureMode.VIDEO) {
            controller.startRecording(
                capture = capture,
                withAudio = true,
                onStarted = { isRecording = true },
                onFinished = { isRecording = false }
            )
        }
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
                        val minZoom = if (ultraWide) 0.5f else 1f
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

                    if (mode == CaptureMode.VIDEO) {
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
                supportsUltraWide = capabilities?.supportsUltraWide == true && !frontCamera,
                ultraWideRatio = capabilities?.ultraWideZoomRatio ?: 0.5f,
                value = zoom,
                onValueChange = { ratio ->
                    if (ratio < 1f) {
                        ultraWide = true
                        zoom = 0.5f
                    } else {
                        ultraWide = false
                        zoom = ratio
                    }
                }
            )

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
                    if (mode != CaptureMode.VIDEO && mode != CaptureMode.DUAL) {
                        controller.imageCapture?.let { image ->
                            image.flashMode = flashMode.toImageFlashMode()
                            capture.capture(image) { uri ->
                                if (uri != null) lastPhotoUri = uri
                            }
                        }
                    } else {
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
                                    onStarted = { isRecording = true },
                                    onFinished = { isRecording = false }
                                )
                            } else {
                                microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
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
                                mode == CaptureMode.VIDEO && flashMode == FlashMode.ON -> Icons.Rounded.FlashOn
                                mode == CaptureMode.VIDEO -> Icons.Rounded.FlashOff
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
@Composable
private fun ZoomControl(
    maxZoom: Float,
    supportsUltraWide: Boolean,
    ultraWideRatio: Float,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    if (maxZoom <= 1.01f && !supportsUltraWide) return

    val options = buildList {
        if (supportsUltraWide) add(ultraWideRatio.coerceIn(0.5f, 0.8f))
        add(1f)
        if (maxZoom >= 2f) add(2f)
        if (maxZoom >= 3f) add(3f)
    }.distinct()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { ratio ->
            val active = abs(value - ratio) < 0.08f
            Box(
                modifier = Modifier
                    .size(if (active) 44.dp else 38.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) Color(0x995C5B45) else Color.Transparent
                    )
                    .clickable { onValueChange(ratio) },
                contentAlignment = Alignment.Center
            ) {
                val label = if (ratio < 1f) {
                    String.format(Locale.US, "%.1f", ratio)
                } else if (ratio % 1f == 0f) {
                    ratio.toInt().toString()
                } else {
                    String.format(Locale.US, "%.1f", ratio)
                }
                Text(
                    text = label + "×",
                    color = if (active) Color(0xFFFFD60A) else Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
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
        if (capabilities.supportsConcurrentCamera && capabilities.hasFrontCamera) add(CaptureMode.DUAL)
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
        val sidePadding = ((maxWidth - itemWidth) / 2f).coerceAtLeast(0.dp)

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
                        .size(width = itemWidth, height = 38.dp)
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
                    if (active) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(19.dp),
                            color = Color(0x661F1F1F),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.08f)
                            )
                        ) {}
                    }

                    Text(
                        text = item.label,
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
            onClick = onCapture
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
    onClick: () -> Unit
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
            .clickable {
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
