package com.usharik.app.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.usharik.app.MainActivity;
import com.usharik.app.R;

/**
 * Centralises notification channel creation and posting.
 * Call {@link #createChannel(Context)} once on app start (idempotent).
 */
public final class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    /**
     * The notification channel ID. This is a programmatic constant — it must
     * never be a translated string resource, as that would create duplicate
     * orphan channels per locale.
     */
    public static final String CHANNEL_ID = "daily_reminder";

    private static final int NOTIFICATION_ID = 1001;
    private static final int WELCOME_NOTIFICATION_ID = 1002;

    /** SharedPreferences file name — shared with the rest of the app. */
    private static final String PREFS_NAME = "czech_declension_quiz";
    /** Flag persisted after the welcome notification has been shown once. */
    private static final String PREF_WELCOME_SHOWN = "welcome_notification_shown";

    private NotificationHelper() {}

    /** Creates the notification channel (safe to call multiple times). */
    public static void createChannel(Context context) {
        CharSequence name = context.getString(R.string.notification_channel_name);
        String description = context.getString(R.string.notification_channel_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * Posts the daily reminder notification and logs the analytics event.
     *
     * <p>This is the single entry point for daily reminder notifications —
     * callers must not log analytics separately for this event.</p>
     *
     * @param context            application context
     * @param isActive           true if the user was active yesterday (different message)
     * @param inactivityStreak   current inactivity streak (for analytics)
     * @param wordsYesterday     words completed yesterday (for analytics)
     * @param exercisesYesterday exercises completed yesterday (for analytics)
     */
    public static void showDailyReminder(Context context, boolean isActive,
                                         int inactivityStreak, int wordsYesterday, int exercisesYesterday) {
        // Android 13+ POST_NOTIFICATIONS permission check
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        String title = context.getString(R.string.notification_title);
        String body = context.getString(isActive
                ? R.string.notification_body_active
                : R.string.notification_body_inactive);

        // Main intent — opens app when notification is tapped
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Action button intent — same target, different request code
        Intent actionIntent = new Intent(context, MainActivity.class);
        actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent actionPendingIntent = PendingIntent.getActivity(
                context, 1, actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String actionText = context.getString(R.string.notification_action_start_quiz);
        String appName = context.getString(R.string.app_name);

        Bitmap largeIcon = BitmapFactory.decodeResource(
                context.getResources(), R.mipmap.ic_launcher_round);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                // Small icon must be monochrome white-on-transparent for the status bar.
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                // Large icon shows the app icon inside the notification card.
                .setLargeIcon(largeIcon)
                // SubText adds the app name to the notification header line.
                .setSubText(appName)
                .setContentTitle(title)
                .setContentText(body)
                // BigTextStyle makes the card expandable and shows the full body text.
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_notifications_black_24dp, actionText, actionPendingIntent);

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());

        // Analytics — logged here so callers never duplicate this event.
        try {
            Bundle bundle = new Bundle();
            bundle.putInt("inactivity_streak", inactivityStreak);
            bundle.putInt("words_completed_yesterday", wordsYesterday);
            bundle.putInt("exercises_completed_yesterday", exercisesYesterday);
            FirebaseAnalytics.getInstance(context).logEvent("daily_reminder_shown", bundle);
        } catch (Exception e) {
            Log.w(TAG, "Analytics logging failed", e);
        }
    }

    /**
     * Shows a one-time welcome notification after the user first grants POST_NOTIFICATIONS.
     * Checks and sets a flag in SharedPreferences to ensure it fires only once.
     */
    public static void showWelcomeNotificationIfNeeded(Context context) {
        android.content.SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(PREF_WELCOME_SHOWN, false)) {
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        String title = context.getString(R.string.notification_welcome_title);
        String body = context.getString(R.string.notification_welcome_body);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 2, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String appName = context.getString(R.string.app_name);

        Bitmap largeIcon = BitmapFactory.decodeResource(
                context.getResources(), R.mipmap.ic_launcher_round);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setLargeIcon(largeIcon)
                .setSubText(appName)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(WELCOME_NOTIFICATION_ID, builder.build());

        prefs.edit().putBoolean(PREF_WELCOME_SHOWN, true).apply();

        try {
            FirebaseAnalytics.getInstance(context).logEvent("welcome_notification_shown", null);
        } catch (Exception e) {
            Log.w(TAG, "Analytics logging failed for welcome notification", e);
        }
    }
}

