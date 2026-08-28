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
    var todayScore by mutableStateOf(0)
        private set
    var recentWords by mutableStateOf<List<String>>(emptyList()) // oldest→newest
        private set

    /** Today's progress towards the daily points goal, for the quit-quiz nudge. */
    val dailyGoal: DailyGoal.Progress get() = DailyGoal.Progress(completed = todayScore)

    suspend fun load() {
        recentWords = stats.recentWords()
        refresh()
    }

    /** Awards points for a single correctly placed/answered form. */
    suspend fun countCorrectForm() {
        stats.addScorePoints(Scoring.POINTS_PER_CORRECT_FORM)
        refresh()
    }

    /** Awards the word-completion bonus, plus the perfect-word bonus when there were no mistakes. */
    suspend fun countWordCompleted(word: String, perfect: Boolean) {
        recentWords = ((recentWords - word) + word).takeLast(3)
        stats.saveRecentWords(recentWords)
        stats.incrementWordsCompleted()
        val bonus = Scoring.POINTS_WORD_COMPLETED + if (perfect) Scoring.POINTS_PERFECT_BONUS else 0
        stats.addScorePoints(bonus)
        refresh()
    }

    suspend fun countExerciseCompleted() {
        stats.incrementExercisesCompleted()
        refresh()
    }

    suspend fun countError() = stats.incrementErrorsCount()

    /** Deducts a small penalty for negative behaviors (too many mistakes, timeout, skipping). */
    suspend fun applyPenalty() {
        stats.addScorePoints(-Scoring.POINTS_PENALTY)
        refresh()
    }

    private suspend fun refresh() {
        val s = stats.todayStats()
        todayWords = s?.wordsCompleted ?: 0
        todayExercises = s?.exercisesCompleted ?: 0
        todayScore = s?.score ?: 0
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

    /** Whether the word being left behind was completed without any mistakes (perfect bonus). */
    protected open fun isCurrentWordPerfect(): Boolean = true

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

    /**
     * Moves to a new word. [skipped] marks an explicit "next word" action taken before the
     * current word was actually completed (e.g. the toolbar next button), which earns a small
     * penalty instead of the word-completion bonus.
     */
    fun nextWord(tryAgain: Boolean = false, skipped: Boolean = false) {
        val current = word
        if (tryAgain && current != null) {
            restart(current)
            return
        }
        scope.launch { applyWord(app.wordService.nextWord(current), countStats = true, skipped = skipped) }
    }

    private fun applyWord(newWord: WordInfo, countStats: Boolean, skipped: Boolean = false) {
        // Captured before onWordApplied resets the per-word error state below.
        val perfect = isCurrentWordPerfect()
        word = newWord
        app.lastWordStore.saveLastWord(lastWordMode, newWord.word())
        onWordApplied(newWord)
        if (countStats) {
            scope.launch {
                if (skipped) progress.applyPenalty() else progress.countWordCompleted(newWord.word(), perfect)
            }
        }
    }
}