package com.kino.screenrecorder.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kino.screenrecorder.data.model.AudioSource
import com.kino.screenrecorder.data.model.RecordingMode
import com.kino.screenrecorder.data.model.RecordingSettings
import com.kino.screenrecorder.data.model.Resolution
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    
    private object PreferencesKeys {
        val FRAME_RATE = intPreferencesKey("frame_rate")
        val RESOLUTION = stringPreferencesKey("resolution")
        val BITRATE = intPreferencesKey("bitrate")
        val SHOW_TOUCHES = booleanPreferencesKey("show_touches")
        val AUDIO_SOURCE = stringPreferencesKey("audio_source")
        val RECORDING_MODE = stringPreferencesKey("recording_mode")
        val INSTANT_REPLAY_DURATION = intPreferencesKey("instant_replay_duration")
        val TIMED_RECORDING_DURATION = intPreferencesKey("timed_recording_duration")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    }
    
    val recordingSettings: Flow<RecordingSettings> = context.dataStore.data.map { preferences ->
        RecordingSettings(
            frameRate = preferences[PreferencesKeys.FRAME_RATE] ?: 30,
            resolution = Resolution.valueOf(
                preferences[PreferencesKeys.RESOLUTION] ?: Resolution.HD_1080P.name
            ),
            bitrate = preferences[PreferencesKeys.BITRATE] ?: 8000000,
            showTouches = preferences[PreferencesKeys.SHOW_TOUCHES] ?: false,
            audioSource = AudioSource.valueOf(
                preferences[PreferencesKeys.AUDIO_SOURCE] ?: AudioSource.MUTE.name
            ),
            recordingMode = RecordingMode.valueOf(
                preferences[PreferencesKeys.RECORDING_MODE] ?: RecordingMode.STANDARD.name
            ),
            instantReplayDuration = preferences[PreferencesKeys.INSTANT_REPLAY_DURATION] ?: 30,
            timedRecordingDuration = preferences[PreferencesKeys.TIMED_RECORDING_DURATION] ?: 60
        )
    }
    
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.FIRST_LAUNCH] ?: true
    }
    
    suspend fun updateFrameRate(frameRate: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FRAME_RATE] = frameRate
        }
    }
    
    suspend fun updateResolution(resolution: Resolution) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RESOLUTION] = resolution.name
        }
    }
    
    suspend fun updateBitrate(bitrate: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BITRATE] = bitrate
        }
    }
    
    suspend fun updateShowTouches(showTouches: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_TOUCHES] = showTouches
        }
    }
    
    suspend fun updateAudioSource(audioSource: AudioSource) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_SOURCE] = audioSource.name
        }
    }
    
    suspend fun updateRecordingMode(recordingMode: RecordingMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RECORDING_MODE] = recordingMode.name
        }
    }
    
    suspend fun updateInstantReplayDuration(duration: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.INSTANT_REPLAY_DURATION] = duration
        }
    }
    
    suspend fun updateTimedRecordingDuration(duration: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TIMED_RECORDING_DURATION] = duration
        }
    }
    
    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_LAUNCH] = false
        }
    }
    
    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}