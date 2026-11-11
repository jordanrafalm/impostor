package com.impostor.app

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class IosLaunchSoundPlayer : LaunchSoundPlayer {
    private var player: AVAudioPlayer? = null
    private val delegate = object : NSObject(), AVAudioPlayerDelegateProtocol {
        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
            if (!successfully) println("Launch audio did not finish successfully")
            releasePlayer(player)
        }

        override fun audioPlayerDecodeErrorDidOccur(player: AVAudioPlayer, error: NSError?) {
            println("Launch audio decode failed: ${error?.localizedDescription}")
            releasePlayer(player)
        }
    }

    // The nullable Objective-C initializer lets invalid audio fail without throwing.
    @Suppress("DEPRECATION", "DEPRECATION_ERROR")
    override fun play(bytes: ByteArray) {
        release()
        if (bytes.isEmpty()) {
            println("Launch audio is empty")
            return
        }
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            val next = AVAudioPlayer.alloc()?.initWithData(data, error.ptr)
            if (next == null) {
                println("Cannot load launch audio: ${error.value?.localizedDescription}")
                return
            }
            player = next
            next.delegate = delegate
            if (!next.prepareToPlay() || !next.play()) {
                println("Cannot start launch audio")
                releasePlayer(next)
            }
        }
    }

    private fun releasePlayer(candidate: AVAudioPlayer) {
        if (player === candidate) {
            candidate.delegate = null
            candidate.stop()
            player = null
        }
    }

    override fun release() {
        player?.let(::releasePlayer)
    }
}

internal actual fun platformLaunchSoundPlayer(): LaunchSoundPlayer = IosLaunchSoundPlayer()
