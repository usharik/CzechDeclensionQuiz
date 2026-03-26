package com.usharik.app.notification;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.usharik.database.TrainingStatsRepository;
import com.usharik.database.dao.DailyTrainingStatsEntity;
import com.usharik.database.dao.DocumentDatabase;
import com.usharik.database.dao.ReminderStateEntity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Runs daily (via WorkManager) to decide whether to show a reminder.
 *
 * Logic:
 *  - If user was active yesterday → show notification, reset inactivityStreak.
 *  - If user is inactive → increment inactivityStreak, show notification only
 *    on days that are a power-of-2 multiple of the streak start
 *    (day 1, 2, 4, 8, 16 … capped at 32 days).
 *  - Never show more than one notification per calendar day.
 */
public class DailyReminderWorker extends Worker {

    private static final String TAG = "DailyReminderWorker";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** Maximum gap in days between reminders for long-inactive users. */
    private static final int MAX_BACKOFF_DAYS = 32;

    public DailyReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Context context = getApplicationContext();

            DocumentDatabase db = DocumentDatabase.getDocumentDatabase(context);
            TrainingStatsRepository repo = new TrainingStatsRepository(db);

            String today = LocalDate.now().format(DATE_FMT);
            String yesterday = LocalDate.now().minusDays(1).format(DATE_FMT);

            // Load reminder state (create default if missing)
            ReminderStateEntity state = repo.getReminderState().blockingGet();
            if (state == null) {
                state = new ReminderStateEntity();
            }

            // Guard: never send more than one notification per day.
            if (today.equals(state.lastNotificationDate)) {
                Log.d(TAG, "Notification already sent today, skipping.");
                return Result.success();
            }

            DailyTrainingStatsEntity yesterdayStats = repo.getStatsForDateBlocking(yesterday);
            boolean wasActiveYesterday = yesterdayStats != null
                    && (yesterdayStats.wordsCompleted > 0 || yesterdayStats.exercisesCompleted > 0);

            boolean shouldNotify;
            if (wasActiveYesterday) {
                state.inactivityStreak = 0;
                state.lastActiveDate = yesterday;
                shouldNotify = true;
            } else {
                state.inactivityStreak++;
                // Exponential backoff: notify on streak = 1, 2, 4, 8, 16, 32, 32, 32, ...
                int effectiveStreak = Math.min(state.inactivityStreak, MAX_BACKOFF_DAYS);
                shouldNotify = isPowerOfTwo(effectiveStreak)
                        || effectiveStreak >= MAX_BACKOFF_DAYS;
            }

            if (shouldNotify) {
                NotificationHelper.showDailyReminder(context, wasActiveYesterday);
                state.lastNotificationDate = today;
                logAnalyticsEvent(context, state, yesterdayStats);
            }

            repo.saveReminderStateBlocking(state);
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Worker failed", e);
            return Result.failure();
        }
    }

    private static boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }

    private static void logAnalyticsEvent(Context context,
                                          ReminderStateEntity state,
                                          DailyTrainingStatsEntity stats) {
        try {
            Bundle bundle = new Bundle();
            bundle.putInt("inactivity_streak", state.inactivityStreak);
            if (stats != null) {
                bundle.putInt("words_completed_yesterday", stats.wordsCompleted);
                bundle.putInt("exercises_completed_yesterday", stats.exercisesCompleted);
                bundle.putInt("errors_yesterday", stats.errorsCount);
            }
            FirebaseAnalytics.getInstance(context).logEvent("daily_reminder_shown", bundle);
        } catch (Exception e) {
            Log.w(TAG, "Analytics logging failed", e);
        }
    }
}

