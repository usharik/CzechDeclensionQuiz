package com.usharik.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.usharik.app.App
import com.usharik.app.BuildConfig
import com.usharik.app.R
import com.usharik.app.TestTags
import com.usharik.app.ui.components.BannerAd
import com.usharik.app.ui.components.CellFeedback
import com.usharik.app.ui.components.DragOverlay
import com.usharik.app.ui.components.DragAndDropState
import com.usharik.app.ui.components.RowCase
import com.usharik.app.ui.components.WordBank
import com.usharik.app.ui.components.WordChip
import com.usharik.app.ui.components.WordModel
import com.usharik.app.ui.components.localizedTranslation
import com.usharik.app.ui.theme.AppColors
import com.usharik.app.ui.theme.Dimens
import com.usharik.database.WordInfo

@Composable
fun DeclensionQuizContent(
    app: App,
    word: WordInfo?,
    models: List<WordModel>,
    dnd: DragAndDropState,
    wordFor: (Int) -> String,
    cellIdx: (Int, Int) -> Int,
    feedback: Map<String, CellFeedback>,
    wrongAttempts: Int,
    maxWrongAttempts: Int,
    actual: List<Int>,
    remainingSeconds: Int,
    totalSeconds: Int,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = Dimens.spacingXxs, vertical = Dimens.spacingXs)) {
            if (word == null) {
                Text("…", Modifier.padding(Dimens.spacingMd))
            } else {
                QuizHeader(word, wrongAttempts, maxWrongAttempts, remainingSeconds, totalSeconds)
                WordBank(
                    models = models,
                    dnd = dnd,
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                )
                HorizontalDivider()
                // All seven case rows share the remaining height so the whole table
                // is always on screen without scrolling.
                Column(
                    Modifier.fillMaxWidth().weight(2f).padding(vertical = Dimens.spacingXs),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingContent),
                ) {
                    for (i in 0..6) {
                        RowCase(
                            num = i,
                            dnd = dnd,
                            singularText = wordFor(actual[cellIdx(0, i)]),
                            pluralText = wordFor(actual[cellIdx(1, i)]),
                            singularFeedback = feedback["0_$i"],
                            pluralFeedback = feedback["1_$i"],
                            fillHeight = true,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                    }
                }
                BannerAd(app, BuildConfig.ADMOB_BANNER_AD_UNIT_ID)
            }
        }
        DragOverlay(dnd) { WordChip(it) }
    }
}

/** Amber used for the "warning" mid-tier of the error/timer color scales; no theme slot fits it. */
private val WarningYellow = Color(0xFFF9A825)
/** Orange used for the "danger" tier before the maximum is reached. */
private val WarningOrange = Color(0xFFE65100)

@Composable
private fun QuizHeader(word: WordInfo, wrongAttempts: Int, maxWrongAttempts: Int, remainingSeconds: Int, totalSeconds: Int) {
    val translation = localizedTranslation(word)
    Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.spacingXxs), verticalAlignment = Alignment.Bottom) {
        Text(word.word(), Modifier.testTag(TestTags.FULL_WORD), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Text(
            stringResource(R.string.declension_pattern, word.declensionType()),
            Modifier.weight(1f).padding(start = Dimens.spacingXs),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
        )
        Text(word.gender(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.spacingXxs, vertical = Dimens.spacingXs),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(translation, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        TimerBadge(remainingSeconds, totalSeconds)
        Text(" ", fontSize = 11.sp)
        ErrorCounter(wrongAttempts, maxWrongAttempts)
    }
}

/** Mistake counter that bounces on each error and is color-coded: green / yellow / red. */
@Composable
private fun ErrorCounter(wrongAttempts: Int, maxWrongAttempts: Int) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(wrongAttempts) {
        if (wrongAttempts == 0) return@LaunchedEffect
        scale.snapTo(1f)
        scale.animateTo(1.3f, tween(125, easing = FastOutSlowInEasing))
        scale.animateTo(1f, tween(125, easing = FastOutSlowInEasing))
    }
    val targetColor = when {
        wrongAttempts <= 0 -> AppColors.correct
        wrongAttempts < 4 -> WarningYellow
        wrongAttempts < 7 -> WarningOrange
        else -> AppColors.incorrect
    }
    val color by animateColorAsState(targetColor, label = "errorCounterColor")
    Text(
        "$wrongAttempts/$maxWrongAttempts",
        Modifier.testTag(TestTags.FULL_ERROR_COUNTER).graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
    )
}

/** Countdown badge color-coded: green(>20s) / yellow(10-20s) / red(<10s). */
@Composable
private fun TimerBadge(remainingSeconds: Int, totalSeconds: Int) {
    val targetColor = when {
        remainingSeconds > totalSeconds * 2 / 3 -> AppColors.correct
        remainingSeconds > totalSeconds / 3 -> WarningYellow
        else -> AppColors.incorrect
    }
    val color by animateColorAsState(targetColor, label = "timerColor")
    Text(
        "${remainingSeconds}s",
        Modifier.testTag(TestTags.FULL_TIMER),
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
    )
}
