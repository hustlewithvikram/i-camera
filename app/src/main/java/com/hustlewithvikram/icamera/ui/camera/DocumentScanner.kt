package com.hustlewithvikram.icamera.ui.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun DocumentScannerControls(
    pageCount: Int,
    reviewUri: Uri?,
    processing: Boolean,
    captureEnabled: Boolean,
    onCapture: () -> Unit,
    onRetake: () -> Unit,
    onAddPage: () -> Unit,
    onDone: () -> Unit,
    onExit: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        if (reviewUri == null) {
            DocumentGuide(Modifier.fillMaxSize())
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
                shape = RoundedCornerShape(18.dp), color = Color(0x990F0F0F)
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(Icons.Rounded.DocumentScanner, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text("Document", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
            if (pageCount > 0) {
                Surface(Modifier.align(Alignment.TopEnd).padding(14.dp), CircleShape, color = Color(0xAA0F0F0F)) {
                    Text("\$pageCount", color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp)
                    .size(84.dp).clip(CircleShape).clickable(enabled = captureEnabled, onClick = onCapture),
                shape = CircleShape, color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PhotoCamera, "Scan page", tint = Color.Black, modifier = Modifier.size(30.dp))
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 28.dp)
                    .clip(CircleShape).clickable(onClick = onExit),
                shape = CircleShape, color = Color(0x99111111)
            ) {
                Icon(Icons.Rounded.Close, "Close document scanner", tint = Color.White, modifier = Modifier.padding(14.dp))
            }
        } else {
            DocumentReview(reviewUri, pageCount, processing, onRetake, onAddPage, onDone)
        }
    }
}

