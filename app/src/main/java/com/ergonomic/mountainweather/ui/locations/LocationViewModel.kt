package com.ergonomic.mountainweather.ui.locations

import android.annotation.SuppressLint
import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ergonomic.mountainweather.data.GeocodingApi
import com.ergonomic.mountainweather.data.GeocodingResult
import com.ergonomic.mountainweather.data.PhotonApi
import com.ergonomic.mountainweather.data.local.AppDatabase
import com.ergonomic.mountainweather.data.local.SavedLocationEntity
import com.ergonomic.mountainweather.data.repository.SavedLocationRepository
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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

data class LocationUiState(
    val query: String = "",
    val results: List<GeocodingResult> = emptyList(),
    val placeResults: List<GeocodingResult> = emptyList(),
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
    private val photonApi = PhotonApi.create()
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
            _uiState.update { it.copy(results = emptyList(), placeResults = emptyList(), isSearching = false) }
        }
    }

    private val coordPattern = Regex(
        """^\s*(-?\d+[.,]\d+)\s*[,;\s]\s*(-?\d+[.,]\d+)\s*$"""
    )

    private fun parseCoordinates(query: String): Pair<Double, Double>? {
        val match = coordPattern.matchEntire(query) ?: return null
        val lat = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val lon = match.groupValues[2].replace(',', '.').toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return lat to lon
    }

    companion object {
        private val CITY_OSM_VALUES = setOf(
            "city", "town", "village", "hamlet", "suburb",
            "borough", "quarter", "neighbourhood",
            "municipality", "administrative", "county", "state", "country"
        )
    }

    private suspend fun performSearch(query: String) {
        val coords = parseCoordinates(query)
        if (coords != null) {
            performCoordinateSearch(coords.first, coords.second)
            return
        }
        _uiState.update { it.copy(isSearching = true, error = null) }
        val lang = Locale.getDefault().language

        viewModelScope.launch {
            val cityDeferred = async {
                try {
                    geocodingApi.searchCity(name = query, language = lang).results ?: emptyList()
                } catch (_: Exception) { emptyList() }
            }
            val placeDeferred = async {
                try {
                    val response = photonApi.search(query = query, lang = lang)
                    response.features
                        ?.filter { f ->
                            val v = f.properties?.osmValue
                            val k = f.properties?.osmKey
                            v !in CITY_OSM_VALUES && k != "boundary"
                        }
                        ?.mapNotNull { f ->
                            val coords2 = f.geometry?.coordinates ?: return@mapNotNull null
                            if (coords2.size < 2) return@mapNotNull null
                            val name = f.properties?.name ?: return@mapNotNull null
                            val props = f.properties
                            val detail = listOfNotNull(props.city, props.state, props.country)
                                .joinToString(", ")
                            GeocodingResult(
                                id = (name.hashCode().toLong() * 31 + coords2[1].hashCode()),
                                name = name,
                                latitude = coords2[1],
                                longitude = coords2[0],
                                country = props.country,
                                region = detail.ifEmpty { null }
                            )
                        }
                        ?.distinctBy { "%.4f_%.4f".format(it.latitude, it.longitude) }
                        ?: emptyList()
                } catch (_: Exception) { emptyList() }
            }

            val cities = cityDeferred.await()
            val places = placeDeferred.await()

            val cityCoords = cities.map { "%.3f_%.3f".format(it.latitude, it.longitude) }.toSet()
            val filteredPlaces = places.filter {
                "%.3f_%.3f".format(it.latitude, it.longitude) !in cityCoords
            }

            _uiState.update {
                it.copy(
                    isSearching = false,
                    placeResults = filteredPlaces,
                    results = cities
                )
            }
        }
    }

    private suspend fun performCoordinateSearch(lat: Double, lon: Double) {
        _uiState.update { it.copy(isSearching = true, error = null) }
        val name = resolveLocationName(lat, lon)
        val result = GeocodingResult(
            id = 0,
            name = name,
            latitude = lat,
            longitude = lon,
            country = null,
            region = null
        )
        _uiState.update { it.copy(isSearching = false, results = listOf(result)) }
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

    fun reorderFavorites(orderedIds: List<Long>) {
        viewModelScope.launch { savedLocationRepo.reorderFavorites(orderedIds) }
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
                ?: "(%.2f, %.2f)".format(lat, lon)
        } catch (_: Exception) {
            "(%.2f, %.2f)".format(lat, lon)
        }
    }
}
