package com.littlewords.app.data

import androidx.room.withTransaction
import com.littlewords.app.domain.PracticeConfig
import com.littlewords.app.domain.PracticeMode
import com.littlewords.app.domain.SENTENCE_CATEGORY
import com.littlewords.app.domain.SentenceSelector
import com.littlewords.app.domain.Selector
import com.littlewords.app.domain.CATALOGUE_VERSION
import com.littlewords.app.domain.Catalog
import com.littlewords.app.domain.ProgressAttempt
import com.littlewords.app.domain.StageCatalog
import com.littlewords.app.domain.WordProgressCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlin.random.Random

data class ProfileReading(
    val profileId: Long,
    val settings: AppSettings,
    val activeSession: SessionEntity?,
    val currentCard: CardEntity?,
    val history: List<HistoryItem>,
    val sessions: List<SessionEntity>,
    val achievements: List<StageAchievementEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingRepository(private val db: ReadingDatabase) {
    private val dao = db.dao()
    val profiles = dao.observeProfiles()
    val reading = dao.observeProfileId().flatMapLatest { profileId ->
        combine(
            dao.observeSettings(profileId), dao.observeActiveSession(profileId),
            dao.observeCurrentCard(profileId), dao.observeHistory(profileId), dao.observeSessions(profileId),
        ) { settings, session, card, history, sessions ->
            ProfileReading(profileId, settings?.toSettings() ?: AppSettings(), session, card, history, sessions)
        }.combine(dao.observeAchievements(profileId)) { reading, achievements -> reading.copy(achievements = achievements) }
    }
    val settings = reading.map { it.settings }
    val activeSession = reading.map { it.activeSession }
    val currentCard = reading.map { it.currentCard }
    val history = reading.map { it.history }
    val sessions = reading.map { it.sessions }

    suspend fun createProfile(name: String): Long = db.withTransaction {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Enter a profile name." }
        require(clean.length <= 40) { "Profile names must be 40 characters or shorter." }
        require(dao.profiles().none { it.name.equals(clean, ignoreCase = true) }) { "A profile with that name already exists." }
        val id = dao.insertProfile(ProfileEntity(name = clean))
        dao.selectProfile(ActiveProfileEntity(profileId = id))
        id
    }

    suspend fun selectProfile(id: Long) = db.withTransaction {
        require(dao.hasProfile(id)) { "That profile is no longer available." }
        dao.selectProfile(ActiveProfileEntity(profileId = id))
    }

    suspend fun syncAchievementsForAllProfiles() = db.withTransaction {
        dao.profiles().forEach { syncAchievements(it.id) }
    }

    private suspend fun syncAchievements(profileId: Long) {
        val attempts = dao.validHistory(profileId).map {
            ProgressAttempt(it.word, it.category, it.sessionId, it.success, it.attemptId)
        }
        val milestones = WordProgressCalculator.calculate(Catalog.words, attempts)
        val saved = dao.achievements(profileId)
        StageCatalog.stages.forEach { stage ->
            val complete = WordProgressCalculator.summarize(stage.id, milestones).collectionComplete
            val existing = saved.any { it.stageId == stage.id && it.catalogueVersion == CATALOGUE_VERSION }
            if (complete && !existing) dao.putAchievement(StageAchievementEntity(profileId, stage.id, CATALOGUE_VERSION, System.currentTimeMillis()))
            if (!complete && existing) dao.removeAchievement(profileId, stage.id, CATALOGUE_VERSION)
        }
    }

    suspend fun saveSettings(value: AppSettings) {
        val errors = Selector.validate(value.config)
        require(errors.isEmpty()) { errors.joinToString("\n") }
        db.withTransaction { dao.putSettings(SettingsEntity.from(value, dao.profileId())) }
    }

    suspend fun setTheme(theme: ThemeMode) = db.withTransaction {
        val profileId = dao.profileId()
        val current = dao.settings(profileId)?.toSettings() ?: AppSettings()
        dao.putSettings(SettingsEntity.from(current.copy(theme = theme), profileId))
    }

    suspend fun startSession(mode: PracticeMode = PracticeMode.WORDS) = db.withTransaction {
        val profileId = dao.profileId()
        if (dao.activeSession(profileId) != null) return@withTransaction
        val settings = dao.settings(profileId)?.toSettings() ?: AppSettings()
        val sessionConfig = settings.config.copy(mode = mode)
        val errors = when (mode) {
            PracticeMode.WORDS -> Selector.validate(sessionConfig)
            PracticeMode.SENTENCES -> SentenceSelector.validate(sessionConfig)
        }
        require(errors.isEmpty()) { errors.joinToString("\n") }
        val session = SessionEntity(
            profileId = profileId,
            startedAt = System.currentTimeMillis(),
            config = ConfigCodec.encode(sessionConfig),
            contentVersion = if (sessionConfig.selectedSubskills == null) 2 else CATALOGUE_VERSION + 2,
        )
        val id = dao.insertSession(session)
        val next = nextCard(session.copy(id = id), sessionConfig)
        dao.updateSession(session.copy(id = id, currentCardId = next, remainingBag = ""))
    }

    suspend fun score(cardId: Long, success: Boolean, durationMs: Long? = null): Boolean = db.withTransaction {
        val session = dao.activeSession(dao.profileId()) ?: return@withTransaction false
        if (session.currentCardId != cardId) return@withTransaction false
        require(durationMs == null || durationMs >= 0) { "Reading time cannot be negative." }
        val attemptId = dao.insertAttempt(AttemptEntity(
            cardId = cardId, sessionId = session.id,
            direction = if (success) "right" else "left", success = success,
            respondedAt = System.currentTimeMillis(),
            durationMs = durationMs,
        ))
        val next = nextCard(session, ConfigCodec.decode(session.config))
        dao.updateSession(session.copy(currentCardId = next, remainingBag = "", lastAttemptId = attemptId))
        syncAchievements(session.profileId)
        true
    }

    suspend fun undo(): Boolean = db.withTransaction {
        val session = dao.activeSession(dao.profileId()) ?: return@withTransaction false
        val attempt = session.lastAttemptId?.let { dao.attempt(it) } ?: return@withTransaction false
        if (attempt.voidedAt != null) return@withTransaction false
        dao.updateAttempt(attempt.copy(voidedAt = System.currentTimeMillis()))
        dao.updateSession(session.copy(currentCardId = attempt.cardId, remainingBag = "", lastAttemptId = null))
        syncAchievements(session.profileId)
        true
    }

    suspend fun endSession() = db.withTransaction {
        dao.activeSession(dao.profileId())?.let { dao.updateSession(it.copy(endedAt = System.currentTimeMillis(), currentCardId = null, lastAttemptId = null)) }
    }

    private suspend fun nextCard(session: SessionEntity, config: PracticeConfig): Long {
        val history = dao.validHistory(session.profileId)
        val recent = dao.recentPresentedWords(session.profileId, 5)
        val missed = history.distinctBy { it.word }.filterNot { it.success }.map { it.word }.toSet()
        val item = when (config.mode) {
            PracticeMode.WORDS -> Selector.choose(config, recent, missed, Random.Default,
                if (config.selectedSubskills == null) emptyList() else dao.presentedWords(session.profileId)).let {
                Triple(it.text, it.category.name, it.patterns)
            }
            PracticeMode.SENTENCES -> SentenceSelector.choose(config, dao.presentedSentences(session.profileId), missed, Random.Default).let {
                Triple(it.text, SENTENCE_CATEGORY, it.patterns)
            }
        }
        return dao.insertCard(CardEntity(
            sessionId = session.id, word = item.first, category = item.second,
            patterns = item.third.sorted().joinToString(","), shownAt = System.currentTimeMillis(),
            contentVersion = session.contentVersion,
        ))
    }
}
