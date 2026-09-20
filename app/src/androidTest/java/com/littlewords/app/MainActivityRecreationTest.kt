package com.littlewords.app

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
}
