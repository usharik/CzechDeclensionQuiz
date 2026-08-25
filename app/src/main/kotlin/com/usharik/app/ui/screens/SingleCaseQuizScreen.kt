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
import com.usharik.app.ui.components.QuitQuizDialog
import com.usharik.app.ui.state.SingleCaseQuizSession
import com.usharik.app.utils.HapticFeedback

/**
 * One-case-at-a-time quiz. Faithful Compose port of SingleCaseQuizFragment +
 * SingleCaseQuizViewModel: four answer buttons per case/number question; answering colors the
 * correct answer green (and a wrong pick red) and unlocks "Next case". Back shows the quit
 * overlay with today's stats. Quiz logic and state live in [SingleCaseQuizSession]; this
 * composable only wires UI concerns (haptics, ads, dialogs).
 */
@Composable
fun SingleCaseQuizScreen(
    app: App,
    onQuit: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val session = remember { SingleCaseQuizSession(app, scope) }
    var showQuit by remember { mutableStateOf(false) }

    fun continueWithPotentialInterstitial(action: () -> Unit) {
        activity?.let { host ->
            app.adManager.showAdIfNeeded(app.adPolicy.onSingleCaseNavigation(), host, BuildConfig.ADMOB_SINGLE_CASE_QUIZ_INTERSTITIAL_AD_UNIT_ID, action)
        } ?: action()
    }

    LaunchedEffect(Unit) {
        activity?.let { app.adManager.loadAd(it, BuildConfig.ADMOB_SINGLE_CASE_QUIZ_INTERSTITIAL_AD_UNIT_ID) }
        session.start()
    }
    BackHandler(enabled = !showQuit) { showQuit = true }

    SingleCaseQuizContent(
        app = app,
        word = session.word,
        caseIndex = session.caseIndex,
        plural = session.plural,
        answers = session.answers,
        correct = session.correct,
        answered = session.answered,
        selectedIndex = session.selectedIndex,
        onAnswer = { index ->
            HapticFeedback.light(context)
            session.selectAnswer(index)?.let { isCorrect ->
                if (isCorrect) HapticFeedback.success(context) else HapticFeedback.error(context)
            }
        },
        onNextCase = {
            HapticFeedback.light(context)
            app.analyticsService.logSingleCaseNavigation("NEXT_CASE", session.word?.word().orEmpty())
            continueWithPotentialInterstitial(session::nextStep)
        },
        onNextWord = {
            HapticFeedback.light(context)
            app.analyticsService.logSingleCaseNavigation("NEXT_WORD", session.word?.word().orEmpty())
            continueWithPotentialInterstitial { session.nextWord() }
        },
    )

    if (showQuit) {
        QuitQuizDialog(
            words = session.progress.todayWords,
            exercises = session.progress.todayExercises,
            recentWords = session.progress.recentWords.reversed(),
            onKeepGoing = { HapticFeedback.light(context); showQuit = false },
            onLeave = { HapticFeedback.light(context); showQuit = false; onQuit() },
            onDismiss = { showQuit = false },
        )
    }
}
