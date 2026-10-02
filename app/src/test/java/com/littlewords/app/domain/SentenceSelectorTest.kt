package com.littlewords.app.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceSelectorTest {
    @Test fun catalogueCoversEveryRealWordWithAllowedHelpers() {
        assertEquals(344, Catalog.sentences.size)
        assertEquals(Catalog.sentences.size, Catalog.sentences.map { it.text }.distinct().size)
        assertTrue(Catalog.sentences.all { it.text.matches(Regex("(?:I|[a-z]+)(?:,? (?:I|[a-z]+)){1,7}[.?]")) })

        val realWords = Catalog.words.filter { it.category != Category.THREE_SILLY }.associateBy { it.text }
        val helpers = mapOf("I" to setOf("tricky"), "a" to setOf("tricky"),
            "my" to setOf("tricky"), "to" to setOf("tricky"))
        val covered = mutableSetOf<String>()
        assertTrue(Catalog.sentences.all { sentence ->
            val tokens = sentence.text.dropLast(1).replace(",", "").split(" ")
            covered += tokens.filter(realWords::containsKey)
            tokens.all { it in realWords || it in helpers } &&
                sentence.patterns == tokens.flatMap { realWords[it]?.patterns ?: helpers.getValue(it) }.toSet()
        })
        assertEquals(realWords.keys, covered)
    }

    @Test fun eligibilityUsesTheSameLettersAndPatternsAsWordPractice() {
        val simple = PracticeConfig(letters = "acnpt", patterns = setOf("short_a"))
        val eligible = SentenceSelector.eligible(simple)

        assertTrue(eligible.any { it.text == "cat can nap." })
        assertTrue(eligible.all { it.patterns == setOf("short_a") })
        assertTrue(eligible.all { sentence -> sentence.text.filter(Char::isLetter).all { it in simple.letters } })
        assertFalse(eligible.any { it.text == "fish can swim." })
    }

    @Test fun uppercaseIUsesTheSelectedLowercaseLetterAndTrickyPattern() {
        val base = PracticeConfig(letters = "adgilm", patterns = setOf("short_a", "blends"))
        assertFalse(SentenceSelector.eligible(base).any { it.text == "I am glad." })
        assertFalse(SentenceSelector.eligible(base.copy(patterns = base.patterns + "tricky", letters = "adglm"))
            .any { it.text == "I am glad." })
        assertTrue(SentenceSelector.eligible(base.copy(patterns = base.patterns + "tricky"))
            .any { it.text == "I am glad." })
    }

    @Test fun questionAndCommaSentencesRemainEligible() {
        val allPatterns = PracticeConfig().copy(patterns = Catalog.patternLabels.keys)
        val eligible = SentenceSelector.eligible(allPatterns).map { it.text }
        assertTrue("what is in the box?" in eligible)
        assertTrue("if you can, run." in eligible)
    }

    @Test fun chooseAvoidsRecentSentencesAndPrioritizesMisses() {
        val config = PracticeConfig(patterns = Catalog.patternLabels.keys)
        val missed = "I am glad."

        val chosen = SentenceSelector.choose(config, listOf("cat can nap."), setOf(missed), Random(4))

        assertEquals(missed, chosen.text)
    }

    @Test fun emptySelectionKeepsItsValidationMessage() {
        val config = PracticeConfig(letters = "z", patterns = emptySet())
        assertTrue(SentenceSelector.eligible(config).isEmpty())
        assertEquals(listOf("There are no short sentences for the selected letters and sound patterns."),
            SentenceSelector.validate(config))
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
