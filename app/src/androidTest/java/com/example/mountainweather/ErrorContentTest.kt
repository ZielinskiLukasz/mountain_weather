package com.example.mountainweather

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.mountainweather.ui.theme.MountainWeatherTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ErrorContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun errorContent_displaysMessage() {
        composeTestRule.setContent {
            MountainWeatherTheme {
                ErrorContent(
                    message = "Network timeout",
                    onRetry = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Network timeout").assertIsDisplayed()
    }

    @Test
    fun errorContent_retryButtonClickable() {
        var clicked = false
        composeTestRule.setContent {
            MountainWeatherTheme {
                ErrorContent(
                    message = "Error occurred",
                    onRetry = { clicked = true }
                )
            }
        }
        composeTestRule.onNodeWithText(substring = true, text = "etry").performClick()
        assertTrue(clicked)
    }
}
