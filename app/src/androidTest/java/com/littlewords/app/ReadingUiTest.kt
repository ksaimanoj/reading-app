package com.littlewords.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.geometry.Offset
import androidx.test.core.app.ApplicationProvider
import com.littlewords.app.data.*
import com.littlewords.app.domain.Category
import com.littlewords.app.ui.LittleWordsApp
import com.littlewords.app.ui.ReadingViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*

class ReadingUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: ReadingDatabase
    private lateinit var repo: ReadingRepository
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val name = "ui-${java.util.UUID.randomUUID()}.db"
    @Before fun setup() {
        db = ReadingDatabase.open(context, name)
        repo = ReadingRepository(db)
        compose.setContent { LittleWordsApp(ReadingViewModel(repo)) }
        compose.waitUntil(10000) { compose.onAllNodesWithText("Start reading").fetchSemanticsNodes().isNotEmpty() }
    }
    @After fun cleanup() { db.close(); context.deleteDatabase(name) }

    @Test fun gesturesAndUndoUpdateStoredHistory() {
        compose.onNodeWithText("Start reading").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("readingWord").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("readingSurface").performTouchInput { swipeRight() }
        compose.waitUntil(10000) { runBlocking { repo.history.first().count { it.voidedAt == null } == 1 } }
        assertTrue(runBlocking { repo.history.first().first().success })
        compose.onNodeWithTag("readingSurface").performTouchInput { swipeLeft() }
        compose.waitUntil(10000) { runBlocking { repo.history.first().count { it.voidedAt == null } == 2 } }
        assertFalse(runBlocking { repo.history.first().first().success })
        compose.onNodeWithText("Pause").performClick()
        compose.onNodeWithText("Undo last swipe").performClick()
        compose.waitUntil(10000) { runBlocking { repo.history.first().count { it.voidedAt == null } == 1 } }
    }

    @Test fun appearanceChoiceIsSaved() {
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Dark", useUnmergedTree = true).performClick()
        compose.waitUntil(10000) { runBlocking { repo.settings.first().theme == ThemeMode.DARK } }
        compose.onNodeWithText("Light", useUnmergedTree = true).performClick()
        compose.waitUntil(10000) { runBlocking { repo.settings.first().theme == ThemeMode.LIGHT } }
        compose.onNodeWithText("System", useUnmergedTree = true).performClick()
        compose.waitUntil(10000) { runBlocking { repo.settings.first().theme == ThemeMode.SYSTEM } }
    }

    @Test fun settingsUseWordGroupSwitchesInsteadOfPercentages() {
        compose.onNodeWithText("Settings").performClick()

        compose.onNodeWithText("Word groups").assertExists()
        compose.onNodeWithText("Share").assertDoesNotExist()
        compose.onNodeWithText("CVC starter").assertDoesNotExist()
        compose.onNodeWithTag("category_FOUR_REAL").performClick()
        compose.onNodeWithText("Save & back").performClick()

        compose.waitUntil(10000) {
            Category.FOUR_REAL in runBlocking { repo.settings.first().config.enabledCategories }
        }
    }

    @Test fun shortAndVerticalGesturesRemainUnscored() {
        compose.onNodeWithText("Start reading").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("readingSurface").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("readingSurface").performTouchInput {
            swipe(start = center, end = center + Offset(20f, 0f), durationMillis = 100)
        }
        compose.onNodeWithTag("readingSurface").performTouchInput {
            swipe(start = center, end = center + Offset(0f, 240f), durationMillis = 150)
        }
        compose.waitForIdle()
        assertTrue(runBlocking { repo.history.first().isEmpty() })
    }

    @Test fun scoringButtonRecordsAnOutcome() {
        runBlocking {
            val current = repo.settings.first()
            repo.saveSettings(current.copy(showButtons = true))
        }
        compose.onNodeWithText("Start reading").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Read it  →").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Read it  →").performClick()
        compose.waitUntil(10000) { runBlocking { repo.history.first().size == 1 } }
        assertEquals(1, runBlocking { repo.history.first().count { it.voidedAt == null && it.success } })
    }

    @Test fun rapidGesturesRecordOnlyOneOutcome() {
        compose.onNodeWithText("Start reading").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("readingSurface").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("readingSurface").performTouchInput { swipeRight(); swipeRight() }
        compose.waitUntil(10000) { runBlocking { repo.history.first().size == 1 } }
        assertEquals(1, runBlocking { repo.history.first().count { it.voidedAt == null } })
    }
}
