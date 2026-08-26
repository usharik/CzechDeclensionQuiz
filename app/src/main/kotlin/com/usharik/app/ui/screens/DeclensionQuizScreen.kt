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
import com.usharik.app.ui.components.CorrectAnswerDialog
import com.usharik.app.ui.components.QuitQuizDialog
import com.usharik.app.ui.components.rememberDragAndDropState
import com.usharik.app.ui.state.DeclensionQuizSession
import com.usharik.app.ui.state.DeclensionQuizSession.DropOutcome
import com.usharik.app.utils.HapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full declension-table quiz. Faithful Compose port of DeclensionQuizFragment +
 * DeclensionQuizViewModel: a shuffled pool of forms is dragged into a 7×2 case grid; correct
 * placements bounce and stay, wrong ones shake and return, and completing the table shows the
 * correct-answer dialog. Back shows the quit overlay with today's stats. Quiz logic and state
 * live in [DeclensionQuizSession]; this composable only wires UI concerns (haptics, ads, dialogs).
 */
/** Per-word time budget (seconds) before an ad is shown if the table isn't completed yet. */
private const val WORD_TIMEOUT_SECONDS = 90

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
    val session = remember { DeclensionQuizSession(app, scope) }
    var showCorrect by remember { mutableStateOf(false) }
    var showQuit by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableStateOf(WORD_TIMEOUT_SECONDS) }

    fun wrongAnswerAd() {
        activity?.let { host -> scope.launch { delay(600); app.adManager.showAdIfNeeded(app.adPolicy.onDeclensionWrongAnswer(), host, BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID) {} } }
    }

    SideEffect {
        dnd.onDrop = { tag, tgt ->
            when (session.handleDrop(tag, tgt)) {
                DropOutcome.CORRECT -> HapticFeedback.success(context)
                DropOutcome.COMPLETED -> {
                    HapticFeedback.success(context)
                    scope.launch { delay(300); showCorrect = true }
                }
                DropOutcome.WRONG -> {
                    HapticFeedback.error(context)
                    wrongAnswerAd()
                }
                DropOutcome.IGNORED -> Unit
            }
        }
    }

    LaunchedEffect(Unit) {
        activity?.let { app.adManager.loadAd(it, BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID) }
        session.start()
    }

    // 30s per-word countdown: if the player hasn't completed the table in time, show an ad. Any
    // fresh word or a completed table bumps timerResetToken, restarting the countdown.
    LaunchedEffect(session.timerResetToken) {
        remainingSeconds = WORD_TIMEOUT_SECONDS
        while (remainingSeconds > 0) {
            delay(1_000)
            remainingSeconds--
        }
        activity?.let { host ->
            app.adManager.showAdIfNeeded(app.adPolicy.onDeclensionTimeout(), host, BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID) {}
        }
    }
    DisposableEffect(Unit) {
        registerNext { session.nextWord() }
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
        word = session.word,
        models = session.models,
        dnd = dnd,
        wordFor = session::wordFor,
        cellIdx = session::cellIdx,
        feedback = session.feedback,
        wrongAttempts = session.wrongAttempts,
        actual = session.actual,
        remainingSeconds = remainingSeconds,
        totalSeconds = WORD_TIMEOUT_SECONDS,
    )

    if (showCorrect) {
        CorrectAnswerDialog(
            onNextWord = { HapticFeedback.light(context); showCorrect = false; activity?.let { host -> app.adManager.showAdIfNeeded(app.adPolicy.onDeclensionWordCompleted(), host, BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID) { session.nextWord() } } ?: session.nextWord() },
            onStayHere = { HapticFeedback.light(context); showCorrect = false },
            onTryAgain = { HapticFeedback.light(context); showCorrect = false; session.nextWord(tryAgain = true) },
            onRateApp = { HapticFeedback.light(context); showCorrect = false; rateApp() },
        )
    }
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
