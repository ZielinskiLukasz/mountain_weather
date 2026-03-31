package com.ergonomic.mountainweather

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ergonomic.mountainweather.data.local.DailyForecastEntity
import com.ergonomic.mountainweather.data.local.HourlyForecastEntity
import com.ergonomic.mountainweather.data.local.WeatherEntity
import com.ergonomic.mountainweather.data.repository.ForecastSettings
import com.ergonomic.mountainweather.ui.locations.LocationScreen
import com.ergonomic.mountainweather.ui.settings.SettingsScreen
import com.ergonomic.mountainweather.ui.theme.CardBorderDark
import com.ergonomic.mountainweather.ui.theme.CardBorderLight
import com.ergonomic.mountainweather.ui.theme.MountainWeatherTheme
import com.ergonomic.mountainweather.util.WeatherParams
import com.ergonomic.mountainweather.util.weatherCodeToInfo
import com.ergonomic.mountainweather.util.windDirectionToArrow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        reconcileBackgroundSync()
        setContent {
            MountainWeatherTheme {
                AppNavigation()
            }
        }
    }

    private fun reconcileBackgroundSync() {
        val settingsRepo = com.ergonomic.mountainweather.data.repository.SettingsRepository(this)
        kotlinx.coroutines.MainScope().launch {
            val settings = settingsRepo.forecastSettings.first()
            if (settings.syncIntervalMinutes > 0) {
                com.ergonomic.mountainweather.data.sync.SyncScheduler.enable(
                    this@MainActivity, settings.syncIntervalMinutes
                )
            }
        }
    }
}

