package com.impostor.app

import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

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
    private val impactGenerator = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val pulseGenerator = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val notificationGenerator = UINotificationFeedbackGenerator()

    override fun start() {
        impactGenerator.prepare()
        impactGenerator.impactOccurred()
    }

    override fun pulse() {
        pulseGenerator.prepare()
        pulseGenerator.impactOccurred()
    }

    override fun complete() {
        notificationGenerator.prepare()
        notificationGenerator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
    }
}

actual fun platformThermalFeedback(): ThermalFeedback = IosThermalFeedback()