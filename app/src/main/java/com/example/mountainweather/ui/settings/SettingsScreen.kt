package com.example.mountainweather.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mountainweather.R
import com.example.mountainweather.data.repository.ForecastSettings
import com.example.mountainweather.data.repository.SettingsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepo: SettingsRepository,
    onBack: () -> Unit
) {
    val settings by settingsRepo.forecastSettings.collectAsState(initial = ForecastSettings())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
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
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.forecast_types),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggle(
                label = stringResource(R.string.show_hourly_24h),
                checked = settings.showHourly,
                onCheckedChange = { scope.launch { settingsRepo.setShowHourly(it) } }
            )

            SettingsToggle(
                label = stringResource(R.string.show_daily_3),
                checked = settings.showDaily3,
                onCheckedChange = { enabled ->
                    scope.launch {
                        settingsRepo.setShowDaily3(enabled)
                        if (enabled) settingsRepo.setShowDaily5(false)
                    }
                }
            )

            SettingsToggle(
                label = stringResource(R.string.show_daily_5),
                checked = settings.showDaily5,
                onCheckedChange = { enabled ->
                    scope.launch {
                        settingsRepo.setShowDaily5(enabled)
                        if (enabled) settingsRepo.setShowDaily3(false)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}
