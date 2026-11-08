package com.xboyrex.miunlocktool

import android.app.Application
import android.content.Context

class MiUnlockApplication : Application() {

    companion object {
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        ThemeSettings.init(this)
        GlassQuality.init(this)
        GlassSettings.init(this)
        LanguageSettings.init(this)
    }
}
