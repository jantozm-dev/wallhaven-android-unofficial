package dev.jantozm.wallhaven

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.jantozm.wallhaven.work.DailyWallpaperWorker

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }

    var query by remember { mutableStateOf(prefs.searchQuery) }
    var alsoLockScreen by remember { mutableStateOf(prefs.alsoSetLockScreen) }
    var lastStatus by remember { mutableStateOf(prefs.lastStatus) }

    val workInfos by remember {
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow("manual-wallpaper-update")
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val isRunning = workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }

    LaunchedEffect(workInfos) {
        if (!isRunning) lastStatus = prefs.lastStatus
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "A new SFW wallpaper from wallhaven.cc is applied once a day while the device is online.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search tags (optional)") },
                placeholder = { Text("e.g. nature, landscape") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Also set lock screen",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = alsoLockScreen,
                    onCheckedChange = { alsoLockScreen = it }
                )
            }

            Button(
                onClick = {
                    prefs.searchQuery = query
                    prefs.alsoSetLockScreen = alsoLockScreen
                    WallpaperScheduler.runNow(context)
                },
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Set wallpaper now")
            }

            if (isRunning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    Text("Fetching wallpaper…", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Last update",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = lastStatus.ifBlank { "Not updated yet" },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
