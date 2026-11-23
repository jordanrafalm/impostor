package com.impostor.app

import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.impostor.data.initializeBundledCatalog
import com.impostor.data.initializePlatformStoreBilling

class MainActivity : ComponentActivity() {
    private var notificationPermissionResult: ((Boolean) -> Unit)? = null
    private val notificationPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationPermissionResult?.invoke(granted)
            notificationPermissionResult = null
        }

    fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        notificationPermissionResult = onResult
        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        currentActivity = this
        initializePreferenceStore(applicationContext)
        initializeBundledCatalog(applicationContext)
        initializePlatformStoreBilling(this)
        enableEdgeToEdge()
        setContent { ImpostorApp(debugUnlockCategories = com.rafal.impostorparty.BuildConfig.DEBUG) }
    }

    override fun onDestroy() {
        if (currentActivity === this) currentActivity = null
        super.onDestroy()
    }

    companion object {
        var currentActivity: MainActivity? = null
            private set
    }
}
