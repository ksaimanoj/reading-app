package com.littlewords.app.domain

const val CATALOGUE_VERSION = 1

data class Subskill(val id: String, val label: String, val explanation: String = "")
data class LearningStage(val id: String, val label: String, val subskills: List<Subskill>)

object StageCatalog {
    val stages = listOf(
        LearningStage("cvc", "Short-vowel CVC words", listOf(
            Subskill("short_a", "Short a"), Subskill("short_e", "Short e"),
            Subskill("short_i", "Short i"), Subskill("short_o", "Short o"),
            Subskill("short_u", "Short u"),
        )),
        LearningStage("digraphs", "Digraphs and doubled consonants", listOf(
            Subskill("sh", "sh"), Subskill("ch", "ch"), Subskill("th", "th"),
            Subskill("ck", "ck"), Subskill("double", "Doubled consonants"),
            Subskill("combined", "Combined patterns", "Practises words with two digraph or doubled-consonant skills."),
        )),
        LearningStage("blends", "Consonant blends", listOf(
            Subskill("plain", "Blends"),
            Subskill("combined", "Blends with other patterns", "Assumes the blend and the digraph or doubled-consonant patterns shown on each word."),
        )),
        LearningStage("additional", "Additional practice", listOf(
            Subskill("two_letter", "Two-letter words"),
            Subskill("tricky", "Tricky words"),
            Subskill("two_syllables", "Two-syllable words"),
        )),
    )
    const val SILLY_KEY = "silly:silly"
    fun key(stageId: String, subskillId: String) = "$stageId:$subskillId"
    fun contains(stageId: String, subskillId: String) =
        stages.any { it.id == stageId && it.subskills.any { skill -> skill.id == subskillId } }
    fun words(stageId: String, subskillId: String? = null): List<Word> = Catalog.words.filter {
        it.stageId == stageId && (subskillId == null || it.subskillId == subskillId)
    }
    fun suggestedSelection(): Set<String> = stages.first().subskills.map { key("cvc", it.id) }.toSet()
    fun supportedPatterns(selected: Set<String>): Set<String> = Catalog.words.asSequence()
        .filter { it.category != Category.THREE_SILLY && key(it.stageId, it.subskillId) in selected }
        .flatMap { it.patterns.asSequence() }.toSet()

    fun saveChoices(base: PracticeConfig, selected: Set<String>): PracticeConfig = base.copy(
        selectedSubskills = selected,
        letters = if (base.selectedSubskills == null) "abcdefghijklmnopqrstuvwxyz" else base.letters,
        patterns = if (base.selectedSubskills == null) Catalog.patternLabels.keys else base.patterns,
        catalogueVersion = CATALOGUE_VERSION,
    )
}
