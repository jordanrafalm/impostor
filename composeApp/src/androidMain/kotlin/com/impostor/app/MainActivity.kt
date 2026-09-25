package com.impostor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.impostor.data.initializeBundledCatalog
import com.impostor.data.initializePlatformStoreBilling

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentActivity = this
        initializeBundledCatalog(applicationContext)
        initializePlatformStoreBilling(this)
        enableEdgeToEdge()
        setContent { ImpostorApp() }
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
