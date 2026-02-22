package com.example.mountainweather

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mountainweather.data.OpenMeteoApi
import com.example.mountainweather.data.local.AppDatabase
import com.example.mountainweather.data.local.SavedLocationEntity
import com.example.mountainweather.data.local.WeatherEntity
import com.example.mountainweather.data.repository.CachedDataException
import com.example.mountainweather.data.repository.SavedLocationRepository
import com.example.mountainweather.data.repository.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val weather: WeatherEntity? = null,
    val isOfflineData: Boolean = false,
    val isFavorite: Boolean = false,
    val locationName: String = "Kraków",
    val latitude: Double = 50.06,
    val longitude: Double = 19.94,
    val error: String? = null
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = WeatherRepository(OpenMeteoApi.create(), db.weatherDao())
    private val savedLocationRepo = SavedLocationRepository(db.savedLocationDao())
    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState

    private var cacheObserverJob: Job? = null
    private var favoriteObserverJob: Job? = null

    init {
        observeCache()
        observeFavoriteStatus()
        fetchWeather()
    }

    fun setLocation(name: String, lat: Double, lon: Double) {
        _uiState.update {
            it.copy(
                locationName = name,
                latitude = lat,
                longitude = lon,
                weather = null,
                isLoading = true,
                isOfflineData = false,
                isFavorite = false,
                error = null
            )
        }
        observeCache()
        observeFavoriteStatus()
        fetchWeather()
    }

    private fun observeFavoriteStatus() {
        favoriteObserverJob?.cancel()
        val state = _uiState.value
        favoriteObserverJob = viewModelScope.launch {
            savedLocationRepo.observeFavoriteByCoordinates(state.latitude, state.longitude)
                .collect { isFav ->
                    _uiState.update { it.copy(isFavorite = isFav) }
                }
        }
    }

    fun toggleFavorite() {
        val state = _uiState.value
        viewModelScope.launch {
            val dao = db.savedLocationDao()
            val existing = dao.findByCoordinates(state.latitude, state.longitude)
            if (existing != null) {
                dao.toggleFavorite(existing.id)
            } else {
                dao.insert(
                    SavedLocationEntity(
                        name = state.locationName,
                        latitude = state.latitude,
                        longitude = state.longitude,
                        isFavorite = true
                    )
                )
            }
        }
    }

    private fun observeCache() {
        cacheObserverJob?.cancel()
        val state = _uiState.value
        val key = WeatherRepository.locationKey(state.latitude, state.longitude)
        cacheObserverJob = viewModelScope.launch {
            repository.observeCachedWeather(key).collect { cached ->
                if (cached != null && _uiState.value.isLoading && _uiState.value.weather == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            weather = cached,
                            locationName = cached.locationName,
                            isOfflineData = true
                        )
                    }
                }
            }
        }
    }

    fun fetchWeather() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.weather == null, error = null) }
            handleResult(
                repository.refreshWeather(state.latitude, state.longitude, state.locationName)
            )
        }
    }

    fun refresh() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            handleResult(
                repository.refreshWeather(state.latitude, state.longitude, state.locationName)
            )
        }
    }

    private fun handleResult(result: Result<WeatherEntity>) {
        result.fold(
            onSuccess = { entity ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    weather = entity,
                    isOfflineData = false,
                    locationName = entity.locationName,
                    error = null
                )
            },
            onFailure = { error ->
                if (error is CachedDataException) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            weather = error.cachedData,
                            isOfflineData = true,
                            locationName = error.cachedData.locationName,
                            error = error.cause?.message
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = error.message
                        )
                    }
                }
            }
        )
    }
}
