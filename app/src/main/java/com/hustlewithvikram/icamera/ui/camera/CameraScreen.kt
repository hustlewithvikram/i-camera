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
        val selector = if (frontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        error = null
        controller.bind(
            previewView = previewView,
            lifecycleOwner = lifecycleOwner,
            selector = selector,
            videoMode = mode == CaptureMode.VIDEO || mode == CaptureMode.DUAL,
            photoMode = mode.photoMode ?: PhotoMode.PHOTO,
            onReady = { caps ->
                capabilities = caps
                zoom = zoom.coerceIn(1f, caps.maxZoomRatio)
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

    LaunchedEffect(mode, frontCamera) {
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
                        if (maxZoom > 1f) {
                            val next = (zoom * zoomChange).coerceIn(1f, maxZoom)
                            zoom = next
                            controller.setZoom(next)
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
            Box(
                modifier = Modifier.fillMaxWidth().padding(end = 10.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                CameraIconButton(
                    onClick = { showSettings = !showSettings },
                    selected = showSettings,
                    size = 44.dp
                ) {
                    Text("...", color = Color.White)
                }
            }

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
                onGridClick = { showGrid = !showGrid }
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
                value = zoom,
                onValueChange = {
                    zoom = it
                    controller.setZoom(it)
                }
            )

            CameraModeRail(
                selected = mode,
                modes = availableCaptureModes(capabilities),
                onSelected = { next ->
                    if (!isRecording) mode = next
                },
                onShowAllModes = {
                    if (!isRecording) showModeSheet = true
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
                        flashMode = FlashMode.AUTO
                    }
                }
            )
        }

        ModePickerSheet(
            visible = showModeSheet,
            selected = mode,
            modes = availableCaptureModes(capabilities),
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
    onGridClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (capabilities?.hasFlash == true) {
            CameraIconButton(
                onClick = onFlashClick,
                selected = flashMode != FlashMode.OFF
            ) {
                Icon(
                    imageVector = if (mode == CaptureMode.VIDEO) {
                        if (flashMode == FlashMode.ON) Icons.Rounded.FlashOn
                        else Icons.Rounded.FlashOff
                    } else {
                        when (flashMode) {
                            FlashMode.AUTO -> Icons.Rounded.FlashAuto
                            FlashMode.ON -> Icons.Rounded.FlashOn
                            FlashMode.OFF -> Icons.Rounded.FlashOff
                        }
                    },
                    contentDescription = "Flash"
                )
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (capabilities?.exposureSupported == true) {
                CameraIconButton(
                    onClick = onExposureClick,
                    selected = showExposure
                ) {
                    Icon(Icons.Rounded.Exposure, contentDescription = "Exposure")
                }
            }

            CameraIconButton(
                onClick = onGridClick,
                selected = showGrid
            ) {
                Icon(Icons.Rounded.Grid3x3, contentDescription = "Grid")
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
    value: Float,
    onValueChange: (Float) -> Unit
) {
    if (maxZoom <= 1.01f) return

    val options = buildList {
        add(1f)
        if (maxZoom >= 2f) add(2f)
        if (maxZoom >= 3f) add(3f)
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        items(options) { ratio ->
            Surface(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange(ratio) },
                shape = CircleShape,
                color = if (abs(value - ratio) < 0.08f) {
                    Color(0xAAFFFFFF)
                } else {
                    Color(0x66111111)
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val label = if (ratio % 1f == 0f) {
                        ratio.toInt().toString() + "×"
                    } else {
                        String.format(Locale.US, "%.1f×", ratio)
                    }
                    Text(
                        label,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
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
    val flingBehavior = rememberSnapFlingBehavior(
        lazyListState = listState,
        snapPosition = SnapPosition.Center
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        val sidePadding = ((maxWidth - 92.dp) / 2f).coerceAtLeast(0.dp)

        LaunchedEffect(selected, modes) {
            val index = modes.indexOf(selected)
            if (index >= 0) {
                val centerOffsetPx = with(density) {
                    ((maxWidth.toPx() - 92.dp.toPx()) / 2f).roundToInt()
                }
                listState.animateScrollToItem(index, scrollOffset = -centerOffsetPx)
            }
        }

        LaunchedEffect(listState, modes) {
            androidx.compose.runtime.snapshotFlow {
                val visible = listState.layoutInfo.visibleItemsInfo
                val center = (listState.layoutInfo.viewportStartOffset +
                    listState.layoutInfo.viewportEndOffset) / 2
                listState.isScrollInProgress to visible.map {
                    it.index to (it.offset + it.size / 2)
                }
            }.collect { (scrolling, centers) ->
                if (!scrolling && centers.isNotEmpty()) {
                    val center = (listState.layoutInfo.viewportStartOffset +
                        listState.layoutInfo.viewportEndOffset) / 2
                    val nearest = centers.minByOrNull {
                        kotlin.math.abs(it.second - center)
                    }?.first
                    if (nearest != null && nearest in modes.indices &&
                        modes[nearest] != selected
                    ) {
                        onSelected(modes[nearest])
                    }
                }
            }
        }

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            flingBehavior = flingBehavior,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = sidePadding)
        ) {
            items(modes, key = { it.name }) { item ->
                val itemIndex = modes.indexOf(item)
                val active = item == selected
                val scale = remember { Animatable(if (active) 1f else 0.84f) }
                var verticalDrag by remember { mutableFloatStateOf(0f) }
                val itemVisualProgress by remember(itemIndex, modes) {
                    derivedStateOf {
                        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == itemIndex }
                        val center = (listState.layoutInfo.viewportStartOffset +
                            listState.layoutInfo.viewportEndOffset) / 2f
                        val itemCenter = info?.let { it.offset + it.size / 2f } ?: center
                        (1f - (kotlin.math.abs(itemCenter - center) / (92.dp.toPx() * 2.2f)))
                            .coerceIn(0f, 1f)
                    }
                }

                LaunchedEffect(active) {
                    if (active) {
                        scale.snapTo(0.86f)
                        scale.animateTo(
                            1f,
                            spring(
                                dampingRatio = 0.62f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(width = 92.dp, height = 42.dp)
                        .graphicsLayer {
                            val carouselProgress = if (active) 1f else itemVisualProgress
                            val dynamicScale = 0.78f + carouselProgress * 0.22f
                            scaleX = if (active) scale.value else dynamicScale
                            scaleY = if (active) scale.value else dynamicScale
                            alpha = 0.30f + carouselProgress * 0.70f
                        }
                        .pointerInput(active, item) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { _, dragAmount ->
                                    if (active) verticalDrag += dragAmount
                                },
                                onDragEnd = {
                                    if (active && verticalDrag < -42f) onShowAllModes()
                                    verticalDrag = 0f
                                },
                                onDragCancel = { verticalDrag = 0f }
                            )
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            if (!active) {
                                scope.launch {
                                    val index = modes.indexOf(item)
                                    if (index >= 0) {
                                        val centerOffsetPx = with(density) {
                                            ((maxWidth.toPx() - 92.dp.toPx()) / 2f).roundToInt()
                                        }
                                        listState.animateScrollToItem(index, scrollOffset = -centerOffsetPx)
                                    }
                                }
                            }
                            onSelected(item)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(20.dp),
                        color = if (active) Color(0xE6FFFFFF) else Color(0x33111111),
                        tonalElevation = if (active) 3.dp else 0.dp,
                        shadowElevation = if (active) 2.dp else 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = item.label,
                                color = if (active) Color.Black else Color.White,
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 22.dp, vertical = 12.dp),
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
