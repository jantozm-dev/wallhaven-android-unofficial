package dev.jantozm.wallhaven

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.jantozm.wallhaven.data.WallhavenApi
import dev.jantozm.wallhaven.ui.BrowseScreen
import dev.jantozm.wallhaven.ui.DailyScreen
import dev.jantozm.wallhaven.ui.DetailScreen
import dev.jantozm.wallhaven.ui.theme.WallhavenTheme
import dev.jantozm.wallhaven.work.DailyWallpaperWorker

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WallhavenTheme {
                WallhavenAppUi()
            }
        }
    }
}

@Composable
fun WallhavenAppUi() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    var tab by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<WallhavenApi.Wallpaper?>(null) }
    var lastStatus by remember { mutableStateOf(prefs.lastStatus) }

    val workInfos by remember {
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow("manual-wallpaper-update")
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val isRunning = workInfos.any {
        it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
    }

    LaunchedEffect(workInfos) {
        val finished = workInfos.firstOrNull { it.state.isFinished }
        if (finished != null && !isRunning) {
            lastStatus = prefs.lastStatus
            snackbarHostState.showSnackbar(
                if (finished.state == WorkInfo.State.SUCCEEDED) {
                    "Wallpaper applied"
                } else {
                    prefs.lastStatus.ifBlank { "Could not apply wallpaper" }
                }
            )
        }
    }

    val selectedWallpaper = selected
    if (selectedWallpaper != null) {
        DetailScreen(
            wallpaper = selectedWallpaper,
            applying = isRunning,
            lockScreenEnabled = prefs.alsoSetLockScreen,
            onBack = { selected = null },
            onSetWallpaper = {
                DailyWallpaperWorker.enqueueOneTime(context, selectedWallpaper.path)
            }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text("Browse") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                    label = { Text("Daily") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                0 -> BrowseScreen(onOpen = { selected = it })
                else -> DailyScreen(
                    prefs = prefs,
                    isRunning = isRunning,
                    lastStatus = lastStatus,
                    onSetNow = { WallpaperScheduler.runNow(context) }
                )
            }
        }
    }
}
