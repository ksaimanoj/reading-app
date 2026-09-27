package com.littlewords.app.domain

data class ProgressAttempt(
    val word: String,
    val category: String,
    val sessionId: Long,
    val success: Boolean,
    val id: Long,
)

enum class WordStatus { NOT_TRIED, PRACTISING, CONFIDENT }
data class WordMilestone(val status: WordStatus, val needsReview: Boolean = false, val independentSessions: Int = 0)
data class StageProgress(
    val stageId: String,
    val total: Int,
    val confident: Int,
    val practising: Int,
    val notTried: Int,
    val needsReview: Int,
    val firstIndependent: Boolean,
) {
    val firstTenConfident get() = total >= 10 && confident >= 10
    val collectionComplete get() = total > 0 && confident == total
}

object WordProgressCalculator {
    fun calculate(words: List<Word>, attempts: List<ProgressAttempt>): Map<String, WordMilestone> {
        val validWords = words.filter { it.category != Category.THREE_SILLY }.associateBy { it.text }
        val byWord = attempts.filter { attempt ->
            attempt.category != SENTENCE_CATEGORY && attempt.category != Category.THREE_SILLY.name &&
                attempt.word.lowercase() in validWords
        }.groupBy { it.word.lowercase() }
        return validWords.mapValues { (word, _) ->
            val records = byWord[word].orEmpty()
            val sessions = records.filter { it.success }.map { it.sessionId }.distinct().size
            val status = when {
                sessions >= 2 -> WordStatus.CONFIDENT
                records.isNotEmpty() -> WordStatus.PRACTISING
                else -> WordStatus.NOT_TRIED
            }
            WordMilestone(status, status == WordStatus.CONFIDENT && records.maxByOrNull { it.id }?.success == false, sessions)
        }
    }

    fun summarize(stageId: String, milestones: Map<String, WordMilestone>): StageProgress {
        val words = StageCatalog.words(stageId)
        val values = words.map { milestones[it.text] ?: WordMilestone(WordStatus.NOT_TRIED) }
        val confident = values.count { it.status == WordStatus.CONFIDENT }
        val practising = values.count { it.status == WordStatus.PRACTISING }
        return StageProgress(stageId, words.size, confident, practising, words.size - confident - practising,
            values.count { it.needsReview }, values.any { it.independentSessions > 0 })
    }
}
