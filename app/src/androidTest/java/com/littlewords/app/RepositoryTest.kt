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
import com.littlewords.app.domain.PracticeMode
import com.littlewords.app.domain.SENTENCE_CATEGORY
import com.littlewords.app.domain.SentenceSelector
import com.littlewords.app.domain.StageCatalog

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

    @Test fun profilesKeepSettingsProgressAndOpenSessionsSeparateAcrossRestart() = runBlocking {
        val gaganSettings = AppSettings(theme = ThemeMode.DARK, showButtons = true)
        repo.saveSettings(gaganSettings)
        repo.startSession()
        val gaganCard = repo.currentCard.first { it != null }!!
        assertTrue(repo.score(gaganCard.id, false))
        val gaganNext = repo.currentCard.first { it?.id != gaganCard.id && it != null }!!

        val testingId = repo.createProfile(" Testing ")
        assertEquals("Testing", repo.profiles.first().last().name)
        assertEquals(testingId, repo.reading.first().profileId)
        assertEquals(AppSettings(), repo.settings.first())
        assertTrue(repo.history.first().isEmpty())
        assertNull(repo.activeSession.first())
        repo.startSession()
        val testCard = repo.currentCard.first { it != null }!!
        assertTrue(repo.score(testCard.id, true))
        assertEquals(1, repo.history.first().size)

        repo.selectProfile(1)
        assertEquals(gaganSettings, repo.settings.first())
        assertEquals(gaganNext.id, repo.currentCard.first()!!.id)
        assertEquals(gaganCard.word, repo.history.first().single().word)
        assertFalse(repo.history.first().single().success)

        db.close()
        db = ReadingDatabase.open(context, databaseName)
        repo = ReadingRepository(db)
        assertEquals(1L, repo.reading.first().profileId)
        assertEquals(gaganNext.id, repo.currentCard.first()!!.id)
        repo.selectProfile(testingId)
        assertEquals(1, repo.history.first().size)
        assertTrue(repo.history.first().single().success)
        assertEquals(AppSettings(), repo.settings.first())
    }

    @Test fun stageChoicesBelongToEachProfileAndDoNotChangeALegacySession() = runBlocking {
        repo.startSession()
        val legacySnapshot = repo.activeSession.first()!!.config
        val sh = StageCatalog.saveChoices(repo.settings.first().config, setOf("digraphs:sh"))
        repo.saveSettings(AppSettings(config = sh))
        assertEquals(legacySnapshot, repo.activeSession.first()!!.config)
        val second = repo.createProfile("Second")
        val cvc = StageCatalog.saveChoices(repo.settings.first().config, setOf("cvc:short_a"))
        repo.saveSettings(AppSettings(config = cvc))
        db.close()
        db = ReadingDatabase.open(context, databaseName)
        repo = ReadingRepository(db)
        assertEquals(setOf("cvc:short_a"), repo.settings.first().config.selectedSubskills)
        repo.selectProfile(1)
        assertEquals(setOf("digraphs:sh"), repo.settings.first().config.selectedSubskills)
        assertEquals(legacySnapshot, repo.activeSession.first()!!.config)
        repo.selectProfile(second)
        assertEquals(setOf("cvc:short_a"), repo.settings.first().config.selectedSubskills)
    }

    @Test fun invalidStageSaveKeepsTheLastSavedConfiguration() = runBlocking {
        val valid = StageCatalog.saveChoices(repo.settings.first().config, setOf("digraphs:sh"))
        repo.saveSettings(AppSettings(config = valid))
        try {
            repo.saveSettings(AppSettings(config = valid.copy(selectedSubskills = emptySet())))
            fail("Empty stage choices should be rejected")
        } catch (_: IllegalArgumentException) { }
        assertEquals(setOf("digraphs:sh"), repo.settings.first().config.selectedSubskills)
    }

    @Test fun duplicateProfileNamesAreRejectedIgnoringCase() = runBlocking {
        try {
            repo.createProfile(" gAGan ")
            fail("Duplicate profile should be rejected")
        } catch (_: IllegalArgumentException) { }
        assertEquals(listOf("Gagan"), repo.profiles.first().map { it.name })
    }

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

    @Test fun sentenceSessionStoresExactReviewedSentenceAndMode() = runBlocking {
        repo.startSession(PracticeMode.SENTENCES)

        val session = repo.activeSession.first()!!
        val card = repo.currentCard.first { it != null }!!

        assertEquals(PracticeMode.SENTENCES, ConfigCodec.decode(session.config).mode)
        assertEquals(SENTENCE_CATEGORY, card.category)
        assertTrue(card.word.endsWith("."))
        assertTrue(card.word.contains(" "))
        assertTrue(repo.score(card.id, true))
        assertEquals(card.word, repo.history.first().single().word)
    }

    @Test fun sentenceSessionShowsEveryEligibleSentenceBeforeRepeating() = runBlocking {
        val config = PracticeConfig(patterns = setOf("short_a"))
        val eligible = SentenceSelector.eligible(config).map { it.text }.toSet()
        assertTrue(eligible.size > 1)
        repo.saveSettings(AppSettings(config = config))
        repo.startSession(PracticeMode.SENTENCES)

        val seen = mutableSetOf<String>()
        repeat(eligible.size) {
            val card = repo.currentCard.first { it != null }!!
            assertTrue(seen.add(card.word))
            assertTrue(repo.score(card.id, true))
        }
        assertEquals(eligible, seen)
        val next = repo.currentCard.first { it != null }!!
        assertTrue(next.word in seen)
    }

    @Test fun readingTimeIsStoredWithTheAttemptAndOlderAttemptsCanBeUnknown() = runBlocking {
        repo.startSession()
        val first = repo.currentCard.first { it != null }!!
        assertTrue(repo.score(first.id, true, durationMs = 2_350))

        val timed = repo.history.first().single()
        assertEquals(2_350L, timed.durationMs)

        val second = repo.currentCard.first { it?.id != first.id && it != null }!!
        assertTrue(repo.score(second.id, false))
        assertNull(repo.history.first().first().durationMs)
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

    @Test fun readingScreenButtonChoicePersistsWithoutChangingTheCurrentSessionOrOtherProfiles() = runBlocking {
        val original = AppSettings(theme = ThemeMode.DARK, config = PracticeConfig(
            enabledCategories = setOf(Category.THREE_REAL),
        ))
        repo.saveSettings(original)
        repo.startSession()
        val sessionConfig = repo.activeSession.first()!!.config

        repo.setShowButtons(true)
        assertEquals(original.copy(showButtons = true), repo.settings.first())
        assertEquals(sessionConfig, repo.activeSession.first()!!.config)

        val second = repo.createProfile("Second")
        assertFalse(repo.settings.first().showButtons)
        repo.selectProfile(1)
        assertTrue(repo.settings.first().showButtons)

        db.close()
        db = ReadingDatabase.open(context, databaseName)
        repo = ReadingRepository(db)
        assertTrue(repo.settings.first().showButtons)
        repo.selectProfile(second)
        assertFalse(repo.settings.first().showButtons)
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
        assertEquals(listOf(displaced.word, first.word), db.dao().recentPresentedWords(1, 5))

        assertTrue(repo.undo())
        assertEquals(listOf(displaced.word), db.dao().recentPresentedWords(1, 5))
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
