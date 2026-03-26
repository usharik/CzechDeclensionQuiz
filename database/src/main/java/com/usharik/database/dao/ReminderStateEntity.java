package com.usharik.database.dao;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Singleton row (id=1) that tracks the notification schedule state.
 * inactivityStreak counts consecutive days without any training activity.
 */
@Entity(tableName = "reminder_state")
public class ReminderStateEntity {

    @PrimaryKey
    public int id = 1;

    /** yyyy-MM-dd of the last day the user was active, or null if never. */
    @ColumnInfo(name = "last_active_date")
    public String lastActiveDate;

    /** yyyy-MM-dd of the last day a notification was shown, or null if never. */
    @ColumnInfo(name = "last_notification_date")
    public String lastNotificationDate;

    /** How many consecutive days the user has NOT been active. */
    @ColumnInfo(name = "inactivity_streak")
    public int inactivityStreak;
}

