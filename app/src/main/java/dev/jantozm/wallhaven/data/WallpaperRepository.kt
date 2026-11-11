package dev.jantozm.wallhaven.data

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import kotlin.random.Random

class WallpaperRepository(context: Context) {

    private val appContext = context.applicationContext
    private val api = WallhavenApi()

    fun fetchRandomWallpaperFile(query: String): File {
        val metrics = screenMetrics()
        val results = api.searchRandomSfw(
            query = query,
            minWidth = metrics.widthPixels.coerceAtLeast(1280) / 2,
            minHeight = metrics.heightPixels.coerceAtLeast(720) / 2
        )
        if (results.isEmpty()) {
            throw IllegalStateException("Wallhaven returned no results for this search")
        }
        val wallpaper = results[Random.nextInt(results.size)]
        val destination = File(appContext.cacheDir, "daily-wallpaper.jpg")
        if (destination.exists()) destination.delete()
        return api.download(wallpaper.path, destination)
    }

    private fun screenMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()
        val windowManager = appContext.getSystemService(WindowManager::class.java)
        if (windowManager != null) {
            windowManager.defaultDisplay.getRealMetrics(metrics)
        } else {
            metrics.widthPixels = 1920
            metrics.heightPixels = 1080
        }
        return metrics
    }
}
