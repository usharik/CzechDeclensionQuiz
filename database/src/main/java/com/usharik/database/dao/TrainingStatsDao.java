package com.usharik.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;

@Dao
public interface TrainingStatsDao {

    @Query("SELECT * FROM daily_training_stats WHERE date = :date")
    Maybe<DailyTrainingStatsEntity> getStatsByDate(String date);

    // ── Atomic increment support (INSERT OR IGNORE + UPDATE in one transaction) ──

    /** Creates a zero-value row for the given date if none exists yet. */
    @Query("INSERT OR IGNORE INTO daily_training_stats (date, words_completed, exercises_completed, errors_count, updated_at) VALUES (:date, 0, 0, 0, :updatedAt)")
    void insertIgnoreStatsRow(String date, long updatedAt);

    @Query("UPDATE daily_training_stats SET words_completed = words_completed + 1, updated_at = :updatedAt WHERE date = :date")
    void addOneWordsCompleted(String date, long updatedAt);

    @Query("UPDATE daily_training_stats SET exercises_completed = exercises_completed + 1, updated_at = :updatedAt WHERE date = :date")
    void addOneExercisesCompleted(String date, long updatedAt);

    @Query("UPDATE daily_training_stats SET errors_count = errors_count + 1, updated_at = :updatedAt WHERE date = :date")
    void addOneErrorsCount(String date, long updatedAt);

    @Transaction
    default void atomicIncrementWordsCompleted(String date, long updatedAt) {
        insertIgnoreStatsRow(date, updatedAt);
        addOneWordsCompleted(date, updatedAt);
    }

    @Transaction
    default void atomicIncrementExercisesCompleted(String date, long updatedAt) {
        insertIgnoreStatsRow(date, updatedAt);
        addOneExercisesCompleted(date, updatedAt);
    }

    @Transaction
    default void atomicIncrementErrorsCount(String date, long updatedAt) {
        insertIgnoreStatsRow(date, updatedAt);
        addOneErrorsCount(date, updatedAt);
    }

    @Query("SELECT * FROM reminder_state WHERE id = 1")
    Maybe<ReminderStateEntity> getReminderState();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertOrReplaceReminderState(ReminderStateEntity state);

    @Query("SELECT * FROM recent_words WHERE id = 1")
    Maybe<RecentWordsEntity> getRecentWords();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertOrReplaceRecentWords(RecentWordsEntity entity);
}
