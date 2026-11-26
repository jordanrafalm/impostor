package com.impostor.app

import android.content.Intent
import android.net.Uri
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal actual suspend fun isAppUpdateAvailable(): Boolean {
    val activity = MainActivity.currentActivity ?: run {
        println("App update check unavailable: no active Android activity")
        return false
    }
    return suspendCancellableCoroutine { continuation ->
        try {
            AppUpdateManagerFactory.create(activity).appUpdateInfo
                .addOnSuccessListener { info ->
                    if (continuation.isActive) continuation.resume(
                        info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE,
                    )
                }
                .addOnFailureListener { error ->
                    println("App update check failed: ${error.message ?: "unknown error"}")
                    if (continuation.isActive) continuation.resume(false)
                }
        } catch (error: Exception) {
            println("App update check failed: ${error.message ?: "unknown error"}")
            if (continuation.isActive) continuation.resume(false)
        }
    }
}

internal actual suspend fun openAppUpdateStore(): Boolean {
    val activity = MainActivity.currentActivity ?: run {
        println("Cannot open update store: no active Android activity")
        return false
    }
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}"))
        activity.startActivity(intent)
        true
    } catch (error: Exception) {
        println("Cannot open update store: ${error.message ?: "unknown error"}")
        false
    }
}