@Composable
private fun DocumentGuide(modifier: Modifier) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val left = size.width * .08f; val right = size.width * .92f
            val top = size.height * .20f; val bottom = size.height * .78f
            val line = Color.White.copy(alpha = .92f)
            drawRect(Color.Black.copy(alpha = .24f))
            drawRoundRect(
                color = line, topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                style = Stroke(2.5.dp.toPx())
            )
            val c = 28.dp.toPx(); val s = 4.dp.toPx()
            fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                drawLine(line, Offset(x, y), Offset(x + dx * c, y), s)
                drawLine(line, Offset(x, y), Offset(x, y + dy * c), s)
            }
            corner(left, top, 1f, 1f); corner(right, top, -1f, 1f)
            corner(left, bottom, 1f, -1f); corner(right, bottom, -1f, -1f)
        }
        Text("Fit the entire page inside the frame",
            Modifier.align(Alignment.Center).padding(top = 260.dp),
            color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DocumentReview(
    uri: Uri, pageCount: Int, processing: Boolean,
    onRetake: () -> Unit, onAddPage: () -> Unit, onDone: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = produceState<Bitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } }.getOrNull()
        }
    }.value
    Column(Modifier.fillMaxSize().background(Color(0xFF080808)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Review scan", color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("\${pageCount + 1} pages", color = Color.White.copy(alpha = .65f), style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF171717)), contentAlignment = Alignment.Center) {
            bitmap?.let { Image(it.asImageBitmap(), "Scanned document", Modifier.fillMaxSize().padding(8.dp)) }
            if (processing) Surface(shape = RoundedCornerShape(18.dp), color = Color(0xDD111111)) {
                Text("Enhancing scan…", color = Color.White, modifier = Modifier.padding(18.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ScannerAction("Retake", Icons.Rounded.RestartAlt, onRetake, Modifier.weight(1f))
            ScannerAction("Add page", Icons.Rounded.Check, onAddPage, Modifier.weight(1f))
            ScannerAction("Done", Icons.Rounded.Check, onDone, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ScannerAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Surface(modifier.height(52.dp).clip(RoundedCornerShape(17.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(17.dp), color = Color(0xFF202020)) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

internal suspend fun processDocumentPage(context: Context, source: Uri): Uri? = withContext(Dispatchers.Default) {
    val original = context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
    val bounds = detectDocumentBounds(original)
    val cropped = Bitmap.createBitmap(
        original, bounds.left.coerceIn(0, original.width - 1), bounds.top.coerceIn(0, original.height - 1),
        bounds.width().coerceIn(1, original.width - bounds.left.coerceAtLeast(0)),
        bounds.height().coerceIn(1, original.height - bounds.top.coerceAtLeast(0))
    )
    original.recycle()
    val enhanced = enhanceDocument(cropped); cropped.recycle()

    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "iCamera_Scan_\${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/iCamera/Scans")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val output = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
    val saved = runCatching {
        context.contentResolver.openOutputStream(output)?.use { enhanced.compress(Bitmap.CompressFormat.JPEG, 94, it) } == true
    }.getOrDefault(false)
    enhanced.recycle()
    if (!saved) { context.contentResolver.delete(output, null, null); return@withContext null }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.contentResolver.update(output, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
    }
    output
}

private fun detectDocumentBounds(bitmap: Bitmap): android.graphics.Rect {
    val scale = min(220f / bitmap.width, 260f / bitmap.height).coerceAtMost(1f)
    val w = max(32, (bitmap.width * scale).toInt()); val h = max(32, (bitmap.height * scale).toInt())
    val small = Bitmap.createScaledBitmap(bitmap, w, h, true)
    val pixels = IntArray(w * h); small.getPixels(pixels, 0, w, 0, 0, w, h)
    var borderSum = 0L; var borderCount = 0
    for (y in 0 until h) for (x in 0 until w) if (x < 4 || y < 4 || x >= w - 4 || y >= h - 4) {
        borderSum += luminance(pixels[y * w + x]); borderCount++
    }
    val threshold = ((borderSum / borderCount.coerceAtLeast(1)) + 18).toInt().coerceAtMost(245)
    val visited = BooleanArray(w * h); val qx = IntArray(w * h); val qy = IntArray(w * h)
    var best: android.graphics.Rect? = null; var bestArea = 0
    for (sy in 2 until h - 2 step 3) for (sx in 2 until w - 2 step 3) {
        val start = sy * w + sx
        if (visited[start] || luminance(pixels[start]) < threshold) continue
        var head = 0; var tail = 0; var count = 0
        qx[tail] = sx; qy[tail] = sy; tail++; visited[start] = true
        var minX = sx; var maxX = sx; var minY = sy; var maxY = sy
        while (head < tail && count < w * h / 2) {
            val x = qx[head]; val y = qy[head]; head++; count++
            minX = min(minX, x); maxX = max(maxX, x); minY = min(minY, y); maxY = max(maxY, y)
            val neighbors = intArrayOf((x - 1) + y * w, (x + 1) + y * w, x + (y - 1) * w, x + (y + 1) * w)
            for (i in neighbors) {
                val nx = i % w; val ny = i / w
                if (nx !in 1 until w - 1 || ny !in 1 until h - 1 || visited[i] || luminance(pixels[i]) < threshold) continue
                visited[i] = true; qx[tail] = nx; qy[tail] = ny; tail++
            }
        }
        val area = (maxX - minX + 1) * (maxY - minY + 1)
        if (area > bestArea && area > w * h * .20f) { bestArea = area; best = android.graphics.Rect(minX, minY, maxX + 1, maxY + 1) }
    }
    small.recycle()
    val chosen = best ?: android.graphics.Rect((w * .04f).toInt(), (h * .04f).toInt(), (w * .96f).toInt(), (h * .96f).toInt())
    val px = (chosen.width() * .025f).toInt(); val py = (chosen.height() * .025f).toInt()
    return android.graphics.Rect(
        ((chosen.left - px) / scale).toInt().coerceAtLeast(0), ((chosen.top - py) / scale).toInt().coerceAtLeast(0),
        ((chosen.right + px) / scale).toInt().coerceAtMost(bitmap.width), ((chosen.bottom + py) / scale).toInt().coerceAtMost(bitmap.height)
    )
}

private fun luminance(color: Int): Int =
    (0.2126f * android.graphics.Color.red(color) + 0.7152f * android.graphics.Color.green + 0.0722f * android.graphics.Color.blue(color)).toInt()

private fun enhanceDocument(bitmap: Bitmap): Bitmap {
    val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val matrix = ColorMatrix().apply {
        setSaturation(.18f)
        val contrast = 1.16f; val translate = -128f * (contrast - 1f)
        postConcat(ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, translate, 0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate, 0f, 0f, 0f, 1f, 0f
        )))
    }
    Canvas(output).drawBitmap(bitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(matrix) })
    return output
}
