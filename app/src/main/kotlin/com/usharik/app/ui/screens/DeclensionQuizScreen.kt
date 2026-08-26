package com.usharik.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import kotlin.math.roundToInt

/**
 * Full declension-table quiz. Faithful Compose port of DeclensionQuizFragment +
 * DeclensionQuizViewModel: a shuffled pool of forms is dragged into a 7×2 case grid; correct
 * placements bounce and stay, wrong ones shake and return, and completing the table shows the
 * correct-answer dialog. Back shows the quit overlay with today's stats. Quiz logic and state
 * live in [DeclensionQuizSession]; this composable only wires UI concerns (haptics, ads, dialogs).
 *
 * Swiping right opens the handbook as an overlay (without leaving this screen, so the quiz
 * session, timer and error counter keep running untouched); swiping left (or back) closes it.
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
    var showHandbook by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableStateOf(WORD_TIMEOUT_SECONDS) }
    // Horizontal offset of the handbook panel, in px: screenWidthPx (fully off-screen, right) to
    // 0 (fully covering the quiz). Follows the finger while dragging, then springs to whichever
    // side it's closer to on release, so the reveal tracks the swipe gesture in real time.
    var screenWidthPx by remember { mutableIntStateOf(0) }
    val handbookOffsetX = remember { Animatable(0f) }
    LaunchedEffect(screenWidthPx) { if (!showHandbook) handbookOffsetX.snapTo(screenWidthPx.toFloat()) }

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
    // Back closes the handbook overlay first, then falls through to the quit overlay.
    BackHandler(enabled = showHandbook) {
        showHandbook = false
        scope.launch { handbookOffsetX.animateTo(screenWidthPx.toFloat(), spring(stiffness = Spring.StiffnessMediumLow)) }
    }
    BackHandler(enabled = !showHandbook && !showQuit) { showQuit = true }

    fun rateApp() {
        val pkg = context.packageName
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))) }
            .recoverCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))) }
            .onFailure { Toast.makeText(context, R.string.rate_app_unavailable, Toast.LENGTH_SHORT).show() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { screenWidthPx = it.width }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { scope.launch { handbookOffsetX.stop() } },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val next = (handbookOffsetX.value + dragAmount).coerceIn(0f, screenWidthPx.toFloat())
                        scope.launch { handbookOffsetX.snapTo(next) }
                    },
                    onDragEnd = {
                        // Settle to whichever side the panel is closer to, so a partial swipe
                        // still completes the reveal/hide instead of freezing mid-way.
                        val target = if (handbookOffsetX.value < screenWidthPx / 2f) 0f else screenWidthPx.toFloat()
                        showHandbook = target == 0f
                        scope.launch { handbookOffsetX.animateTo(target, spring(stiffness = Spring.StiffnessMediumLow)) }
                    },
                )
            },
    ) {
        // The quiz itself stays composed underneath the overlay, so its session/timer state is
        // never reset while the handbook is shown; only the visible layer changes.
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
        // Opaque surface behind the handbook so the quiz table underneath never shows through
        // while dragging or once fully open.
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(handbookOffsetX.value.roundToInt(), 0) },
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 4.dp,
        ) {
            HandbookScreen(app)
        }
    }

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
