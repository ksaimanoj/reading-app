package com.littlewords.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.littlewords.app.data.AppSettings
import com.littlewords.app.domain.PracticeConfig
import com.littlewords.app.domain.PracticeMode
import com.littlewords.app.domain.SentenceSelector
import com.littlewords.app.ui.HomeScreen
import com.littlewords.app.ui.LittleWordsTheme
import com.littlewords.app.ui.ReadingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeSentenceAvailabilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun noEligibleSentencesOfferAnAdjustmentRoute() {
        var adjusted = false
        val state = ReadingState(settings = AppSettings(config = PracticeConfig(patterns = emptySet())))
        compose.setContent {
            LittleWordsTheme(state.settings.theme) {
                HomeScreen(state, busy = false, onStart = {}, onSettings = {},
                    onAdjustSentences = { adjusted = true }, onProgress = {}, onProfiles = {})
            }
        }

        compose.onNodeWithText("No short sentences match these choices.").assertExists()
        compose.onNodeWithContentDescription("Try short sentences").assertDoesNotExist()
        compose.onNodeWithText("Adjust choices").performClick()
        assertTrue(adjusted)
    }

    @Test fun availableSentenceCountComesFromTheRealSelector() {
        var started: PracticeMode? = null
        val state = ReadingState(settings = AppSettings())
        val count = SentenceSelector.eligible(state.settings.config).size
        compose.setContent {
            LittleWordsTheme(state.settings.theme) {
                HomeScreen(state, busy = false, onStart = { started = it }, onSettings = {},
                    onAdjustSentences = {}, onProgress = {}, onProfiles = {})
            }
        }

        compose.onNodeWithText("$count short sentences available with these choices.").assertExists()
        compose.onNodeWithContentDescription("Try short sentences").performClick()
        assertEquals(PracticeMode.SENTENCES, started)
    }
}
