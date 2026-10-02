package com.littlewords.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.littlewords.app.data.AppSettings
import com.littlewords.app.data.CardEntity
import com.littlewords.app.domain.Category
import com.littlewords.app.ui.PracticeScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PracticeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun scoringHintAppearsUntilFirstScoreAndButtonsCanBeToggledOnCard() {
        var card by mutableStateOf(CardEntity(id = 1, sessionId = 1, word = "cat",
            category = Category.THREE_REAL.name, patterns = "", shownAt = 0))
        var settings by mutableStateOf(AppSettings())
        var scoreCount by mutableIntStateOf(0)
        compose.setContent {
            PracticeScreen(card, settings, itemNumber = scoreCount + 1, busy = false,
                onScore = {
                    scoreCount++
                    card = card.copy(id = card.id + 1, word = "dog")
                },
                onPause = {},
                onToggleButtons = { settings = settings.copy(showButtons = !settings.showButtons) },
            )
        }

        val hint = "Swipe right for Read it; swipe left for Needs practice. Or show scoring buttons."
        compose.onNodeWithText(hint).assertExists()
        compose.onNodeWithText("Show buttons").performClick()
        compose.onNodeWithText(hint).assertDoesNotExist()
        compose.onNodeWithText("Read it  →").assertExists()
        compose.onNodeWithText("Hide buttons").performClick()
        compose.onNodeWithText(hint).assertExists()

        compose.onNodeWithTag("readingSurface").performTouchInput { swipeRight() }
        compose.runOnIdle { assertEquals(1, scoreCount) }
        compose.onNodeWithText(hint).assertDoesNotExist()
        compose.onNodeWithText("Show buttons").performClick()
        compose.onNodeWithText("Read it  →").performClick()
        compose.runOnIdle { assertEquals(2, scoreCount) }
    }
}
