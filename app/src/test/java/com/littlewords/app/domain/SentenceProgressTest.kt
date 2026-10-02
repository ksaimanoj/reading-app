package com.littlewords.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceProgressTest {
    @Test fun sentenceProgressUsesDistinctSuccessfulSessionsAndLatestOutcome() {
        val sentences = Catalog.sentences.filter { it.text in setOf("cat can nap.", "a tan dog ran.") }
        val attempts = listOf(
            ProgressAttempt("cat can nap.", SENTENCE_CATEGORY, 1, true, 1),
            ProgressAttempt("cat can nap.", SENTENCE_CATEGORY, 1, true, 2),
            ProgressAttempt("cat can nap.", SENTENCE_CATEGORY, 2, true, 3),
            ProgressAttempt("cat can nap.", SENTENCE_CATEGORY, 3, false, 4),
            ProgressAttempt("a tan dog ran.", SENTENCE_CATEGORY, 3, false, 5),
        )
        val progress = SentenceProgressCalculator.calculate(sentences, attempts)
        assertEquals(WordStatus.CONFIDENT, progress.getValue("cat can nap.").status)
        assertEquals(2, progress.getValue("cat can nap.").independentSessions)
        assertTrue(progress.getValue("cat can nap.").needsReview)
        assertEquals(WordStatus.PRACTISING, progress.getValue("a tan dog ran.").status)

        val afterUndo = SentenceProgressCalculator.calculate(sentences, attempts.filter { it.id != 4L })
        assertFalse(afterUndo.getValue("cat can nap.").needsReview)
    }

    @Test fun wordAttemptsDoNotChangeSentenceProgress() {
        val sentences = Catalog.sentences.filter { it.text == "cat can nap." }
        val attempts = listOf(ProgressAttempt("cat can nap.", Category.THREE_REAL.name, 1, true, 1))
        assertEquals(WordStatus.NOT_TRIED, SentenceProgressCalculator.calculate(sentences, attempts).getValue("cat can nap.").status)
    }
}
