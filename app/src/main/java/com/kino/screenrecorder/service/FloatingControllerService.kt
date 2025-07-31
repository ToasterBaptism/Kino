package com.kino.screenrecorder.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.kino.screenrecorder.ui.theme.KinoTheme

class FloatingControllerService : Service() {
    
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isRecording = false
    private var isPaused = false
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingView()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_CONTROLLER -> showFloatingController()
            ACTION_HIDE_CONTROLLER -> hideFloatingController()
            ACTION_UPDATE_STATE -> {
                isRecording = intent.getBooleanExtra(EXTRA_IS_RECORDING, false)
                isPaused = intent.getBooleanExtra(EXTRA_IS_PAUSED, false)
                updateControllerState()
            }
        }
        return START_NOT_STICKY
    }
    
    private fun createFloatingView() {
        val composeView = ComposeView(this)
        composeView.setContent {
            KinoTheme {
                FloatingController(
                    isRecording = isRecording,
                    isPaused = isPaused,
                    onPlayPauseClick = { handlePlayPauseClick() },
                    onStopClick = { handleStopClick() }
                )
            }
        }
        
        floatingView = composeView
        setupTouchListener()
    }
    
    @Composable
    private fun FloatingController(
        isRecording: Boolean,
        isPaused: Boolean,
        onPlayPauseClick: () -> Unit,
        onStopClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = CircleShape
                )
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRecording && !isPaused) Color.Red else MaterialTheme.colorScheme.primary
                    )
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    tint = Color.White
                )
            }
            
            IconButton(
                onClick = onStopClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = Color.White
                )
            }
        }
    }
    
    private fun setupTouchListener() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        
        floatingView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val params = floatingView?.layoutParams as WindowManager.LayoutParams
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val params = floatingView?.layoutParams as WindowManager.LayoutParams
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Check if dragged to remove zone (bottom of screen)
                    val screenHeight = resources.displayMetrics.heightPixels
                    if (event.rawY > screenHeight - 200) {
                        hideFloatingController()
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    private fun showFloatingController() {
        if (floatingView?.parent != null) return
        
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }
        
        windowManager?.addView(floatingView, params)
    }
    
    private fun hideFloatingController() {
        floatingView?.let { view ->
            if (view.parent != null) {
                windowManager?.removeView(view)
            }
        }
        stopSelf()
    }
    
    private fun updateControllerState() {
        // Trigger recomposition by recreating the view
        floatingView?.let { view ->
            if (view.parent != null) {
                windowManager?.removeView(view)
                createFloatingView()
                showFloatingController()
            }
        }
    }
    
    private fun handlePlayPauseClick() {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = if (isPaused) {
                RecordingService.ACTION_RESUME_RECORDING
            } else {
                RecordingService.ACTION_PAUSE_RECORDING
            }
        }
        startService(intent)
    }
    
    private fun handleStopClick() {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP_RECORDING
        }
        startService(intent)
        hideFloatingController()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        hideFloatingController()
    }
    
    companion object {
        const val ACTION_SHOW_CONTROLLER = "com.kino.screenrecorder.SHOW_CONTROLLER"
        const val ACTION_HIDE_CONTROLLER = "com.kino.screenrecorder.HIDE_CONTROLLER"
        const val ACTION_UPDATE_STATE = "com.kino.screenrecorder.UPDATE_STATE"
        
        const val EXTRA_IS_RECORDING = "is_recording"
        const val EXTRA_IS_PAUSED = "is_paused"
    }
}