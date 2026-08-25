package com.usharik.database.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "DOCUMENT", indices = [Index(value = ["word"]), Index(value = ["gender"]), Index(value = ["declension_type"])])
data class DocumentEntity(
    @ColumnInfo(name = "word_id") var wordId: Long?,
    @ColumnInfo(name = "word") var word: String?,
    @ColumnInfo(name = "gender") var gender: String?,
    @ColumnInfo(name = "declension_type") var declensionType: String?,
    @ColumnInfo(name = "json") var json: String?,
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
)

@Entity(tableName = "daily_training_stats")
data class DailyTrainingStatsEntity(
    @PrimaryKey @ColumnInfo(name = "date") var date: String = "",
    @ColumnInfo(name = "words_completed") var wordsCompleted: Int = 0,
    @ColumnInfo(name = "exercises_completed") var exercisesCompleted: Int = 0,
    @ColumnInfo(name = "errors_count") var errorsCount: Int = 0,
    @ColumnInfo(name = "updated_at") var updatedAt: Long = 0,
)

@Entity(tableName = "reminder_state")
data class ReminderStateEntity(
    @PrimaryKey var id: Int = 1,
    @ColumnInfo(name = "last_active_date") var lastActiveDate: String? = null,
    @ColumnInfo(name = "last_notification_date") var lastNotificationDate: String? = null,
    @ColumnInfo(name = "inactivity_streak") var inactivityStreak: Int = 0,
)

@Entity(tableName = "recent_words")
data class RecentWordsEntity(
    @PrimaryKey var id: Int = 1,
    // Kept nullable to match the schema shipped in database versions 5–7.
    @ColumnInfo(name = "words") var words: String? = "",
)
