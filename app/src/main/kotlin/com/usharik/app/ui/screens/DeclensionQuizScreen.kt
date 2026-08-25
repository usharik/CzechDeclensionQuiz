package com.usharik.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.usharik.app.App
import com.usharik.app.BuildConfig
import com.usharik.app.R
import com.usharik.app.service.LastWordStore
import com.usharik.app.ui.components.CellFeedback
import com.usharik.app.ui.components.POOL_KEY
import com.usharik.app.ui.components.WordModel
import com.usharik.app.ui.components.rememberDragAndDropState
import com.usharik.app.utils.HapticFeedback
import com.usharik.database.WordInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full declension-table quiz. Faithful Compose port of DeclensionQuizFragment +
 * DeclensionQuizViewModel: a shuffled pool of forms is dragged into a 7×2 case grid; correct
 * placements bounce and stay, wrong ones shake and return, and completing the table shows the
 * correct-answer dialog. Back shows the quit overlay with today's stats.
 */
@Composable
fun DeclensionQuizScreen(
    app: App,
    onQuit: () -> Unit,
    registerNext: ((() -> Unit)?) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val dnd = rememberDragAndDropState()

    var word by remember { mutableStateOf<WordInfo?>(null) }
    var models by remember { mutableStateOf<List<WordModel>>(emptyList()) }
    var actual by remember { mutableStateOf(List(14) { -1 }) } // idx = number*7 + caseNum
    var wrongAttempts by remember { mutableStateOf(0) }
    var errorCount by remember { mutableStateOf(0) }
    var feedback by remember { mutableStateOf<Map<String, CellFeedback>>(emptyMap()) }
    var showCorrect by remember { mutableStateOf(false) }
    var showQuit by remember { mutableStateOf(false) }
    var recent by remember { mutableStateOf<List<String>>(emptyList()) } // oldest→newest
    var todayWords by remember { mutableStateOf(0) }
    var todayExercises by remember { mutableStateOf(0) }

    fun wordFor(ix: Int) = if (ix < 0 || ix >= models.size) "" else models[ix].word
    fun correctAt(number: Int, caseNum: Int) = word?.cases(number, caseNum).orEmpty()
    fun cellIdx(number: Int, caseNum: Int) = number * 7 + caseNum
    fun setVisible(ix: Int, v: Boolean) { models = models.mapIndexed { i, m -> if (i == ix) m.copy(visible = v) else m } }
    fun setActual(idx: Int, v: Int) { actual = actual.toMutableList().also { it[idx] = v } }
    fun mark(tn: Int, tc: Int, ok: Boolean) { feedback = feedback + ("${tn}_$tc" to CellFeedback(ok)) }

    fun isComplete(): Boolean {
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

    suspend fun refreshStats() {
        val s = app.statsRepository.todayStats()
        todayWords = s?.wordsCompleted ?: 0
        todayExercises = s?.exercisesCompleted ?: 0
    }

    fun applyWord(w: WordInfo, countStats: Boolean) {
        word = w
        app.lastWordStore.saveLastWord(LastWordStore.MODE_FULL_DECLENSION, w.word())
        val list = ArrayList<WordModel>(14)
        for (i in 0..6) {
            val s = w.cases(0, i); val p = w.cases(1, i)
            list.add(WordModel(s, s.isNotEmpty()))
            list.add(WordModel(p, p.isNotEmpty()))
        }
        list.shuffle()
        models = list
        actual = List(14) { -1 }
        wrongAttempts = 0; errorCount = 0; feedback = emptyMap()
        if (countStats) {
            recent = (recent - w.word()) + w.word()
            if (recent.size > 3) recent = recent.takeLast(3)
            scope.launch { app.statsRepository.saveRecentWords(recent); app.statsRepository.incrementWordsCompleted(); refreshStats() }
        }
    }

    fun nextWord(tryAgain: Boolean) {
        val cur = word
        if (tryAgain && cur != null) { applyWord(cur, false); return }
        scope.launch { applyWord(app.wordService.nextWord(cur), true) }
    }

    fun onComplete() {
        val w = word ?: return
        scope.launch { app.statsRepository.incrementExercisesCompleted(); refreshStats() }
        if (errorCount == 0) app.appState.removeWordFromErrorMap(w.word())
        if (errorCount > 2) app.appState.putWordToErrorMap(w.word(), errorCount)
        app.persistWordsWithErrors()
        scope.launch { delay(300); showCorrect = true }
    }

    fun wrongAnswerAd() {
        activity?.let { host -> scope.launch { delay(600); app.adManager.showAdIfNeeded(app.adPolicy.onDeclensionWrongAnswer(), host, BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID) {} } }
    }

    fun handleDrop(tag: String, target: Any?) {
        word ?: return
        if (target == null) return
        val poolItem = !tag.contains("_")
        if (target == POOL_KEY) {
            if (!poolItem) {
                val (n, c) = tag.split("_").map { it.toInt() }
                val idx = cellIdx(n, c); val wordNum = actual[idx]
                if (wordNum != -1) { setActual(idx, -1); setVisible(wordNum, true) }
            }
            return
        }
        val (tn, tc) = (target as String).split("_").map { it.toInt() }
        val tIdx = cellIdx(tn, tc)
        if (poolItem) {
            val wordNum = tag.toInt()
            val existing = actual[tIdx]
            if (existing != -1) setVisible(existing, true)
            setActual(tIdx, wordNum); setVisible(wordNum, false)
            if (correctAt(tn, tc) == wordFor(wordNum)) {
                mark(tn, tc, true); HapticFeedback.success(context)
                if (isComplete()) onComplete()
            } else {
                mark(tn, tc, false); HapticFeedback.error(context)
                wrongAttempts++; errorCount++
                scope.launch { delay(500); if (actual[tIdx] == wordNum) { setActual(tIdx, -1); setVisible(wordNum, true) } }
                wrongAnswerAd()
            }
        } else {
            val (sn, sc) = tag.split("_").map { it.toInt() }
            val sIdx = cellIdx(sn, sc)
            val oldTarget = actual[tIdx]; val oldSource = actual[sIdx]
            setActual(tIdx, oldSource); setActual(sIdx, oldTarget)
            val ok1 = oldSource != -1 && correctAt(tn, tc) == wordFor(oldSource)
            val ok2 = oldTarget != -1 && correctAt(sn, sc) == wordFor(oldTarget)
            if (ok1 && ok2) {
                mark(tn, tc, true); HapticFeedback.success(context)
                if (isComplete()) onComplete()
            } else {
                mark(tn, tc, false); HapticFeedback.error(context)
                wrongAttempts++; errorCount++
                scope.launch { delay(500); val a = actual[tIdx]; val b = actual[sIdx]; setActual(tIdx, b); setActual(sIdx, a) }
                wrongAnswerAd()
            }
        }
    }

    SideEffect { dnd.onDrop = { tag, tgt -> handleDrop(tag, tgt) } }

    LaunchedEffect(Unit) {
        activity?.let { app.adManager.loadAd(it, BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID) }
        recent = app.statsRepository.recentWords()
        refreshStats()
        if (word == null) {
            val saved = app.lastWordStore.getLastWord(LastWordStore.MODE_FULL_DECLENSION)
            val restored = saved?.takeIf { it.isNotBlank() }?.let { app.wordService.wordByName(it) }
            if (restored != null) applyWord(restored, false) else nextWord(false)
        }
    }
    DisposableEffect(Unit) {
        registerNext { nextWord(false) }
        onDispose { registerNext(null) }
    }
    BackHandler(enabled = !showQuit) { showQuit = true }

    fun rateApp() {
        val pkg = context.packageName
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))) }
            .recoverCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))) }
            .onFailure { Toast.makeText(context, R.string.rate_app_unavailable, Toast.LENGTH_SHORT).show() }
    }

    DeclensionQuizContent(
        app = app,
        word = word,
        models = models,
        dnd = dnd,
        wordFor = ::wordFor,
        cellIdx = ::cellIdx,
        feedback = feedback,
        wrongAttempts = wrongAttempts,
        actual = actual,
    )

    if (showCorrect) {
        com.usharik.app.ui.components.CorrectAnswerDialog(
            onNextWord = { HapticFeedback.light(context); showCorrect = false; activity?.let { host -> app.adManager.showAdIfNeeded(app.adPolicy.onDeclensionWordCompleted(), host, BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID) { nextWord(false) } } ?: nextWord(false) },
            onStayHere = { HapticFeedback.light(context); showCorrect = false },
            onTryAgain = { HapticFeedback.light(context); showCorrect = false; nextWord(true) },
            onRateApp = { HapticFeedback.light(context); showCorrect = false; rateApp() },
        )
    }
    if (showQuit) {
        com.usharik.app.ui.components.QuitQuizDialog(
            words = todayWords,
            exercises = todayExercises,
            recentWords = recent.reversed(),
            onKeepGoing = { HapticFeedback.light(context); showQuit = false },
            onLeave = { HapticFeedback.light(context); showQuit = false; onQuit() },
            onDismiss = { showQuit = false },
        )
    }
}
