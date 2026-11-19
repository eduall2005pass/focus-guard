package com.focusguard

import android.app.Application
import android.util.Log

class FocusGuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("FocusGuardApp", "Focus Guard initialized.")
    }
}
