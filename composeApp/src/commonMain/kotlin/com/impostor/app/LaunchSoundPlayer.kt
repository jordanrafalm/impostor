package com.impostor.app

internal interface LaunchSoundPlayer {
    fun play(bytes: ByteArray)
    fun release()
}

internal expect fun platformLaunchSoundPlayer(): LaunchSoundPlayer
