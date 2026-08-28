package com.usharik.app.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.usharik.app.App
import com.usharik.app.CzechCase
import com.usharik.app.service.LastWordStore
import com.usharik.database.WordInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Session state holder for the one-case-at-a-time quiz: walks the 7 cases × 2 numbers of each
 * word (skipping intentionally empty forms), builds the four answer choices and records stats.
 */
class SingleCaseQuizSession(app: App, scope: CoroutineScope) :
    QuizSession(app, scope, LastWordStore.MODE_SINGLE_CASE) {

    var caseIndex by mutableStateOf(0)
        private set
    var plural by mutableStateOf(false)
        private set
    var answers by mutableStateOf<List<String>>(emptyList())
        private set
    var correct by mutableStateOf("")
        private set
    var answered by mutableStateOf(false)
        private set
    var selectedIndex by mutableStateOf(-1)
        private set
    private var hasMistake = false

    override fun isCurrentWordPerfect(): Boolean = !hasMistake

    /** True once the player has reached and answered the word's very last question. */
    fun isWordComplete(): Boolean = plural && caseIndex == 6 && answered

    override fun onWordApplied(word: WordInfo) { hasMistake = false; resetToFirstQuestion(word) }

    /** "Try again" restarts the same word from its first question without re-counting stats. */
    override fun restart(word: WordInfo) { hasMistake = false; resetToFirstQuestion(word) }

    /** Registers the pick; returns whether it was correct, or null when the tap is ignored. */
    fun selectAnswer(index: Int): Boolean? {
        if (answered || index >= answers.size) return null
        val selected = answers[index]
        val isCorrect = selected == correct
        if (isCorrect) scope.launch { progress.countCorrectForm() } else { hasMistake = true; scope.launch { progress.countError() } }
        app.analyticsService.logSingleCaseAnswer(
            isCorrect, selected, correct,
            word?.word().orEmpty(), CzechCase.fromIndex(caseIndex).displayName,
        )
        selectedIndex = index
        answered = true
        return isCorrect
    }

    // Each answered case = one exercise (visible increment in the quit dialog).
    fun nextStep() {
        scope.launch { progress.countExerciseCompleted() }
        val w = word ?: return
        var more = advance()
        while (more && currentFormIsEmpty(w)) more = advance()
        if (!more) {
            nextWord()
            return
        }
        prepareQuestion()
    }

    // Mirrors the legacy buildAnswers: unique distractors from all forms of the current word,
    // shuffled, then the correct answer mixed in with up to three of them.
    private fun buildAnswers(w: WordInfo, correctAnswer: String): List<String> {
        val distractors = LinkedHashSet<String>()
        for (i in 0..6) {
            val sg = w.cases(0, i); if (sg.isNotEmpty() && sg != correctAnswer) distractors.add(sg)
            val pl = w.cases(1, i); if (pl.isNotEmpty() && pl != correctAnswer) distractors.add(pl)
        }
        val unique = distractors.toMutableList().also { it.shuffle() }
        val result = mutableListOf(correctAnswer)
        for (s in unique) { if (result.size >= 4) break; result.add(s) }
        result.shuffle()
        return result
    }

    private fun prepareQuestion() {
        val w = word ?: run { correct = ""; answers = emptyList(); answered = false; return }
        correct = w.cases(if (plural) 1 else 0, caseIndex)
        answers = buildAnswers(w, correct)
        answered = false
        selectedIndex = -1
    }

    // Advances caseIndex/plural to the next position; false once the word is exhausted.
    private fun advance(): Boolean {
        when {
            caseIndex < 6 -> caseIndex++
            !plural -> { plural = true; caseIndex = 0 }
            else -> return false
        }
        return true
    }

    // Entries can have intentionally empty forms (e.g. plural-only words); those are skipped
    // instead of being presented as blank answers.
    private fun currentFormIsEmpty(w: WordInfo) = w.cases(if (plural) 1 else 0, caseIndex).isEmpty()

    private fun resetToFirstQuestion(w: WordInfo) {
        caseIndex = 0
        plural = false
        while (currentFormIsEmpty(w)) { if (!advance()) break }
        prepareQuestion()
    }
}