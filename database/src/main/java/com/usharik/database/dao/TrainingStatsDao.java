package com.usharik.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;

@Dao
public interface TrainingStatsDao {

    @Query("SELECT * FROM daily_training_stats WHERE date = :date")
    Maybe<DailyTrainingStatsEntity> getStatsByDate(String date);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertOrReplaceStats(DailyTrainingStatsEntity stats);

    @Query("SELECT * FROM reminder_state WHERE id = 1")
    Maybe<ReminderStateEntity> getReminderState();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertOrReplaceReminderState(ReminderStateEntity state);
}

