package com.example.reproductor.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream


object ThumbnailUtil {
    private const val TAG = "ThumbnailUtil"
    private const val THUMBNAIL_SIZE = 320 // Thumbnail size in pixels
    
    suspend fun getVideoThumbnail(context: Context, videoUri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        var retriever: MediaMetadataRetriever? = null
        try {
            Log.d(TAG, "Starting thumbnail generation for URI: $videoUri")
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)

            // Get video duration first to check if video is valid
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0
            Log.d(TAG, "Video duration: ${duration}ms")

            // Get frame at 1 second (1,000,000 microseconds) if video is long enough
            val frameTime = if (duration > 1000) 1000000L else 0L
            Log.d(TAG, "Extracting frame at time: ${frameTime}μs")

            val bitmap = retriever.getFrameAtTime(frameTime, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

            if (bitmap != null) {
                Log.d(TAG, "Successfully generated thumbnail for: $videoUri (${bitmap.width}x${bitmap.height})")
                return@withContext bitmap
            } else {
                Log.w(TAG, "getFrameAtTime returned null for: $videoUri")
                return@withContext null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error generating thumbnail for $videoUri: ${e.message}")
            try {
                Log.d(TAG, "Attempting fallback thumbnail generation at time 0")
                // Fallback: try to get frame at time 0
                val fallbackBitmap = retriever?.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (fallbackBitmap != null) {
                    Log.d(TAG, "Fallback thumbnail generation succeeded")
                    return@withContext fallbackBitmap
                } else {
                    Log.e(TAG, "Fallback also returned null bitmap")
                    return@withContext null
                }
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Fallback thumbnail generation also failed: ${fallbackError.message}")
                return@withContext null
            }
        } finally {
            try {
                retriever?.release()
                Log.d(TAG, "MediaMetadataRetriever released")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever: ${e.message}")
            }
        }
    }

    fun getVideoThumbnailUri(context: Context, videoId: String): Uri? {
        try {
            val contentResolver = context.contentResolver
            val thumbnailUri = Uri.withAppendedPath(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                "$videoId"
            )
            
            // First try to get a thumbnail directly from MediaStore
            val projection = arrayOf(MediaStore.Video.Thumbnails.DATA)
            val selection = MediaStore.Video.Thumbnails.VIDEO_ID + "=?"
            val selectionArgs = arrayOf(videoId)
            
            contentResolver.query(
                MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val thumbnailPath = cursor.getString(0)
                    if (thumbnailPath != null) {
                        val thumbnailFile = File(thumbnailPath)
                        if (thumbnailFile.exists()) {
                            return Uri.fromFile(thumbnailFile)
                        }
                    }
                }
            }
            
            // If no thumbnail is found, return the video URI to generate one at runtime
            return thumbnailUri
        } catch (e: Exception) {
            Log.e(TAG, "Error getting thumbnail URI: ${e.message}")
            return null
        }
    }
    
    suspend fun cacheThumbnail(context: Context, bitmap: Bitmap, videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Caching thumbnail for video $videoId (${bitmap.width}x${bitmap.height})")
            val cacheDir = File(context.cacheDir, "video_thumbnails")
            if (!cacheDir.exists()) {
                val created = cacheDir.mkdirs()
                Log.d(TAG, "Created cache directory: $created")
            }
            
            val thumbnailFile = File(cacheDir, "thumb_$videoId.jpg")
            FileOutputStream(thumbnailFile).use { out ->
                val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                Log.d(TAG, "Bitmap compression result: $compressed")
            }
            
            if (thumbnailFile.exists()) {
                Log.d(TAG, "Successfully cached thumbnail for video $videoId at: ${thumbnailFile.absolutePath} (${thumbnailFile.length()} bytes)")
                return@withContext thumbnailFile.absolutePath
            } else {
                Log.e(TAG, "Thumbnail file was not created for video $videoId")
                return@withContext null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error caching thumbnail for video $videoId: ${e.message}")
            return@withContext null
        }
    }

    suspend fun generateAndCacheThumbnail(context: Context, videoUri: Uri, videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating and caching thumbnail for video $videoId")
            val bitmap = getVideoThumbnail(context, videoUri)
            if (bitmap != null) {
                return@withContext cacheThumbnail(context, bitmap, videoId)
            } else {
                Log.e(TAG, "Could not generate thumbnail for video $videoId")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateAndCacheThumbnail for video $videoId: ${e.message}")
            return@withContext null
        }
    }

    fun getCachedThumbnailPath(context: Context, videoId: String): String? {
        try {
            val cacheDir = File(context.cacheDir, "video_thumbnails")
            val thumbnailFile = File(cacheDir, "thumb_$videoId.jpg")

            return if (thumbnailFile.exists() && thumbnailFile.length() > 0) {
                Log.d(TAG, "Found cached thumbnail for video $videoId at: ${thumbnailFile.absolutePath}")
                thumbnailFile.absolutePath
            } else {
                Log.d(TAG, "No cached thumbnail found for video $videoId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking cached thumbnail for video $videoId: ${e.message}")
            return null
        }
    }
}
