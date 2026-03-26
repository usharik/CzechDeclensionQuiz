package com.usharik.database.dao;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Stores per-day training statistics. One row per calendar date (yyyy-MM-dd). */
@Entity(tableName = "daily_training_stats")
public class DailyTrainingStatsEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "date")
    public String date = "";

    @ColumnInfo(name = "words_completed")
    public int wordsCompleted;

    @ColumnInfo(name = "exercises_completed")
    public int exercisesCompleted;

    @ColumnInfo(name = "errors_count")
    public int errorsCount;

    /** Epoch-millis of the last update, for debugging / ordering. */
    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}

