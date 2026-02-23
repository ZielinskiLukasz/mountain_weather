package com.ergonomic.mountainweather

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ergonomic.mountainweather.data.local.WeatherEntity
import com.ergonomic.mountainweather.data.repository.ForecastSettings
import com.ergonomic.mountainweather.ui.theme.MountainWeatherTheme
import org.junit.Rule
import org.junit.Test

class WeatherScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleWeather() = WeatherEntity(
        locationKey = "50.06_19.94",
        locationName = "Kraków",
        latitude = 50.06,
        longitude = 19.94,
        temperature = 15.0,
        apparentTemperature = 13.5,
        weatherCode = 0,
        windSpeed = 10.0,
        windDirection = 180,
        humidity = 65,
        precipitation = 0.0,
        pressure = 1013.0,
        time = "2025-01-01T12:00",
        cachedAt = System.currentTimeMillis()
    )

    @Test
    fun weatherContent_displaysTemperature() {
        composeTestRule.setContent {
            MountainWeatherTheme {
                WeatherContent(
                    locationName = "Kraków",
                    weather = sampleWeather(),
                    hourlyForecast = emptyList(),
                    dailyForecast = emptyList(),
                    settings = ForecastSettings(),
                    isOffline = false,
                    isFavorite = false,
                    onChangeLocation = {},
                    onOpenSettings = {},
                    onToggleFavorite = {}
                )
            }
        }
        composeTestRule.onNodeWithText("15.0°C").assertIsDisplayed()
    }

    @Test
    fun weatherContent_displaysLocationName() {
        composeTestRule.setContent {
            MountainWeatherTheme {
                WeatherContent(
                    locationName = "Zakopane",
                    weather = sampleWeather().copy(locationName = "Zakopane"),
                    hourlyForecast = emptyList(),
                    dailyForecast = emptyList(),
                    settings = ForecastSettings(),
                    isOffline = false,
                    isFavorite = false,
                    onChangeLocation = {},
                    onOpenSettings = {},
                    onToggleFavorite = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Zakopane").assertIsDisplayed()
    }

    @Test
    fun weatherContent_displaysWindSpeed() {
        composeTestRule.setContent {
            MountainWeatherTheme {
                WeatherContent(
                    locationName = "Kraków",
                    weather = sampleWeather(),
                    hourlyForecast = emptyList(),
                    dailyForecast = emptyList(),
                    settings = ForecastSettings(),
                    isOffline = false,
                    isFavorite = false,
                    onChangeLocation = {},
                    onOpenSettings = {},
                    onToggleFavorite = {}
                )
            }
        }
        composeTestRule.onNodeWithText("10.0 km/h  ↓ S").assertIsDisplayed()
    }

    @Test
    fun offlineBanner_displayedWhenOffline() {
        composeTestRule.setContent {
            MountainWeatherTheme {
                OfflineBanner(cachedAt = System.currentTimeMillis())
            }
        }
        composeTestRule.onNodeWithText(
            substring = true,
            text = "ffline"
        ).assertIsDisplayed()
    }
}
