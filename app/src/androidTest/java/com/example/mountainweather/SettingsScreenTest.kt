package com.example.mountainweather

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.example.mountainweather.data.repository.SettingsRepository
import com.example.mountainweather.ui.settings.SettingsScreen
import com.example.mountainweather.ui.theme.MountainWeatherTheme
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun settingsRepo(): SettingsRepository {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return SettingsRepository(context)
    }

    @Test
    fun settingsScreen_displaysForecastToggles() {
        composeTestRule.setContent {
            MountainWeatherTheme {
                SettingsScreen(
                    settingsRepo = settingsRepo(),
                    onBack = {}
                )
            }
        }
        composeTestRule.onNodeWithText(substring = true, text = "24h").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysNetworkSection() {
        composeTestRule.setContent {
            MountainWeatherTheme {
                SettingsScreen(
                    settingsRepo = settingsRepo(),
                    onBack = {}
                )
            }
        }
        composeTestRule.onNodeWithText(substring = true, text = "esilient").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysBackgroundSyncSection() {
        composeTestRule.setContent {
            MountainWeatherTheme {
                SettingsScreen(
                    settingsRepo = settingsRepo(),
                    onBack = {}
                )
            }
        }
        val nodes = composeTestRule.onAllNodesWithText(
            substring = true, text = "ackground"
        ).fetchSemanticsNodes()
        assertTrue("Expected background sync nodes on screen", nodes.isNotEmpty())
    }
}
