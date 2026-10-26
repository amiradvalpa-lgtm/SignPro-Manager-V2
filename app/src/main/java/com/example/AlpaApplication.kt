package com.example

import android.app.Application
import com.example.util.CrashHandler

class AlpaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize global crash handler at the very first moment
        CrashHandler.init(this)
    }
}
