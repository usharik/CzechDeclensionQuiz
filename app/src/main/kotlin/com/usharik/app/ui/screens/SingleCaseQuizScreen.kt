package com.usharik.app.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.usharik.app.App
import com.usharik.app.BuildConfig
import com.usharik.app.CzechCase
import com.usharik.app.service.LastWordStore
import com.usharik.app.ui.components.QuitQuizDialog
import com.usharik.app.utils.HapticFeedback
import com.usharik.database.WordInfo
import kotlinx.coroutines.launch

/**
 * One-case-at-a-time quiz. Faithful Compose port of SingleCaseQuizFragment +
 * SingleCaseQuizViewModel: four answer buttons per case/number question; answering colors the
 * correct answer green (and a wrong pick red) and unlocks "Next case". Back shows the quit
 * overlay with today's stats.
 */
@Composable
fun SingleCaseQuizScreen(
    app: App,
    onQuit: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var word by remember { mutableStateOf<WordInfo?>(null) }
    var caseIndex by remember { mutableStateOf(0) }
    var plural by remember { mutableStateOf(false) }
    var answers by remember { mutableStateOf<List<String>>(emptyList()) }
    var correct by remember { mutableStateOf("") }
    var answered by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(-1) }
    var showQuit by remember { mutableStateOf(false) }
    var recent by remember { mutableStateOf<List<String>>(emptyList()) } // oldest→newest
    var todayWords by remember { mutableStateOf(0) }
    var todayExercises by remember { mutableStateOf(0) }

    // Mirrors SingleCaseQuizViewModel.buildAnswers: unique distractors from all forms of the
    // current word, shuffled, then the correct answer mixed in with up to three of them.
    fun buildAnswers(w: WordInfo, correctAnswer: String): List<String> {
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

    fun prepareQuestion() {
        val w = word ?: run { correct = ""; answers = emptyList(); answered = false; return }
        correct = w.cases(if (plural) 1 else 0, caseIndex)
        answers = buildAnswers(w, correct)
        answered = false
        selectedIndex = -1
    }

    // Advances caseIndex/plural to the next position; false once the word is exhausted.
    fun advance(): Boolean {
        when {
            caseIndex < 6 -> caseIndex++
            !plural -> { plural = true; caseIndex = 0 }
            else -> return false
        }
        return true
    }

    // Entries can have intentionally empty forms (e.g. plural-only words); those are skipped
    // instead of being presented as blank answers.
    fun currentFormIsEmpty(w: WordInfo) = w.cases(if (plural) 1 else 0, caseIndex).isEmpty()

    fun resetToFirstQuestion(w: WordInfo) {
        caseIndex = 0; plural = false
        while (currentFormIsEmpty(w)) { if (!advance()) break }
        prepareQuestion()
    }

    suspend fun refreshStats() {
        val s = app.statsRepository.todayStats()
        todayWords = s?.wordsCompleted ?: 0
        todayExercises = s?.exercisesCompleted ?: 0
    }

    fun applyWord(w: WordInfo, countStats: Boolean) {
        word = w
        app.lastWordStore.saveLastWord(LastWordStore.MODE_SINGLE_CASE, w.word())
        resetToFirstQuestion(w)
        if (countStats) {
            recent = (recent - w.word()) + w.word()
            if (recent.size > 3) recent = recent.takeLast(3)
            scope.launch { app.statsRepository.saveRecentWords(recent); app.statsRepository.incrementWordsCompleted(); refreshStats() }
        }
    }

    fun nextWord(tryAgain: Boolean) {
        val cur = word
        if (tryAgain && cur != null) { resetToFirstQuestion(cur); return }
        scope.launch { applyWord(app.wordService.nextWord(cur), true) }
    }

    // Each answered case = one exercise (visible increment in the quit dialog).
    fun nextStep() {
        scope.launch { app.statsRepository.incrementExercisesCompleted(); refreshStats() }
        val w = word ?: return
        var more = advance()
        while (more && currentFormIsEmpty(w)) more = advance()
        if (!more) { nextWord(false); return }
        prepareQuestion()
    }

    fun onAnswerSelected(index: Int) {
        if (answered || index >= answers.size) return
        val selected = answers[index]
        val isCorrect = selected == correct
        if (isCorrect) HapticFeedback.success(context) else HapticFeedback.error(context)
        if (!isCorrect) scope.launch { app.statsRepository.incrementErrorsCount() }
        app.analyticsService.logSingleCaseAnswer(
            isCorrect, selected, correct,
            word?.word().orEmpty(), CzechCase.fromIndex(caseIndex).displayName,
        )
        selectedIndex = index
        answered = true
    }

    fun continueWithPotentialInterstitial(action: () -> Unit) {
        activity?.let { host ->
            app.adManager.showAdIfNeeded(app.adPolicy.onSingleCaseNavigation(), host, BuildConfig.ADMOB_SINGLE_CASE_QUIZ_INTERSTITIAL_AD_UNIT_ID, action)
        } ?: action()
    }

    LaunchedEffect(Unit) {
        activity?.let { app.adManager.loadAd(it, BuildConfig.ADMOB_SINGLE_CASE_QUIZ_INTERSTITIAL_AD_UNIT_ID) }
        app.dictionaryReady.await()
        recent = app.statsRepository.recentWords()
        refreshStats()
        if (word == null) {
            val saved = app.lastWordStore.getLastWord(LastWordStore.MODE_SINGLE_CASE)
            val restored = saved?.takeIf { it.isNotBlank() }?.let { app.wordService.wordByName(it) }
            if (restored != null) applyWord(restored, false) else nextWord(false)
        }
    }
    BackHandler(enabled = !showQuit) { showQuit = true }

    SingleCaseQuizContent(
        app = app,
        word = word,
        caseIndex = caseIndex,
        plural = plural,
        answers = answers,
        correct = correct,
        answered = answered,
        selectedIndex = selectedIndex,
        onAnswer = { HapticFeedback.light(context); onAnswerSelected(it) },
        onNextCase = {
            HapticFeedback.light(context)
            app.analyticsService.logSingleCaseNavigation("NEXT_CASE", word?.word().orEmpty())
            continueWithPotentialInterstitial(::nextStep)
        },
        onNextWord = {
            HapticFeedback.light(context)
            app.analyticsService.logSingleCaseNavigation("NEXT_WORD", word?.word().orEmpty())
            continueWithPotentialInterstitial { nextWord(false) }
        },
    )

    if (showQuit) {
        QuitQuizDialog(
            words = todayWords,
            exercises = todayExercises,
            recentWords = recent.reversed(),
            onKeepGoing = { HapticFeedback.light(context); showQuit = false },
            onLeave = { HapticFeedback.light(context); showQuit = false; onQuit() },
            onDismiss = { showQuit = false },
        )
    }
}
