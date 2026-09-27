package com.littlewords.app.domain

import kotlin.random.Random

object Selector {
    fun validate(config: PracticeConfig): List<String> = buildList {
        val known = StageCatalog.stages.flatMap { stage ->
            stage.subskills.map { StageCatalog.key(stage.id, it.id) }
        }.toSet() + StageCatalog.SILLY_KEY
        if (config.selectedSubskills?.any { it !in known } == true) {
            add("An unavailable practice group was selected. Choose from the current catalogue.")
        }
        if (config.selectedSubskills != null && config.selectedSubskills.isEmpty()) {
            add("Include at least one practice group.")
        } else if (config.selectedSubskills == null && config.enabledCategories.isEmpty()) {
            add("Enable at least one word group.")
        } else if (eligible(config).isEmpty()) {
            add("The selected practice groups have no eligible words for the selected letters and patterns.")
        }
    }

    fun eligible(config: PracticeConfig): List<Word> = Catalog.words.filter { word ->
        (config.selectedSubskills?.let { StageCatalog.key(word.stageId, word.subskillId) in it }
            ?: (word.category in config.enabledCategories)) && matchesLettersAndPatterns(word, config)
    }

    fun eligible(config: PracticeConfig, category: Category): List<Word> = Catalog.words.filter { word ->
        word.category == category && matchesLettersAndPatterns(word, config)
    }

    fun choose(
        config: PracticeConfig,
        recent: List<String>,
        missed: Set<String>,
        random: Random,
        presented: List<String> = emptyList(),
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
        val coveragePool = if (config.selectedSubskills == null) spacedPool else {
            val counts = presented.groupingBy { it }.eachCount()
            val fewest = spacedPool.minOf { counts[it.text] ?: 0 }
            spacedPool.filter { (counts[it.text] ?: 0) == fewest }
        }
        val reviewPool = coveragePool.filter { it.text in missed }.ifEmpty { coveragePool }
        return reviewPool[random.nextInt(reviewPool.size)]
    }

    private fun matchesLettersAndPatterns(word: Word, config: PracticeConfig): Boolean =
        word.text.all { it in config.letters } && word.patterns.all { it in config.patterns }
}
