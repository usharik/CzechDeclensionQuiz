package com.usharik.app.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.usharik.app.App
import com.usharik.database.TrainingStatsRepository
import com.usharik.database.WordInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Today's training counters and the recent-words list shown in the quit-quiz overlay. */
class QuizProgress(private val stats: TrainingStatsRepository) {
    var todayWords by mutableStateOf(0)
        private set
    var todayExercises by mutableStateOf(0)
        private set
    var recentWords by mutableStateOf<List<String>>(emptyList()) // oldest→newest
        private set

    suspend fun load() {
        recentWords = stats.recentWords()
        refresh()
    }

    suspend fun countWordCompleted(word: String) {
        recentWords = ((recentWords - word) + word).takeLast(3)
        stats.saveRecentWords(recentWords)
        stats.incrementWordsCompleted()
        refresh()
    }

    suspend fun countExerciseCompleted() {
        stats.incrementExercisesCompleted()
        refresh()
    }

    suspend fun countError() = stats.incrementErrorsCount()

    private suspend fun refresh() {
        val s = stats.todayStats()
        todayWords = s?.wordsCompleted ?: 0
        todayExercises = s?.exercisesCompleted ?: 0
    }
}

/**
 * Base class for the two quiz-session state holders. Owns the current word, the shared session
 * progress and the word lifecycle: restoring the last word on start, moving to the next word and
 * counting per-word stats. Composables observe the exposed snapshot state; UI-only concerns
 * (haptics, ads, dialogs) stay in the screens.
 */
abstract class QuizSession(
    protected val app: App,
    protected val scope: CoroutineScope,
    private val lastWordMode: String,
) {
    var word by mutableStateOf<WordInfo?>(null)
        private set
    val progress = QuizProgress(app.statsRepository)

    /** Resets the per-mode question state for a freshly applied word. */
    protected abstract fun onWordApplied(word: WordInfo)

    /** Restarts the current word for "try again"; defaults to a full re-apply (reshuffle). */
    protected open fun restart(word: WordInfo) = applyWord(word, countStats = false)

    /** Awaits the dictionary, loads progress and restores the last word (or picks a fresh one). */
    suspend fun start() {
        app.dictionaryReady.await()
        progress.load()
        if (word == null) {
            val saved = app.lastWordStore.getLastWord(lastWordMode)
            val restored = saved?.takeIf { it.isNotBlank() }?.let { app.wordService.wordByName(it) }
            if (restored != null) applyWord(restored, countStats = false) else nextWord()
        }
    }

    fun nextWord(tryAgain: Boolean = false) {
        val current = word
        if (tryAgain && current != null) {
            restart(current)
            return
        }
        scope.launch { applyWord(app.wordService.nextWord(current), countStats = true) }
    }

    private fun applyWord(newWord: WordInfo, countStats: Boolean) {
        word = newWord
        app.lastWordStore.saveLastWord(lastWordMode, newWord.word())
        onWordApplied(newWord)
        if (countStats) scope.launch { progress.countWordCompleted(newWord.word()) }
    }
}