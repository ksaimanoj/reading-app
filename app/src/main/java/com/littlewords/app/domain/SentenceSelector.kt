package com.littlewords.app.domain

import kotlin.random.Random

object SentenceSelector {
    fun validate(config: PracticeConfig): List<String> = buildList {
        if (eligible(config).isEmpty()) {
            add("There are no short sentences for the selected letters and sound patterns.")
        }
    }

    fun eligible(config: PracticeConfig): List<Sentence> {
        val supported = config.selectedSubskills?.let(StageCatalog::supportedPatterns)
        return Catalog.sentences.filter { sentence ->
        sentence.patterns.all { it in config.patterns } &&
            (supported == null || sentence.patterns.all { it in supported }) &&
            sentence.text.asSequence().filter { it.isLetter() }.all { it.lowercaseChar() in config.letters }
        }
    }

    fun choose(
        config: PracticeConfig,
        presented: List<String>,
        missed: Set<String>,
        random: Random,
    ): Sentence {
        val pool = eligible(config)
        require(pool.isNotEmpty()) {
            "There are no short sentences for the selected letters and sound patterns."
        }

        // Complete a pass through the eligible catalogue before showing a sentence again.
        // Presentation counts, rather than just the last five cards, persist across sessions.
        val counts = presented.groupingBy { it }.eachCount()
        val fewestPresentations = pool.minOf { counts[it.text] ?: 0 }
        val leastSeen = pool.filter { (counts[it.text] ?: 0) == fewestPresentations }
        val recentItems = presented.take(5).toSet()
        val notRecent = leastSeen.filterNot { it.text in recentItems }
        val spacedPool = if (notRecent.isNotEmpty()) {
            notRecent
        } else {
            leastSeen.filterNot { it.text == presented.firstOrNull() }.ifEmpty { leastSeen }
        }
        val reviewPool = spacedPool.filter { it.text in missed }.ifEmpty { spacedPool }
        return reviewPool[random.nextInt(reviewPool.size)]
    }
}
