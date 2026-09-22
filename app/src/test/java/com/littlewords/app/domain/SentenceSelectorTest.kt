package com.littlewords.app.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceSelectorTest {
    @Test fun catalogueContainsOnlyShortReviewedSentences() {
        assertTrue(Catalog.sentences.size >= 30)
        assertEquals(Catalog.sentences.size, Catalog.sentences.map { it.text }.distinct().size)
        assertTrue(Catalog.sentences.all { it.text.matches(Regex("[a-z]+(?: [a-z]+){1,5}\\.")) })

        val realWords = Catalog.words.filter { it.category != Category.THREE_SILLY }.associateBy { it.text }
        assertTrue(Catalog.sentences.all { sentence ->
            val tokens = sentence.text.removeSuffix(".").split(" ")
            tokens.all(realWords::containsKey) &&
                sentence.patterns == tokens.flatMap { realWords.getValue(it).patterns }.toSet()
        })
    }

    @Test fun eligibilityUsesTheSameLettersAndPatternsAsWordPractice() {
        val simple = PracticeConfig(letters = "acdst", patterns = setOf("short_a"))
        val eligible = SentenceSelector.eligible(simple)

        assertTrue(eligible.any { it.text == "cat sat." })
        assertTrue(eligible.all { it.patterns == setOf("short_a") })
        assertTrue(eligible.all { sentence -> sentence.text.filter(Char::isLetter).all { it in simple.letters } })
        assertFalse(eligible.any { it.text == "fish can swim." })
    }

    @Test fun trickySentencesStayHiddenUntilTheirPatternsAreEnabled() {
        val simple = PracticeConfig(patterns = PracticeConfig().patterns)
        val withTrickyWords = simple.copy(patterns = simple.patterns + setOf("tricky", "th"))

        assertFalse(SentenceSelector.eligible(simple).any { it.text == "the cat sat." })
        assertTrue(SentenceSelector.eligible(withTrickyWords).any { it.text == "the cat sat." })
    }

    @Test fun chooseAvoidsRecentSentencesAndPrioritizesMisses() {
        val config = PracticeConfig(letters = "acdhnrst", patterns = setOf("short_a"))
        val missed = "dad sat."

        val chosen = SentenceSelector.choose(config, listOf("cat sat."), setOf(missed), Random(4))

        assertEquals(missed, chosen.text)
    }

    @Test fun everyEligibleSentenceAppearsBeforeAnyRepeat() {
        val config = PracticeConfig(patterns = PracticeConfig().patterns)
        val eligible = SentenceSelector.eligible(config).map { it.text }.toSet()
        val presented = mutableListOf<String>()

        repeat(eligible.size) { step ->
            val chosen = SentenceSelector.choose(config, presented, emptySet(), Random(step)).text
            assertFalse("Repeated before exhausting the eligible catalogue: $chosen", chosen in presented)
            presented.add(0, chosen)
        }

        assertEquals(eligible, presented.toSet())
    }

    @Test fun nextCyclePrefersLessPresentedSentences() {
        val config = PracticeConfig(patterns = PracticeConfig().patterns)
        val eligible = SentenceSelector.eligible(config).map { it.text }
        val twiceShown = eligible.first()
        val presented = listOf(twiceShown) + eligible

        repeat(20) { seed ->
            val chosen = SentenceSelector.choose(config, presented, emptySet(), Random(seed))
            assertFalse(chosen.text == twiceShown)
        }
    }
}
