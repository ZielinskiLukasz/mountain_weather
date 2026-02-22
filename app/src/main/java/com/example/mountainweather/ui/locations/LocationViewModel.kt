package com.example.mountainweather.ui.locations

import android.annotation.SuppressLint
import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mountainweather.data.GeocodingApi
import com.example.mountainweather.data.GeocodingResult
import com.example.mountainweather.data.local.AppDatabase
import com.example.mountainweather.data.local.SavedLocationEntity
import com.example.mountainweather.data.repository.SavedLocationRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

data class LocationUiState(
    val query: String = "",
    val results: List<GeocodingResult> = emptyList(),
    val isSearching: Boolean = false,
    val isLocating: Boolean = false,
    val error: String? = null
)

data class SelectedLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

@OptIn(FlowPreview::class)
class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val geocodingApi = GeocodingApi.create()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val savedLocationRepo = SavedLocationRepository(
        AppDatabase.getInstance(application).savedLocationDao()
    )

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState

    private val _selectedLocation = MutableStateFlow<SelectedLocation?>(null)
    val selectedLocation: StateFlow<SelectedLocation?> = _selectedLocation

    val favorites: StateFlow<List<SavedLocationEntity>> =
        savedLocationRepo.observeFavorites()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLocations: StateFlow<List<SavedLocationEntity>> =
        savedLocationRepo.observeRecent()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _queryFlow
                .debounce(350)
                .distinctUntilChanged()
                .filter { it.length >= 2 }
                .collect { query -> performSearch(query) }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        _queryFlow.value = query
        if (query.length < 2) {
            _uiState.update { it.copy(results = emptyList(), isSearching = false) }
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true, error = null) }
        try {
            val lang = Locale.getDefault().language
            val response = geocodingApi.searchCity(name = query, language = lang)
            _uiState.update {
                it.copy(
                    isSearching = false,
                    results = response.results ?: emptyList()
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isSearching = false, error = e.message)
            }
        }
    }

    fun selectSearchResult(result: GeocodingResult) {
        viewModelScope.launch {
            savedLocationRepo.saveAsRecent(
                name = result.name,
                latitude = result.latitude,
                longitude = result.longitude,
                country = result.country,
                region = result.region
            )
            _selectedLocation.value = SelectedLocation(
                name = result.name,
                latitude = result.latitude,
                longitude = result.longitude
            )
        }
    }

    fun selectSavedLocation(location: SavedLocationEntity) {
        viewModelScope.launch {
            savedLocationRepo.saveAsRecent(
                name = location.name,
                latitude = location.latitude,
                longitude = location.longitude,
                country = location.country,
                region = location.region
            )
            _selectedLocation.value = SelectedLocation(
                name = location.name,
                latitude = location.latitude,
                longitude = location.longitude
            )
        }
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch { savedLocationRepo.toggleFavorite(id) }
    }

    fun deleteLocation(id: Long) {
        viewModelScope.launch { savedLocationRepo.delete(id) }
    }

    @SuppressLint("MissingPermission")
    fun requestGpsLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true, error = null) }
            try {
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).await()

                if (location != null) {
                    val name = resolveLocationName(location.latitude, location.longitude)
                    savedLocationRepo.saveAsRecent(
                        name = name,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                    _selectedLocation.value = SelectedLocation(
                        name = name,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                } else {
                    _uiState.update {
                        it.copy(isLocating = false, error = "GPS unavailable")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLocating = false, error = e.message)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveLocationName(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(getApplication(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            addresses?.firstOrNull()?.locality
                ?: addresses?.firstOrNull()?.subAdminArea
                ?: "%.2f, %.2f".format(lat, lon)
        } catch (_: Exception) {
            "%.2f, %.2f".format(lat, lon)
        }
    }
}
