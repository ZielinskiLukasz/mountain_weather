package com.example.mountainweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mountainweather.data.local.DailyForecastEntity
import com.example.mountainweather.data.local.HourlyForecastEntity
import com.example.mountainweather.data.local.WeatherEntity
import com.example.mountainweather.data.repository.ForecastSettings
import com.example.mountainweather.ui.locations.LocationScreen
import com.example.mountainweather.ui.settings.SettingsScreen
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.mountainweather.ui.theme.MountainWeatherTheme
import com.example.mountainweather.util.weatherCodeToInfo
import com.example.mountainweather.util.windDirectionToArrow
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
        val settingsRepo = com.example.mountainweather.data.repository.SettingsRepository(this)
        kotlinx.coroutines.MainScope().launch {
            val settings = settingsRepo.forecastSettings.first()
            if (settings.backgroundSync) {
                com.example.mountainweather.data.sync.SyncScheduler.enable(this@MainActivity)
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

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
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
                onRefresh = { viewModel.refresh() },
                modifier = modifier.fillMaxSize()
            ) {
                Column {
                    if (state.isOfflineData) {
                        OfflineBanner(cachedAt = state.weather?.cachedAt ?: 0L)
                    }
                    WeatherContent(
                        locationName = state.locationName,
                        weather = state.weather!!,
                        hourlyForecast = state.hourlyForecast,
                        dailyForecast = state.dailyForecast,
                        settings = settings,
                        isOffline = state.isOfflineData,
                        isFavorite = state.isFavorite,
                        onChangeLocation = onChangeLocation,
                        onOpenSettings = onOpenSettings,
                        onToggleFavorite = { viewModel.toggleFavorite() }
                    )
                }
            }
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
            .background(MaterialTheme.colorScheme.tertiaryContainer)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherContent(
    locationName: String,
    weather: WeatherEntity,
    hourlyForecast: List<HourlyForecastEntity>,
    dailyForecast: List<DailyForecastEntity>,
    settings: ForecastSettings,
    isOffline: Boolean,
    isFavorite: Boolean,
    onChangeLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val weatherInfo = weatherCodeToInfo(weather.weatherCode)
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            modifier = Modifier.clickable(onClick = onChangeLocation),
            title = {
                Text(
                    text = locationName,
                    style = MaterialTheme.typography.headlineMedium
                )
            },
            actions = {
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
                        tint = MaterialTheme.colorScheme.primary
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
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = weatherInfo.icon, fontSize = 72.sp)

        Text(
            text = "${weather.temperature}°C",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = stringResource(R.string.feels_like, weather.apparentTemperature.toString()),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(weatherInfo.descriptionRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow(
                    "💨 ${stringResource(R.string.wind)}",
                    "${weather.windSpeed} km/h  ${windDirectionToArrow(weather.windDirection)}"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                DetailRow(
                    "💧 ${stringResource(R.string.humidity)}",
                    "${weather.humidity}%"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                DetailRow(
                    "🌧️ ${stringResource(R.string.precipitation)}",
                    "${weather.precipitation} mm"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                DetailRow(
                    "🌡️ ${stringResource(R.string.pressure)}",
                    "${weather.pressure.toInt()} hPa"
                )
            }
        }

        if (settings.showHourly && hourlyForecast.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            HourlyForecastSection(hourlyForecast)
        }

        if ((settings.showDaily3 || settings.showDaily5) && dailyForecast.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            DailyForecastSection(dailyForecast)
        }

        Spacer(modifier = Modifier.height(16.dp))

        val formattedTime = remember(weather.time) {
            try {
                LocalDateTime.parse(weather.time, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .format(DateTimeFormatter.ofPattern("HH:mm, dd.MM.yyyy"))
            } catch (_: Exception) {
                weather.time
            }
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
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HourlyForecastSection(hourlyForecast: List<HourlyForecastEntity>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.hourly_forecast),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            LazyRow(
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                items(hourlyForecast, key = { it.time }) { item ->
                    HourlyForecastItem(item)
                }
            }
        }
    }
}

@Composable
fun HourlyForecastItem(item: HourlyForecastEntity) {
    val hour = remember(item.time) {
        try {
            LocalDateTime.parse(item.time, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            item.time.takeLast(5)
        }
    }
    val info = weatherCodeToInfo(item.weatherCode)

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
fun DailyForecastSection(dailyForecast: List<DailyForecastEntity>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.daily_forecast),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                dailyForecast.forEachIndexed { index, item ->
                    DailyForecastItem(item)
                    if (index < dailyForecast.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DailyForecastItem(item: DailyForecastEntity) {
    val dayName = remember(item.date) {
        try {
            val date = LocalDate.parse(item.date)
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        } catch (_: Exception) {
            item.date
        }
    }
    val info = weatherCodeToInfo(item.weatherCode)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayName,
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Text(text = "⚠️", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.connection_error),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}
