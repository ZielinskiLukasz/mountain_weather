package com.ergonomic.mountainweather.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ergonomic.mountainweather.R
import com.ergonomic.mountainweather.data.repository.ForecastSettings
import com.ergonomic.mountainweather.data.repository.SettingsRepository
import com.ergonomic.mountainweather.data.sync.SyncScheduler
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepo: SettingsRepository,
    onBack: () -> Unit
) {
    val settings by settingsRepo.forecastSettings.collectAsState(initial = ForecastSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
                .verticalScroll(rememberScrollState())
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
                        settingsRepo.setDailyMode(daily3 = enabled, daily5 = if (enabled) false else settings.showDaily5)
                    }
                }
            )

            SettingsToggle(
                label = stringResource(R.string.show_daily_5),
                checked = settings.showDaily5,
                onCheckedChange = { enabled ->
                    scope.launch {
                        settingsRepo.setDailyMode(daily3 = if (enabled) false else settings.showDaily3, daily5 = enabled)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.network_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggle(
                label = stringResource(R.string.resilient_sync),
                checked = settings.resilientSync,
                onCheckedChange = { scope.launch { settingsRepo.setResilientSync(it) } }
            )

            Text(
                text = stringResource(R.string.resilient_sync_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.background_sync),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.background_sync_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            val intervalLabels = mapOf(
                0 to stringResource(R.string.sync_off),
                10 to stringResource(R.string.sync_10_min),
                30 to stringResource(R.string.sync_30_min),
                60 to stringResource(R.string.sync_1h),
                180 to stringResource(R.string.sync_3h),
                360 to stringResource(R.string.sync_6h),
                720 to stringResource(R.string.sync_12h)
            )

            var expanded by remember { mutableStateOf(false) }
            val currentLabel = intervalLabels[settings.syncIntervalMinutes]
                ?: stringResource(R.string.sync_off)

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = currentLabel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    SyncScheduler.INTERVAL_OPTIONS.forEach { minutes ->
                        DropdownMenuItem(
                            text = { Text(intervalLabels[minutes] ?: "$minutes min") },
                            onClick = {
                                expanded = false
                                scope.launch {
                                    settingsRepo.setSyncInterval(minutes)
                                    if (minutes > 0) {
                                        SyncScheduler.enable(context, minutes)
                                    } else {
                                        SyncScheduler.disable(context)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            val packageInfo = remember {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            Text(
                text = "${packageInfo.versionName} (${packageInfo.longVersionCode})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
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
