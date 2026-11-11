package dev.jantozm.wallhaven

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wallhaven_prefs", Context.MODE_PRIVATE)

    var searchQuery: String
        get() = prefs.getString(KEY_QUERY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_QUERY, value).apply()

    var alsoSetLockScreen: Boolean
        get() = prefs.getBoolean(KEY_LOCK_SCREEN, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_SCREEN, value).apply()

    var lastAppliedAt: Long
        get() = prefs.getLong(KEY_LAST_APPLIED, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_APPLIED, value).apply()

    var lastStatus: String
        get() = prefs.getString(KEY_LAST_STATUS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_STATUS, value).apply()

    companion object {
        private const val KEY_QUERY = "search_query"
        private const val KEY_LOCK_SCREEN = "also_lock_screen"
        private const val KEY_LAST_APPLIED = "last_applied_at"
        private const val KEY_LAST_STATUS = "last_status"
    }
}
