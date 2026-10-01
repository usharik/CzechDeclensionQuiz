package com.usharik.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usharik.app.R
import com.usharik.app.TestTags
import com.usharik.app.ui.state.DailyGoal
import com.usharik.app.ui.state.DayScore
import com.usharik.app.ui.state.ProgressOverview
import com.usharik.app.ui.state.Streak
import com.usharik.app.ui.theme.AppColors
import java.time.format.TextStyle

/** Flame orange for a streak already extended today; no theme slot fits it. */
private val StreakFlame = Color(0xFFFF7A1A)

/**
 * Hub progress summary: the practice streak, today's goal ring, a 7-day points chart and, when
 * the mistakes list isn't empty, a shortcut into review mode.
 */
@Composable
fun ProgressCard(overview: ProgressOverview, onReview: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier.fillMaxWidth().testTag(TestTags.HUB_PROGRESS_CARD),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StreakBadge(overview.streak, Modifier.weight(1f))
                GoalRing(overview.goal)
            }
            Text(
                statusLine(overview),
                Modifier.fillMaxWidth().padding(top = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            WeekChart(overview.week, overview.goal.target, Modifier.padding(top = 10.dp))
            if (overview.reviewCount > 0) {
                OutlinedModernButton(
                    text = pluralStringResource(R.plurals.progress_review_button, overview.reviewCount, overview.reviewCount),
                    icon = painterResource(R.drawable.ic_star),
                    fontSize = 14.sp,
                    onClick = onReview,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).testTag(TestTags.HUB_REVIEW),
                )
            }
        }
    }
}

@Composable
private fun statusLine(overview: ProgressOverview): String {
    val goal = overview.goal
    val streak = overview.streak
    return when {
        goal.isReached -> stringResource(R.string.progress_status_goal_reached)
        streak.todayCounts -> pluralStringResource(R.plurals.progress_status_streak_kept, goal.remaining, goal.remaining)
        streak.current > 0 -> pluralStringResource(R.plurals.progress_status_keep_streak, streak.pointsToKeep, streak.pointsToKeep)
        else -> pluralStringResource(R.plurals.progress_status_start_streak, streak.pointsToKeep, streak.pointsToKeep)
    }
}

@Composable
private fun StreakBadge(streak: Streak.Summary, modifier: Modifier) {
    Row(modifier.testTag(TestTags.HUB_STREAK), verticalAlignment = Alignment.CenterVertically) {
        // Dimmed until today counts: the flame "lights up" once the streak is safe for the day.
        Text("🔥", Modifier.alpha(if (streak.todayCounts) 1f else 0.4f), fontSize = 34.sp)
        Column(Modifier.padding(start = 8.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${streak.current}",
                    color = if (streak.todayCounts) StreakFlame else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                )
                Text(
                    pluralStringResource(R.plurals.progress_streak_days, streak.current),
                    Modifier.padding(start = 6.dp, bottom = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                )
            }
            if (streak.best > 0) {
                Text(
                    stringResource(R.string.progress_best_streak, streak.best),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun GoalRing(goal: DailyGoal.Progress) {
    val fraction by animateFloatAsState(goal.fraction, label = "goalRing")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxSize(),
                color = if (goal.isReached) AppColors.correct else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                strokeWidth = 6.dp,
                strokeCap = StrokeCap.Round,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${goal.completed}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("/${goal.target}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
        Text(
            stringResource(R.string.progress_goal_label),
            Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
    }
}

/** Points per day for the last week; a full bar means that day's goal was reached. */
@Composable
private fun WeekChart(week: List<DayScore>, target: Int, modifier: Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        week.forEachIndexed { index, day ->
            val isToday = index == week.lastIndex
            val fill = if (target <= 0) 0f else (day.score.toFloat() / target).coerceIn(0f, 1f)
            val color = when {
                day.score >= target -> AppColors.correct
                Streak.isPracticeDay(day.score) -> primary
                day.score > 0 -> primary.copy(alpha = 0.45f)
                else -> muted.copy(alpha = 0.2f)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.height(BAR_HEIGHT), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .width(14.dp)
                            .height(maxOf(BAR_HEIGHT * fill, 4.dp))
                            .background(color, RoundedCornerShape(4.dp)),
                    )
                }
                Text(
                    day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                    Modifier.padding(top = 4.dp),
                    color = if (isToday) primary else muted,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

private val BAR_HEIGHT = 28.dp
