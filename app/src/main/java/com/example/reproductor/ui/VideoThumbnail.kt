package com.example.reproductor.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.reproductor.data.Video
import com.example.reproductor.util.ThumbnailUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun VideoThumbnail(
    video: Video,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 80.dp
) {
    val context = LocalContext.current
    var thumbnailBitmap by remember(video.id) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(video.id) { mutableStateOf(true) }
    var hasError by remember(video.id) { mutableStateOf(false) }

    LaunchedEffect(video.id) {
        try {
            Log.d("VideoThumbnail", "Starting thumbnail generation for video: ${video.title} (ID: ${video.id})")
            isLoading = true
            hasError = false
            
            val cachedPath = ThumbnailUtil.getCachedThumbnailPath(context, video.id)
            if (cachedPath != null) {
                Log.d("VideoThumbnail", "Found cached thumbnail for ${video.id} at: $cachedPath")
                withContext(Dispatchers.IO) {
                    try {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(cachedPath)
                        if (bitmap != null) {
                            Log.d("VideoThumbnail", "Successfully loaded cached thumbnail for ${video.id}")
                            thumbnailBitmap = bitmap
                        } else {
                            Log.w("VideoThumbnail", "Cached file exists but failed to decode for ${video.id}")
                            hasError = true
                        }
                    } catch (e: Exception) {
                        Log.e("VideoThumbnail", "Error loading cached thumbnail for ${video.id}: ${e.message}")
                        val generatedPath = ThumbnailUtil.generateAndCacheThumbnail(context, video.uri, video.id)
                        if (generatedPath != null) {
                            val bitmap = android.graphics.BitmapFactory.decodeFile(generatedPath)
                            if (bitmap != null) {
                                Log.d("VideoThumbnail", "Successfully generated new thumbnail for ${video.id}")
                                thumbnailBitmap = bitmap
                            } else {
                                Log.e("VideoThumbnail", "Failed to decode generated thumbnail for ${video.id}")
                                hasError = true
                            }
                        } else {
                            Log.e("VideoThumbnail", "Failed to generate thumbnail for ${video.id}")
                            hasError = true
                        }
                    }
                }
            } else {
                Log.d("VideoThumbnail", "No cached thumbnail found for ${video.id}, generating new one")
                val generatedPath = ThumbnailUtil.generateAndCacheThumbnail(context, video.uri, video.id)
                if (generatedPath != null) {
                    Log.d("VideoThumbnail", "Generated thumbnail at: $generatedPath for ${video.id}")
                    withContext(Dispatchers.IO) {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(generatedPath)
                        if (bitmap != null) {
                            Log.d("VideoThumbnail", "Successfully loaded generated thumbnail for ${video.id}")
                            thumbnailBitmap = bitmap
                        } else {
                            Log.e("VideoThumbnail", "Failed to decode generated thumbnail for ${video.id}")
                            hasError = true
                        }
                    }
                } else {
                    Log.e("VideoThumbnail", "Failed to generate thumbnail for ${video.id}")
                    hasError = true
                }
            }
        } catch (e: Exception) {
            Log.e("VideoThumbnail", "Exception during thumbnail processing for ${video.id}: ${e.message}")
            hasError = true
        } finally {
            isLoading = false
            Log.d("VideoThumbnail", "Finished thumbnail processing for ${video.id} - Success: ${thumbnailBitmap != null}, Error: $hasError")
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            thumbnailBitmap != null -> {
                Image(
                    bitmap = thumbnailBitmap!!.asImageBitmap(),
                    contentDescription = "Video thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            hasError -> {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = "Video thumbnail unavailable",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}