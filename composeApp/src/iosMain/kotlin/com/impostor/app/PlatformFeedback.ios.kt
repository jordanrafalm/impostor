package com.impostor.app

import platform.AudioToolbox.AudioServicesPlaySystemSound

private class IosSoundPlayer : SoundPlayer {
    override fun playThermalStart() {
        AudioServicesPlaySystemSound(1103u)
    }

    override fun playFreezeCrack() {
        AudioServicesPlaySystemSound(1104u)
    }
}

actual fun platformSoundPlayer(): SoundPlayer = IosSoundPlayer()

private class IosThermalFeedback : ThermalFeedback {
    override fun start() = Unit

    override fun pulse() = Unit

    override fun complete() = Unit
}

actual fun platformThermalFeedback(): ThermalFeedback = IosThermalFeedback()