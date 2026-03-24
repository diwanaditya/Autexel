package com.autexel.app

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

class AutoxelApplication : Application() {

    companion object {
        private const val TAG = "AutoxelApplication"
    }

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning received - clearing caches")
        // Trim caches to avoid OOM during large POI operations
        System.gc()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            Log.w(TAG, "Moderate memory trim requested, level=$level")
            System.gc()
        }
    }
}

class AppLifecycleObserver : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        // App moved to foreground
    }
    override fun onStop(owner: LifecycleOwner) {
        // App moved to background — good time to release heavy resources
    }
}
