package com.hustlewithvikram.icamera.domain.model

data class CameraCapabilities(
    val supportsFlash: Boolean = false,
    val supportsTorch: Boolean = false,
    val supportsRaw: Boolean = false,
    val hasUltraWide: Boolean = false,
    val hasTelephoto: Boolean = false,
)
