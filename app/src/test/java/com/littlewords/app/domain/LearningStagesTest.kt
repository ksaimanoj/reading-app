package com.littlewords.app.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningStagesTest {
    @Test fun catalogueHasOnePrimaryMembershipPerRealWord() {
        val real = Catalog.words.filter { it.category != Category.THREE_SILLY }
        assertEquals(475, real.size)
        assertEquals(475, real.map { it.text }.toSet().size)
        assertTrue(real.all { StageCatalog.contains(it.stageId, it.subskillId) })
        assertEquals("cvc", real.first { it.text == "cat" }.stageId)
        assertEquals("sh", real.first { it.text == "ship" }.subskillId)
        assertEquals("combined", real.first { it.text == "brush" }.subskillId)
        val emptyProgress = WordProgressCalculator.calculate(Catalog.words, emptyList())
        StageCatalog.stages.forEach { stage ->
            val summary = WordProgressCalculator.summarize(stage.id, emptyProgress)
            assertEquals(summary.total, summary.confident + summary.practising + summary.notTried)
        }
    }

    @Test fun confidenceNeedsTwoDistinctSuccessfulSessionsAndReviewUsesLatestValidAttempt() {
        val words = Catalog.words.filter { it.text == "cat" }
        val attempts = listOf(
            ProgressAttempt("cat", "THREE_REAL", 1, true, 1),
            ProgressAttempt("cat", "THREE_REAL", 1, true, 2),
            ProgressAttempt("cat", "THREE_REAL", 2, true, 3),
            ProgressAttempt("cat", "THREE_REAL", 2, false, 4),
        )
        val beforeSecondSession = WordProgressCalculator.calculate(words, attempts.take(2))
        assertEquals(WordStatus.PRACTISING, beforeSecondSession.getValue("cat").status)
        val complete = WordProgressCalculator.calculate(words, attempts)
        assertEquals(WordStatus.CONFIDENT, complete.getValue("cat").status)
        assertTrue(complete.getValue("cat").needsReview)
        val undone = WordProgressCalculator.calculate(words, attempts.filter { it.id != 3L })
        assertEquals(WordStatus.PRACTISING, undone.getValue("cat").status)
        assertFalse(undone.getValue("cat").needsReview)
    }

    @Test fun successfulSentenceCreditsItsWordsAcrossDistinctSessions() {
        val words = Catalog.words.filter { it.text in setOf("cat", "can", "nap", "dad") }
        val first = ProgressAttempt("cat can nap.", SENTENCE_CATEGORY, 1, true, 1)
        val sameSession = ProgressAttempt("cat can nap.", SENTENCE_CATEGORY, 1, true, 2)
        val second = ProgressAttempt("cat can nap.", SENTENCE_CATEGORY, 2, true, 3)

        val afterFirst = WordProgressCalculator.calculate(words, listOf(first, sameSession))
        assertEquals(WordStatus.PRACTISING, afterFirst.getValue("cat").status)
        assertEquals(1, afterFirst.getValue("can").independentSessions)
        assertEquals(WordStatus.NOT_TRIED, afterFirst.getValue("dad").status)

        val afterSecond = WordProgressCalculator.calculate(words, listOf(first, sameSession, second))
        setOf("cat", "can", "nap").forEach { word ->
            assertEquals(WordStatus.CONFIDENT, afterSecond.getValue(word).status)
            assertEquals(2, afterSecond.getValue(word).independentSessions)
        }
    }

    @Test fun wordAndSentenceSuccessesCanTogetherBuildConfidence() {
        val words = Catalog.words.filter { it.text == "cat" }
        val attempts = listOf(
            ProgressAttempt("cat", Category.THREE_REAL.name, 1, true, 1),
            ProgressAttempt("cat can nap.", SENTENCE_CATEGORY, 2, true, 2),
        )
        assertEquals(WordStatus.CONFIDENT, WordProgressCalculator.calculate(words, attempts).getValue("cat").status)
    }

    @Test fun helperWordsNeverGainMilestonesButHistoricalSentenceWordsDo() {
        val words = Catalog.words
        val retiredSentence = "I am a lad."
        assertFalse(Catalog.sentences.any { it.text == retiredSentence })
        val attempts = listOf(
            ProgressAttempt("I am glad.", SENTENCE_CATEGORY, 1, true, 1),
            ProgressAttempt("I am glad.", SENTENCE_CATEGORY, 2, true, 2),
            ProgressAttempt(retiredSentence, SENTENCE_CATEGORY, 1, true, 3),
            ProgressAttempt(retiredSentence, SENTENCE_CATEGORY, 2, true, 4),
        )
        val milestones = WordProgressCalculator.calculate(words, attempts)
        setOf("am", "glad", "lad").forEach { word ->
            assertEquals(WordStatus.CONFIDENT, milestones.getValue(word).status)
        }
        assertFalse("i" in milestones)
        assertFalse("a" in milestones)
    }

    @Test fun missedSentenceDoesNotMarkEveryWordAsNeedingPractice() {
        val words = Catalog.words.filter { it.text in setOf("cat", "can", "nap") }
        val attempts = listOf(
            ProgressAttempt("cat can nap.", SENTENCE_CATEGORY, 1, true, 1),
            ProgressAttempt("cat can nap.", SENTENCE_CATEGORY, 2, true, 2),
            ProgressAttempt("cat can nap.", SENTENCE_CATEGORY, 3, false, 3),
        )
        val milestones = WordProgressCalculator.calculate(words, attempts)
        words.forEach { assertFalse(milestones.getValue(it.text).needsReview) }
        val undone = WordProgressCalculator.calculate(words, attempts.drop(1))
        words.forEach { assertEquals(WordStatus.PRACTISING, undone.getValue(it.text).status) }
    }

    @Test fun stageChoicesIgnoreOldLengthGroupsAndSeparateCombinedSkills() {
        val config = PracticeConfig(
            enabledCategories = setOf(Category.THREE_REAL),
            patterns = Catalog.patternLabels.keys,
            selectedSubskills = setOf("digraphs:sh"),
        )
        val eligible = Selector.eligible(config).map { it.text }.toSet()
        assertTrue("ship" in eligible)
        assertFalse("chat" in eligible)
        assertFalse("brush" in eligible)
    }

    @Test fun stageSelectionGivesEveryEligibleWordAnOpportunityBeforeRepeating() {
        val config = PracticeConfig(patterns = Catalog.patternLabels.keys, selectedSubskills = setOf("additional:two_letter"))
        val pool = Selector.eligible(config)
        val presented = mutableListOf<String>()
        repeat(pool.size) {
            val chosen = Selector.choose(config, presented.takeLast(5).reversed(), setOf("at"), Random(7), presented)
            presented += chosen.text
        }
        assertEquals(pool.size, presented.toSet().size)
    }

    @Test fun stageDenominatorDoesNotShrinkWithAdvancedRestrictions() {
        val full = PracticeConfig(patterns = Catalog.patternLabels.keys, selectedSubskills = setOf("digraphs:sh"))
        val restricted = full.copy(letters = "ship")
        assertTrue(Selector.eligible(restricted).size < Selector.eligible(full).size)
        assertEquals(16, StageCatalog.words("digraphs", "sh").size)
    }

    @Test fun sentencePracticeMayUseSupportingCvcWordsWithoutEnablingTheCvcStage() {
        val config = PracticeConfig(patterns = Catalog.patternLabels.keys,
            selectedSubskills = setOf("digraphs:sh", "additional:tricky"))
        assertTrue(SentenceSelector.eligible(config).any { it.text == "hush a pup." })
        assertFalse(SentenceSelector.eligible(config).any { "ch" in it.patterns })
        assertTrue(SentenceSelector.eligible(config.copy(selectedSubskills = setOf(StageCatalog.SILLY_KEY))).isEmpty())
    }

    @Test fun unknownStageChoiceCannotBeSavedBesideAValidOne() {
        val config = PracticeConfig(patterns = Catalog.patternLabels.keys,
            selectedSubskills = setOf("digraphs:sh", "future:silent_e"))
        assertTrue(Selector.validate(config).any { "unavailable" in it.lowercase() })
    }

    @Test fun stageCoverageRespectsRecentSpacingWhenAnotherWordIsAvailable() {
        val config = PracticeConfig(patterns = Catalog.patternLabels.keys, selectedSubskills = setOf("additional:two_letter"))
        val other = Selector.eligible(config).map { it.text }.filterNot { it == "am" }
        val presented = other + other + "am"
        val chosen = Selector.choose(config, listOf("am"), emptySet(), Random(5), presented)
        assertFalse(chosen.text == "am")
    }
}