@Composable
fun AppNavigation(weatherViewModel: WeatherViewModel = viewModel()) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                WeatherScreen(
                    modifier = Modifier.padding(padding),
                    snackbarHostState = snackbarHostState,
                    viewModel = weatherViewModel,
                    onChangeLocation = { navController.navigate("locations") },
                    onOpenSettings = { navController.navigate("settings") }
                )
            }
        }
        composable("locations") {
            LocationScreen(
                onLocationSelected = { name, lat, lon ->
                    weatherViewModel.setLocation(name, lat, lon)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                settingsRepo = weatherViewModel.settingsRepo,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    viewModel: WeatherViewModel,
    onChangeLocation: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val settings by viewModel.forecastSettings.collectAsState()
    val pages = state.locationPages
    val context = LocalContext.current

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.refreshGpsAltitude()
    }

    val gpsAltitudeEnabled = WeatherParams.GPS_ALTITUDE in settings.enabledCurrentParams
    LaunchedEffect(gpsAltitudeEnabled) {
        if (gpsAltitudeEnabled) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                viewModel.refreshGpsAltitude()
            } else {
                gpsPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    val refreshErrorMessage = stringResource(R.string.refresh_error_snackbar)
    LaunchedEffect(state.error) {
        if (state.error != null && state.weather != null) {
            snackbarHostState.showSnackbar(refreshErrorMessage)
        }
    }

    val pageCount = pages.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(
        initialPage = state.currentPageIndex.coerceIn(0, pageCount - 1),
        pageCount = { pageCount }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            viewModel.onPageChanged(page)
        }
    }

    LaunchedEffect(state.locationSelectionVersion) {
        if (pagerState.currentPage != 0) {
            pagerState.scrollToPage(0)
        }
    }

    when {
        state.isLoading && state.weather == null -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.loading_weather),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        state.weather == null && state.error != null -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ErrorContent(
                    message = state.error!!,
                    onRetry = { viewModel.fetchWeather() }
                )
            }
        }
        else -> {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = {
                    viewModel.refresh()
                    if (gpsAltitudeEnabled) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) viewModel.refreshGpsAltitude()
                    }
                },
                modifier = modifier.fillMaxSize()
            ) {
                Column {
                    if (pages.size > 1) {
                        PageIndicator(
                            pageCount = pageCount,
                            currentPage = pagerState.currentPage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    if (state.isOfflineData) {
                        OfflineBanner(cachedAt = state.weather?.cachedAt ?: 0L)
                    }
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        val page = pages.getOrNull(pageIndex)
                        val isActivePage = pageIndex == pagerState.settledPage
                        val locationKey = if (page != null)
                            com.ergonomic.mountainweather.data.repository.WeatherRepository.locationKey(page.latitude, page.longitude)
                        else null
                        val pageWeather = locationKey?.let { state.weatherByLocation[it] } ?: state.weather
                        val pageHourly = locationKey?.let { state.hourlyByLocation[it] } ?: emptyList()
                        val pageDaily = locationKey?.let { state.dailyByLocation[it] } ?: emptyList()
                        val pageName = page?.name ?: state.locationName
                        val pageIsFavorite = if (isActivePage) state.isFavorite
                            else page?.isCurrent == false

                        if (pageWeather != null) {
                            WeatherContent(
                                locationName = pageName,
                                weather = pageWeather,
                                hourlyForecast = pageHourly,
                                dailyForecast = pageDaily,
                                settings = settings,
                                isOffline = state.isOfflineData && isActivePage,
                                isFavorite = pageIsFavorite,
                                selectedHourlyDate = state.selectedHourlyDate,
                                gpsAltitude = state.gpsAltitude,
                                onChangeLocation = onChangeLocation,
                                onOpenSettings = onOpenSettings,
                                onToggleFavorite = { viewModel.toggleFavorite() },
                                onReorder = { viewModel.saveParamOrder(it) },
                                onSelectDay = { viewModel.selectHourlyDay(it) }
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = page?.name ?: "",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(R.string.pull_to_refresh_hint),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PageIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isSelected) 10.dp else 7.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

@Composable
fun OfflineBanner(cachedAt: Long) {
    val formattedTime = remember(cachedAt) {
        if (cachedAt > 0) {
            val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(cachedAt), ZoneId.systemDefault())
            dt.format(DateTimeFormatter.ofPattern("HH:mm, dd.MM"))
        } else "—"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f))
            .padding(vertical = 6.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.offline_data, formattedTime),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

data class DetailItem(val key: String, val icon: String, val label: String, val value: String)

@Composable
fun WeatherContent(
    locationName: String,
    weather: WeatherEntity,
    hourlyForecast: List<HourlyForecastEntity>,
    dailyForecast: List<DailyForecastEntity>,
    settings: ForecastSettings,
    isOffline: Boolean,
    isFavorite: Boolean,
    selectedHourlyDate: String?,
    gpsAltitude: Double? = null,
    onChangeLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFavorite: () -> Unit,
    onReorder: (List<String>) -> Unit,
    onSelectDay: (String?) -> Unit
) {
    val weatherInfo = weatherCodeToInfo(weather.weatherCode)
    val scrollState = rememberScrollState()

    val cardShape = RoundedCornerShape(16.dp)
    val cardBorder = if (MaterialTheme.colorScheme.background == com.ergonomic.mountainweather.ui.theme.BackgroundDark)
        CardBorderDark else CardBorderLight

    val detailItems = buildDetailItems(weather, settings.enabledCurrentParams, settings.paramOrder, gpsAltitude)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onChangeLocation)
                .padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = locationName,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Toggle favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
                )
            }
            IconButton(onClick = onChangeLocation) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Change location",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(text = weatherInfo.icon, fontSize = 72.sp)

        Text(
            text = "${weather.temperature}°C",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )

        if (WeatherParams.APPARENT_TEMP in settings.enabledCurrentParams) {
            Text(
                text = stringResource(R.string.feels_like, weather.apparentTemperature.toString()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(weatherInfo.descriptionRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (detailItems.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorder, cardShape),
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    DraggableDetailGrid(
                        items = detailItems,
                        paramOrder = settings.paramOrder,
                        onReorder = onReorder
                    )
                }
            }
        }

        if (settings.showHourly && hourlyForecast.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            val todayStr = LocalDate.now().toString()
            val filterDate = selectedHourlyDate ?: todayStr
            val filteredHourly = hourlyForecast.filter { it.time.startsWith(filterDate) }
            val displayHourly = filteredHourly.ifEmpty {
                hourlyForecast.filter { it.time.startsWith(todayStr) }
            }
            val effectiveDate = if (filteredHourly.isNotEmpty()) selectedHourlyDate else null
            if (displayHourly.isNotEmpty()) {
                HourlyForecastSection(
                    hourlyForecast = displayHourly,
                    selectedDate = effectiveDate,
                    onBackToToday = { onSelectDay(null) },
                    noDataForDate = filteredHourly.isEmpty() && selectedHourlyDate != null
                )
            }
        }

        if (settings.dailyForecastDays > 0 && dailyForecast.isNotEmpty()) {
            val maxDays = settings.dailyForecastDays
            val today = LocalDate.now().toString()
            val futureDays = dailyForecast.filter { it.date > today }.take(maxDays)
            if (futureDays.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                DailyForecastSection(
                    dailyForecast = futureDays,
                    selectedDate = selectedHourlyDate,
                    showHourly = settings.showHourly,
                    onDayClick = { date -> onSelectDay(if (date == selectedHourlyDate) null else date) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val formattedTime = remember(weather.cachedAt) {
            val dt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(weather.cachedAt), ZoneId.systemDefault()
            )
            dt.format(DateTimeFormatter.ofPattern("HH:mm, dd.MM.yyyy"))
        }
        Text(
            text = stringResource(R.string.update_time, formattedTime),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DraggableDetailGrid(
    items: List<DetailItem>,
    paramOrder: List<String>,
    onReorder: (List<String>) -> Unit
) {
    if (items.isEmpty()) return

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    var currentOrder by remember { mutableStateOf(items.map { it.key }) }

    LaunchedEffect(items.map { it.key }.toSet()) {
        val newKeys = items.map { it.key }
        currentOrder = currentOrder.filter { it in newKeys } + newKeys.filter { it !in currentOrder }
    }

    val orderedItems = remember(items, currentOrder) {
        val map = items.associateBy { it.key }
        currentOrder.mapNotNull { map[it] }
    }

    val columns = 2
    val rowCount = (orderedItems.size + columns - 1) / columns
    val cellHeightDp = 50.dp

    val dragShape = RoundedCornerShape(12.dp)
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val dragBorderBrush = Brush.linearGradient(listOf(primary, tertiary))
    val dragBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellWidthPx = with(density) { (maxWidth / columns).toPx() }
        val cellHeightPx = with(density) { cellHeightDp.toPx() }
        val cellWidthDp = maxWidth / columns

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cellHeightDp * rowCount)
        ) {
            orderedItems.forEach { item ->
                key(item.key) {
                    val isDragged = item.key == draggedKey
                    val index by remember { derivedStateOf { currentOrder.indexOf(item.key) } }
                    val targetXPx by remember { derivedStateOf { (index % columns).toFloat() * cellWidthPx } }
                    val targetYPx by remember { derivedStateOf { (index / columns).toFloat() * cellHeightPx } }

                    val springSpec = spring<Float>(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                    val animX by animateFloatAsState(targetXPx, animationSpec = springSpec, label = "ax_${item.key}")
                    val animY by animateFloatAsState(targetYPx, animationSpec = springSpec, label = "ay_${item.key}")

                    val dragScale by animateFloatAsState(
                        targetValue = if (isDragged) 1.08f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "scale_${item.key}"
                    )

                    Box(
                        modifier = Modifier
                            .width(cellWidthDp)
                            .height(cellHeightDp)
                            .zIndex(if (isDragged) 10f else 0f)
                            .offset {
                                if (isDragged) {
                                    IntOffset(
                                        (targetXPx + dragOffset.x).roundToInt(),
                                        (targetYPx + dragOffset.y).roundToInt()
                                    )
                                } else {
                                    IntOffset(animX.roundToInt(), animY.roundToInt())
                                }
                            }
                            .graphicsLayer {
                                scaleX = dragScale
                                scaleY = dragScale
                                if (isDragged) {
                                    shadowElevation = 16f
                                    shape = dragShape
                                    clip = true
                                    rotationZ = 1.5f
                                }
                            }
                            .then(
                                if (isDragged) Modifier
                                    .clip(dragShape)
                                    .background(dragBg, dragShape)
                                    .border(2.dp, dragBorderBrush, dragShape)
                                else Modifier
                            )
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        draggedKey = item.key
                                        dragOffset = Offset.Zero
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount

                                        val myIdx = currentOrder.indexOf(item.key)
                                        if (myIdx < 0) return@detectDragGesturesAfterLongPress
                                        val baseX = (myIdx % columns) * cellWidthPx
                                        val baseY = (myIdx / columns) * cellHeightPx
                                        val centerX = baseX + dragOffset.x + cellWidthPx / 2
                                        val centerY = baseY + dragOffset.y + cellHeightPx / 2
                                        val tCol = (centerX / cellWidthPx).toInt().coerceIn(0, columns - 1)
                                        val tRow = (centerY / cellHeightPx).toInt().coerceIn(0, rowCount - 1)
                                        val targetIdx = (tRow * columns + tCol).coerceIn(0, currentOrder.lastIndex)

                                        if (targetIdx != myIdx) {
                                            val newOrder = currentOrder.toMutableList()
                                            newOrder.removeAt(myIdx)
                                            newOrder.add(targetIdx, item.key)

                                            val dCol = (myIdx % columns) - (targetIdx % columns)
                                            val dRow = (myIdx / columns) - (targetIdx / columns)
                                            dragOffset = Offset(
                                                dragOffset.x + dCol * cellWidthPx,
                                                dragOffset.y + dRow * cellHeightPx
                                            )
                                            currentOrder = newOrder
                                        }
                                    },
                                    onDragEnd = {
                                        draggedKey = null
                                        dragOffset = Offset.Zero
                                        onReorder(currentOrder)
                                    },
                                    onDragCancel = {
                                        draggedKey = null
                                        dragOffset = Offset.Zero
                                    }
                                )
                            }
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                    ) {
                        Column {
                            Text(
                                text = "${item.icon} ${item.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDragged) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = item.value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDragged) MaterialTheme.colorScheme.onSurface
                                    else Color.Unspecified
                            )
                        }
                    }
                }
            }
        }
    }
}

private val pm25Thresholds = doubleArrayOf(10.0, 25.0, 50.0)
private val pm10Thresholds = doubleArrayOf(20.0, 50.0, 100.0)

private fun pmLevel(value: Double, thresholds: DoubleArray): String = when {
    value <= thresholds[0] -> "🟢"
    value <= thresholds[1] -> "🟡"
    value <= thresholds[2] -> "🟠"
    else -> "🔴"
}

@Composable
fun buildDetailItems(
    weather: WeatherEntity,
    enabled: Set<String>,
    paramOrder: List<String>,
    gpsAltitude: Double? = null
): List<DetailItem> {
    val allItems = mutableMapOf<String, DetailItem>()

    if (WeatherParams.TEMPERATURE in enabled && weather.temperatureMax != null && weather.temperatureMin != null) {
        allItems[WeatherParams.TEMPERATURE] = DetailItem(
            WeatherParams.TEMPERATURE,
            "🌡️", stringResource(R.string.param_temperature),
            stringResource(R.string.temp_max_min,
                weather.temperatureMax.toInt().toString(),
                weather.temperatureMin.toInt().toString())
        )
    }
    if (WeatherParams.WIND in enabled) {
        allItems[WeatherParams.WIND] = DetailItem(
            WeatherParams.WIND,
            "💨", stringResource(R.string.param_wind),
            "${weather.windSpeed} km/h ${windDirectionToArrow(weather.windDirection)}"
        )
    }
    if (WeatherParams.HUMIDITY in enabled) {
        allItems[WeatherParams.HUMIDITY] = DetailItem(
            WeatherParams.HUMIDITY,
            "💧", stringResource(R.string.param_humidity),
            "${weather.humidity}%"
        )
    }
    if (WeatherParams.PRECIPITATION in enabled) {
        allItems[WeatherParams.PRECIPITATION] = DetailItem(
            WeatherParams.PRECIPITATION,
            "🌧️", stringResource(R.string.param_precipitation),
            "${weather.precipitation} mm"
        )
    }
    if (WeatherParams.PRESSURE in enabled) {
        allItems[WeatherParams.PRESSURE] = DetailItem(
            WeatherParams.PRESSURE,
            "⏲️", stringResource(R.string.param_pressure),
            "${weather.pressure.toInt()} hPa"
        )
    }
    if (WeatherParams.CLOUD_COVER in enabled && weather.cloudCover != null) {
        allItems[WeatherParams.CLOUD_COVER] = DetailItem(
            WeatherParams.CLOUD_COVER,
            "☁️", stringResource(R.string.param_clouds),
            "${weather.cloudCover}%"
        )
    }
    if (WeatherParams.WIND_GUSTS in enabled && weather.windGusts != null) {
        allItems[WeatherParams.WIND_GUSTS] = DetailItem(
            WeatherParams.WIND_GUSTS,
            "🌬️", stringResource(R.string.param_wind_gusts),
            "${weather.windGusts} km/h"
        )
    }
    if (WeatherParams.WIND_DIRECTION in enabled) {
        allItems[WeatherParams.WIND_DIRECTION] = DetailItem(
            WeatherParams.WIND_DIRECTION,
            "🧭", stringResource(R.string.param_wind_dir),
            "${weather.windDirection}° ${windDirectionToArrow(weather.windDirection)}"
        )
    }
    if (WeatherParams.SNOWFALL in enabled && weather.snowfall != null) {
        allItems[WeatherParams.SNOWFALL] = DetailItem(
            WeatherParams.SNOWFALL,
            "❄️", stringResource(R.string.param_snowfall),
            "${weather.snowfall} cm"
        )
    }
    if (WeatherParams.RAIN in enabled && weather.rain != null) {
        allItems[WeatherParams.RAIN] = DetailItem(
            WeatherParams.RAIN,
            "🌦️", stringResource(R.string.param_rain),
            "${weather.rain} mm"
        )
    }
    if (WeatherParams.SUNRISE_SUNSET in enabled && weather.sunrise != null && weather.sunset != null) {
        val rise = weather.sunrise.takeLast(5)
        val set = weather.sunset.takeLast(5)
        allItems[WeatherParams.SUNRISE_SUNSET] = DetailItem(
            WeatherParams.SUNRISE_SUNSET,
            "🌅", stringResource(R.string.param_sunrise_sunset),
            "$rise / $set"
        )
    }
    if (WeatherParams.UV_INDEX in enabled && weather.uvIndexMax != null) {
        allItems[WeatherParams.UV_INDEX] = DetailItem(
            WeatherParams.UV_INDEX,
            "☀️", stringResource(R.string.param_uv_index),
            "${weather.uvIndexMax}"
        )
    }
    if (WeatherParams.RAIN_SUM in enabled && weather.rainSum != null) {
        allItems[WeatherParams.RAIN_SUM] = DetailItem(
            WeatherParams.RAIN_SUM,
            "💦", stringResource(R.string.param_rain_sum),
            "${weather.rainSum} mm"
        )
    }
    if (WeatherParams.SHOWERS_SUM in enabled && weather.showersSum != null) {
        allItems[WeatherParams.SHOWERS_SUM] = DetailItem(
            WeatherParams.SHOWERS_SUM,
            "🚿", stringResource(R.string.param_showers_sum),
            "${weather.showersSum} mm"
        )
    }
    if (WeatherParams.SNOWFALL_SUM in enabled && weather.snowfallSum != null) {
        allItems[WeatherParams.SNOWFALL_SUM] = DetailItem(
            WeatherParams.SNOWFALL_SUM,
            "🌨️", stringResource(R.string.param_snowfall_sum),
            "${weather.snowfallSum} cm"
        )
    }
    if (WeatherParams.PRECIP_HOURS in enabled && weather.precipitationHours != null) {
        allItems[WeatherParams.PRECIP_HOURS] = DetailItem(
            WeatherParams.PRECIP_HOURS,
            "⏱️", stringResource(R.string.param_precip_hours),
            "${weather.precipitationHours.toInt()} h"
        )
    }
    if (WeatherParams.PRECIP_PROBABILITY in enabled && weather.precipitationProbabilityMax != null) {
        allItems[WeatherParams.PRECIP_PROBABILITY] = DetailItem(
            WeatherParams.PRECIP_PROBABILITY,
            "📊", stringResource(R.string.param_precip_prob),
            "${weather.precipitationProbabilityMax}%"
        )
    }
    if (WeatherParams.SUNSHINE_DURATION in enabled && weather.sunshineDuration != null) {
        val hours = (weather.sunshineDuration / 3600).toInt()
        val minutes = ((weather.sunshineDuration % 3600) / 60).toInt()
        allItems[WeatherParams.SUNSHINE_DURATION] = DetailItem(
            WeatherParams.SUNSHINE_DURATION,
            "🌤️", stringResource(R.string.param_sunshine),
            "${hours}h ${minutes}m"
        )
    }
    if (WeatherParams.WIND_GUSTS_MAX in enabled && weather.windGustsMax != null) {
        allItems[WeatherParams.WIND_GUSTS_MAX] = DetailItem(
            WeatherParams.WIND_GUSTS_MAX,
            "💥", stringResource(R.string.param_gusts_max),
            "${weather.windGustsMax} km/h"
        )
    }
    if (WeatherParams.DOMINANT_WIND_DIR in enabled && weather.dominantWindDirection != null) {
        allItems[WeatherParams.DOMINANT_WIND_DIR] = DetailItem(
            WeatherParams.DOMINANT_WIND_DIR,
            "🔄", stringResource(R.string.param_dom_wind),
            "${weather.dominantWindDirection}° ${windDirectionToArrow(weather.dominantWindDirection)}"
        )
    }
    if (WeatherParams.DEW_POINT in enabled && weather.dewPoint != null) {
        allItems[WeatherParams.DEW_POINT] = DetailItem(
            WeatherParams.DEW_POINT,
            "🌫️", stringResource(R.string.param_dew_point),
            "${weather.dewPoint}°C"
        )
    }
    if (WeatherParams.VISIBILITY in enabled && weather.visibility != null) {
        val km = weather.visibility / 1000.0
        allItems[WeatherParams.VISIBILITY] = DetailItem(
            WeatherParams.VISIBILITY,
            "👁️", stringResource(R.string.param_visibility),
            "${"%.1f".format(km)} km"
        )
    }
    if (WeatherParams.FREEZING_LEVEL in enabled && weather.freezingLevelHeight != null) {
        allItems[WeatherParams.FREEZING_LEVEL] = DetailItem(
            WeatherParams.FREEZING_LEVEL,
            "🏔️", stringResource(R.string.param_freezing_level),
            "${weather.freezingLevelHeight.toInt()} m"
        )
    }
    if (WeatherParams.AQI_EU in enabled && weather.aqiEu != null) {
        allItems[WeatherParams.AQI_EU] = DetailItem(
            WeatherParams.AQI_EU,
            "🟢", stringResource(R.string.param_aqi_eu),
            "${weather.aqiEu} EAQI"
        )
    }
    if (WeatherParams.AQI_US in enabled && weather.aqiUs != null) {
        allItems[WeatherParams.AQI_US] = DetailItem(
            WeatherParams.AQI_US,
            "🟡", stringResource(R.string.param_aqi_us),
            "${weather.aqiUs} USAQI"
        )
    }
    if (WeatherParams.PM25 in enabled && weather.pm25 != null) {
        val level = pmLevel(weather.pm25, pm25Thresholds)
        allItems[WeatherParams.PM25] = DetailItem(
            WeatherParams.PM25,
            "🫁", stringResource(R.string.param_pm25),
            "$level ${"%.1f".format(weather.pm25)} μg/m³"
        )
    }
    if (WeatherParams.PM10 in enabled && weather.pm10 != null) {
        val level = pmLevel(weather.pm10, pm10Thresholds)
        allItems[WeatherParams.PM10] = DetailItem(
            WeatherParams.PM10,
            "💨", stringResource(R.string.param_pm10),
            "$level ${"%.1f".format(weather.pm10)} μg/m³"
        )
    }
    if (WeatherParams.OZONE in enabled && weather.ozone != null) {
        allItems[WeatherParams.OZONE] = DetailItem(
            WeatherParams.OZONE,
            "🛡️", stringResource(R.string.param_ozone),
            "${"%.0f".format(weather.ozone)} μg/m³"
        )
    }
    if (WeatherParams.ELEVATION in enabled && weather.elevation != null) {
        allItems[WeatherParams.ELEVATION] = DetailItem(
            WeatherParams.ELEVATION,
            "⛰️", stringResource(R.string.param_elevation),
            "${"%.0f".format(weather.elevation)} m"
        )
    }
    if (WeatherParams.GPS_ALTITUDE in enabled) {
        val altitudeValue = if (gpsAltitude != null) "${"%.0f".format(gpsAltitude)} m" else "— m"
        allItems[WeatherParams.GPS_ALTITUDE] = DetailItem(
            WeatherParams.GPS_ALTITUDE,
            "📍", stringResource(R.string.param_gps_altitude),
            altitudeValue
        )
    }

    val orderedKeys = paramOrder.filter { it in allItems } + allItems.keys.filter { it !in paramOrder }
    return orderedKeys.mapNotNull { allItems[it] }
}

@Composable
fun HourlyForecastSection(
    hourlyForecast: List<HourlyForecastEntity>,
    selectedDate: String? = null,
    onBackToToday: () -> Unit = {},
    noDataForDate: Boolean = false
) {
    val cardShape = RoundedCornerShape(16.dp)
    val cardBorder = if (MaterialTheme.colorScheme.background == com.ergonomic.mountainweather.ui.theme.BackgroundDark)
        CardBorderDark else CardBorderLight

    val currentHour = remember { LocalDateTime.now().hour }
    val scrollToIndex = remember(hourlyForecast) {
        hourlyForecast.indexOfFirst { entry ->
            try {
                LocalDateTime.parse(entry.time, DateTimeFormatter.ISO_LOCAL_DATE_TIME).hour == currentHour
            } catch (_: Exception) { false }
        }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState()
    LaunchedEffect(hourlyForecast) {
        listState.scrollToItem(scrollToIndex)
    }

    val headerLabel = if (selectedDate != null) {
        val date = try {
            val d = LocalDate.parse(selectedDate)
            val dow = d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
            "${d.format(DateTimeFormatter.ofPattern("dd.MM"))} ($dow)"
        } catch (_: Exception) { selectedDate }
        "${stringResource(R.string.hourly_forecast)} · $date"
    } else {
        stringResource(R.string.hourly_forecast)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = headerLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            if (selectedDate != null) {
                Text(
                    text = stringResource(R.string.today_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onBackToToday)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        if (noDataForDate) {
            Text(
                text = stringResource(R.string.hourly_no_data),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardBorder, cardShape),
            shape = cardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            LazyRow(
                state = listState,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                items(hourlyForecast, key = { it.time }) { item ->
                    val itemHour = remember(item.time) {
                        try {
                            LocalDateTime.parse(item.time, DateTimeFormatter.ISO_LOCAL_DATE_TIME).hour
                        } catch (_: Exception) { -1 }
                    }
                    HourlyForecastItem(item, isCurrentHour = itemHour == currentHour)
                }
            }
        }
    }
}

@Composable
fun HourlyForecastItem(item: HourlyForecastEntity, isCurrentHour: Boolean = false) {
    val hourData = remember(item.time) {
        try {
            val dt = LocalDateTime.parse(item.time, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            dt.format(DateTimeFormatter.ofPattern("HH:mm")) to dt.hour
        } catch (_: Exception) {
            item.time.takeLast(5) to 12
        }
    }
    val hour = hourData.first
    val isDay = hourData.second in 6..20
    val info = weatherCodeToInfo(item.weatherCode, isDay)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        Text(
            text = hour,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = info.icon, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${item.temperature.toInt()}°",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        if (item.precipitation > 0) {
            Text(
                text = "${item.precipitation}mm",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun DailyForecastSection(
    dailyForecast: List<DailyForecastEntity>,
    selectedDate: String? = null,
    showHourly: Boolean = false,
    onDayClick: (String) -> Unit = {}
) {
    val cardShape = RoundedCornerShape(16.dp)
    val cardBorder = if (MaterialTheme.colorScheme.background == com.ergonomic.mountainweather.ui.theme.BackgroundDark)
        CardBorderDark else CardBorderLight
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.daily_forecast),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardBorder, cardShape),
            shape = cardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                dailyForecast.forEachIndexed { index, item ->
                    val isSelected = item.date == selectedDate
                    DailyForecastItem(
                        item = item,
                        isSelected = isSelected,
                        isClickable = showHourly,
                        onClick = { onDayClick(item.date) }
                    )
                    if (index < dailyForecast.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DailyForecastItem(
    item: DailyForecastEntity,
    isSelected: Boolean = false,
    isClickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    val dayLabel = remember(item.date) {
        try {
            val date = LocalDate.parse(item.date)
            val dow = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val formatted = date.format(DateTimeFormatter.ofPattern("dd.MM"))
            "$formatted ($dow)"
        } catch (_: Exception) {
            item.date
        }
    }
    val info = weatherCodeToInfo(item.weatherCode)
    val selectedBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(selectedBg, RoundedCornerShape(8.dp))
                else Modifier
            )
            .then(
                if (isClickable) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayLabel,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(text = info.icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
        Text(
            text = stringResource(
                R.string.temp_max_min,
                item.temperatureMax.toInt().toString(),
                item.temperatureMin.toInt().toString()
            ),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        if (item.precipitationSum > 0) {
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Text(
                text = "${item.precipitationSum}mm",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ErrorContent(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.padding(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Text(text = "📡", fontSize = 56.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.connection_error),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.connection_error_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}
