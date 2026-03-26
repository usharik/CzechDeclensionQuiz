package com.usharik.app.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.usharik.app.MainActivity;
import com.usharik.app.R;

/**
 * Centralises notification channel creation and posting.
 * Call {@link #createChannel(Context)} once on app start (idempotent).
 */
public final class NotificationHelper {

    /**
     * The notification channel ID. This is a programmatic constant — it must
     * never be a translated string resource, as that would create duplicate
     * orphan channels per locale.
     */
    public static final String CHANNEL_ID = "daily_reminder";

    private static final int NOTIFICATION_ID = 1001;

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
     * Posts the daily reminder notification.
     *
     * @param context   application context
     * @param isActive  true if the user was active yesterday (different message)
     */
    public static void showDailyReminder(Context context, boolean isActive) {
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

        // Main intent - opens app when notification is tapped
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Action button intent - same as main intent but with different request code
        Intent actionIntent = new Intent(context, MainActivity.class);
        actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent actionPendingIntent = PendingIntent.getActivity(
                context, 1, actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String actionText = context.getString(R.string.notification_action_start_quiz);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                // Use a monochrome vector drawable — launcher icons render as a
                // white blob on the status bar and are not allowed as small icons.
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                // Add action button to start quiz
                .addAction(R.drawable.ic_notifications_black_24dp, actionText, actionPendingIntent);

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }
}

