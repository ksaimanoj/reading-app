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
    private val sentenceWordPattern = Regex("[a-z]+")

    fun calculate(words: List<Word>, attempts: List<ProgressAttempt>): Map<String, WordMilestone> {
        val validWords = words.filter { it.category != Category.THREE_SILLY }.associateBy { it.text }
        val byWord = attempts.flatMap { attempt ->
            when {
                attempt.category == SENTENCE_CATEGORY && attempt.success ->
                    sentenceWordPattern.findAll(attempt.word.lowercase()).map { it.value }.toSet()
                        .filter { it in validWords }.map { attempt.copy(word = it) }
                attempt.category == SENTENCE_CATEGORY || attempt.category == Category.THREE_SILLY.name -> emptyList()
                attempt.word.lowercase() in validWords -> listOf(attempt.copy(word = attempt.word.lowercase()))
                else -> emptyList()
            }
        }.groupBy { it.word }
        return validWords.mapValues { (word, _) ->
            milestoneFor(byWord[word].orEmpty())
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

object SentenceProgressCalculator {
    fun calculate(sentences: List<Sentence>, attempts: List<ProgressAttempt>): Map<String, WordMilestone> {
        val validSentences = sentences.associateBy { it.text }
        val bySentence = attempts.filter { it.category == SENTENCE_CATEGORY && it.word in validSentences }
            .groupBy { it.word }
        return validSentences.mapValues { (sentence, _) -> milestoneFor(bySentence[sentence].orEmpty()) }
    }
}

private fun milestoneFor(records: List<ProgressAttempt>): WordMilestone {
    val sessions = records.filter { it.success }.map { it.sessionId }.distinct().size
    val status = when {
        sessions >= 2 -> WordStatus.CONFIDENT
        records.isNotEmpty() -> WordStatus.PRACTISING
        else -> WordStatus.NOT_TRIED
    }
    return WordMilestone(status, status == WordStatus.CONFIDENT && records.maxByOrNull { it.id }?.success == false, sessions)
}
