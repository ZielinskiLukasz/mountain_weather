package com.ergonomic.mountainweather.ui.locations

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ergonomic.mountainweather.R
import com.ergonomic.mountainweather.data.GeocodingResult
import com.ergonomic.mountainweather.data.local.SavedLocationEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    onLocationSelected: (name: String, lat: Double, lon: Double) -> Unit,
    onBack: () -> Unit,
    viewModel: LocationViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val selected by viewModel.selectedLocation.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val recent by viewModel.recentLocations.collectAsState()

    LaunchedEffect(selected) {
        selected?.let {
            onLocationSelected(it.name, it.latitude, it.longitude)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            viewModel.requestGpsLocation()
        }
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
                    .padding(horizontal = 16.dp),
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
                isSearchActive && state.results.isEmpty() && !state.isSearching -> {
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
                    LazyColumn {
                        items(state.results, key = { it.id }) { result ->
                            SearchResultItem(
                                result = result,
                                onClick = { viewModel.selectSearchResult(result) }
                            )
                        }
                    }
                }
                else -> {
                    SavedLocationsContent(
                        favorites = favorites,
                        recent = recent,
                        onSelect = { viewModel.selectSavedLocation(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDelete = { viewModel.deleteLocation(it) }
                    )
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
    onDelete: (Long) -> Unit
) {
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

    LazyColumn {
        if (favorites.isNotEmpty()) {
            item(key = "fav_header") {
                SectionHeader(stringResource(R.string.favorites))
            }
            items(favorites, key = { "fav_${it.id}" }) { location ->
                SavedLocationItem(
                    location = location,
                    onSelect = { onSelect(location) },
                    onToggleFavorite = { onToggleFavorite(location.id) },
                    onDelete = { onDelete(location.id) }
                )
            }
        }

        if (recent.isNotEmpty()) {
            item(key = "recent_header") {
                SectionHeader(stringResource(R.string.recent_locations))
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
fun SearchResultItem(result: GeocodingResult, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(result.name) },
        supportingContent = {
            val parts = listOfNotNull(result.region, result.country)
            if (parts.isNotEmpty()) {
                Text(parts.joinToString(", "))
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
