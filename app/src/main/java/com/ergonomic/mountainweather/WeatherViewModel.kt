package com.ergonomic.mountainweather

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
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
import com.ergonomic.mountainweather.widget.WeatherWidgetUpdater
import com.ergonomic.mountainweather.widget.WidgetDataRequirements
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val needsInitialSetup: Boolean = false,
    val showEnableLocation: Boolean = false,
    val favoriteLimitReached: Boolean = false
)

private const val TAG_WIDGET = "WidgetVM"

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
    private var pagesDataJob: Job? = null
    private var observedPageKeys: List<String> = emptyList()
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

    fun requestGpsWeather() {
        viewModelScope.launch {
            val lm = getApplication<android.app.Application>()
                .getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            if (!lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
                && !lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                _uiState.update { it.copy(showEnableLocation = true) }
                return@launch
            }
            try {
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).await() ?: return@launch
                val name = try {
                    val geocoder = android.location.Geocoder(getApplication(), java.util.Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    addresses?.firstOrNull()?.locality
                        ?: addresses?.firstOrNull()?.subAdminArea
                        ?: "(%.2f, %.2f)".format(location.latitude, location.longitude)
                } catch (_: Exception) {
                    "(%.2f, %.2f)".format(location.latitude, location.longitude)
                }
                setLocation(name, location.latitude, location.longitude)
            } catch (_: Exception) { }
        }
    }

    fun dismissEnableLocation() {
        _uiState.update { it.copy(showEnableLocation = false) }
    }

    private fun observeFavoritesList() {
        favoritesListJob?.cancel()
        favoritesListJob = viewModelScope.launch {
            savedLocationRepo.observeFavorites()
                .distinctUntilChanged { a, b -> favoritesFingerprint(a) == favoritesFingerprint(b) }
                .collect { favorites ->
                    rebuildLocationPages(favorites)
                    WeatherWidgetUpdater.refreshAll(getApplication())
                }
        }
    }

    private fun favoritesFingerprint(favorites: List<SavedLocationEntity>): String =
        favorites.joinToString("|") { "${it.id}:${it.sortOrder}:${it.isFavorite}:${it.name}" }

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
        _uiState.update {
            it.copy(
                locationPages = pages,
                currentPageIndex = newIndex,
                locationSelectionVersion = it.locationSelectionVersion + 1
            )
        }
        preloadCacheForPages(pages)
        observePagesData(pages)
    }

    private fun preloadCacheForPages(pages: List<LocationPage>) {
        viewModelScope.launch {
            val state = _uiState.value
            val wMap = state.weatherByLocation.toMutableMap()
            val hMap = state.hourlyByLocation.toMutableMap()
            val dMap = state.dailyByLocation.toMutableMap()
            var changed = false
            for (page in pages) {
                val key = WeatherRepository.locationKey(page.latitude, page.longitude)
                if (key !in wMap) {
                    db.weatherDao().getWeather(key)?.let {
                        wMap[key] = it
                        changed = true
                    }
                }
                if (key !in hMap) {
                    val hourly = db.hourlyForecastDao().getAll(key)
                    if (hourly.isNotEmpty()) {
                        hMap[key] = hourly
                        changed = true
                    }
                }
                if (key !in dMap) {
                    val daily = db.dailyForecastDao().getAll(key)
                    if (daily.isNotEmpty()) {
                        dMap[key] = daily
                        changed = true
                    }
                }
            }
            if (changed) {
                _uiState.update {
                    it.copy(weatherByLocation = wMap, hourlyByLocation = hMap, dailyByLocation = dMap)
                }
            }
        }
    }

    private data class PageCacheEmission(
        val key: String,
        val weather: WeatherEntity?,
        val hourly: List<HourlyForecastEntity>,
        val daily: List<DailyForecastEntity>
    )

    /** Observe cache for every pager city once. Swiping must not resubscribe. */
    private fun observePagesData(pages: List<LocationPage>) {
        val keys = pages.map { WeatherRepository.locationKey(it.latitude, it.longitude) }.distinct()
        if (keys == observedPageKeys && pagesDataJob?.isActive == true) return
        observedPageKeys = keys
        pagesDataJob?.cancel()
        cacheObserverJob?.cancel()
        if (keys.isEmpty()) return
        pagesDataJob = viewModelScope.launch {
            val flows = keys.map { key ->
                combine(
                    repository.observeCachedWeather(key),
                    repository.observeHourlyForecast(key),
                    repository.observeDailyForecast(key)
                ) { weather, hourly, daily ->
                    PageCacheEmission(key, weather, hourly, daily)
                }
            }
            combine(flows) { emissions -> emissions.toList() }.collect { applyPageCacheEmissions(it) }
        }
    }

    private fun applyPageCacheEmissions(emissions: List<PageCacheEmission>) {
        _uiState.update { state ->
            val wMap = state.weatherByLocation.toMutableMap()
            val hMap = state.hourlyByLocation.toMutableMap()
            val dMap = state.dailyByLocation.toMutableMap()
            var mapsChanged = false
            for (e in emissions) {
                if (e.weather != null && wMap[e.key] != e.weather) {
                    wMap[e.key] = e.weather
                    mapsChanged = true
                }
                if (e.hourly.isNotEmpty() && hMap[e.key] != e.hourly) {
                    hMap[e.key] = e.hourly
                    mapsChanged = true
                }
                if (e.daily.isNotEmpty() && dMap[e.key] != e.daily) {
                    dMap[e.key] = e.daily
                    mapsChanged = true
                }
            }
            val currentKey = WeatherRepository.locationKey(state.latitude, state.longitude)
            val currentWeather = wMap[currentKey] ?: state.weather
            val currentHourly = hMap[currentKey] ?: state.hourlyForecast
            val currentDaily = dMap[currentKey] ?: state.dailyForecast
            val loading = if (wMap[currentKey] != null) false else state.isLoading
            if (!mapsChanged &&
                currentWeather === state.weather &&
                currentHourly === state.hourlyForecast &&
                currentDaily === state.dailyForecast &&
                loading == state.isLoading
            ) {
                state
            } else {
                state.copy(
                    weatherByLocation = if (mapsChanged) wMap else state.weatherByLocation,
                    hourlyByLocation = if (mapsChanged) hMap else state.hourlyByLocation,
                    dailyByLocation = if (mapsChanged) dMap else state.dailyByLocation,
                    weather = currentWeather,
                    hourlyForecast = currentHourly,
                    dailyForecast = currentDaily,
                    isLoading = loading
                )
            }
        }
    }

    fun onPageChanged(pageIndex: Int) {
        val pages = _uiState.value.locationPages
        if (pageIndex !in pages.indices) {
            Log.d(TAG_WIDGET, "onPageChanged: pageIndex=$pageIndex out of range (size=${pages.size})")
            return
        }
        val page = pages[pageIndex]
        Log.d(
            TAG_WIDGET,
            "onPageChanged: idx=$pageIndex page=${page.name} " +
                "(curState=${_uiState.value.locationName})"
        )
        val state = _uiState.value
        if (page.latitude == state.latitude && page.longitude == state.longitude) {
            Log.d(TAG_WIDGET, "onPageChanged: same location -> no save/refresh")
            if (state.currentPageIndex != pageIndex) {
                _uiState.update { it.copy(currentPageIndex = pageIndex) }
            }
            return
        }
        val newKey = WeatherRepository.locationKey(page.latitude, page.longitude)
        _uiState.update {
            it.copy(
                currentPageIndex = pageIndex,
                locationName = page.name,
                latitude = page.latitude,
                longitude = page.longitude,
                weather = it.weatherByLocation[newKey] ?: it.weather,
                isFavorite = !page.isCurrent,
                error = null
            )
        }
        viewModelScope.launch {
            settingsRepo.saveLastLocation(page.name, page.latitude, page.longitude)
        }
        observeFavoriteStatus()
        val settings = forecastSettings.value
        val cachedHourly = _uiState.value.hourlyByLocation[newKey].orEmpty()
        val cachedDaily = _uiState.value.dailyByLocation[newKey].orEmpty()
        val needsWeather = _uiState.value.weatherByLocation[newKey] == null
        val needsHourly = settings.showHourly && cachedHourly.isEmpty()
        val needsDaily = settings.dailyForecastDays > 0 && cachedDaily.isEmpty()
        if (needsWeather || needsHourly || needsDaily) {
            fetchWeatherEnriched(settings)
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
        viewModelScope.launch {
            settingsRepo.saveLastLocation(name, lat, lon)
            WeatherWidgetUpdater.refreshAll(getApplication())
        }
        observeFavoriteStatus()
        observeFavoritesList()
        fetchWeatherWithSettings(forecastSettings.value)
    }

    private fun observeFavoriteStatus() {
        favoriteObserverJob?.cancel()
        val state = _uiState.value
        favoriteObserverJob = viewModelScope.launch {
            savedLocationRepo.observeFavoriteByCoordinates(state.latitude, state.longitude)
                .collect { isFav ->
                    _uiState.update { current ->
                        if (current.isFavorite == isFav) current else current.copy(isFavorite = isFav)
                    }
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

    fun clearFavoriteLimitReached() {
        _uiState.update { it.copy(favoriteLimitReached = false) }
    }

    fun toggleFavorite() {
        val state = _uiState.value
        viewModelScope.launch {
            val dao = db.savedLocationDao()
            val existing = dao.findByCoordinates(state.latitude, state.longitude)
            if (existing != null && existing.isFavorite) {
                dao.toggleFavorite(existing.id)
            } else {
                val currentCount = dao.getFavorites().size
                if (currentCount >= 10) {
                    _uiState.update { it.copy(favoriteLimitReached = true) }
                    return@launch
                }
                if (existing != null) {
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
            WeatherWidgetUpdater.refreshAll(getApplication())
        }
    }

    private fun observeCache() {
        cacheObserverJob?.cancel()
        val state = _uiState.value
        val key = WeatherRepository.locationKey(state.latitude, state.longitude)
        cacheObserverJob = viewModelScope.launch {
            repository.observeCachedWeather(key).collect { cached ->
                if (cached != null) {
                    val stillCurrent = WeatherRepository.locationKey(
                        _uiState.value.latitude, _uiState.value.longitude
                    ) == key
                    val updatedMap = _uiState.value.weatherByLocation.toMutableMap()
                    updatedMap[key] = cached
                    _uiState.update {
                        it.copy(
                            isLoading = if (stillCurrent && it.weather == null) false else it.isLoading,
                            weather = if (stillCurrent) cached else it.weather,
                            locationName = if (stillCurrent) cached.locationName else it.locationName,
                            weatherByLocation = updatedMap
                        )
                    }
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
            val extraDaily = WidgetDataRequirements.extraDailyFields(getApplication())
            val result = repository.refreshAll(
                latitude = state.latitude,
                longitude = state.longitude,
                locationName = state.locationName,
                enabledParams = settings.enabledCurrentParams,
                showHourly = settings.showHourly,
                dailyDays = settings.dailyForecastDays,
                extraDailyFields = extraDaily
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
            refreshOtherFavorites(settings, extraDaily)
        }
    }

    private fun fetchWeatherResilient(settings: ForecastSettings) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.weather == null, error = null) }
            val extraDaily = WidgetDataRequirements.extraDailyFields(getApplication())
            val syncResult = syncManager.syncAll(
                state.latitude, state.longitude, state.locationName, settings,
                extraDailyFields = extraDaily
            )
            syncResult.currentWeather?.let { handleResult(it) }
            refreshOtherFavorites(settings, extraDaily)
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
            val extraDaily = WidgetDataRequirements.extraDailyFields(getApplication())
            if (settings.resilientSync) {
                val syncResult = syncManager.syncAll(
                    state.latitude, state.longitude, state.locationName, settings,
                    extraDailyFields = extraDaily
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
                    dailyDays = settings.dailyForecastDays,
                    extraDailyFields = extraDaily
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
            refreshOtherFavorites(settings, extraDaily)
        }
    }

    private suspend fun refreshOtherFavorites(
        settings: ForecastSettings,
        extraDaily: Set<String>
    ) {
        val current = _uiState.value
        val favorites = runCatching { db.savedLocationDao().getFavorites() }.getOrDefault(emptyList())
        for (loc in favorites) {
            val sameAsCurrent =
                kotlin.math.abs(loc.latitude - current.latitude) < 0.005 &&
                    kotlin.math.abs(loc.longitude - current.longitude) < 0.005
            if (sameAsCurrent) continue
            runCatching {
                val result = repository.refreshAll(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    locationName = loc.name,
                    enabledParams = settings.enabledCurrentParams,
                    showHourly = settings.showHourly,
                    dailyDays = settings.dailyForecastDays,
                    extraDailyFields = extraDaily
                )
                applyForecastToState(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    weather = result.weather.getOrNull(),
                    hourly = result.hourly,
                    daily = result.daily
                )
            }
        }
        WeatherWidgetUpdater.refreshAll(getApplication())
    }

    private fun applyForecastToState(
        latitude: Double,
        longitude: Double,
        weather: WeatherEntity?,
        hourly: List<HourlyForecastEntity>,
        daily: List<DailyForecastEntity>
    ) {
        val key = WeatherRepository.locationKey(latitude, longitude)
        val state = _uiState.value
        val isCurrent =
            kotlin.math.abs(state.latitude - latitude) < 0.005 &&
                kotlin.math.abs(state.longitude - longitude) < 0.005
        val wMap = state.weatherByLocation.toMutableMap()
        val hMap = state.hourlyByLocation.toMutableMap()
        val dMap = state.dailyByLocation.toMutableMap()
        if (weather != null) wMap[key] = weather
        if (hourly.isNotEmpty()) hMap[key] = hourly
        if (daily.isNotEmpty()) dMap[key] = daily
        _uiState.update {
            it.copy(
                weatherByLocation = wMap,
                hourlyByLocation = hMap,
                dailyByLocation = dMap,
                weather = if (isCurrent && weather != null) weather else it.weather,
                hourlyForecast = if (isCurrent && hourly.isNotEmpty()) hourly else it.hourlyForecast,
                dailyForecast = if (isCurrent && daily.isNotEmpty()) daily else it.dailyForecast
            )
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
                    settingsRepo.saveGpsAltitude(location.altitude)
                    _uiState.update { it.copy(gpsAltitude = location.altitude, gpsAltitudeError = false) }
                    WeatherWidgetUpdater.refreshAll(getApplication())
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
                viewModelScope.launch { WeatherWidgetUpdater.refreshAll(getApplication()) }
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
                    viewModelScope.launch { WeatherWidgetUpdater.refreshAll(getApplication()) }
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


