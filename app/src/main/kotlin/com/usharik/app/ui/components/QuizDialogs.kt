package com.usharik.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.usharik.app.R
import com.usharik.app.TestTags
import com.usharik.app.ui.state.DailyGoal
import com.usharik.app.ui.theme.AppColors

/**
 * Shared frame of the quiz dialogs: a 28dp-rounded surface with a centered title, an optional
 * message and the dialog body. [testTag] is exposed as a resource id for the UI tests.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun QuizDialogFrame(
    title: String,
    testTag: String,
    onDismissRequest: () -> Unit,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    message: String? = null,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(dismissOnBackPress = dismissible, dismissOnClickOutside = dismissible)) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
            // Dialogs are separate windows, so the resource-id flag must be re-enabled for their tree.
            Column(Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true }.testTag(testTag).padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp)) {
                Text(title, Modifier.fillMaxWidth().padding(bottom = if (message != null) 6.dp else 20.dp), color = titleColor, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center)
                if (message != null) {
                    Text(message, Modifier.fillMaxWidth().padding(bottom = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
                content()
            }
        }
    }
}

/** Non-cancelable dialog shown when the table is complete. */
@Composable
fun CorrectAnswerDialog(
    dailyGoal: DailyGoal.Progress,
    onNextWord: () -> Unit,
    onStayHere: () -> Unit,
    onTryAgain: () -> Unit,
    onRateApp: () -> Unit,
) {
    QuizDialogFrame(stringResource(R.string.correct_answer), TestTags.FULL_COMPLETION_DIALOG, onDismissRequest = {}, titleColor = AppColors.successText, dismissible = false) {
        DailyGoalProgress(dailyGoal, Modifier.testTag(TestTags.FULL_DIALOG_DAILY_GOAL))
        GradientButton(stringResource(R.string.next_word), AppColors.gradientPrimary, Modifier.fillMaxWidth().testTag(TestTags.FULL_DIALOG_NEXT_WORD).padding(bottom = 12.dp)) { onNextWord() }
        OutlinedModernButton(stringResource(R.string.stay_here), Modifier.fillMaxWidth().testTag(TestTags.FULL_DIALOG_STAY_HERE).padding(bottom = 12.dp)) { onStayHere() }
        OutlinedModernButton(stringResource(R.string.try_again), Modifier.fillMaxWidth().testTag(TestTags.FULL_DIALOG_TRY_AGAIN)) { onTryAgain() }
        StrokeTextButton(stringResource(R.string.rate_app), Modifier.fillMaxWidth(), icon = painterResource(R.drawable.ic_star)) { onRateApp() }
    }
}

/** Quit-quiz overlay: today's stats, recent-word chips and two actions. */
@Composable
fun QuitQuizDialog(
    words: Int,
    exercises: Int,
    score: Int,
    recentWords: List<String>,
    dailyGoal: DailyGoal.Progress,
    onKeepGoing: () -> Unit,
    onLeave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message = if (dailyGoal.isOneWordAway) stringResource(R.string.quit_quiz_daily_goal_one_away) else stringResource(R.string.quit_quiz_message)
    QuizDialogFrame(stringResource(R.string.quit_quiz_title), TestTags.FULL_QUIT_DIALOG, onDismissRequest = onDismiss, message = message) {
        StatsCard(words, exercises, score)
        DailyGoalProgress(dailyGoal, Modifier.testTag(TestTags.FULL_QUIT_DAILY_GOAL))
        if (recentWords.isNotEmpty()) {
            Text(stringResource(R.string.quit_quiz_recent_words_label), Modifier.fillMaxWidth().padding(bottom = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
            Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                recentWords.take(3).forEach { WordChip(it) }
            }
        }
        GradientButton(stringResource(R.string.quit_quiz_keep_going), AppColors.gradientPrimary, Modifier.fillMaxWidth().padding(bottom = 12.dp)) { onKeepGoing() }
        OutlinedModernButton(stringResource(R.string.quit_quiz_leave), Modifier.fillMaxWidth().testTag(TestTags.FULL_QUIT_LEAVE).padding(bottom = 12.dp)) { onLeave() }
    }
}

/** Shown when review mode has cleared every word from the mistakes list. */
@Composable
fun ReviewCompleteDialog(dailyGoal: DailyGoal.Progress, onBackToMenu: () -> Unit) {
    QuizDialogFrame(
        stringResource(R.string.review_complete_title),
        TestTags.REVIEW_COMPLETE_DIALOG,
        onDismissRequest = onBackToMenu,
        titleColor = AppColors.successText,
        message = stringResource(R.string.review_complete_message),
    ) {
        DailyGoalProgress(dailyGoal)
        GradientButton(stringResource(R.string.review_complete_back), AppColors.gradientPrimary, Modifier.fillMaxWidth().padding(bottom = 12.dp)) { onBackToMenu() }
    }
}

@Composable
private fun StatsCard(words: Int, exercises: Int, score: Int) {
    Surface(
        Modifier.fillMaxWidth().padding(bottom = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            StatColumn(score, stringResource(R.string.quit_quiz_points_label), Modifier.weight(1f).testTag(TestTags.FULL_QUIT_SCORE))
            Box(Modifier.width(1.dp).height(56.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)))
            StatColumn(words, stringResource(R.string.quit_quiz_words_label), Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(56.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)))
            StatColumn(exercises, stringResource(R.string.quit_quiz_exercises_label), Modifier.weight(1f).testTag(TestTags.FULL_QUIT_EXERCISES))
        }
    }
}

@Composable
private fun DailyGoalProgress(goal: DailyGoal.Progress, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Text(
            if (goal.isReached) stringResource(R.string.quit_quiz_daily_goal_reached)
            else stringResource(R.string.quit_quiz_daily_goal_progress, goal.completed, goal.target),
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        LinearProgressIndicator(
            progress = { goal.fraction },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
        )
    }
}

@Composable
private fun StatColumn(value: Int, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 40.sp)
        Text(label, Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 15.sp, textAlign = TextAlign.Center)
    }
}
