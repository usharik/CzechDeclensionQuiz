package com.usharik.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.usharik.app.App
import com.usharik.app.BuildConfig
import com.usharik.app.TestTags
import com.usharik.app.ui.components.BannerAd
import com.usharik.app.ui.components.CaseColumnsHeader
import com.usharik.app.ui.components.CellFeedback
import com.usharik.app.ui.components.DragAndDropState
import com.usharik.app.ui.components.DragOverlay
import com.usharik.app.ui.components.RowCase
import com.usharik.app.ui.components.SectionHeader
import com.usharik.app.ui.components.WordBank
import com.usharik.app.ui.components.WordChip
import com.usharik.app.ui.components.WordModel
import com.usharik.app.ui.components.lexemeSubtitle
import com.usharik.app.ui.components.localizedTranslation
import com.usharik.app.ui.state.FormCell
import com.usharik.app.ui.state.FormTable
import com.usharik.app.ui.state.Lexeme
import com.usharik.app.ui.state.SectionTitle
import com.usharik.app.ui.theme.AppColors
import com.usharik.app.ui.theme.Dimens

/**
 * Full-table quiz content: the headword card, the shuffled word pool and the drop grid built
 * from [table] (7 case rows for nouns and adjectives, three verb sections for a verb).
 */
@Composable
fun DeclensionQuizContent(
    app: App,
    table: FormTable?,
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
        Column(Modifier.fillMaxSize().padding(horizontal = Dimens.spacingSm, vertical = Dimens.spacingXs)) {
            if (table == null) {
                LoadingIndicator(Modifier.weight(1f))
            } else {
                QuizHeader(table.lexeme, wrongAttempts, maxWrongAttempts, remainingSeconds, totalSeconds)
                WordBank(
                    models = models,
                    dnd = dnd,
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                // All rows share the remaining height so the whole table is always on screen
                // without scrolling. Section titles (verbs) take their natural height.
                Column(
                    Modifier.fillMaxWidth().weight(2f).padding(vertical = Dimens.spacingXs),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingContent),
                ) {
                    CaseColumnsHeader(table.columnKind, Modifier.fillMaxWidth())
                    fun placed(cell: FormCell) = wordFor(actual.getOrElse(cellIdx(cell.column, cell.row)) { -1 })
                    // A section none of whose cells exist (no imperative for modal verbs) is left out entirely.
                    table.sections.filter { s -> s.rows.any { r -> r.cells.any { it.exists } } }.forEach { section ->
                        if (section.title != SectionTitle.NONE) SectionHeader(section.title)
                        section.rows.forEach { row ->
                            RowCase(
                                row = row,
                                dnd = dnd,
                                text = ::placed,
                                feedback = { feedback[it.key] },
                                fillHeight = true,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            )
                        }
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

/** Headword, grammar line and translation on the left; timer and mistake badges on the right. */
@Composable
private fun QuizHeader(lexeme: Lexeme, wrongAttempts: Int, maxWrongAttempts: Int, remainingSeconds: Int, totalSeconds: Int) {
    val translation = localizedTranslation(lexeme)
    Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.spacingXs), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(lexeme.headword, Modifier.testTag(TestTags.FULL_WORD), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                lexemeSubtitle(lexeme),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(translation, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontStyle = FontStyle.Italic, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Dimens.spacingXs)) {
            TimerBadge(remainingSeconds, totalSeconds)
            ErrorCounter(wrongAttempts, maxWrongAttempts)
        }
    }
}

/** Small tinted pill used for the timer and the mistake counter; [valueModifier] tags the value text. */
@Composable
private fun Badge(symbol: String, value: String, color: Color, modifier: Modifier = Modifier, valueModifier: Modifier = Modifier) {
    Row(
        modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = Dimens.spacingSm, vertical = Dimens.spacingXxs),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(symbol, color = color, fontSize = 12.sp)
        Text(value, valueModifier, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
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
    Badge(
        "✕",
        "$wrongAttempts/$maxWrongAttempts",
        color,
        Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        Modifier.testTag(TestTags.FULL_ERROR_COUNTER),
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
    Badge("⏱", "%d:%02d".format(remainingSeconds / 60, remainingSeconds % 60), color, valueModifier = Modifier.testTag(TestTags.FULL_TIMER))
}
