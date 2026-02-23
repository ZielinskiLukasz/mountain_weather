package com.ergonomic.mountainweather

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ergonomic.mountainweather.data.OpenMeteoApi
import com.ergonomic.mountainweather.data.local.AppDatabase
import com.ergonomic.mountainweather.data.local.DailyForecastEntity
import com.ergonomic.mountainweather.data.local.HourlyForecastEntity
import com.ergonomic.mountainweather.data.local.SavedLocationEntity
import com.ergonomic.mountainweather.data.local.WeatherEntity
import com.ergonomic.mountainweather.data.repository.CachedDataException
import com.ergonomic.mountainweather.data.repository.ForecastSettings
import com.ergonomic.mountainweather.data.repository.SavedLocationRepository
import com.ergonomic.mountainweather.data.repository.SettingsRepository
import com.ergonomic.mountainweather.data.repository.WeatherRepository
import com.ergonomic.mountainweather.data.sync.NetworkMonitor
import com.ergonomic.mountainweather.data.sync.ResilientSyncManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val weather: WeatherEntity? = null,
    val hourlyForecast: List<HourlyForecastEntity> = emptyList(),
    val dailyForecast: List<DailyForecastEntity> = emptyList(),
    val isOfflineData: Boolean = false,
    val isFavorite: Boolean = false,
    val locationName: String = "Kraków",
    val latitude: Double = 50.06,
    val longitude: Double = 19.94,
    val error: String? = null
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = WeatherRepository(
        OpenMeteoApi.create(), db.weatherDao(), db.hourlyForecastDao(), db.dailyForecastDao()
    )
    private val savedLocationRepo = SavedLocationRepository(db.savedLocationDao())
    val settingsRepo = SettingsRepository(application)
    private val syncManager = ResilientSyncManager(repository)
    private val networkMonitor = NetworkMonitor(application)

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState

    val forecastSettings: StateFlow<ForecastSettings> =
        settingsRepo.forecastSettings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ForecastSettings())

    private var cacheObserverJob: Job? = null
    private var hourlyObserverJob: Job? = null
    private var dailyObserverJob: Job? = null
    private var dailyFetchJob: Job? = null
    private var favoriteObserverJob: Job? = null
    private var settingsJob: Job? = null
    private var networkJob: Job? = null

    init {
        viewModelScope.launch {
            val saved = settingsRepo.getLastLocation()
            if (saved != null) {
                _uiState.update {
                    it.copy(
                        locationName = saved.name,
                        latitude = saved.latitude,
                        longitude = saved.longitude
                    )
                }
            }
            observeCache()
            observeFavoriteStatus()
            observeSettings()
            observeNetwork()

            delay(1500)
            fetchWeather()
        }
    }

    private fun observeNetwork() {
        networkJob?.cancel()
        networkJob = viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                val settings = forecastSettings.value
                if (online && settings.resilientSync && _uiState.value.isOfflineData) {
                    fetchWeatherResilient(settings)
                }
            }
        }
    }

    private fun observeSettings() {
        settingsJob?.cancel()
        settingsJob = viewModelScope.launch {
            settingsRepo.forecastSettings.collect { settings ->
                if (settings.showHourly) observeHourlyCache() else {
                    hourlyObserverJob?.cancel()
                    _uiState.update { it.copy(hourlyForecast = emptyList()) }
                }
                if (settings.showDaily3 || settings.showDaily5) observeDailyCache() else {
                    dailyObserverJob?.cancel()
                    _uiState.update { it.copy(dailyForecast = emptyList()) }
                }
                fetchForecasts(settings)
            }
        }
    }

    fun setLocation(name: String, lat: Double, lon: Double) {
        _uiState.update {
            it.copy(
                locationName = name,
                latitude = lat,
                longitude = lon,
                weather = null,
                hourlyForecast = emptyList(),
                dailyForecast = emptyList(),
                isLoading = true,
                isOfflineData = false,
                isFavorite = false,
                error = null
            )
        }
        viewModelScope.launch { settingsRepo.saveLastLocation(name, lat, lon) }
        observeCache()
        observeFavoriteStatus()
        val settings = forecastSettings.value
        if (settings.showHourly) observeHourlyCache()
        if (settings.showDaily3 || settings.showDaily5) observeDailyCache()
        fetchWeather()
        fetchForecasts(settings)
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
                            locationName = cached.locationName
                        )
                    }
                }
            }
        }
    }

    private fun observeHourlyCache() {
        hourlyObserverJob?.cancel()
        val state = _uiState.value
        val key = WeatherRepository.locationKey(state.latitude, state.longitude)
        hourlyObserverJob = viewModelScope.launch {
            repository.observeHourlyForecast(key).collect { hourly ->
                if (hourly.isNotEmpty()) {
                    _uiState.update { it.copy(hourlyForecast = hourly) }
                }
            }
        }
    }

    private fun observeDailyCache() {
        dailyObserverJob?.cancel()
        val state = _uiState.value
        val key = WeatherRepository.locationKey(state.latitude, state.longitude)
        dailyObserverJob = viewModelScope.launch {
            repository.observeDailyForecast(key).collect { daily ->
                if (daily.isNotEmpty()) {
                    _uiState.update { it.copy(dailyForecast = daily) }
                }
            }
        }
    }

    fun fetchWeather() {
        val settings = forecastSettings.value
        if (settings.resilientSync) {
            fetchWeatherResilient(settings)
        } else {
            fetchWeatherSimple()
        }
    }

    private fun fetchWeatherSimple() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.weather == null, error = null) }
            handleResult(
                repository.refreshWeather(state.latitude, state.longitude, state.locationName)
            )
        }
    }

    private fun fetchWeatherResilient(settings: ForecastSettings) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.weather == null, error = null) }
            val syncResult = syncManager.syncAll(
                state.latitude, state.longitude, state.locationName, settings
            )
            syncResult.currentWeather?.let { handleResult(it) }
        }
    }

    private fun fetchForecasts(settings: ForecastSettings) {
        if (settings.resilientSync) return // handled by fetchWeatherResilient

        val state = _uiState.value
        if (settings.showHourly) {
            viewModelScope.launch {
                repository.refreshHourlyForecast(state.latitude, state.longitude)
            }
        }
        val days = when {
            settings.showDaily5 -> 5
            settings.showDaily3 -> 3
            else -> 0
        }
        dailyFetchJob?.cancel()
        if (days > 0) {
            _uiState.update { it.copy(dailyForecast = emptyList()) }
            dailyFetchJob = viewModelScope.launch {
                repository.refreshDailyForecast(state.latitude, state.longitude, days)
            }
        }
    }

    fun refresh() {
        val state = _uiState.value
        val settings = forecastSettings.value
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            if (settings.resilientSync) {
                val syncResult = syncManager.syncAll(
                    state.latitude, state.longitude, state.locationName, settings
                )
                syncResult.currentWeather?.let { handleResult(it) }
            } else {
                handleResult(
                    repository.refreshWeather(state.latitude, state.longitude, state.locationName)
                )
            }
        }
        if (!settings.resilientSync) {
            fetchForecasts(settings)
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
