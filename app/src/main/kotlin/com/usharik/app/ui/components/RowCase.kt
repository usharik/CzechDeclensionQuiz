package com.usharik.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.usharik.app.R
import com.usharik.app.TestTags
import com.usharik.app.ui.theme.AppColors
import com.usharik.app.ui.theme.Dimens

/** A single feedback event on a cell; a fresh instance re-triggers the bounce/shake animation. */
class CellFeedback(val correct: Boolean)

/**
 * One case row mirroring row_case.xml: a header line (number, name, hint, question) above two
 * value cells (singular = blue, plural = red) that act as drag sources when filled and drop
 * targets. With a null [dnd] the row is a static display (handbook table).
 * With [fillHeight] the cells stretch to fill the row's remaining height so a weighted column
 * of rows always fits on one screen without scrolling.
 */
@Composable
fun RowCase(
    num: Int,
    dnd: DragAndDropState?,
    singularText: String,
    pluralText: String,
    singularFeedback: CellFeedback? = null,
    pluralFeedback: CellFeedback? = null,
    fillHeight: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val names = stringArrayResource(R.array.caseName)
    val hints = stringArrayResource(R.array.caseHint)
    val questions = stringArrayResource(R.array.caseQuestion)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.spacingXxs)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderText("${num + 1}", Modifier.width(Dimens.caseNumWidth))
            HeaderText(names.getOrElse(num) { "" }, Modifier.width(Dimens.caseNameWidth))
            HeaderText(hints.getOrElse(num) { "" }, Modifier.weight(1f))
            HeaderText(questions.getOrElse(num) { "" }, Modifier.weight(1.4f))
        }
        Row(
            if (fillHeight) Modifier.weight(1f) else Modifier,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXxs),
        ) {
            val cellModifier = if (fillHeight) Modifier.weight(1f).fillMaxHeight() else Modifier.weight(1f)
            AnswerCell("0_$num", singularText, AppColors.answerCorrect, dnd, singularFeedback, fillHeight, cellModifier)
            AnswerCell("1_$num", pluralText, AppColors.answerIncorrect, dnd, pluralFeedback, fillHeight, cellModifier)
        }
    }
}

/** Tight single-line text: no extra font padding so the header claims minimal row height. */
@Composable
private fun HeaderText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        fontSize = Dimens.staticFontSize,
        maxLines = 1,
        style = TextStyle(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeight = Dimens.staticFontSize,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
        ),
    )
}

/**
 * A value cell. Shows the placed word (draggable when present) or nothing, over the column colour.
 * Correct placements bounce (scale 1→1.2→1); wrong placements shake horizontally — matching the
 * ObjectAnimator animations in DeclensionQuizFragment.
 */
@Composable
fun AnswerCell(
    cellKey: String,
    text: String,
    background: Color,
    dnd: DragAndDropState?,
    feedback: CellFeedback?,
    fillHeight: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Dimens.cornerLarge)
    val scale = remember { Animatable(1f) }
    val shift = remember { Animatable(0f) }
    LaunchedEffect(feedback) {
        if (feedback == null) return@LaunchedEffect
        if (feedback.correct) {
            scale.snapTo(1f)
            scale.animateTo(1.2f, tween(150, easing = FastOutSlowInEasing))
            scale.animateTo(1f, tween(150, easing = FastOutSlowInEasing))
        } else {
            shift.snapTo(0f)
            shift.animateTo(
                0f,
                keyframes {
                    durationMillis = 500
                    25f at 50; (-25f) at 100; 25f at 150; (-25f) at 200
                    15f at 300; (-15f) at 350; 6f at 420; (-6f) at 470
                },
            )
        }
    }
    val occupied = text.isNotEmpty()
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale.value; scaleY = scale.value; translationX = shift.value
            }
            .testTag("${TestTags.FULL_CELL_PREFIX}$cellKey")
            .defaultMinSize(minHeight = 40.dp)
            .clip(shape)
            .background(background, shape)
            .border(Dimens.strokeThin, AppColors.stroke, shape)
            .then(if (dnd != null) Modifier.dropTarget(dnd, cellKey) else Modifier)
            .then(if (dnd != null && occupied) Modifier.dragSource(dnd, cellKey, text) else Modifier)
            .padding(horizontal = Dimens.spacingSm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = Dimens.draggableFontSize,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
