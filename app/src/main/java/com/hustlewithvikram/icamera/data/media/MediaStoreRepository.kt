package com.hustlewithvikram.icamera.data.media

import android.content.ContentResolver
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore

class MediaStoreRepository(private val resolver: ContentResolver) {
    fun imageValues(fileName: String): ContentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/iCamera")
        }
    }
}
