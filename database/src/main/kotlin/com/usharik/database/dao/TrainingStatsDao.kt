package com.usharik.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface TrainingStatsDao {
    @Query("SELECT * FROM daily_training_stats WHERE date = :date") suspend fun statsByDate(date: String): DailyTrainingStatsEntity?
    @Query("INSERT OR IGNORE INTO daily_training_stats (date, words_completed, exercises_completed, errors_count, updated_at) VALUES (:date, 0, 0, 0, :updatedAt)") suspend fun insertIgnoreStatsRow(date: String, updatedAt: Long)
    @Query("UPDATE daily_training_stats SET words_completed = words_completed + 1, updated_at = :updatedAt WHERE date = :date") suspend fun addOneWordsCompleted(date: String, updatedAt: Long)
    @Query("UPDATE daily_training_stats SET exercises_completed = exercises_completed + 1, updated_at = :updatedAt WHERE date = :date") suspend fun addOneExercisesCompleted(date: String, updatedAt: Long)
    @Query("UPDATE daily_training_stats SET errors_count = errors_count + 1, updated_at = :updatedAt WHERE date = :date") suspend fun addOneErrorsCount(date: String, updatedAt: Long)
    @Transaction suspend fun incrementWordsCompleted(date: String, updatedAt: Long) { insertIgnoreStatsRow(date, updatedAt); addOneWordsCompleted(date, updatedAt) }
    @Transaction suspend fun incrementExercisesCompleted(date: String, updatedAt: Long) { insertIgnoreStatsRow(date, updatedAt); addOneExercisesCompleted(date, updatedAt) }
    @Transaction suspend fun incrementErrorsCount(date: String, updatedAt: Long) { insertIgnoreStatsRow(date, updatedAt); addOneErrorsCount(date, updatedAt) }
    @Query("SELECT * FROM reminder_state WHERE id = 1") suspend fun reminderState(): ReminderStateEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertReminderState(state: ReminderStateEntity)
    @Query("SELECT * FROM recent_words WHERE id = 1") suspend fun recentWords(): RecentWordsEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertRecentWords(entity: RecentWordsEntity)
}
