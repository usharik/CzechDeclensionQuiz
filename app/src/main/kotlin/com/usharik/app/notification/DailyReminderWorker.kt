package com.usharik.app.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.usharik.app.App
import com.usharik.app.ui.state.Streak
import com.usharik.app.ui.state.dayScores
import com.usharik.database.dao.ReminderStateEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Runs hourly; [ReminderPlanner] decides whether this run posts a reminder. */
class DailyReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val app = applicationContext as App
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val stats = app.statsRepository
        val days = stats.dayScores()
        val streak = Streak.summarize(days, today)
        val practicedYesterday = days.any { it.date == today.minusDays(1) && Streak.isPracticeDay(it.score) }
        val reviewCount = app.appState.getWordsWithErrors().size
        val plan = ReminderPlanner.plan(
            now = now,
            preferredHour = ReminderPlanner.preferredHour(lastPracticeHour(app)),
            streak = streak,
            practicedYesterday = practicedYesterday,
            reviewCount = reviewCount,
            state = stats.reminderState() ?: ReminderStateEntity(),
        )
        plan.kind?.let { kind ->
            app.notificationHelper.showReminder(applicationContext, kind, streak, reviewCount, plan.state.inactivityStreak)
        }
        stats.saveReminderState(plan.state)
        Result.success()
    }.getOrElse { error -> Log.e(TAG, "Worker failed", error); Result.retry() }

    /** Local hour of the last update on the most recent practice day: when the player tends to play. */
    private suspend fun lastPracticeHour(app: App): Int? {
        val last = app.statsRepository.allStats().lastOrNull { Streak.isPracticeDay(it.score) && it.updatedAt > 0 } ?: return null
        return Instant.ofEpochMilli(last.updatedAt).atZone(ZoneId.systemDefault()).hour
    }

    private companion object { const val TAG = "DailyReminderWorker" }
}
