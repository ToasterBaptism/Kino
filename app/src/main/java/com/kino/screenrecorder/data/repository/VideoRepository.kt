package com.kino.screenrecorder.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Environment
import com.kino.screenrecorder.data.model.SortOption
import com.kino.screenrecorder.data.model.VideoProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class VideoRepository(private val context: Context) {
    
    private val _videos = MutableStateFlow<List<VideoProject>>(emptyList())
    val videos = _videos.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val _sortOption = MutableStateFlow(SortOption.DATE_NEWEST)
    val sortOption = _sortOption.asStateFlow()
    
    val filteredAndSortedVideos: Flow<List<VideoProject>> = combine(
        videos,
        searchQuery,
        sortOption
    ) { videoList, query, sort ->
        val filtered = if (query.isBlank()) {
            videoList
        } else {
            videoList.filter { it.name.contains(query, ignoreCase = true) }
        }
        
        when (sort) {
            SortOption.DATE_NEWEST -> filtered.sortedByDescending { it.createdAt }
            SortOption.SIZE_LARGEST -> filtered.sortedByDescending { it.size }
            SortOption.NAME_AZ -> filtered.sortedBy { it.name }
        }
    }
    
    private val videosDirectory: File
        get() = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Kino")
    
    init {
        // Ensure videos directory exists
        if (!videosDirectory.exists()) {
            videosDirectory.mkdirs()
        }
    }
    
    suspend fun refreshVideos() {
        withContext(Dispatchers.IO) {
            val videoFiles = videosDirectory.listFiles { file ->
                file.isFile && file.extension.lowercase() in listOf("mp4", "mkv", "avi", "mov")
            } ?: emptyArray()
            
            val videoProjects = videoFiles.mapNotNull { file ->
                try {
                    createVideoProjectFromFile(file)
                } catch (e: Exception) {
                    null // Skip corrupted files
                }
            }
            
            _videos.value = videoProjects
        }
    }
    
    private fun createVideoProjectFromFile(file: File): VideoProject {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()?.toInt() ?: 30
            
            VideoProject(
                id = UUID.nameUUIDFromBytes(file.absolutePath.toByteArray()).toString(),
                name = file.nameWithoutExtension,
                filePath = file.absolutePath,
                duration = duration,
                size = file.length(),
                createdAt = file.lastModified(),
                resolution = "${width}x${height}",
                frameRate = frameRate
            )
        } finally {
            retriever.release()
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun updateSortOption(sortOption: SortOption) {
        _sortOption.value = sortOption
    }
    
    suspend fun deleteVideo(videoProject: VideoProject): Boolean {
        return withContext(Dispatchers.IO) {
            val deleted = videoProject.file.delete()
            if (deleted) {
                refreshVideos()
            }
            deleted
        }
    }
    
    fun getVideoFile(fileName: String): File {
        return File(videosDirectory, fileName)
    }
    
    fun getTempDirectory(): File {
        val tempDir = File(context.externalCacheDir, "temp")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        return tempDir
    }
    
    suspend fun cleanupTempFiles() {
        withContext(Dispatchers.IO) {
            getTempDirectory().listFiles()?.forEach { file ->
                if (file.isFile && file.name.startsWith("segment_")) {
                    file.delete()
                }
            }
        }
    }
}