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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usharik.app.R
import com.usharik.app.TestTags
import com.usharik.app.ui.state.FormCell
import com.usharik.app.ui.state.FormRow
import com.usharik.app.ui.state.SectionTitle
import com.usharik.app.ui.theme.AppColors
import com.usharik.app.ui.theme.Dimens

/** A single feedback event on a cell; a fresh instance re-triggers the bounce/shake animation. */
class CellFeedback(val correct: Boolean)

/**
 * One row of a [com.usharik.app.ui.state.FormTable]: for case rows a header line (number, name,
 * hint, question) above two value cells (singular / plural tints); verb rows have no header, the
 * person is shown inside each cell instead. Cells act as drag sources when filled and drop
 * targets. With a null [dnd] the row is a static display (handbook, mistakes page).
 * [text] gives the form currently placed in a cell ("" when empty), [feedback] its last outcome.
 * With [fillHeight] the cells stretch to fill the row's remaining height so a weighted column
 * of rows always fits on one screen without scrolling.
 */
@Composable
fun RowCase(
    row: FormRow,
    dnd: DragAndDropState?,
    text: (FormCell) -> String,
    modifier: Modifier = Modifier,
    feedback: (FormCell) -> CellFeedback? = { null },
    fillHeight: Boolean = false,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.spacingXxs)) {
        row.caseIndex?.let { CaseHeader(it) }
        Row(
            if (fillHeight) Modifier.weight(1f) else Modifier,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXxs),
        ) {
            val cellModifier = if (fillHeight) Modifier.weight(1f).fillMaxHeight() else Modifier.weight(1f)
            row.cells.forEachIndexed { column, cell ->
                AnswerCell(
                    cell = cell,
                    text = text(cell),
                    background = if (column == 0) AppColors.singularCell else AppColors.pluralCell,
                    dnd = dnd,
                    feedback = feedback(cell),
                    fillHeight = fillHeight,
                    modifier = cellModifier,
                )
            }
        }
    }
}

/** The case header line: number, localized case name, helper word and question. */
@Composable
private fun CaseHeader(caseIndex: Int) {
    val names = stringArrayResource(R.array.caseName)
    val hints = stringArrayResource(R.array.caseHint)
    val questions = stringArrayResource(R.array.caseQuestion)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderText("${caseIndex + 1}", Modifier.width(Dimens.caseNumWidth))
        HeaderText(names.getOrElse(caseIndex) { "" }, Modifier.width(Dimens.caseNameWidth))
        HeaderText(hints.getOrElse(caseIndex) { "" }, Modifier.weight(1f))
        HeaderText(questions.getOrElse(caseIndex) { "" }, Modifier.weight(1.4f))
    }
}

/** "Singular | Plural" labels aligned over the two value columns; verb tables label their person columns the same way. */
@Composable
fun CaseColumnsHeader(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXxs)) {
        listOf(R.string.singular, R.string.plural).forEach { label ->
            Text(
                stringResource(label),
                Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/** Title line of a verb table section (present / past / imperative). */
@Composable
fun SectionHeader(title: SectionTitle, modifier: Modifier = Modifier) {
    val res = when (title) {
        SectionTitle.PRESENT -> R.string.section_present
        SectionTitle.PAST -> R.string.section_past
        SectionTitle.IMPERATIVE -> R.string.section_imperative
        SectionTitle.NONE -> return
    }
    Text(
        stringResource(res),
        modifier.padding(start = Dimens.spacingXs, top = Dimens.spacingXxs),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
    )
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
 * A value cell. Shows the fixed prefix/suffix of the cell (person, agreeing noun) in a muted
 * colour around the placed form (draggable when present), over the column colour. A cell whose
 * target does not exist for this word is drawn blank and takes no drops.
 * Correct placements bounce (scale 1→1.2→1); wrong placements shake horizontally.
 */
@Composable
fun AnswerCell(
    cell: FormCell,
    text: String,
    background: Color,
    dnd: DragAndDropState?,
    feedback: CellFeedback?,
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
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
    val active = dnd != null && cell.exists
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale.value; scaleY = scale.value; translationX = shift.value
            }
            .testTag("${TestTags.FULL_CELL_PREFIX}${cell.key}")
            .defaultMinSize(minHeight = 40.dp)
            .clip(shape)
            .background(if (cell.exists) background else background.copy(alpha = 0.35f), shape)
            .border(Dimens.strokeThin, AppColors.stroke, shape)
            .then(if (active) Modifier.dropTarget(dnd, cell.key) else Modifier)
            .then(if (active && occupied) Modifier.dragSource(dnd, cell.key, text) else Modifier)
            .padding(horizontal = Dimens.spacingSm),
        contentAlignment = Alignment.Center,
    ) {
        if (cell.exists && cell.suffix.isNotEmpty()) {
            // Agreement cells: the adjective form on top, the noun it agrees with underneath.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = Dimens.draggableFontSize,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                )
                Text(
                    text = cell.suffix,
                    color = hintColor,
                    fontSize = Dimens.textSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                )
            }
        } else if (cell.exists) {
            Text(
                text = buildAnnotatedString {
                    if (cell.prefix.isNotEmpty()) withStyle(SpanStyle(color = hintColor, fontSize = Dimens.staticFontSize)) { append(cell.prefix); append(' ') }
                    append(text)
                },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = if (cell.prefix.isEmpty()) Dimens.draggableFontSize else 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
