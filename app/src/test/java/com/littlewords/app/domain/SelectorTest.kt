package com.littlewords.app.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SelectorTest {
    @Test fun combinedPoolContainsEveryEligibleEnabledCategory() {
        val config = PracticeConfig(
            enabledCategories = setOf(Category.TWO_REAL, Category.THREE_REAL),
            letters = "abcmst",
            patterns = setOf("short_a"),
        )

        val combined = Selector.eligible(config)

        assertEquals(
            Selector.eligible(config, Category.TWO_REAL).size +
                Selector.eligible(config, Category.THREE_REAL).size,
            combined.size,
        )
        assertTrue(combined.any { it.category == Category.TWO_REAL })
        assertTrue(combined.any { it.category == Category.THREE_REAL })
        assertTrue(combined.none { it.category == Category.THREE_SILLY })
    }

    @Test fun eligibleExcludesWordsContainingDisabledLetters() {
        val config = PracticeConfig(letters = "abcm")

        val words = Selector.eligible(config, Category.THREE_REAL)

        assertTrue(words.any { it.text == "cab" })
        assertTrue(words.all { word -> word.text.all { it in config.letters } })
        assertFalse(words.any { it.text == "cat" })
    }

    @Test fun eligibleRequiresEveryPatternUsedByAWord() {
        val simple = PracticeConfig(patterns = setOf("short_i"))
        val withDigraph = simple.copy(patterns = setOf("short_i", "sh"))

        assertFalse(Selector.eligible(simple, Category.FOUR_REAL).any { it.text == "ship" })
        assertTrue(Selector.eligible(withDigraph, Category.FOUR_REAL).any { it.text == "ship" })
    }

    @Test fun validationRejectsNoEnabledCategories() {
        val errors = Selector.validate(PracticeConfig(enabledCategories = emptySet()))

        assertTrue(errors.any { "word group" in it.lowercase() })
    }

    @Test fun validationAllowsAnEnabledEmptyCategoryWhenCombinedPoolHasWords() {
        val config = PracticeConfig(
            enabledCategories = setOf(Category.TWO_REAL, Category.FIVE_REAL),
            letters = "at",
            patterns = setOf("short_a"),
        )

        assertTrue(Selector.eligible(config, Category.FIVE_REAL).isEmpty())
        assertTrue(Selector.validate(config).isEmpty())
    }

    @Test fun validationRejectsAnEmptyCombinedPool() {
        val config = PracticeConfig(
            enabledCategories = setOf(Category.FIVE_REAL),
            letters = "q",
            patterns = setOf("short_a"),
        )

        assertTrue(Selector.validate(config).any { "no eligible words" in it.lowercase() })
    }

    @Test fun chooseUsesTheCombinedEnabledPool() {
        val config = PracticeConfig(
            enabledCategories = setOf(Category.TWO_REAL, Category.THREE_REAL),
            letters = "abcmst",
            patterns = setOf("short_a"),
        )

        val chosenCategories = (0 until 100).map { seed ->
            Selector.choose(config, emptyList(), emptySet(), Random(seed)).category
        }.toSet()

        assertEquals(setOf(Category.TWO_REAL, Category.THREE_REAL), chosenCategories)
    }

    @Test fun chooseAvoidsRecentWordsWhenAnotherWordIsAvailable() {
        val config = PracticeConfig(
            enabledCategories = setOf(Category.THREE_REAL),
            letters = "abcmst",
            patterns = setOf("short_a"),
        )

        repeat(20) { seed ->
            val chosen = Selector.choose(config, listOf("cab", "cat", "mat"), emptySet(), Random(seed))
            assertFalse(chosen.text in setOf("cab", "cat", "mat"))
        }
    }

    @Test fun choosePrioritizesAMissedWordWithinTheAvailablePool() {
        val config = PracticeConfig(
            enabledCategories = setOf(Category.THREE_REAL),
            letters = "abcmst",
            patterns = setOf("short_a"),
        )

        val chosen = Selector.choose(config, emptyList(), setOf("bat"), Random(5))

        assertEquals("bat", chosen.text)
    }

    @Test fun chooseTerminatesWhenOnlyOneWordIsEligibleEvenIfItIsRecent() {
        val config = PracticeConfig(
            enabledCategories = setOf(Category.TWO_REAL),
            letters = "at",
            patterns = setOf("short_a"),
        )

        val chosen = Selector.choose(config, listOf("at"), emptySet(), Random(6))

        assertEquals("at", chosen.text)
    }

    @Test fun chooseDoesNotImmediatelyRepeatNewestWhenTheWholePoolIsRecent() {
        val config = PracticeConfig(
            enabledCategories = setOf(Category.TWO_REAL),
            letters = "amt",
            patterns = setOf("short_a"),
        )

        repeat(20) { seed ->
            val chosen = Selector.choose(config, listOf("am", "at"), emptySet(), Random(seed))
            assertEquals("at", chosen.text)
        }
    }

    @Test fun chooseExplainsWhenTheEligiblePoolIsEmpty() {
        try {
            Selector.choose(
                PracticeConfig(
                    enabledCategories = setOf(Category.FIVE_REAL),
                    letters = "q",
                    patterns = setOf("short_a"),
                ),
                emptyList(),
                emptySet(),
                Random(7),
            )
            fail("Expected an empty-pool exception")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message.orEmpty().contains("no eligible words", ignoreCase = true))
        }
    }

    @Test fun catalogueHasReviewedShapesAndRequiredPatternTags() {
        val requiredPatterns = setOf(
            "short_a", "short_e", "short_i", "short_o", "short_u",
            "blends", "sh", "ch", "th", "ck", "double", "tricky", "two_syllables",
        )

        assertTrue(Catalog.words.size >= 707)
        assertEquals(Catalog.words.size, Catalog.words.map { it.text }.distinct().size)
        assertTrue(Catalog.words.all { it.text.matches(Regex("[a-z]+")) })
        assertTrue(Catalog.words.all { word ->
            "tricky" in word.patterns || word.patterns.any { it in setOf(
                "short_a", "short_e", "short_i", "short_o", "short_u",
            ) }
        })
        assertTrue(Catalog.words.all { word ->
            when (word.category) {
                Category.TWO_REAL -> word.text.length == 2
                Category.THREE_REAL, Category.THREE_SILLY -> word.text.length == 3
                Category.FOUR_REAL -> word.text.length == 4
                Category.FIVE_REAL -> word.text.length == 5
            }
        })
        assertTrue(requiredPatterns.all(Catalog.patternLabels::containsKey))
        assertTrue(requiredPatterns.all { pattern -> Catalog.words.any { pattern in it.patterns } })
        assertTrue(Catalog.words.any { it.category == Category.THREE_SILLY && "blends" in it.patterns })
        assertTrue(Catalog.words.any { it.category == Category.THREE_SILLY && "blends" !in it.patterns })
    }

    @Test fun multisyllabicLongerWordsRequireTheirExplicitPattern() {
        val shortVowels = PracticeConfig().patterns
        val simpleConfig = PracticeConfig(
            enabledCategories = setOf(Category.FOUR_REAL, Category.FIVE_REAL),
            patterns = shortVowels,
        )
        val multisyllabicConfig = simpleConfig.copy(patterns = shortVowels + "two_syllables")

        assertTrue(Selector.eligible(simpleConfig, Category.FOUR_REAL).isEmpty())
        assertTrue(Selector.eligible(simpleConfig, Category.FIVE_REAL).isEmpty())
        assertTrue(Selector.eligible(multisyllabicConfig, Category.FIVE_REAL).any { it.text == "cabin" })
        assertFalse(Selector.eligible(multisyllabicConfig, Category.FIVE_REAL).any { it.text == "visit" })
        assertTrue(Selector.eligible(
            multisyllabicConfig.copy(patterns = multisyllabicConfig.patterns + "tricky"),
            Category.FIVE_REAL,
        ).any { it.text == "visit" })
    }
}
