package com.littlewords.app.ui

import com.littlewords.app.data.HistoryItem
import com.littlewords.app.data.SessionEntity
import com.littlewords.app.domain.Category
import com.littlewords.app.domain.SENTENCE_CATEGORY
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressHistoryTest {
    @Test fun filteredHistoryShowsOnlySessionsWithMatchingAttempts() {
        val sessions = listOf(
            SessionEntity(id = 1, startedAt = 1000, config = "{}"),
            SessionEntity(id = 2, startedAt = 2000, config = "{}"),
            SessionEntity(id = 3, startedAt = 3000, config = "{}"),
        )
        val attempts = listOf(
            HistoryItem(1, 1, "cat", Category.THREE_REAL.name, true, 1500, null, 500),
            HistoryItem(2, 2, "cat can nap.", SENTENCE_CATEGORY, true, 2500, null, 500),
        )
        assertEquals(listOf(2L), historySessions(sessions, attempts.filter { it.category == SENTENCE_CATEGORY }, "Sentences").map { it.id })
        assertEquals(listOf(1L, 2L, 3L), historySessions(sessions, attempts, "All").map { it.id })
    }

    @Test fun sessionDurationUsesElapsedStartToEndTime() {
        assertEquals("8m", formatSessionDuration(0, 480_000))
        assertEquals("1h 2m", formatSessionDuration(0, 3_720_000))
        assertEquals("45s", formatSessionDuration(0, 45_000))
    }
}
