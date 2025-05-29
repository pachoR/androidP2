package com.example.reproductor.data

import android.net.Uri

/**
 * Represents a video file in the application
 */
data class Video(
    val id: String,
    val title: String,
    val uri: Uri,
    val thumbnailUri: Uri? = null,
    val duration: Long = 0,
    val size: Long = 0,
    val path: String = ""
)