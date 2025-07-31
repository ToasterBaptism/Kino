package com.kino.screenrecorder.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kino.screenrecorder.KinoApplication
import com.kino.screenrecorder.data.model.AudioSource
import com.kino.screenrecorder.data.model.RecordingMode
import com.kino.screenrecorder.data.model.RecordingSettings
import com.kino.screenrecorder.data.model.Resolution
import com.kino.screenrecorder.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val settingsRepository: SettingsRepository = 
        (application as KinoApplication).settingsRepository
    
    val recordingSettings = settingsRepository.recordingSettings
    
    private val _showResetDialog = MutableStateFlow(false)
    val showResetDialog: StateFlow<Boolean> = _showResetDialog.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun updateFrameRate(frameRate: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.updateFrameRate(frameRate)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update frame rate"
            }
        }
    }
    
    fun updateResolution(resolution: Resolution) {
        viewModelScope.launch {
            try {
                settingsRepository.updateResolution(resolution)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update resolution"
            }
        }
    }
    
    fun updateBitrate(bitrate: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.updateBitrate(bitrate)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update bitrate"
            }
        }
    }
    
    fun updateShowTouches(showTouches: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateShowTouches(showTouches)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update show touches setting"
            }
        }
    }
    
    fun updateAudioSource(audioSource: AudioSource) {
        viewModelScope.launch {
            try {
                settingsRepository.updateAudioSource(audioSource)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update audio source"
            }
        }
    }
    
    fun updateRecordingMode(recordingMode: RecordingMode) {
        viewModelScope.launch {
            try {
                settingsRepository.updateRecordingMode(recordingMode)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update recording mode"
            }
        }
    }
    
    fun updateInstantReplayDuration(duration: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.updateInstantReplayDuration(duration)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update instant replay duration"
            }
        }
    }
    
    fun updateTimedRecordingDuration(duration: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.updateTimedRecordingDuration(duration)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update timed recording duration"
            }
        }
    }
    
    fun showResetDialog() {
        _showResetDialog.value = true
    }
    
    fun hideResetDialog() {
        _showResetDialog.value = false
    }
    
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                settingsRepository.resetToDefaults()
                _showResetDialog.value = false
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to reset settings"
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}