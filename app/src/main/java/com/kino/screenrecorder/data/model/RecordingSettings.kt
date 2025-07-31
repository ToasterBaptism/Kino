package com.kino.screenrecorder.data.model

import java.io.Serializable

data class RecordingSettings(
    val frameRate: Int = 30,
    val resolution: Resolution = Resolution.HD_1080P,
    val bitrate: Int = 8000000, // 8 Mbps
    val showTouches: Boolean = false,
    val audioSource: AudioSource = AudioSource.MUTE,
    val recordingMode: RecordingMode = RecordingMode.STANDARD,
    val instantReplayDuration: Int = 30, // seconds
    val timedRecordingDuration: Int = 60 // seconds
) : Serializable

enum class Resolution(val width: Int, val height: Int, val displayName: String) : Serializable {
    HD_720P(1280, 720, "720p HD"),
    HD_1080P(1920, 1080, "1080p Full HD"),
    QHD_1440P(2560, 1440, "1440p QHD"),
    UHD_4K(3840, 2160, "4K UHD")
}

enum class AudioSource(val displayName: String) : Serializable {
    MUTE("Mute"),
    MICROPHONE("Microphone"),
    INTERNAL("Internal Audio")
}

enum class RecordingMode(val displayName: String) : Serializable {
    STANDARD("Standard Mode"),
    INSTANT_REPLAY("Instant Replay Mode"),
    TIMED("Timed Recording Mode")
}

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    PROCESSING
}