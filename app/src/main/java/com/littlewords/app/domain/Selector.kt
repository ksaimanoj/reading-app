package com.littlewords.app.domain

import kotlin.random.Random

object Selector {
    fun validate(config: PracticeConfig): List<String> = buildList {
        if (config.enabledCategories.isEmpty()) {
            add("Enable at least one word group.")
        } else if (eligible(config).isEmpty()) {
            add("The enabled word groups have no eligible words for the selected letters and patterns.")
        }
    }

    fun eligible(config: PracticeConfig): List<Word> = Catalog.words.filter { word ->
        word.category in config.enabledCategories && matchesLettersAndPatterns(word, config)
    }

    fun eligible(config: PracticeConfig, category: Category): List<Word> = Catalog.words.filter { word ->
        word.category == category && matchesLettersAndPatterns(word, config)
    }

    fun choose(
        config: PracticeConfig,
        recent: List<String>,
        missed: Set<String>,
        random: Random,
    ): Word {
        val pool = eligible(config)
        require(pool.isNotEmpty()) {
            "The enabled word groups have no eligible words for the selected letters and patterns."
        }

        val recentWords = recent.toSet()
        val notRecent = pool.filterNot { it.text in recentWords }
        val spacedPool = if (notRecent.isNotEmpty()) {
            notRecent
        } else {
            pool.filterNot { it.text == recent.firstOrNull() }.ifEmpty { pool }
        }
        val reviewPool = spacedPool.filter { it.text in missed }.ifEmpty { spacedPool }
        return reviewPool[random.nextInt(reviewPool.size)]
    }

    private fun matchesLettersAndPatterns(word: Word, config: PracticeConfig): Boolean =
        word.text.all { it in config.letters } && word.patterns.all { it in config.patterns }
}
