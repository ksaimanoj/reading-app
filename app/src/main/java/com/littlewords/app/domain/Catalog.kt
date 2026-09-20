package com.littlewords.app.domain

object Catalog {
    val patternLabels: Map<String, String> = linkedMapOf(
        "short_a" to "Short a",
        "short_e" to "Short e",
        "short_i" to "Short i",
        "short_o" to "Short o",
        "short_u" to "Short u",
        "blends" to "Consonant blends",
        "sh" to "sh digraph",
        "ch" to "ch digraph",
        "th" to "th digraph",
        "ck" to "ck ending",
        "double" to "Double consonants",
        "tricky" to "Tricky words",
        "two_syllables" to "Two syllables",
    )

    val words: List<Word> = CatalogData.words
}
