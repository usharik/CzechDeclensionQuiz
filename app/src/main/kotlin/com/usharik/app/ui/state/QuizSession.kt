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
class QuizProgress(
    private val stats: TrainingStatsRepository,
    private val goalTarget: () -> Int,
    private val onGoalReached: () -> Unit = {},
) {
    var todayWords by mutableStateOf(0)
        private set
    var todayExercises by mutableStateOf(0)
        private set
    var todayScore by mutableStateOf(0)
        private set
    var recentWords by mutableStateOf<List<String>>(emptyList()) // oldest→newest
        private set

    /** Today's progress towards the daily points goal, for the quit-quiz nudge. */
    val dailyGoal: DailyGoal.Progress get() = DailyGoal.Progress(completed = todayScore, target = goalTarget())

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

    /** Awards an extra bonus (e.g. for clearing a word in review mode). */
    suspend fun addBonus(points: Int) {
        stats.addScorePoints(points)
        refresh()
    }

    /** Deducts a small penalty for skipping a word before completing it. */
    suspend fun applyPenalty() {
        stats.addScorePoints(-Scoring.POINTS_PENALTY)
        refresh()
    }

    private suspend fun refresh() {
        val s = stats.todayStats()
        val before = todayScore
        todayWords = s?.wordsCompleted ?: 0
        todayExercises = s?.exercisesCompleted ?: 0
        todayScore = s?.score ?: 0
        if (loaded && before < goalTarget() && todayScore >= goalTarget()) onGoalReached()
        loaded = true
    }

    // The first refresh only loads today's score; it must not report a goal reached earlier.
    private var loaded = false
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
    var isAdvancing by mutableStateOf(false)
        private set
    /** Set when [pickNextWord] runs out of words (review mode with an emptied mistakes list). */
    var noMoreWords by mutableStateOf(false)
        private set
    val progress = QuizProgress(
        app.statsRepository,
        goalTarget = { app.appState.getDailyGoal() },
        onGoalReached = { app.analyticsService.logEvent("daily_goal_reached") },
    )

    /** Chooses the word after [current]; null ends the session (see [noMoreWords]). */
    protected open suspend fun pickNextWord(current: WordInfo?): WordInfo? = app.wordService.nextWord(current)

    /** Whether the word saved from the previous visit may be resumed. */
    protected open fun canResume(word: WordInfo): Boolean = true

    /** Resets the per-mode question state for a freshly applied word. */
    protected abstract fun onWordApplied(word: WordInfo)

    /** Whether the word being left behind was completed without any mistakes (perfect bonus). */
    protected open fun isCurrentWordPerfect(): Boolean = true

    /** Restarts the current word for "try again"; defaults to a full re-apply (reshuffle). */
    protected open fun restart(word: WordInfo) = applyWord(word)

    /** Awaits the dictionary, loads progress and restores the last word (or picks a fresh one). */
    suspend fun start() {
        app.dictionaryReady.await()
        progress.load()
        if (word == null) {
            val saved = app.lastWordStore.getLastWord(lastWordMode)
            val restored = saved?.takeIf { it.isNotBlank() }?.let { app.wordService.wordByName(it) }?.takeIf(::canResume)
            if (restored != null) applyWord(restored) else nextWord()
        }
    }

    /**
     * Moves to a new word. [skipped] marks an explicit "next word" action taken before the
     * current word was actually completed (e.g. the toolbar next button), which earns a small
     * penalty instead of the word-completion bonus.
     */
    fun nextWord(
        tryAgain: Boolean = false,
        skipped: Boolean = false,
        expectedCurrentWord: WordInfo? = word,
    ) {
        // Navigation has side effects (stats, history and word selection). Keep it single-flight
        // so a double tap cannot score or skip the same word twice. The expected word also
        // rejects a queued toolbar click that belongs to the word we have already replaced.
        if (isAdvancing || word !== expectedCurrentWord) return
        val current = word
        if (tryAgain && current != null) {
            restart(current)
            return
        }
        isAdvancing = true
        scope.launch {
            try {
                // The word being left is what earned the completion bonus and belongs in history.
                // In particular, opening the first fresh word (where [current] is null) must not
                // create a phantom completion or perfect-word bonus.
                if (current != null) {
                    if (skipped) progress.applyPenalty()
                    else progress.countWordCompleted(current.word(), isCurrentWordPerfect())
                }
                val next = pickNextWord(current)
                if (next == null) noMoreWords = true else applyWord(next)
            } finally {
                isAdvancing = false
            }
        }
    }

    private fun applyWord(newWord: WordInfo) {
        word = newWord
        app.lastWordStore.saveLastWord(lastWordMode, newWord.word())
        onWordApplied(newWord)
    }
}
