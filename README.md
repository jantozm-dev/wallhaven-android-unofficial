# Wallhaven Daily (unofficial)

Unofficial Android app that automatically sets a new wallpaper once a day using the
[Wallhaven API](https://wallhaven.cc/help/api).

**SFW only** — every request is locked to `purity=100`, so no API key is required.

## Features

- Daily wallpaper refresh via [WorkManager](https://developer.android.com/jetpack/androidx/releases/work)
  (runs once per day when the device has network access; survives reboots).
- Manual "Set wallpaper now" button.
- Optional search tags (e.g. `nature`, `landscape`) forwarded to the Wallhaven search API.
- Option to also apply the wallpaper to the lock screen.
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
├── MainActivity.kt            # Compose UI: search tags, lock-screen toggle, manual update
├── Prefs.kt                   # SharedPreferences for settings and last status
├── WallpaperScheduler.kt      # Periodic + one-time work scheduling
├── data/WallhavenApi.kt       # OkHttp client for the Wallhaven v1 search API
├── data/WallpaperRepository.kt# Picks a random result and downloads it to cache
└── work/DailyWallpaperWorker.kt # Decodes and applies the wallpaper
```

This is an unofficial project and is not affiliated with Wallhaven.
