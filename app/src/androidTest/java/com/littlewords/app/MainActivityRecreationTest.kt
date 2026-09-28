package com.littlewords.app

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainActivityRecreationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun currentWordSurvivesActivityRecreation() {
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Start reading").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("Resume reading").fetchSemanticsNodes().isNotEmpty()
        }
        val start = if (compose.onAllNodesWithText("Start reading").fetchSemanticsNodes().isNotEmpty()) "Start reading" else "Resume reading"
        compose.onNodeWithText(start).performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("readingWord").fetchSemanticsNodes().isNotEmpty() }
        val before = compose.onNodeWithTag("readingWord").fetchSemanticsNode().config[SemanticsProperties.Text].single().text

        compose.activityRule.scenario.recreate()

        compose.waitUntil(10000) { compose.onAllNodesWithTag("readingWord").fetchSemanticsNodes().isNotEmpty() }
        val after = compose.onNodeWithTag("readingWord").fetchSemanticsNode().config[SemanticsProperties.Text].single().text
        assertEquals(before, after)
    }

    @Test fun readingAndPauseKeepTheirCardAcrossOrientationChanges() {
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Start reading").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("Resume reading").fetchSemanticsNodes().isNotEmpty()
        }
        val phone = compose.activity.resources.configuration.smallestScreenWidthDp < 600 && !compose.activity.isInMultiWindowMode
        if (phone) compose.waitUntil(10000) {
            compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        }
        compose.onNodeWithText(if (compose.onAllNodesWithText("Start reading").fetchSemanticsNodes().isNotEmpty())
            "Start reading" else "Resume reading").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("readingWord").fetchSemanticsNodes().isNotEmpty() }
        if (phone) compose.waitUntil(10000) {
            compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }
        val word = compose.onNodeWithTag("readingWord").fetchSemanticsNode().config[SemanticsProperties.Text].single().text
        compose.onNodeWithText("Pause").performClick()
        compose.onNodeWithText("Take your time").assertIsDisplayed()
        assertEquals(if (phone) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            compose.activity.requestedOrientation)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Take your time").assertIsDisplayed()
        compose.onNodeWithText("Keep reading").performClick()
        assertEquals(word, compose.onNodeWithTag("readingWord").fetchSemanticsNode().config[SemanticsProperties.Text].single().text)
        compose.onNodeWithText("Pause").performClick()
        compose.onNodeWithText("Save & go home").performClick()
        if (phone) compose.waitUntil(10000) {
            compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        }
        compose.onNodeWithText("Resume reading").assertIsDisplayed().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("readingWord").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(word, compose.onNodeWithTag("readingWord").fetchSemanticsNode().config[SemanticsProperties.Text].single().text)
    }
}
