package com.kino.screenrecorder.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kino.screenrecorder.KinoApplication
import com.kino.screenrecorder.data.model.SortOption
import com.kino.screenrecorder.data.model.VideoProject
import com.kino.screenrecorder.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val videoRepository: VideoRepository = 
        (application as KinoApplication).videoRepository
    
    val videos = videoRepository.filteredAndSortedVideos
    val searchQuery = videoRepository.searchQuery
    val sortOption = videoRepository.sortOption
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        refreshVideos()
    }
    
    fun refreshVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                videoRepository.refreshVideos()
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateSearchQuery(query: String) {
        videoRepository.updateSearchQuery(query)
    }
    
    fun updateSortOption(sortOption: SortOption) {
        videoRepository.updateSortOption(sortOption)
    }
    
    fun deleteVideo(videoProject: VideoProject) {
        viewModelScope.launch {
            try {
                val deleted = videoRepository.deleteVideo(videoProject)
                if (!deleted) {
                    _error.value = "Failed to delete video"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete video"
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}