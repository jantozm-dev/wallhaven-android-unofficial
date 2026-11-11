package dev.jantozm.wallhaven.data

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import kotlin.random.Random

class WallpaperRepository(context: Context) {

    private val appContext = context.applicationContext
    private val api = WallhavenApi()

    fun search(
        query: String,
        categories: Set<String>,
        sorting: String,
        page: Int
    ): WallhavenApi.SearchResult {
        val metrics = screenMetrics()
        return api.search(
            query = query,
            categories = categories,
            sorting = sorting,
            page = page,
            minWidth = metrics.widthPixels.coerceAtLeast(1280) / 2,
            minHeight = metrics.heightPixels.coerceAtLeast(720) / 2
        )
    }

    fun fetchRandomWallpaperFile(query: String): File {
        val metrics = screenMetrics()
        val results = api.search(
            query = query,
            categories = emptySet(),
            sorting = "random",
            page = 1,
            minWidth = metrics.widthPixels.coerceAtLeast(1280) / 2,
            minHeight = metrics.heightPixels.coerceAtLeast(720) / 2
        )
        if (results.wallpapers.isEmpty()) {
            throw IllegalStateException("Wallhaven returned no results for this search")
        }
        val wallpaper = results.wallpapers[Random.nextInt(results.wallpapers.size)]
        return downloadWallpaper(wallpaper.path)
    }

    fun downloadWallpaper(url: String): File {
        val destination = File(appContext.cacheDir, "wallpaper.jpg")
        if (destination.exists()) destination.delete()
        return api.download(url, destination)
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
