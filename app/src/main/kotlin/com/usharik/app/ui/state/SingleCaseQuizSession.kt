package com.usharik.app.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.usharik.app.App
import com.usharik.app.CzechCase
import com.usharik.app.PartOfSpeech
import com.usharik.app.service.LastWordStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Session state holder for the one-form-at-a-time quiz: walks the cells of the current
 * [FormTable] in [FormTable.questionOrder] (7 cases × 2 numbers for a noun or adjective, the
 * persons of a verb), builds the four answer choices and records stats.
 */
class SingleCaseQuizSession(app: App, scope: CoroutineScope, partOfSpeech: PartOfSpeech) :
    QuizSession(app, scope, partOfSpeech, LastWordStore.modeSingleForm(partOfSpeech)) {

    /** Index into [FormTable.questionOrder]. */
    var questionIndex by mutableStateOf(0)
        private set
    var answers by mutableStateOf<List<String>>(emptyList())
        private set
    var correct by mutableStateOf("")
        private set
    var answered by mutableStateOf(false)
        private set
    var selectedIndex by mutableStateOf(-1)
        private set
    private var mistakes = 0

    /** The cell currently asked, or null before the first table is loaded. */
    val question: FormCell? get() = table?.questionOrder?.getOrNull(questionIndex)

    override fun isCurrentWordPerfect(): Boolean = mistakes == 0

    /** True once the current form is answered and no later form remains. */
    fun isWordComplete(): Boolean {
        val t = table ?: return false
        return answered && questionIndex >= t.questionOrder.lastIndex
    }

    override fun onTableApplied(table: FormTable) { mistakes = 0; resetToFirstQuestion() }

    /** "Try again" restarts the same word from its first question without re-counting stats. */
    override fun restart(table: FormTable) { mistakes = 0; resetToFirstQuestion() }

    /** Registers the pick; returns whether it was correct, or null when the tap is ignored. */
    fun selectAnswer(index: Int): Boolean? {
        if (answered || index >= answers.size) return null
        val selected = answers[index]
        val isCorrect = selected == correct
        if (isCorrect) scope.launch { progress.countCorrectForm() } else { mistakes++; scope.launch { progress.countError() } }
        app.analyticsService.logSingleCaseAnswer(
            isCorrect, selected, correct,
            lexeme?.key.orEmpty(), questionLabel(),
        )
        selectedIndex = index
        answered = true
        return isCorrect
    }

    // Each answered form = one exercise (visible increment in the quit dialog).
    fun nextStep() {
        if (!answered || isAdvancing) return
        scope.launch { progress.countExerciseCompleted() }
        val t = table ?: return
        if (questionIndex >= t.questionOrder.lastIndex) {
            rememberMistakes(t.lexeme)
            nextWord()
            return
        }
        questionIndex++
        prepareQuestion()
    }

    // Words the player struggled with join the mistakes list, so review mode can revisit them.
    // A clean run doesn't remove them: only a mistake-free full table proves the word is learned.
    private fun rememberMistakes(lexeme: Lexeme) {
        if (mistakes < MISTAKES_TO_REVIEW) return
        app.appState.putWordToErrorMap(lexeme.key, maxOf(mistakes, app.appState.getWordsWithErrors()[lexeme.key] ?: 0))
        app.persistWordsWithErrors()
    }

    private companion object { const val MISTAKES_TO_REVIEW = 2 }

    // Analytics label of the current question: the case name for case rows, the person for verbs.
    private fun questionLabel(): String {
        val q = question ?: return ""
        val row = table?.rows?.getOrNull(q.row)
        return row?.caseIndex?.let { CzechCase.fromIndex(it).displayName } ?: q.question
    }

    /**
     * Unique distractors from all forms of the current lexeme (every case and gender of an
     * adjective, every person/tense of a verb), shuffled, then the correct answer mixed in with
     * up to three of them.
     */
    private fun buildAnswers(lexeme: Lexeme, correctAnswer: String): List<String> {
        val pool: List<String> = when (lexeme) {
            is Lexeme.Noun -> (0..6).flatMap { i -> listOf(lexeme.info.cases(0, i), lexeme.info.cases(1, i)) }
            is Lexeme.Adjective -> lexeme.info.allForms()
            is Lexeme.Verb -> lexeme.info.allForms()
            is Lexeme.Phrase -> table?.targets.orEmpty()
        }
        val unique = pool.filter { it.isNotEmpty() && it != correctAnswer }.distinct().shuffled()
        val result = mutableListOf(correctAnswer)
        for (s in unique) { if (result.size >= 4) break; result.add(s) }
        result.shuffle()
        return result
    }

    private fun prepareQuestion() {
        val t = table
        val q = question
        if (t == null || q == null) { correct = ""; answers = emptyList(); answered = false; return }
        correct = q.target
        answers = buildAnswers(t.lexeme, correct)
        answered = false
        selectedIndex = -1
    }

    private fun resetToFirstQuestion() {
        questionIndex = 0
        prepareQuestion()
    }
}
