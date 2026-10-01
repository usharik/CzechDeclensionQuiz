package com.usharik.app.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.usharik.app.MainActivity
import com.usharik.app.R
import com.usharik.app.service.FirebaseAnalyticsService
import com.usharik.app.ui.state.Streak

/**
 * Manages notification channels and posting daily reminder/welcome notifications.
 * Centralized entry point for all app notifications with analytics integration.
 */
class NotificationHelper(private val analytics: FirebaseAnalyticsService) {

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    /** Posts the reminder chosen by [ReminderPlanner], worded around the player's own progress. */
    fun showReminder(
        context: Context,
        kind: ReminderPlanner.Kind,
        streak: Streak.Summary,
        reviewCount: Int,
        inactivityStreak: Int,
    ) {
        if (!canPost(context)) return
        val res = context.resources
        val (title, body) = when (kind) {
            ReminderPlanner.Kind.STREAK ->
                res.getQuantityString(R.plurals.notification_streak_title, streak.current, streak.current) to
                    res.getQuantityString(R.plurals.notification_streak_body, streak.pointsToKeep, streak.pointsToKeep)
            ReminderPlanner.Kind.RESCUE ->
                res.getQuantityString(R.plurals.notification_rescue_title, streak.current, streak.current) to
                    res.getQuantityString(R.plurals.notification_rescue_body, streak.pointsToKeep, streak.pointsToKeep)
            ReminderPlanner.Kind.REVIEW ->
                res.getQuantityString(R.plurals.notification_review_title, reviewCount, reviewCount) to
                    context.getString(R.string.notification_review_body)
            ReminderPlanner.Kind.COMEBACK ->
                context.getString(R.string.notification_title) to context.getString(R.string.notification_body_inactive)
        }
        notify(
            context = context,
            id = NOTIFICATION_ID,
            title = title,
            body = body,
            action = true,
            destination = if (kind == ReminderPlanner.Kind.REVIEW) MainActivity.DESTINATION_REVIEW else null,
        )
        analytics.logReminderShown(kind.name, streak.current, inactivityStreak, reviewCount)
    }

    fun showWelcomeNotificationIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREF_WELCOME_SHOWN, false) || !canPost(context)) return

        notify(
            context = context,
            id = WELCOME_NOTIFICATION_ID,
            title = context.getString(R.string.notification_welcome_title),
            body = context.getString(R.string.notification_welcome_body),
            action = false
        )
        prefs.edit().putBoolean(PREF_WELCOME_SHOWN, true).apply()
        analytics.logEvent("welcome_notification_shown")
    }

    // Every caller checks canPost() first; lint can't follow that guard.
    @SuppressLint("MissingPermission")
    private fun notify(
        context: Context,
        id: Int,
        title: String,
        body: String,
        action: Boolean,
        destination: String? = null,
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            destination?.let { putExtra(MainActivity.EXTRA_DESTINATION, it) }
        }
        val pending = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setLargeIcon(BitmapFactory.decodeResource(
                context.resources,
                R.mipmap.ic_launcher_round
            ))
            .setSubText(context.getString(R.string.app_name))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)

        if (action) {
            builder.addAction(
                R.drawable.ic_notifications_black_24dp,
                context.getString(R.string.notification_action_start_quiz),
                pending
            )
        }

        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    private fun canPost(context: Context): Boolean =
        android.os.Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val CHANNEL_ID = "daily_reminder"
        private const val NOTIFICATION_ID = 1001
        private const val WELCOME_NOTIFICATION_ID = 1002
        private const val PREFS_NAME = "czech_declension_quiz"
        private const val PREF_WELCOME_SHOWN = "welcome_notification_shown"
    }
}
