package com.impostor.app

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSBundle
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import kotlin.coroutines.resume

internal actual suspend fun isAppUpdateAvailable(): Boolean = suspendCancellableCoroutine { continuation ->
    val center = NSNotificationCenter.defaultCenter
    val requestId = NSUUID.UUID().UUIDString
    var observer: Any? = null
    observer = center.addObserverForName("com.impostor.update.response", null, null) { notification ->
        val values = notification?.userInfo ?: return@addObserverForName
        if (values["requestId"] != requestId || !continuation.isActive) return@addObserverForName
        observer?.let(center::removeObserver)
        val installed = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
        val listed = values["version"] as? String
        val minimum = values["minimumOs"] as? String
        val available = installed != null && listed != null &&
            isNewerCompatibleListing(installed, listed, UIDevice.currentDevice.systemVersion, minimum)
        continuation.resume(available)
    }
    continuation.invokeOnCancellation { observer?.let(center::removeObserver) }
    center.postNotificationName("com.impostor.update.request", null, mapOf("requestId" to requestId))
}

internal actual suspend fun openAppUpdateStore(): Boolean = suspendCancellableCoroutine { continuation ->
    val url = NSURL.URLWithString("https://apps.apple.com/app/id6811896797")
    if (url == null) {
        continuation.resume(false)
    } else {
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>()) { opened ->
            if (continuation.isActive) continuation.resume(opened)
        }
    }
}
