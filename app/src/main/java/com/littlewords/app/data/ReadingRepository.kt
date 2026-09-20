package com.littlewords.app.data

import androidx.room.withTransaction
import com.littlewords.app.domain.PracticeConfig
import com.littlewords.app.domain.Selector
import kotlinx.coroutines.flow.map
import kotlin.random.Random

class ReadingRepository(private val db: ReadingDatabase) {
    private val dao = db.dao()
    val settings = dao.observeSettings().map { it?.toSettings() ?: AppSettings() }
    val activeSession = dao.observeActiveSession()
    val currentCard = dao.observeCurrentCard()
    val history = dao.observeHistory()
    val sessions = dao.observeSessions()

    suspend fun saveSettings(value: AppSettings) {
        val errors = Selector.validate(value.config)
        require(errors.isEmpty()) { errors.joinToString("\n") }
        dao.putSettings(SettingsEntity.from(value))
    }

    suspend fun setTheme(theme: ThemeMode) = db.withTransaction {
        val current = dao.settings()?.toSettings() ?: AppSettings()
        dao.putSettings(SettingsEntity.from(current.copy(theme = theme)))
    }

    suspend fun startSession() = db.withTransaction {
        if (dao.activeSession() != null) return@withTransaction
        val settings = dao.settings()?.toSettings() ?: AppSettings()
        val errors = Selector.validate(settings.config)
        require(errors.isEmpty()) { errors.joinToString("\n") }
        val session = SessionEntity(startedAt = System.currentTimeMillis(), config = ConfigCodec.encode(settings.config))
        val id = dao.insertSession(session)
        val next = nextCard(session.copy(id = id), settings.config)
        dao.updateSession(session.copy(id = id, currentCardId = next, remainingBag = ""))
    }

    suspend fun score(cardId: Long, success: Boolean): Boolean = db.withTransaction {
        val session = dao.activeSession() ?: return@withTransaction false
        if (session.currentCardId != cardId) return@withTransaction false
        val attemptId = dao.insertAttempt(AttemptEntity(
            cardId = cardId, sessionId = session.id,
            direction = if (success) "right" else "left", success = success,
            respondedAt = System.currentTimeMillis(),
        ))
        val next = nextCard(session, ConfigCodec.decode(session.config))
        dao.updateSession(session.copy(currentCardId = next, remainingBag = "", lastAttemptId = attemptId))
        true
    }

    suspend fun undo(): Boolean = db.withTransaction {
        val session = dao.activeSession() ?: return@withTransaction false
        val attempt = session.lastAttemptId?.let { dao.attempt(it) } ?: return@withTransaction false
        if (attempt.voidedAt != null) return@withTransaction false
        dao.updateAttempt(attempt.copy(voidedAt = System.currentTimeMillis()))
        dao.updateSession(session.copy(currentCardId = attempt.cardId, remainingBag = "", lastAttemptId = null))
        true
    }

    suspend fun endSession() = db.withTransaction {
        dao.activeSession()?.let { dao.updateSession(it.copy(endedAt = System.currentTimeMillis(), currentCardId = null, lastAttemptId = null)) }
    }

    private suspend fun nextCard(session: SessionEntity, config: PracticeConfig): Long {
        val history = dao.validHistory()
        val recent = dao.recentPresentedWords(5)
        val missed = history.distinctBy { it.word }.filterNot { it.success }.map { it.word }.toSet()
        val word = Selector.choose(config, recent, missed, Random.Default)
        return dao.insertCard(CardEntity(
            sessionId = session.id, word = word.text, category = word.category.name,
            patterns = word.patterns.sorted().joinToString(","), shownAt = System.currentTimeMillis(),
        ))
    }
}
