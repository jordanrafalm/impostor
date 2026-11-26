package com.impostor.app

/** Unknown or invalid store information is never treated as an available update. */
internal expect suspend fun isAppUpdateAvailable(): Boolean
internal expect suspend fun openAppUpdateStore(): Boolean

internal fun compareNumericVersions(left: String, right: String): Int? {
    fun parts(value: String): List<String>? {
        val components = value.split('.')
        if (components.any { it.isEmpty() || it.any { c -> c !in '0'..'9' } }) return null
        return components.map { it.trimStart('0').ifEmpty { "0" } }
    }
    val a = parts(left) ?: return null
    val b = parts(right) ?: return null
    for (index in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(index) { "0" }
        val y = b.getOrElse(index) { "0" }
        if (x.length != y.length) return x.length.compareTo(y.length)
        if (x != y) return x.compareTo(y)
    }
    return 0
}

internal fun isNewerCompatibleListing(
    installed: String,
    listed: String,
    deviceOs: String,
    minimumOs: String?,
): Boolean {
    val newer = compareNumericVersions(listed, installed) ?: return false
    if (minimumOs != null && (compareNumericVersions(deviceOs, minimumOs) ?: return false) < 0) return false
    return newer > 0
}

internal fun mayOfferUpdate(
    isHome: Boolean,
    isForeground: Boolean,
    hasHigherPriorityPrompt: Boolean,
    notificationStarted: Boolean,
): Boolean = isHome && isForeground && !hasHigherPriorityPrompt && !notificationStarted

