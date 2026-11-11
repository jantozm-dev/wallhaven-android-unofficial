package dev.jantozm.wallhaven

import android.app.Application

class WallhavenApp : Application() {

    override fun onCreate() {
        super.onCreate()
        WallpaperScheduler.scheduleDaily(this)
    }
}
