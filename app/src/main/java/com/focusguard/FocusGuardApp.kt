package com.focusguard

import android.app.Application
import android.util.Log
import com.focusguard.installer.FastInstallSuspender

class FocusGuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("FocusGuardApp", "Focus Guard initialized.")
        FastInstallSuspender.getInstance(this).startWatching()
    }
}
