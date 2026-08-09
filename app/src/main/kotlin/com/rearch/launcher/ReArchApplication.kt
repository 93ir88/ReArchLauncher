package com.rearch.launcher

import android.app.Application
import com.rearch.launcher.services.MagiskBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReArchApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Magisk bridge and enable freeform mode on first launch
        applicationScope.launch(Dispatchers.IO) {
            MagiskBridge.init(applicationContext)
        }
    }

    companion object {
        lateinit var instance: ReArchApplication
            private set
    }
}
