package com.impostor.app

import android.media.MediaDataSource
import android.media.MediaPlayer
import android.util.Log
import java.io.IOException

private const val TAG = "LaunchSound"

private class AndroidLaunchSoundPlayer : LaunchSoundPlayer {
    private var player: MediaPlayer? = null

    override fun play(bytes: ByteArray) {
        release()
        val next = try {
            MediaPlayer()
        } catch (error: RuntimeException) {
            Log.w(TAG, "Cannot create launch audio player", error)
            return
        }
        player = next
        try {
            next.setDataSource(object : MediaDataSource() {
                override fun getSize(): Long = bytes.size.toLong()

                override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
                    if (position < 0 || position >= bytes.size) return -1
                    val start = position.toInt()
                    val count = minOf(size, bytes.size - start)
                    bytes.copyInto(buffer, offset, start, start + count)
                    return count
                }

                override fun close() = Unit
            })
            next.setOnPreparedListener {
                try {
                    it.start()
                } catch (error: IllegalStateException) {
                    Log.w(TAG, "Cannot start launch audio", error)
                    releasePlayer(it)
                }
            }
            next.setOnCompletionListener { releasePlayer(it) }
            next.setOnErrorListener { failed, what, extra ->
                Log.w(TAG, "Launch audio playback failed: $what/$extra")
                releasePlayer(failed)
                true
            }
            next.prepareAsync()
        } catch (error: IOException) {
            Log.w(TAG, "Cannot load launch audio", error)
            releasePlayer(next)
        } catch (error: IllegalArgumentException) {
            Log.w(TAG, "Invalid launch audio", error)
            releasePlayer(next)
        } catch (error: IllegalStateException) {
            Log.w(TAG, "Cannot prepare launch audio", error)
            releasePlayer(next)
        } catch (error: SecurityException) {
            Log.w(TAG, "Launch audio access denied", error)
            releasePlayer(next)
        }
    }

    private fun releasePlayer(candidate: MediaPlayer) {
        if (player === candidate) {
            player = null
            candidate.release()
        }
    }

    override fun release() {
        player?.let(::releasePlayer)
    }
}

internal actual fun platformLaunchSoundPlayer(): LaunchSoundPlayer = AndroidLaunchSoundPlayer()
