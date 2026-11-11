package dev.jantozm.wallhaven.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.jantozm.wallhaven.data.WallhavenApi
import dev.jantozm.wallhaven.data.WallpaperRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CATEGORY_LABELS = linkedMapOf(
    "general" to "General",
    "anime" to "Anime",
    "people" to "People"
)

private val SORTING_OPTIONS = linkedMapOf(
    "relevance" to "Relevance",
    "random" to "Random",
    "date_added" to "Date added",
    "views" to "Views",
    "favorites" to "Favorites",
    "toplist" to "Toplist"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(onOpen: (WallhavenApi.Wallpaper) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { WallpaperRepository(context) }
    val state = remember { BrowseState(scope, repository) }

    var query by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf(setOf("general", "anime", "people")) }
    var sorting by remember { mutableStateOf("relevance") }
    var sortMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(query, categories, sorting) {
        delay(350)
        state.reload(query, categories, sorting)
    }

    val gridState = rememberLazyGridState()
    LaunchedEffect(state) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { index ->
                if (index >= state.items.size - 8) state.loadNext()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallhaven") },
                actions = {
                    Box {
                        IconButton(onClick = { sortMenuOpen = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = "Sort by")
                        }
                        DropdownMenu(
                            expanded = sortMenuOpen,
                            onDismissRequest = { sortMenuOpen = false }
                        ) {
                            SORTING_OPTIONS.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            label,
                                            color = if (sorting == value) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    },
                                    onClick = {
                                        sorting = value
                                        sortMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search wallpapers…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(CATEGORY_LABELS.entries.toList()) { (key, label) ->
                    FilterChip(
                        selected = key in categories,
                        onClick = {
                            categories = if (key in categories && categories.size > 1) {
                                categories - key
                            } else {
                                categories + key
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }

            when {
                state.error != null && state.items.isEmpty() -> ErrorView(
                    message = state.error.orEmpty(),
                    onRetry = { state.retry() }
                )
                state.items.isEmpty() && state.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                state.items.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No wallpapers found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(120.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    gridItems(state.items, key = { it.id }) { wallpaper ->
                        WallpaperCard(wallpaper = wallpaper, onClick = { onOpen(wallpaper) })
                    }
                    if (state.loading) {
                        item(span = { GridItemSpan(3) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                    state.error?.let { message ->
                        item(span = { GridItemSpan(3) }) {
                            Text(
                                message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperCard(wallpaper: WallhavenApi.Wallpaper, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        AsyncImage(
            model = wallpaper.thumbSmall.ifEmpty { wallpaper.path },
            contentDescription = "Wallpaper ${wallpaper.id}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
        )
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onRetry) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.size(4.dp))
            Text("Retry")
        }
    }
}

private class BrowseState(
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val repository: WallpaperRepository
) {
    var items by mutableStateOf(listOf<WallhavenApi.Wallpaper>())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var query = ""
    private var categories = setOf("general", "anime", "people")
    private var sorting = "relevance"
    private var page = 1
    private var lastPage = 1
    private var requestId = 0

    fun reload(query: String, categories: Set<String>, sorting: String) {
        this.query = query
        this.categories = categories
        this.sorting = sorting
        requestId++
        page = 1
        lastPage = 1
        items = emptyList()
        load(requestId)
    }

    fun retry() {
        reload(query, categories, sorting)
    }

    fun loadNext() {
        if (loading || page >= lastPage) return
        load(requestId)
    }

    private fun load(requestId: Int) {
        val nextPage = page
        scope.launch {
            loading = true
            error = null
            try {
                val result = repository.search(query, categories, sorting, nextPage)
                if (requestId == this@BrowseState.requestId) {
                    items = if (nextPage == 1) result.wallpapers else items + result.wallpapers
                    page = result.page
                    lastPage = result.lastPage
                }
            } catch (e: Exception) {
                if (requestId == this@BrowseState.requestId) {
                    error = e.message ?: "Something went wrong"
                }
            } finally {
                loading = false
            }
        }
    }
}
