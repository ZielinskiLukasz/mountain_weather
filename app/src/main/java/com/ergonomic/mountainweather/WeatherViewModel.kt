package com.ergonomic.mountainweather

import android.annotation.SuppressLint
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
import com.ergonomic.mountainweather.util.WeatherParams
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LocationPage(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isCurrent: Boolean = false
)

enum class ErrorType { NONE, NO_INTERNET, API_ERROR }

data class WeatherUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val weather: WeatherEntity? = null,
    val hourlyForecast: List<HourlyForecastEntity> = emptyList(),
    val dailyForecast: List<DailyForecastEntity> = emptyList(),
    val isOfflineData: Boolean = false,
    val isFavorite: Boolean = false,
    val locationName: String = "New York",
    val latitude: Double = 40.7128,
    val longitude: Double = -74.006,
    val error: String? = null,
    val errorType: ErrorType = ErrorType.NONE,
    val locationPages: List<LocationPage> = emptyList(),
    val currentPageIndex: Int = 0,
    val locationSelectionVersion: Int = 0,
    val weatherByLocation: Map<String, WeatherEntity> = emptyMap(),
    val hourlyByLocation: Map<String, List<HourlyForecastEntity>> = emptyMap(),
    val dailyByLocation: Map<String, List<DailyForecastEntity>> = emptyMap(),
    val selectedHourlyDate: String? = null,
    val gpsAltitude: Double? = null,
    val gpsAltitudeError: Boolean = false,
    val needsInitialSetup: Boolean = false
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
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

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
    private var favoritesListJob: Job? = null
    private var settingsJob: Job? = null
    private var networkJob: Job? = null
    private var lastEnabledParams: Set<String>? = null
    private var lastShowHourly: Boolean? = null
    private var lastDailyDays: Int? = null

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
            } else {
                _uiState.update { it.copy(needsInitialSetup = true) }
            }
            observeCache()
            observeFavoriteStatus()
            observeFavoritesList()
            observeSettings()
            observeNetwork()
        }
    }

    fun clearInitialSetup() {
        _uiState.update { it.copy(needsInitialSetup = false) }
    }

    private fun observeFavoritesList() {
        favoritesListJob?.cancel()
        favoritesListJob = viewModelScope.launch {
            savedLocationRepo.observeFavorites().collect { favorites ->
                rebuildLocationPages(favorites)
            }
        }
    }

    private fun rebuildLocationPages(favorites: List<SavedLocationEntity>) {
        val state = _uiState.value
        val favPages = favorites.take(10).map {
            LocationPage(it.name, it.latitude, it.longitude)
        }
        val favIndex = favPages.indexOfFirst {
            Math.abs(it.latitude - state.latitude) < 0.005 &&
            Math.abs(it.longitude - state.longitude) < 0.005
        }
        val pages: List<LocationPage>
        val newIndex: Int
        if (favIndex >= 0) {
            pages = favPages
            newIndex = favIndex
        } else {
            val currentPage = LocationPage(
                name = state.locationName,
                latitude = state.latitude,
                longitude = state.longitude,
                isCurrent = true
            )
            pages = listOf(currentPage) + favPages
            newIndex = 0
        }
        _uiState.update { it.copy(locationPages = pages, currentPageIndex = newIndex) }
        preloadCacheForPages(pages)
    }

    private fun preloadCacheForPages(pages: List<LocationPage>) {
        viewModelScope.launch {
            val wMap = _uiState.value.weatherByLocation.toMutableMap()
            val hMap = _uiState.value.hourlyByLocation.toMutableMap()
            val dMap = _uiState.value.dailyByLocation.toMutableMap()
            for (page in pages) {
                val key = WeatherRepository.locationKey(page.latitude, page.longitude)
                if (key !in wMap) {
                    db.weatherDao().getWeather(key)?.let { wMap[key] = it }
                }
                if (key !in hMap) {
                    val hourly = db.hourlyForecastDao().getAll(key)
                    if (hourly.isNotEmpty()) hMap[key] = hourly
                }
                if (key !in dMap) {
                    val daily = db.dailyForecastDao().getAll(key)
                    if (daily.isNotEmpty()) dMap[key] = daily
                }
            }
            _uiState.update {
                it.copy(weatherByLocation = wMap, hourlyByLocation = hMap, dailyByLocation = dMap)
            }
        }
    }

    fun onPageChanged(pageIndex: Int) {
        val pages = _uiState.value.locationPages
        if (pageIndex !in pages.indices) return
        val page = pages[pageIndex]
        if (page.latitude == _uiState.value.latitude && page.longitude == _uiState.value.longitude) {
            _uiState.update { it.copy(currentPageIndex = pageIndex) }
            return
        }
        _uiState.update {
            it.copy(
                currentPageIndex = pageIndex,
                locationName = page.name,
                latitude = page.latitude,
                longitude = page.longitude,
                hourlyForecast = emptyList(),
                dailyForecast = emptyList(),
                isOfflineData = false,
                error = null
            )
        }
        viewModelScope.launch { settingsRepo.saveLastLocation(page.name, page.latitude, page.longitude) }
        observeCache()
        observeFavoriteStatus()
        val settings = forecastSettings.value
        if (settings.showHourly) observeHourlyCache()
        if (settings.dailyForecastDays > 0) observeDailyCache()
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
                if (settings.dailyForecastDays > 0) observeDailyCache() else {
                    dailyObserverJob?.cancel()
                    _uiState.update { it.copy(dailyForecast = emptyList()) }
                }
                val isFirst = lastEnabledParams == null
                val paramsChanged = lastEnabledParams != settings.enabledCurrentParams
                val forecastChanged = lastShowHourly != settings.showHourly
                        || lastDailyDays != settings.dailyForecastDays
                lastEnabledParams = settings.enabledCurrentParams
                lastShowHourly = settings.showHourly
                lastDailyDays = settings.dailyForecastDays
                if (isFirst || paramsChanged || forecastChanged) {
                    fetchWeatherWithSettings(settings)
                }
            }
        }
    }

    fun setLocation(name: String, lat: Double, lon: Double) {
        val currentPages = _uiState.value.locationPages
        val favIndex = currentPages.indexOfFirst {
            Math.abs(it.latitude - lat) < 0.005 && Math.abs(it.longitude - lon) < 0.005
        }
        val pageIndex = if (favIndex >= 0) favIndex else 0
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
                error = null,
                currentPageIndex = pageIndex,
                locationSelectionVersion = it.locationSelectionVersion + 1,
                selectedHourlyDate = null
            )
        }
        viewModelScope.launch { settingsRepo.saveLastLocation(name, lat, lon) }
        observeCache()
        observeFavoriteStatus()
        observeFavoritesList()
        val settings = forecastSettings.value
        if (settings.showHourly) observeHourlyCache()
        if (settings.dailyForecastDays > 0) observeDailyCache()
        fetchWeatherWithSettings(settings)
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

    fun selectHourlyDay(date: String?) {
        val today = java.time.LocalDate.now().toString()
        _uiState.update { it.copy(selectedHourlyDate = if (date == today) null else date) }
    }

    fun saveParamOrder(order: List<String>) {
        viewModelScope.launch { settingsRepo.saveParamOrder(order) }
    }

    fun toggleFavorite() {
        val state = _uiState.value
        viewModelScope.launch {
            val dao = db.savedLocationDao()
            val existing = dao.findByCoordinates(state.latitude, state.longitude)
            if (existing != null && existing.isFavorite) {
                dao.toggleFavorite(existing.id)
            } else if (existing != null) {
                dao.shiftFavoriteSortOrders()
                dao.makeFavoriteFirst(existing.id)
            } else {
                dao.shiftFavoriteSortOrders()
                dao.insert(
                    SavedLocationEntity(
                        name = state.locationName,
                        latitude = state.latitude,
                        longitude = state.longitude,
                        isFavorite = true,
                        sortOrder = 0
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
                if (cached != null) {
                    val updatedMap = _uiState.value.weatherByLocation.toMutableMap()
                    updatedMap[key] = cached
                    if (_uiState.value.isLoading && _uiState.value.weather == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                weather = cached,
                                locationName = cached.locationName,
                                weatherByLocation = updatedMap
                            )
                        }
                    } else {
                        _uiState.update { it.copy(weatherByLocation = updatedMap) }
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
                    val updatedMap = _uiState.value.hourlyByLocation.toMutableMap()
                    updatedMap[key] = hourly
                    _uiState.update { it.copy(hourlyForecast = hourly, hourlyByLocation = updatedMap) }
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
                    val updatedMap = _uiState.value.dailyByLocation.toMutableMap()
                    updatedMap[key] = daily
                    _uiState.update { it.copy(dailyForecast = daily, dailyByLocation = updatedMap) }
                }
            }
        }
    }

    fun fetchWeather() {
        fetchWeatherWithSettings(forecastSettings.value)
    }

    private fun fetchWeatherWithSettings(settings: ForecastSettings) {
        if (settings.resilientSync) {
            fetchWeatherResilient(settings)
        } else {
            fetchWeatherEnriched(settings)
        }
    }

    private fun fetchWeatherEnriched(settings: ForecastSettings) {
        val state = _uiState.value
        val key = WeatherRepository.locationKey(state.latitude, state.longitude)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.weather == null, error = null) }
            val result = repository.refreshAll(
                latitude = state.latitude,
                longitude = state.longitude,
                locationName = state.locationName,
                enabledParams = settings.enabledCurrentParams,
                showHourly = settings.showHourly,
                dailyDays = settings.dailyForecastDays
            )
            handleResult(result.weather)
            if (result.hourly.isNotEmpty()) {
                val hMap = _uiState.value.hourlyByLocation.toMutableMap()
                hMap[key] = result.hourly
                _uiState.update { it.copy(hourlyForecast = result.hourly, hourlyByLocation = hMap) }
            }
            if (result.daily.isNotEmpty()) {
                val dMap = _uiState.value.dailyByLocation.toMutableMap()
                dMap[key] = result.daily
                _uiState.update { it.copy(dailyForecast = result.daily, dailyByLocation = dMap) }
            }
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
        // When not using resilientSync, refreshAll already fetches everything in one request.
        // This method is only needed for resilientSync fallback.
        if (!settings.resilientSync) return

        val state = _uiState.value
        val key = WeatherRepository.locationKey(state.latitude, state.longitude)
        if (settings.showHourly) {
            val hourlyDays = if (settings.dailyForecastDays > 0) settings.dailyForecastDays + 1 else 1
            viewModelScope.launch {
                repository.refreshHourlyForecast(state.latitude, state.longitude, hourlyDays)
                    .onSuccess { entities ->
                        val hMap = _uiState.value.hourlyByLocation.toMutableMap()
                        hMap[key] = entities
                        _uiState.update { it.copy(hourlyForecast = entities, hourlyByLocation = hMap) }
                    }
            }
        }
        val days = settings.dailyForecastDays
        dailyFetchJob?.cancel()
        if (days > 0) {
            dailyFetchJob = viewModelScope.launch {
                repository.refreshDailyForecast(state.latitude, state.longitude, days + 1)
                    .onSuccess { entities ->
                        val dMap = _uiState.value.dailyByLocation.toMutableMap()
                        dMap[key] = entities
                        _uiState.update { it.copy(dailyForecast = entities, dailyByLocation = dMap) }
                    }
            }
        } else {
            _uiState.update { it.copy(dailyForecast = emptyList()) }
        }
    }

    fun refresh() {
        val state = _uiState.value
        val settings = forecastSettings.value
        val key = WeatherRepository.locationKey(state.latitude, state.longitude)
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            if (settings.resilientSync) {
                val syncResult = syncManager.syncAll(
                    state.latitude, state.longitude, state.locationName, settings
                )
                syncResult.currentWeather?.let { handleResult(it) }
                fetchForecasts(settings)
            } else {
                val result = repository.refreshAll(
                    latitude = state.latitude,
                    longitude = state.longitude,
                    locationName = state.locationName,
                    enabledParams = settings.enabledCurrentParams,
                    showHourly = settings.showHourly,
                    dailyDays = settings.dailyForecastDays
                )
                handleResult(result.weather)
                if (result.hourly.isNotEmpty()) {
                    val hMap = _uiState.value.hourlyByLocation.toMutableMap()
                    hMap[key] = result.hourly
                    _uiState.update { it.copy(hourlyForecast = result.hourly, hourlyByLocation = hMap) }
                }
                if (result.daily.isNotEmpty()) {
                    val dMap = _uiState.value.dailyByLocation.toMutableMap()
                    dMap[key] = result.daily
                    _uiState.update { it.copy(dailyForecast = result.daily, dailyByLocation = dMap) }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshGpsAltitude() {
        viewModelScope.launch {
            try {
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).await()
                if (location != null && location.hasAltitude()) {
                    _uiState.update { it.copy(gpsAltitude = location.altitude, gpsAltitudeError = false) }
                } else {
                    _uiState.update { it.copy(gpsAltitudeError = true) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(gpsAltitudeError = true) }
            }
        }
    }

    fun needsGpsPermission(): Boolean {
        return forecastSettings.value.enabledCurrentParams.contains(WeatherParams.GPS_ALTITUDE)
    }

    private fun classifyError(e: Throwable): ErrorType {
        val root = if (e is CachedDataException) e.cause else e
        return when (root) {
            is java.net.UnknownHostException,
            is java.net.ConnectException,
            is java.net.NoRouteToHostException -> ErrorType.NO_INTERNET
            else -> ErrorType.API_ERROR
        }
    }

    private fun handleResult(result: Result<WeatherEntity>) {
        result.fold(
            onSuccess = { entity ->
                val updatedMap = _uiState.value.weatherByLocation.toMutableMap()
                updatedMap[entity.locationKey] = entity
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    weather = entity,
                    isOfflineData = false,
                    locationName = entity.locationName,
                    error = null,
                    errorType = ErrorType.NONE,
                    weatherByLocation = updatedMap
                )
            },
            onFailure = { error ->
                val type = classifyError(error)
                if (error is CachedDataException) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            weather = error.cachedData,
                            isOfflineData = true,
                            locationName = error.cachedData.locationName,
                            error = error.cause?.message,
                            errorType = type
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = error.message,
                            errorType = type
                        )
                    }
                }
            }
        )
    }
}
