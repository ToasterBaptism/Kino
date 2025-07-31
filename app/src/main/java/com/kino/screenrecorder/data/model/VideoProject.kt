package com.kino.screenrecorder.data.model

import java.io.File

data class VideoProject(
    val id: String,
    val name: String,
    val filePath: String,
    val thumbnailPath: String? = null,
    val duration: Long, // in milliseconds
    val size: Long, // in bytes
    val createdAt: Long, // timestamp
    val resolution: String,
    val frameRate: Int
) {
    val file: File get() = File(filePath)
    val exists: Boolean get() = file.exists()
    val formattedSize: String get() = formatFileSize(size)
    val formattedDuration: String get() = formatDuration(duration)
    
    private fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> "%.1f GB".format(gb)
            mb >= 1 -> "%.1f MB".format(mb)
            kb >= 1 -> "%.1f KB".format(kb)
            else -> "$bytes B"
        }
    }
    
    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "%d:%02d:%02d".format(hours, minutes % 60, seconds % 60)
            else -> "%d:%02d".format(minutes, seconds % 60)
        }
    }
}