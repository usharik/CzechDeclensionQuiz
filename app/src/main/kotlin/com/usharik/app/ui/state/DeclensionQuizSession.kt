package com.usharik.app.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.usharik.app.App
import com.usharik.app.PartOfSpeech
import com.usharik.app.service.LastWordStore
import com.usharik.app.ui.components.CellFeedback
import com.usharik.app.ui.components.POOL_KEY
import com.usharik.app.ui.components.WordModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Session state holder for the full-table quiz: the shuffled form pool, the rows × 2 grid
 * placements, per-cell feedback and error counters. [handleDrop] applies a drag-and-drop move
 * and reports its outcome so the screen can react (haptics, ads, completion dialog).
 *
 * The grid comes from the current [FormTable]: 7 case rows for a noun or an adjective (+ noun),
 * 8 rows in three sections for a verb. Cell keys are `"<column>_<row>"`.
 *
 * With [review] set, words come only from the mistakes list; a mistake-free table clears the word
 * from that list with a bonus, and the session ends once the list is empty.
 */
class DeclensionQuizSession(app: App, scope: CoroutineScope, partOfSpeech: PartOfSpeech, val review: Boolean = false) :
    QuizSession(app, scope, partOfSpeech, if (review) LastWordStore.MODE_REVIEW else LastWordStore.modeFullTable(partOfSpeech)) {

    enum class DropOutcome { IGNORED, CORRECT, WRONG, ERROR_LIMIT_REACHED, COMPLETED }

    var models by mutableStateOf<List<WordModel>>(emptyList())
        private set
    var actual by mutableStateOf(List(14) { -1 }) // idx = column*rowCount + row
        private set
    var wrongAttempts by mutableStateOf(0)
        private set
    var feedback by mutableStateOf<Map<String, CellFeedback>>(emptyMap())
        private set

    /** Bumped whenever the per-word timer should restart: a fresh word or a completed table. */
    var timerResetToken by mutableStateOf(0)
        private set
    private var errorCount = 0
    private var tableCompleted = false
    private val correctPlacementRewards = CorrectPlacementRewards()

    fun wordFor(ix: Int) = if (ix < 0 || ix >= models.size) "" else models[ix].word
    private val rowCount: Int get() = table?.rowCount ?: 7
    fun cellIdx(column: Int, row: Int) = column * rowCount + row
    fun isWordComplete(): Boolean = isComplete()

    override fun isCurrentWordPerfect(): Boolean = errorCount == 0

    override suspend fun pickNextLexeme(current: Lexeme?): Lexeme? =
        // nextReviewLexeme drops words missing from the dictionary; persist so they don't come back.
        if (review) app.wordService.nextReviewLexeme(current).also { app.persistWordsWithErrors() } else super.pickNextLexeme(current)

    override fun canResume(lexeme: Lexeme): Boolean = !review || lexeme.key in app.appState.getWordsWithErrors()

    override fun onTableApplied(table: FormTable) {
        // Pool every cell (including non-existent ones as invisible entries) so pool indices stay
        // stable and shuffled independently of which forms a lexeme happens to lack.
        models = table.cells.map { WordModel(it.target, it.exists) }.shuffled()
        actual = List(table.rowCount * 2) { -1 }
        wrongAttempts = 0; errorCount = 0; feedback = emptyMap(); tableCompleted = false
        correctPlacementRewards.reset()
        timerResetToken++
    }

    /** Applies a drop (pool→cell, cell→cell swap or cell→pool return) and reports the outcome. */
    fun handleDrop(tag: String, target: Any?): DropOutcome {
        table ?: return DropOutcome.IGNORED
        if (tableCompleted) return DropOutcome.IGNORED
        if (target == null) return DropOutcome.IGNORED
        val poolItem = !tag.contains("_")
        if (target == POOL_KEY) {
            if (!poolItem) {
                val (n, c) = tag.split("_").map { it.toInt() }
                val idx = cellIdx(n, c); val wordNum = actual[idx]
                if (wordNum != -1) { setActual(idx, -1); setVisible(wordNum, true) }
            }
            return DropOutcome.IGNORED
        }
        val (tn, tc) = (target as String).split("_").map { it.toInt() }
        val tIdx = cellIdx(tn, tc)
        val ok: Boolean
        if (poolItem) {
            val wordNum = tag.toInt()
            val existing = actual[tIdx]
            if (existing != -1) setVisible(existing, true)
            setActual(tIdx, wordNum); setVisible(wordNum, false)
            ok = correctAt(tn, tc) == wordFor(wordNum)
            mark(tn, tc, ok)
            if (!ok) {
                registerError()
                scope.launch { delay(500); if (actual[tIdx] == wordNum) { setActual(tIdx, -1); setVisible(wordNum, true) } }
            }
        } else {
            val (sn, sc) = tag.split("_").map { it.toInt() }
            val sIdx = cellIdx(sn, sc)
            val oldTarget = actual[tIdx]; val oldSource = actual[sIdx]
            setActual(tIdx, oldSource); setActual(sIdx, oldTarget)
            val ok1 = oldSource != -1 && correctAt(tn, tc) == wordFor(oldSource)
            // A move into an empty counterpart cell is valid; only displacing a form into a
            // cell where it is wrong counts as an error.
            val ok2 = oldTarget == -1 || correctAt(sn, sc) == wordFor(oldTarget)
            ok = ok1 && ok2
            mark(tn, tc, ok)
            if (!ok) {
                registerError()
                scope.launch {
                    delay(500)
                    // Undo only if both cells still hold the swapped values; a user action in
                    // the meantime must not be overwritten by stale swap data.
                    if (actual[tIdx] == oldSource && actual[sIdx] == oldTarget) { setActual(tIdx, oldTarget); setActual(sIdx, oldSource) }
                }
            }
        }
        if (!ok) {
            return if (wrongAttempts == DeclensionQuizRules.MAX_WRONG_ATTEMPTS) {
                DropOutcome.ERROR_LIMIT_REACHED
            } else {
                DropOutcome.WRONG
            }
        }
        // Correctness alone is not enough to earn more points: rearranging a previously solved
        // cell (including dropping it onto itself) must not be a scoring exploit.
        if (correctPlacementRewards.claim(tIdx)) scope.launch { progress.countCorrectForm() }
        if (isComplete()) { onTableCompleted(); return DropOutcome.COMPLETED }
        return DropOutcome.CORRECT
    }

    private fun correctAt(column: Int, row: Int) = table?.target(column, row).orEmpty()
    private fun setVisible(ix: Int, v: Boolean) { models = models.mapIndexed { i, m -> if (i == ix) m.copy(visible = v) else m } }
    private fun setActual(idx: Int, v: Int) { actual = actual.toMutableList().also { it[idx] = v } }
    private fun mark(tn: Int, tc: Int, ok: Boolean) { feedback = feedback + ("${tn}_$tc" to CellFeedback(ok)) }

    private fun registerError() {
        wrongAttempts++; errorCount++
        scope.launch { progress.countError() }
    }

    /**
     * Resets the visible per-word error badge (but not [errorCount], which still feeds the
     * word's overall error-map bookkeeping on completion). Used after the "wrong answer" ad
     * interrupts the player, so the counter starts fresh again instead of accumulating past 5.
     */
    fun resetErrorCounter() { wrongAttempts = 0 }

    /**
     * Restarts the per-word countdown without touching the word or any cells the player has
     * already placed. Used after the timeout ad is dismissed, so the player keeps their progress
     * on the current word instead of being bumped to a fresh one.
     */
    fun resetTimer() { timerResetToken++ }

    private fun isComplete(): Boolean {
        val t = table ?: return false
        for (cell in t.cells) {
            if (cell.exists) {
                val ix = actual[cellIdx(cell.column, cell.row)]
                if (ix == -1 || cell.target != wordFor(ix)) return false
            }
        }
        return true
    }

    // Counts the exercise and syncs the word's error-map entry once the table is filled correctly.
    private fun onTableCompleted() {
        val key = lexeme?.key ?: return
        tableCompleted = true
        val cleared = review && errorCount == 0 && key in app.appState.getWordsWithErrors()
        scope.launch {
            progress.countExerciseCompleted()
            if (cleared) progress.addBonus(Scoring.POINTS_REVIEW_CLEARED)
        }
        if (cleared) app.analyticsService.logEvent("review_word_cleared")
        if (errorCount == 0) app.appState.removeWordFromErrorMap(key)
        if (errorCount > 2) app.appState.putWordToErrorMap(key, errorCount)
        app.persistWordsWithErrors()
        timerResetToken++
    }
}
