package com.impostor.app

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

private class AndroidSoundPlayer : SoundPlayer {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)

    override fun playThermalStart() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 90)
    }

    override fun playFreezeCrack() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 120)
    }
}

actual fun platformSoundPlayer(): SoundPlayer = AndroidSoundPlayer()

private class AndroidThermalFeedback : ThermalFeedback {
    override fun start() {
        vibrate(durationMillis = 70, amplitude = 255)
    }

    override fun pulse() {
        vibrate(durationMillis = 18, amplitude = 180)
    }

    override fun complete() {
        vibrate(durationMillis = 100, amplitude = 255)
    }

    private fun vibrate(durationMillis: Long, amplitude: Int) {
        val vibrator = MainActivity.currentActivity?.getSystemService(Vibrator::class.java)
        if (vibrator?.hasVibrator() != true) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMillis, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMillis)
            }
        } catch (_: SecurityException) { }
    }
}

actual fun platformThermalFeedback(): ThermalFeedback = AndroidThermalFeedback()