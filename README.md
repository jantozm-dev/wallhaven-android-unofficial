# Wallhaven Daily (unofficial)

Unofficial Android app that automatically sets a new wallpaper once a day using the
[Wallhaven API](https://wallhaven.cc/help/api).

**SFW only** — every request is locked to `purity=100`, so no API key is required.

## Features

- **Browse wallpapers**: thumbnail grid with infinite scroll, live search, all categories
  (General / Anime / People) and every sorting option (Relevance, Random, Date added,
  Views, Favorites, Toplist).
- **Detail view** with preview, share button and one-tap "Set wallpaper".
- **Daily wallpaper refresh** via [WorkManager](https://developer.android.com/jetpack/androidx/releases/work)
  (runs once per day when the device has network access; survives reboots), with optional
  search tags and lock-screen support.
- Material You / Material 3 UI with dynamic color on Android 12+ and dark theme.
- SFW only: every request is locked to `purity=100`.
- Wallpapers are requested at (at least) half the device screen resolution and downscaled
  to the wallpaper size before being applied.

## Building

Open the project in Android Studio or run:

```bash
./gradlew assembleDebug
```

Requires JDK 17 and an Android SDK with platform 35.

## Project layout

```
app/src/main/java/dev/jantozm/wallhaven/
├── WallhavenApp.kt            # Application: schedules the daily WorkManager job
├── MainActivity.kt            # Hosts navigation (Browse / Daily) and snackbar feedback
├── Prefs.kt                   # SharedPreferences for settings and last status
├── WallpaperScheduler.kt      # Periodic + one-time work scheduling
├── data/WallhavenApi.kt       # OkHttp client for the Wallhaven v1 search API
├── data/WallpaperRepository.kt# Search + download to cache
├── ui/BrowseScreen.kt         # Thumbnail grid, search, category chips, sorting menu
├── ui/DetailScreen.kt         # Preview, share, set wallpaper
├── ui/DailyScreen.kt          # Daily automation settings
├── ui/theme/Theme.kt          # Material 3 dynamic color theme
└── work/DailyWallpaperWorker.kt # Decodes and applies the wallpaper
```

This is an unofficial project and is not affiliated with Wallhaven.
