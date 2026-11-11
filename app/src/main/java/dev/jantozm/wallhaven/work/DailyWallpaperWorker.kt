package dev.jantozm.wallhaven.work

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.jantozm.wallhaven.Prefs
import dev.jantozm.wallhaven.data.WallpaperRepository
import java.text.DateFormat
import java.util.Date
import kotlin.math.max

class DailyWallpaperWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        val imageUrl = inputData.getString(KEY_IMAGE_URL)
        return try {
            applyWallpaper(prefs, imageUrl)
            prefs.lastAppliedAt = System.currentTimeMillis()
            prefs.lastStatus = "Updated " + DateFormat.getDateTimeInstance().format(Date())
            Result.success()
        } catch (e: Exception) {
            prefs.lastStatus = "Failed: ${e.message ?: "unknown error"}"
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    private fun applyWallpaper(prefs: Prefs, imageUrl: String?) {
        val repository = WallpaperRepository(applicationContext)
        val file = if (imageUrl != null) {
            repository.downloadWallpaper(imageUrl)
        } else {
            repository.fetchRandomWallpaperFile(prefs.searchQuery)
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        val wallpaperManager = WallpaperManager.getInstance(applicationContext)
        val sampleSize = sampleSizeFor(options.outWidth, options.outHeight, wallpaperManager)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            ?: throw IllegalStateException("Downloaded image could not be decoded")
        file.delete()

        val flags = if (prefs.alsoSetLockScreen) {
            WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        } else {
            WallpaperManager.FLAG_SYSTEM
        }
        wallpaperManager.setBitmap(bitmap, null, true, flags)
        bitmap.recycle()
    }

    private fun sampleSizeFor(
        width: Int,
        height: Int,
        wallpaperManager: WallpaperManager
    ): Int {
        if (width <= 0 || height <= 0) return 1
        val desired = wallpaperManager.desiredMinimumWidth
        val targetWidth = if (desired > 0) desired else width
        var sample = 1
        while (width / (sample * 2) >= targetWidth) {
            sample *= 2
        }
        return max(sample, 1)
    }

    companion object {
        private const val KEY_IMAGE_URL = "image_url"
        private const val MAX_RETRIES = 3

        fun enqueueOneTime(context: Context, imageUrl: String? = null) {
            val builder = OneTimeWorkRequestBuilder<DailyWallpaperWorker>()
            if (imageUrl != null) {
                builder.setInputData(workDataOf(KEY_IMAGE_URL to imageUrl))
            }
            WorkManager.getInstance(context).enqueueUniqueWork(
                "manual-wallpaper-update",
                ExistingWorkPolicy.REPLACE,
                builder.build()
            )
        }
    }
}
