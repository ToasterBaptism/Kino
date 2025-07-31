package com.kino.screenrecorder.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kino.screenrecorder.KinoApplication
import com.kino.screenrecorder.data.model.RecordingSettings
import com.kino.screenrecorder.data.model.RecordingState
import com.kino.screenrecorder.data.repository.SettingsRepository
import com.kino.screenrecorder.service.FloatingControllerService
import com.kino.screenrecorder.service.RecordingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RecordingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    private val settingsRepository: SettingsRepository = 
        (application as KinoApplication).settingsRepository
    
    private var recordingService: RecordingService? = null
    private var isServiceBound = false
    
    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()
    
    private val _recordingSettings = MutableStateFlow(RecordingSettings())
    val recordingSettings: StateFlow<RecordingSettings> = _recordingSettings.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RecordingService.RecordingBinder
            recordingService = binder.getService()
            isServiceBound = true
            
            // Observe service state
            viewModelScope.launch {
                recordingService?.recordingState?.collect { state ->
                    _recordingState.value = state
                }
            }
            
            viewModelScope.launch {
                recordingService?.recordingDuration?.collect { duration ->
                    _recordingDuration.value = duration
                }
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            recordingService = null
            isServiceBound = false
        }
    }
    
    init {
        // Load settings
        viewModelScope.launch {
            settingsRepository.recordingSettings.collect { settings ->
                _recordingSettings.value = settings
            }
        }
    }
    
    fun requestScreenRecordingPermission(launcher: ActivityResultLauncher<Intent>) {
        val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        launcher.launch(intent)
    }
    
    fun startRecording(resultCode: Int, data: Intent?) {
        if (data == null) {
            _error.value = "Failed to get screen recording permission"
            return
        }
        
        viewModelScope.launch {
            try {
                val settings = settingsRepository.recordingSettings.first()
                
                val intent = Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_START_RECORDING
                    putExtra(RecordingService.EXTRA_RESULT_CODE, resultCode)
                    putExtra(RecordingService.EXTRA_DATA, data)
                    putExtra(RecordingService.EXTRA_SETTINGS, settings)
                }
                
                context.startForegroundService(intent)
                bindToRecordingService()
                showFloatingController()
                
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to start recording"
            }
        }
    }
    
    fun stopRecording() {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP_RECORDING
        }
        context.startService(intent)
        hideFloatingController()
        unbindFromRecordingService()
    }
    
    fun pauseRecording() {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_PAUSE_RECORDING
        }
        context.startService(intent)
        updateFloatingControllerState()
    }
    
    fun resumeRecording() {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_RESUME_RECORDING
        }
        context.startService(intent)
        updateFloatingControllerState()
    }
    
    fun saveInstantReplay() {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_SAVE_REPLAY
        }
        context.startService(intent)
    }
    
    private fun bindToRecordingService() {
        val intent = Intent(context, RecordingService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun unbindFromRecordingService() {
        if (isServiceBound) {
            context.unbindService(serviceConnection)
            isServiceBound = false
        }
    }
    
    private fun showFloatingController() {
        val intent = Intent(context, FloatingControllerService::class.java).apply {
            action = FloatingControllerService.ACTION_SHOW_CONTROLLER
        }
        context.startService(intent)
    }
    
    private fun hideFloatingController() {
        val intent = Intent(context, FloatingControllerService::class.java).apply {
            action = FloatingControllerService.ACTION_HIDE_CONTROLLER
        }
        context.startService(intent)
    }
    
    private fun updateFloatingControllerState() {
        val intent = Intent(context, FloatingControllerService::class.java).apply {
            action = FloatingControllerService.ACTION_UPDATE_STATE
            putExtra(FloatingControllerService.EXTRA_IS_RECORDING, _recordingState.value == RecordingState.RECORDING)
            putExtra(FloatingControllerService.EXTRA_IS_PAUSED, _recordingState.value == RecordingState.PAUSED)
        }
        context.startService(intent)
    }
    
    fun clearError() {
        _error.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        unbindFromRecordingService()
    }
}