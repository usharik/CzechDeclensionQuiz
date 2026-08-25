package com.usharik.app.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.usharik.app.App
import com.usharik.database.dao.ReminderStateEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DailyReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val app = applicationContext as App
        val today = LocalDate.now().format(DATE_FORMAT)
        val yesterday = LocalDate.now().minusDays(1).format(DATE_FORMAT)
        val state = app.statsRepository.reminderState() ?: ReminderStateEntity()
        if (state.lastNotificationDate == today) return Result.success()
        val yesterdayStats = app.statsRepository.statsForDate(yesterday)
        val active = yesterdayStats?.let { it.wordsCompleted > 0 || it.exercisesCompleted > 0 } == true
        val notify = if (active) {
            state.inactivityStreak = 0
            state.lastActiveDate = yesterday
            true
        } else {
            state.inactivityStreak++
            val effective = state.inactivityStreak.coerceAtMost(MAX_BACKOFF_DAYS)
            effective == MAX_BACKOFF_DAYS || (effective > 0 && effective and (effective - 1) == 0)
        }
        if (notify) {
            app.notificationHelper.showDailyReminder(applicationContext, active, state.inactivityStreak, yesterdayStats?.wordsCompleted ?: 0, yesterdayStats?.exercisesCompleted ?: 0)
            state.lastNotificationDate = today
        }
        app.statsRepository.saveReminderState(state)
        Result.success()
    }.getOrElse { error -> Log.e(TAG, "Worker failed", error); Result.retry() }

    private companion object { val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE; const val MAX_BACKOFF_DAYS = 32; const val TAG = "DailyReminderWorker" }
}
