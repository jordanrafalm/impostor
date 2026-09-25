package com.impostor.app

interface SoundPlayer {
    fun playThermalStart()
    fun playFreezeCrack()
}

expect fun platformSoundPlayer(): SoundPlayer

interface ThermalFeedback {
    fun start()
    fun pulse()
    fun complete()
}

expect fun platformThermalFeedback(): ThermalFeedback