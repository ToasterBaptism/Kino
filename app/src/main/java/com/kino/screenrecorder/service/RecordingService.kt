package com.kino.screenrecorder.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.kino.screenrecorder.KinoApplication
import com.kino.screenrecorder.MainActivity
import com.kino.screenrecorder.R
import com.kino.screenrecorder.data.model.RecordingSettings
import com.kino.screenrecorder.data.model.RecordingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingService : Service() {
    
    private val binder = RecordingBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var surface: Surface? = null
    
    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState = _recordingState.asStateFlow()
    
    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration = _recordingDuration.asStateFlow()
    
    private var recordingStartTime = 0L
    private var pausedDuration = 0L
    private var timerJob: Job? = null
    
    private lateinit var recordingSettings: RecordingSettings
    private var outputFile: File? = null
    
    // Instant replay circular buffer
    private val replaySegments = mutableListOf<File>()
    private var currentSegmentIndex = 0
    private val maxReplaySegments = 30 // 30 seconds worth of 1-second segments
    
    inner class RecordingBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "RecordingService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
                val settings = intent.getSerializableExtra(EXTRA_SETTINGS) as? RecordingSettings
                
                Log.d(TAG, "Start recording - resultCode: $resultCode, data: $data, settings: $settings")
                
                if (resultCode != -1 && data != null && settings != null) {
                    startRecording(resultCode, data, settings)
                } else {
                    Log.e(TAG, "Invalid parameters for start recording")
                    stopSelf()
                }
            }
            ACTION_STOP_RECORDING -> {
                Log.d(TAG, "Stop recording action")
                stopRecording()
            }
            ACTION_PAUSE_RECORDING -> {
                Log.d(TAG, "Pause recording action")
                pauseRecording()
            }
            ACTION_RESUME_RECORDING -> {
                Log.d(TAG, "Resume recording action")
                resumeRecording()
            }
            ACTION_SAVE_REPLAY -> {
                Log.d(TAG, "Save replay action")
                saveInstantReplay()
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent?.action}")
            }
        }
        
        return START_NOT_STICKY
    }
    
    private fun startRecording(resultCode: Int, data: Intent, settings: RecordingSettings) {
        if (_recordingState.value != RecordingState.IDLE) return
        
        recordingSettings = settings
        
        try {
            Log.d(TAG, "Starting recording with settings: $settings")
            
            setupMediaProjection(resultCode, data)
            Log.d(TAG, "MediaProjection setup complete")
            
            setupRecording()
            Log.d(TAG, "Recording setup complete")
            
            _recordingState.value = RecordingState.RECORDING
            recordingStartTime = System.currentTimeMillis()
            
            Log.d(TAG, "Starting foreground service")
            try {
                val notification = createNotification()
                startForeground(NOTIFICATION_ID, notification)
                Log.d(TAG, "Foreground service started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service", e)
                // Try with a simpler notification
                try {
                    val simpleNotification = createSimpleNotification()
                    startForeground(NOTIFICATION_ID, simpleNotification)
                    Log.d(TAG, "Started with simple notification")
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to start with simple notification", e2)
                    throw e2
                }
            }
            
            startTimer()
            
            Log.d(TAG, "Recording started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            // Send error back to UI
            _recordingState.value = RecordingState.IDLE
            stopSelf()
        }
    }
    
    private fun setupMediaProjection(resultCode: Int, data: Intent) {
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
    }
    
    private fun setupRecording() {
        try {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            
            val requestedWidth = recordingSettings.resolution.width
            val requestedHeight = recordingSettings.resolution.height
            val density = displayMetrics.densityDpi
            
            // Use device resolution if it's smaller than requested resolution
            val deviceWidth = displayMetrics.widthPixels
            val deviceHeight = displayMetrics.heightPixels
            
            val width = minOf(requestedWidth, deviceWidth)
            val height = minOf(requestedHeight, deviceHeight)
            
            Log.d(TAG, "Device resolution: ${deviceWidth}x${deviceHeight}")
            Log.d(TAG, "Requested resolution: ${requestedWidth}x${requestedHeight}")
            Log.d(TAG, "Using resolution: ${width}x${height}, density: $density")
            
            // Create output file
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Recording_$timestamp.mp4"
            val videosDir = getExternalFilesDir("Videos")
            if (videosDir != null && !videosDir.exists()) {
                videosDir.mkdirs()
            }
            outputFile = File(videosDir, fileName)
            
            Log.d(TAG, "Output file: ${outputFile?.absolutePath}")
            
            when (recordingSettings.recordingMode) {
                com.kino.screenrecorder.data.model.RecordingMode.STANDARD -> setupStandardRecording(width, height, density)
                com.kino.screenrecorder.data.model.RecordingMode.INSTANT_REPLAY -> setupInstantReplayRecording(width, height, density)
                com.kino.screenrecorder.data.model.RecordingMode.TIMED -> setupTimedRecording(width, height, density)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupRecording", e)
            throw e
        }
    }
    
    private fun setupStandardRecording(width: Int, height: Int, density: Int) {
        try {
            Log.d(TAG, "Setting up MediaRecorder")
            Log.d(TAG, "Audio source: ${recordingSettings.audioSource}")
            
            mediaRecorder = MediaRecorder().apply {
                try {
                    setVideoSource(MediaRecorder.VideoSource.SURFACE)
                    Log.d(TAG, "Video source set")
                    
                    if (recordingSettings.audioSource == com.kino.screenrecorder.data.model.AudioSource.MICROPHONE) {
                        // Check if we have audio permission before setting audio source
                        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            setAudioSource(MediaRecorder.AudioSource.MIC)
                            Log.d(TAG, "Audio source set to microphone")
                        } else {
                            Log.w(TAG, "Audio permission not granted, recording without audio")
                        }
                    }
                    
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                    Log.d(TAG, "Output format and video encoder set")
                    
                    if (recordingSettings.audioSource == com.kino.screenrecorder.data.model.AudioSource.MICROPHONE &&
                        checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        Log.d(TAG, "Audio encoder set")
                    }
                    
                    // Validate and set video parameters
                    val validWidth = if (width % 2 == 0) width else width - 1
                    val validHeight = if (height % 2 == 0) height else height - 1
                    
                    setVideoSize(validWidth, validHeight)
                    Log.d(TAG, "Video size set: ${validWidth}x${validHeight} (adjusted from ${width}x${height})")
                    
                    setVideoFrameRate(recordingSettings.frameRate)
                    Log.d(TAG, "Frame rate set: ${recordingSettings.frameRate}")
                    
                    setVideoEncodingBitRate(recordingSettings.bitrate)
                    Log.d(TAG, "Bitrate set: ${recordingSettings.bitrate}")
                    
                    val outputPath = outputFile?.absolutePath
                    if (outputPath == null) {
                        throw IllegalStateException("Output file path is null")
                    }
                    
                    setOutputFile(outputPath)
                    Log.d(TAG, "Output file set: $outputPath")
                    
                    // Ensure parent directory exists
                    outputFile?.parentFile?.let { parentDir ->
                        if (!parentDir.exists()) {
                            parentDir.mkdirs()
                            Log.d(TAG, "Created parent directory: ${parentDir.absolutePath}")
                        }
                    }
                    
                    prepare()
                    Log.d(TAG, "MediaRecorder prepared successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error configuring MediaRecorder", e)
                    throw e
                }
            }
            
            surface = mediaRecorder?.surface
            Log.d(TAG, "Surface obtained from MediaRecorder")
            
            createVirtualDisplay(width, height, density)
            Log.d(TAG, "Virtual display created")
            
            try {
                mediaRecorder?.start()
                Log.d(TAG, "MediaRecorder started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start MediaRecorder", e)
                throw e
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupStandardRecording", e)
            throw e
        }
    }
    
    private fun setupInstantReplayRecording(width: Int, height: Int, density: Int) {
        // For instant replay, we'll use a circular buffer approach
        // This is a simplified implementation - in production, you'd want more sophisticated buffering
        setupStandardRecording(width, height, density)
    }
    
    private fun setupTimedRecording(width: Int, height: Int, density: Int) {
        setupStandardRecording(width, height, density)
        
        // Schedule automatic stop after the specified duration
        serviceScope.launch {
            delay(recordingSettings.timedRecordingDuration * 1000L)
            if (_recordingState.value == RecordingState.RECORDING) {
                stopRecording()
            }
        }
    }
    
    private fun createVirtualDisplay(width: Int, height: Int, density: Int) {
        try {
            if (mediaProjection == null) {
                throw IllegalStateException("MediaProjection is null")
            }
            if (surface == null) {
                throw IllegalStateException("Surface is null")
            }
            
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenRecording",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, null, null
            )
            
            if (virtualDisplay == null) {
                throw IllegalStateException("Failed to create virtual display")
            }
            
            Log.d(TAG, "Virtual display created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating virtual display", e)
            throw e
        }
    }
    
    private fun pauseRecording() {
        if (_recordingState.value != RecordingState.RECORDING) return
        
        try {
            mediaRecorder?.pause()
            _recordingState.value = RecordingState.PAUSED
            pausedDuration += System.currentTimeMillis() - recordingStartTime
            stopTimer()
            updateNotification()
            Log.d(TAG, "Recording paused")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause recording", e)
        }
    }
    
    private fun resumeRecording() {
        if (_recordingState.value != RecordingState.PAUSED) return
        
        try {
            mediaRecorder?.resume()
            _recordingState.value = RecordingState.RECORDING
            recordingStartTime = System.currentTimeMillis()
            startTimer()
            updateNotification()
            Log.d(TAG, "Recording resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume recording", e)
        }
    }
    
    private fun stopRecording() {
        if (_recordingState.value == RecordingState.IDLE) return
        
        _recordingState.value = RecordingState.PROCESSING
        stopTimer()
        
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            virtualDisplay?.release()
            virtualDisplay = null
            
            mediaProjection?.stop()
            mediaProjection = null
            
            surface?.release()
            surface = null
            
            _recordingState.value = RecordingState.IDLE
            _recordingDuration.value = 0L
            
            Log.d(TAG, "Recording stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
        } finally {
            stopForeground(true)
            stopSelf()
        }
    }
    
    private fun saveInstantReplay() {
        // Implementation for saving instant replay
        // This would concatenate the circular buffer segments
        Log.d(TAG, "Saving instant replay")
    }
    
    private fun startTimer() {
        timerJob = serviceScope.launch {
            while (_recordingState.value == RecordingState.RECORDING) {
                val currentTime = System.currentTimeMillis()
                _recordingDuration.value = currentTime - recordingStartTime + pausedDuration
                updateNotification()
                delay(1000)
            }
        }
    }
    
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val pauseResumeIntent = Intent(this, RecordingService::class.java).apply {
            action = if (_recordingState.value == RecordingState.RECORDING) {
                ACTION_PAUSE_RECORDING
            } else {
                ACTION_RESUME_RECORDING
            }
        }
        val pauseResumePendingIntent = PendingIntent.getService(
            this, 2, pauseResumeIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, KinoApplication.RECORDING_CHANNEL_ID)
            .setContentTitle(getString(R.string.recording_notification_title))
            .setContentText(getNotificationText())
            .setSmallIcon(R.drawable.ic_record)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_stop,
                getString(R.string.stop_recording),
                stopPendingIntent
            )
            .addAction(
                if (_recordingState.value == RecordingState.RECORDING) R.drawable.ic_pause else R.drawable.ic_play,
                if (_recordingState.value == RecordingState.RECORDING) getString(R.string.pause_recording) else getString(R.string.resume_recording),
                pauseResumePendingIntent
            )
            .build()
    }
    
    private fun createSimpleNotification(): Notification {
        return NotificationCompat.Builder(this, KinoApplication.RECORDING_CHANNEL_ID)
            .setContentTitle("Kino Recording")
            .setContentText("Screen recording in progress")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
    
    private fun getNotificationText(): String {
        val duration = _recordingDuration.value / 1000
        val minutes = duration / 60
        val seconds = duration % 60
        val timeText = "%02d:%02d".format(minutes, seconds)
        
        return when (_recordingState.value) {
            RecordingState.RECORDING -> {
                when (recordingSettings.recordingMode) {
                    com.kino.screenrecorder.data.model.RecordingMode.TIMED -> {
                        val remaining = recordingSettings.timedRecordingDuration - duration
                        val remainingMinutes = remaining / 60
                        val remainingSeconds = remaining % 60
                        getString(R.string.timed_recording_active, "%02d:%02d".format(remainingMinutes, remainingSeconds))
                    }
                    com.kino.screenrecorder.data.model.RecordingMode.INSTANT_REPLAY -> getString(R.string.instant_replay_active)
                    else -> getString(R.string.recording_notification_text) + " $timeText"
                }
            }
            RecordingState.PAUSED -> getString(R.string.recording_paused) + " $timeText"
            else -> getString(R.string.recording_notification_text)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        Log.d(TAG, "RecordingService destroyed")
    }
    
    companion object {
        private const val TAG = "RecordingService"
        private const val NOTIFICATION_ID = 1001
        
        const val ACTION_START_RECORDING = "com.kino.screenrecorder.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.kino.screenrecorder.STOP_RECORDING"
        const val ACTION_PAUSE_RECORDING = "com.kino.screenrecorder.PAUSE_RECORDING"
        const val ACTION_RESUME_RECORDING = "com.kino.screenrecorder.RESUME_RECORDING"
        const val ACTION_SAVE_REPLAY = "com.kino.screenrecorder.SAVE_REPLAY"
        
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"
        const val EXTRA_SETTINGS = "settings"
    }
}