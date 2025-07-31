package com.kino.screenrecorder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.kino.screenrecorder.data.repository.SettingsRepository
import com.kino.screenrecorder.data.repository.VideoRepository

class KinoApplication : Application() {
    
    val settingsRepository by lazy { SettingsRepository(this) }
    val videoRepository by lazy { VideoRepository(this) }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        setupCrashHandler()
    }
    
    private fun setupCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            android.util.Log.e("KinoApp", "Uncaught exception in thread ${thread.name}", exception)
            // You can also write to a file or send to crash reporting service
            System.exit(1)
        }
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val recordingChannel = NotificationChannel(
                RECORDING_CHANNEL_ID,
                "Screen Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for screen recording status"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(recordingChannel)
        }
    }
    
    companion object {
        const val RECORDING_CHANNEL_ID = "recording_channel"
    }
}