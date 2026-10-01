package com.usharik.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.usharik.app.App
import com.usharik.app.BuildConfig
import com.usharik.app.R
import com.usharik.app.TestTags
import com.usharik.app.ui.components.BannerAd
import com.usharik.app.ui.components.ButtonFrame
import com.usharik.app.ui.components.GradientButton
import com.usharik.app.ui.components.StrokeTextButton
import com.usharik.app.ui.components.localizedTranslation
import com.usharik.app.ui.theme.AppColors
import com.usharik.app.ui.theme.Dimens
import com.usharik.database.WordInfo

/** Scrollable word header, the case question and four answer choices. */
@Composable
fun SingleCaseQuizContent(
    app: App,
    word: WordInfo?,
    caseIndex: Int,
    plural: Boolean,
    answers: List<String>,
    correct: String,
    answered: Boolean,
    selectedIndex: Int,
    isAdvancing: Boolean,
    onAnswer: (Int) -> Unit,
    onNextCase: () -> Unit,
    onNextWord: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().testTag(TestTags.SC_SCREEN).verticalScroll(rememberScrollState()).padding(Dimens.spacingMd),
    ) {
        if (word == null) {
            LoadingIndicator(Modifier.padding(top = Dimens.spacingXl))
            return@Column
        }
        // The localized case-name/hint/question arrays match what RowCase renders.
        val translation = localizedTranslation(word)
        val caseName = stringArrayResource(R.array.caseName).getOrElse(caseIndex) { "" }
        val caseHint = stringArrayResource(R.array.caseHint).getOrElse(caseIndex) { "-" }
        val question = stringArrayResource(R.array.caseQuestion).getOrElse(caseIndex) { "" }
        val caseQuestion = if (caseHint.isBlank() || caseHint == "-") question else "$caseHint - $question"

        Text(
            word.word(),
            Modifier.fillMaxWidth().testTag(TestTags.SC_WORD).padding(top = Dimens.spacingMd),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = Dimens.textHeading,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.declension_pattern, word.declensionType()),
            Modifier.fillMaxWidth().padding(top = Dimens.spacingXs),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = Dimens.textLabel,
            textAlign = TextAlign.Center,
        )
        Text(
            translation,
            Modifier.fillMaxWidth().padding(top = Dimens.spacingXs),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = Dimens.textBody,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
        )
        Row(
            Modifier.fillMaxWidth().padding(top = Dimens.spacingLg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${caseIndex + 1}. $caseName",
                Modifier.testTag(TestTags.SC_CASE_NAME),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = Dimens.textTitle,
            )
            Text(
                stringResource(if (plural) R.string.plural else R.string.singular),
                Modifier.testTag(TestTags.SC_NUMBER_LABEL),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = Dimens.textBody,
            )
        }
        Text(
            caseQuestion,
            Modifier.fillMaxWidth().testTag(TestTags.SC_QUESTION).padding(top = Dimens.spacingXs),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = Dimens.textBody,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
        )
        for (i in 0 until 4) {
            if (i >= answers.size) continue
            val answer = answers[i]
            AnswerButton(
                text = answer,
                state = when {
                    answered && answer == correct -> AnswerState.CORRECT
                    answered && i == selectedIndex -> AnswerState.INCORRECT
                    else -> AnswerState.NEUTRAL
                },
                pulse = answered && i == selectedIndex,
                enabled = !answered,
                onClick = { onAnswer(i) },
                modifier = Modifier.fillMaxWidth().testTag("${TestTags.SC_ANSWER_PREFIX}$i").padding(top = if (i == 0) Dimens.spacingLg else Dimens.spacingSm),
            )
        }
        GradientButton(
            text = stringResource(R.string.nextCase),
            gradient = AppColors.gradientSecondary,
            enabled = answered && !isAdvancing,
            onClick = onNextCase,
            modifier = Modifier.fillMaxWidth().testTag(TestTags.SC_NEXT_CASE).padding(top = Dimens.spacingLg),
        )
        // Skipping costs a point, so it is a low-emphasis action under the main "Next case".
        StrokeTextButton(
            text = stringResource(R.string.next_word),
            icon = painterResource(R.drawable.ic_arrow_forward),
            enabled = !isAdvancing,
            onClick = onNextWord,
            modifier = Modifier.fillMaxWidth().testTag(TestTags.SC_NEXT_WORD).padding(top = Dimens.spacingSm),
        )
        Spacer(Modifier.height(Dimens.spacingMd))
        BannerAd(app, BuildConfig.ADMOB_SINGLE_CASE_QUIZ_AD_UNIT_ID)
    }
}

private enum class AnswerState { NEUTRAL, CORRECT, INCORRECT }

/**
 * One answer choice: an outlined button whose fill animates to green/red over 250 ms once the
 * question is answered, plus a 1→1.06→1 scale pulse on the tapped button.
 */
@Composable
private fun AnswerButton(
    text: String,
    state: AnswerState,
    pulse: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Dimens.cornerButton)
    val target = when (state) {
        AnswerState.CORRECT -> AppColors.correct
        AnswerState.INCORRECT -> AppColors.incorrect
        AnswerState.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
    }
    val background by animateColorAsState(target, tween(250), label = "answerTint")
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pulse) {
        if (!pulse) return@LaunchedEffect
        scale.animateTo(1.06f, tween(100, easing = FastOutSlowInEasing))
        scale.animateTo(1f, tween(100, easing = FastOutSlowInEasing))
    }
    val textColor by animateColorAsState(
        if (state == AnswerState.NEUTRAL) AppColors.outlineStroke else AppColors.textOnGradient,
        tween(250),
        label = "answerText",
    )
    // Answered buttons stay fully opaque: they're disabled only to block a second pick.
    ButtonFrame(
        text = text,
        contentColor = textColor,
        modifier = modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        decoration = Modifier
            .background(background, shape)
            .border(BorderStroke(2.dp, if (state == AnswerState.NEUTRAL) AppColors.outlineStroke else background), shape),
        enabled = enabled,
        disabledAlpha = 1f,
        onClick = onClick,
    )
}
