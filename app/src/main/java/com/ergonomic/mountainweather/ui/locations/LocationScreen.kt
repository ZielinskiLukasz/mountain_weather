package com.ergonomic.mountainweather.ui.locations

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ergonomic.mountainweather.R
import com.ergonomic.mountainweather.data.GeocodingResult
import com.ergonomic.mountainweather.data.local.SavedLocationEntity
import com.ergonomic.mountainweather.util.findMatching
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    onLocationSelected: (name: String, lat: Double, lon: Double) -> Unit,
    onBack: () -> Unit,
    viewModel: LocationViewModel = viewModel(),
    requestGpsOnOpen: Boolean = false
) {
    val state by viewModel.uiState.collectAsState()
    val selected by viewModel.selectedLocation.collectAsState()
    val savedLists by viewModel.savedLists.collectAsState()
    val favorites = savedLists.favorites
    val recent = savedLists.recent
    val listsReady = savedLists.ready

    LaunchedEffect(selected) {
        selected?.let {
            onLocationSelected(it.name, it.latitude, it.longitude)
        }
    }

    val focusRequester = remember { FocusRequester() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            viewModel.requestGpsLocation()
        }
    }

    LaunchedEffect(requestGpsOnOpen) {
        if (requestGpsOnOpen) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val context = LocalContext.current

    if (state.showEnableLocation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissEnableLocation() },
            title = { Text(stringResource(R.string.location_disabled_title)) },
            text = { Text(stringResource(R.string.location_disabled_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissEnableLocation()
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) {
                    Text(stringResource(R.string.location_disabled_enable))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissEnableLocation() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_city)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                value = state.query,
                onValueChange = { viewModel.onQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.search_city)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                enabled = !state.isLocating
            ) {
                if (state.isLocating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.gps_locating))
                } else {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.use_gps))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            val isSearchActive = state.query.length >= 2

            when {
                state.isSearching -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                isSearchActive && state.results.isEmpty() && state.placeResults.isEmpty() && !state.isSearching -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.no_results),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                isSearchActive -> {
                    val mergedPlaces = remember(state.placeResults, favorites, recent) {
                        mergeSearchHits(state.placeResults, favorites, recent)
                    }
                    val mergedCities = remember(state.results, favorites, recent) {
                        mergeSearchHits(state.results, favorites, recent)
                    }
                    LazyColumn {
                        if (mergedPlaces.isNotEmpty()) {
                            item(key = "places_header") {
                                SectionHeader(stringResource(R.string.search_places))
                            }
                            items(mergedPlaces, key = { "p_${it.result.id}" }) { hit ->
                                SearchResultItem(
                                    result = hit.result,
                                    saved = hit.saved,
                                    onClick = {
                                        val saved = hit.saved
                                        if (saved != null) viewModel.selectSavedLocation(saved)
                                        else viewModel.selectSearchResult(hit.result)
                                    }
                                )
                            }
                        }
                        if (mergedCities.isNotEmpty()) {
                            item(key = "cities_header") {
                                SectionHeader(stringResource(R.string.search_cities))
                            }
                            items(mergedCities, key = { "c_${it.result.id}" }) { hit ->
                                SearchResultItem(
                                    result = hit.result,
                                    saved = hit.saved,
                                    onClick = {
                                        val saved = hit.saved
                                        if (saved != null) viewModel.selectSavedLocation(saved)
                                        else viewModel.selectSearchResult(hit.result)
                                    }
                                )
                            }
                        }
                    }
                }
                else -> {
                    if (!listsReady) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        SavedLocationsContent(
                            favorites = favorites,
                            recent = recent,
                            onSelect = { viewModel.selectSavedLocation(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onDelete = { viewModel.deleteLocation(it) },
                            onReorder = { viewModel.reorderFavorites(it) },
                            onClearAllRecent = { viewModel.clearAllRecent() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedLocationsContent(
    favorites: List<SavedLocationEntity>,
    recent: List<SavedLocationEntity>,
    onSelect: (SavedLocationEntity) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onClearAllRecent: () -> Unit
) {
    var showClearRecentDialog by remember { mutableStateOf(false) }

    if (showClearRecentDialog) {
        AlertDialog(
            onDismissRequest = { showClearRecentDialog = false },
            title = { Text(stringResource(R.string.clear_all_recent)) },
            text = { Text(stringResource(R.string.clear_all_recent_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearRecentDialog = false
                    onClearAllRecent()
                }) {
                    Text(stringResource(R.string.clear_all_recent))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearRecentDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (favorites.isEmpty() && recent.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.no_saved_locations),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val orderedFavs = remember { mutableStateListOf<SavedLocationEntity>() }
    LaunchedEffect(favorites) {
        orderedFavs.clear()
        orderedFavs.addAll(favorites)
    }

    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 72.dp.toPx() }
    val listState = rememberLazyListState()

    // Always open with favorites (top of list) visible.
    LaunchedEffect(Unit) {
        listState.scrollToItem(0)
    }

    LazyColumn(state = listState) {
        if (orderedFavs.isNotEmpty()) {
            item(key = "fav_header") {
                SectionHeader(stringResource(R.string.favorites))
            }
            itemsIndexed(orderedFavs, key = { _, loc -> "fav_${loc.id}" }) { index, location ->
                val isDragged = index == draggedIndex
                Box(
                    modifier = Modifier
                        .zIndex(if (isDragged) 10f else 0f)
                        .offset {
                            if (isDragged) {
                                val positionShift = (draggedIndex - dragStartIndex) * itemHeightPx
                                IntOffset(0, (dragOffsetY - positionShift).roundToInt())
                            } else IntOffset.Zero
                        }
                        .graphicsLayer {
                            if (isDragged) {
                                scaleX = 1.03f
                                scaleY = 1.03f
                                shadowElevation = 8f
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    draggedIndex = index
                                    dragStartIndex = index
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                    val targetIndex = (dragStartIndex + (dragOffsetY / itemHeightPx).roundToInt())
                                        .coerceIn(0, orderedFavs.lastIndex)
                                    if (targetIndex != draggedIndex) {
                                        orderedFavs.add(targetIndex, orderedFavs.removeAt(draggedIndex))
                                        draggedIndex = targetIndex
                                    }
                                },
                                onDragEnd = {
                                    draggedIndex = -1
                                    dragStartIndex = -1
                                    dragOffsetY = 0f
                                    onReorder(orderedFavs.map { it.id })
                                },
                                onDragCancel = {
                                    draggedIndex = -1
                                    dragStartIndex = -1
                                    dragOffsetY = 0f
                                }
                            )
                        }
                ) {
                    SavedLocationItem(
                        location = location,
                        onSelect = { onSelect(location) },
                        onToggleFavorite = { onToggleFavorite(location.id) },
                        onDelete = { onDelete(location.id) }
                    )
                }
            }
        }

        if (recent.isNotEmpty()) {
            item(key = "recent_header") {
                RecentSectionHeader(
                    title = stringResource(R.string.recent_locations),
                    showClearAll = recent.size >= 5,
                    onClearAll = { showClearRecentDialog = true }
                )
            }
            items(recent, key = { "rec_${it.id}" }) { location ->
                SavedLocationItem(
                    location = location,
                    onSelect = { onSelect(location) },
                    onToggleFavorite = { onToggleFavorite(location.id) },
                    onDelete = { onDelete(location.id) }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun RecentSectionHeader(
    title: String,
    showClearAll: Boolean,
    onClearAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        if (showClearAll) {
            TextButton(onClick = onClearAll) {
                Text(stringResource(R.string.clear_all_recent))
            }
        }
    }
}

@Composable
fun SavedLocationItem(
    location: SavedLocationEntity,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        headlineContent = { Text(location.name) },
        supportingContent = {
            val parts = listOfNotNull(location.region, location.country)
            if (parts.isNotEmpty()) {
                Text(parts.joinToString(", "))
            }
        },
        trailingContent = {
            Row {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (location.isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        modifier = Modifier.clickable(onClick = onSelect)
    )
}

@Composable
fun SearchResultItem(
    result: GeocodingResult,
    saved: SavedLocationEntity? = null,
    onClick: () -> Unit
) {
    val title = saved?.name ?: result.name
    val subtitleParts = if (saved != null) {
        listOfNotNull(saved.region, saved.country)
    } else {
        listOfNotNull(result.region, result.country)
    }
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            if (subtitleParts.isNotEmpty()) {
                Text(subtitleParts.joinToString(", "))
            }
        },
        trailingContent = if (saved?.isFavorite == true) {
            {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else null,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

private data class SearchHit(
    val result: GeocodingResult,
    val saved: SavedLocationEntity?
)

private fun mergeSearchHits(
    results: List<GeocodingResult>,
    favorites: List<SavedLocationEntity>,
    recent: List<SavedLocationEntity>
): List<SearchHit> {
    val saved = favorites + recent
    val usedIds = mutableSetOf<Long>()
    return results.mapNotNull { result ->
        val match = saved.findMatching(result.latitude, result.longitude, result.name, result.country)
        if (match != null) {
            if (!usedIds.add(match.id)) return@mapNotNull null
        }
        SearchHit(result, match)
    }
}
