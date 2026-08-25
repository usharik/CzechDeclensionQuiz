package com.usharik.app.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.usharik.app.App
import com.usharik.app.service.LastWordStore
import com.usharik.app.ui.components.CellFeedback
import com.usharik.app.ui.components.POOL_KEY
import com.usharik.app.ui.components.WordModel
import com.usharik.database.WordInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Session state holder for the full declension-table quiz: the shuffled form pool, the 7×2 grid
 * placements, per-cell feedback and error counters. [handleDrop] applies a drag-and-drop move
 * and reports its outcome so the screen can react (haptics, ads, completion dialog).
 */
class DeclensionQuizSession(app: App, scope: CoroutineScope) :
    QuizSession(app, scope, LastWordStore.MODE_FULL_DECLENSION) {

    enum class DropOutcome { IGNORED, CORRECT, WRONG, COMPLETED }

    var models by mutableStateOf<List<WordModel>>(emptyList())
        private set
    var actual by mutableStateOf(List(14) { -1 }) // idx = number*7 + caseNum
        private set
    var wrongAttempts by mutableStateOf(0)
        private set
    var feedback by mutableStateOf<Map<String, CellFeedback>>(emptyMap())
        private set
    private var errorCount = 0

    fun wordFor(ix: Int) = if (ix < 0 || ix >= models.size) "" else models[ix].word
    fun cellIdx(number: Int, caseNum: Int) = number * 7 + caseNum

    override fun onWordApplied(word: WordInfo) {
        val list = ArrayList<WordModel>(14)
        for (i in 0..6) {
            val s = word.cases(0, i); val p = word.cases(1, i)
            list.add(WordModel(s, s.isNotEmpty()))
            list.add(WordModel(p, p.isNotEmpty()))
        }
        list.shuffle()
        models = list
        actual = List(14) { -1 }
        wrongAttempts = 0; errorCount = 0; feedback = emptyMap()
    }

    /** Applies a drop (pool→cell, cell→cell swap or cell→pool return) and reports the outcome. */
    fun handleDrop(tag: String, target: Any?): DropOutcome {
        word ?: return DropOutcome.IGNORED
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
        if (!ok) return DropOutcome.WRONG
        if (isComplete()) { onTableCompleted(); return DropOutcome.COMPLETED }
        return DropOutcome.CORRECT
    }

    private fun correctAt(number: Int, caseNum: Int) = word?.cases(number, caseNum).orEmpty()
    private fun setVisible(ix: Int, v: Boolean) { models = models.mapIndexed { i, m -> if (i == ix) m.copy(visible = v) else m } }
    private fun setActual(idx: Int, v: Int) { actual = actual.toMutableList().also { it[idx] = v } }
    private fun mark(tn: Int, tc: Int, ok: Boolean) { feedback = feedback + ("${tn}_$tc" to CellFeedback(ok)) }

    private fun registerError() {
        wrongAttempts++; errorCount++
        scope.launch { progress.countError() }
    }

    private fun isComplete(): Boolean {
        val w = word ?: return false
        for (i in 0..6) for (n in 0..1) {
            val c = w.cases(n, i)
            if (c.isNotEmpty()) {
                val ix = actual[cellIdx(n, i)]
                if (ix == -1 || c != wordFor(ix)) return false
            }
        }
        return true
    }

    // Counts the exercise and syncs the word's error-map entry once the table is filled correctly.
    private fun onTableCompleted() {
        val w = word ?: return
        scope.launch { progress.countExerciseCompleted() }
        if (errorCount == 0) app.appState.removeWordFromErrorMap(w.word())
        if (errorCount > 2) app.appState.putWordToErrorMap(w.word(), errorCount)
        app.persistWordsWithErrors()
    }
}