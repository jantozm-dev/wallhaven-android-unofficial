package dev.jantozm.wallhaven

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.Constraints
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.jantozm.wallhaven.work.DailyWallpaperWorker
import java.util.concurrent.TimeUnit

object WallpaperScheduler {

    private const val DAILY_WORK_NAME = "daily-wallpaper"

    fun scheduleDaily(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<DailyWallpaperWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun runNow(context: Context) {
        DailyWallpaperWorker.enqueueOneTime(context)
    }
}
