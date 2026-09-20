package com.littlewords.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.littlewords.app.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import com.littlewords.app.domain.Category
import com.littlewords.app.domain.PracticeConfig

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var db: ReadingDatabase
    private lateinit var repo: ReadingRepository
    private lateinit var databaseName: String
    @Before fun setup() {
        databaseName = "test-${java.util.UUID.randomUUID()}.db"
        db = ReadingDatabase.open(context, databaseName)
        repo = ReadingRepository(db)
    }
    @After fun cleanup() { db.close(); context.deleteDatabase(databaseName) }

    @Test fun scoringIsDurableAndConcurrentDuplicateIsIgnored() = runBlocking {
        repo.startSession()
        val card = repo.currentCard.first { it != null }!!
        val results = coroutineScope { listOf(async { repo.score(card.id, true) }, async { repo.score(card.id, false) }).awaitAll() }
        assertEquals(1, results.count { it })
        assertEquals(1, repo.history.first().count { it.voidedAt == null })
        assertEquals(card.word, repo.history.first().single().word)
        val next = repo.currentCard.first { it?.id != card.id && it != null }!!
        db.close()
        db = ReadingDatabase.open(context, databaseName)
        repo = ReadingRepository(db)
        assertEquals(next.id, repo.currentCard.first()!!.id)
        assertEquals(1, repo.history.first().size)
    }

    @Test fun undoKeepsAuditAndRescoringCountsOnce() = runBlocking {
        repo.startSession()
        val card = repo.currentCard.first { it != null }!!
        repo.score(card.id, false)
        assertTrue(repo.undo())
        assertEquals(card.id, repo.currentCard.first()!!.id)
        assertNotNull(repo.history.first().single().voidedAt)
        assertFalse(repo.undo())
        repo.score(card.id, true)
        val history = repo.history.first()
        assertEquals(2, history.size)
        assertEquals(1, history.count { it.voidedAt == null && it.success })
    }

    @Test fun failedWriteDoesNotAdvanceOrRecordAttempt() = runBlocking {
        repo.startSession()
        val card = repo.currentCard.first { it != null }!!
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_attempt BEFORE INSERT ON attempts BEGIN SELECT RAISE(ABORT, 'simulated storage failure'); END")
        var failed = false
        try { repo.score(card.id, true) } catch (_: Exception) { failed = true }
        assertTrue(failed)
        assertEquals(card.id, repo.currentCard.first()!!.id)
        assertTrue(repo.history.first().isEmpty())
    }

    @Test fun settingsSurviveReopenAndSessionKeepsOriginalMix() = runBlocking {
        repo.startSession()
        val original = repo.activeSession.first()!!.config
        val settings = AppSettings(theme = ThemeMode.DARK, showButtons = true,
            config = PracticeConfig(enabledCategories = setOf(Category.THREE_REAL)))
        repo.saveSettings(settings)
        db.close()
        db = ReadingDatabase.open(context, databaseName)
        repo = ReadingRepository(db)
        assertEquals(settings, repo.settings.first())
        assertEquals(original, repo.activeSession.first()!!.config)
    }

    @Test fun endingLeavesUnscoredWordOutOfResults() = runBlocking {
        repo.startSession()
        repo.endSession()
        assertNull(repo.activeSession.first())
        assertNull(repo.currentCard.first())
        assertTrue(repo.history.first().isEmpty())
        assertNotNull(repo.sessions.first().single().endedAt)
    }

    @Test fun displayedUnscoredWordsParticipateInRepetitionSpacing() = runBlocking {
        repo.saveSettings(AppSettings(config = PracticeConfig(
            enabledCategories = setOf(Category.TWO_REAL),
            letters = "amnt",
            patterns = setOf("short_a"),
        )))
        repo.startSession()
        val first = repo.currentCard.first { it != null }!!
        repo.score(first.id, true)
        val displaced = repo.currentCard.first { it?.id != first.id && it != null }!!
        assertEquals(listOf(displaced.word, first.word), db.dao().recentPresentedWords(5))

        assertTrue(repo.undo())
        assertEquals(listOf(displaced.word), db.dao().recentPresentedWords(5))
        assertTrue(repo.score(first.id, true))
        val replacement = repo.currentCard.first { it?.id != first.id && it != null }!!
        assertNotEquals(displaced.word, replacement.word)
    }

    @Test fun applicationOwnsOneRepositoryForActivityRecreation() {
        val application = context.applicationContext as LittleWordsApplication
        assertSame(application.repository, application.repository)
    }

    @Test fun endingSessionSpacesItsUnscoredWordFromNextSession() = runBlocking {
        repo.saveSettings(AppSettings(config = PracticeConfig(
            enabledCategories = setOf(Category.TWO_REAL),
            letters = "amnt",
            patterns = setOf("short_a"),
        )))
        repo.startSession()
        val abandoned = repo.currentCard.first { it != null }!!
        repo.endSession()
        repo.startSession()
        val next = repo.currentCard.first { it != null }!!
        assertNotEquals(abandoned.word, next.word)
    }

    @Test fun legacyWeightSettingsDecodeAfterDatabaseReopen() = runBlocking {
        db.dao().putSettings(SettingsEntity(
            config = """{"weights":[10,70,20,0,0],"letters":"abcdefghijklmnopqrstuvwxyz","patterns":["short_a","short_e","short_i","short_o","short_u"]}""",
            theme = ThemeMode.SYSTEM.name,
            showButtons = false,
            showSillyMarker = true,
        ))

        db.close()
        db = ReadingDatabase.open(context, databaseName)
        repo = ReadingRepository(db)

        assertEquals(
            setOf(Category.TWO_REAL, Category.THREE_REAL, Category.THREE_SILLY),
            repo.settings.first().config.enabledCategories,
        )
    }
}
