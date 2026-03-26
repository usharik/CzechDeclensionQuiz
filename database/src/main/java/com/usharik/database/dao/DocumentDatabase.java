package com.usharik.database.dao;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import android.content.Context;

@Database(
        entities = {
            DocumentEntity.class,
            DailyTrainingStatsEntity.class,
            ReminderStateEntity.class,
            RecentWordsEntity.class
        },
        version = 7
)
@TypeConverters({Converters.class})
public abstract class DocumentDatabase extends RoomDatabase {
    public static final String DB_NAME = "quiz-dictionary-database";

    public abstract DocumentDao documentDao();

    public abstract TrainingStatsDao trainingStatsDao();

    // Direct upgrade from v5 to v7 (squashed migration — covers all schema changes at once)
    static final Migration MIGRATION_5_7 = new Migration(5, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `daily_training_stats` (" +
                "`date` TEXT NOT NULL, " +
                "`words_completed` INTEGER NOT NULL DEFAULT 0, " +
                "`exercises_completed` INTEGER NOT NULL DEFAULT 0, " +
                "`errors_count` INTEGER NOT NULL DEFAULT 0, " +
                "`updated_at` INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY(`date`))"
            );
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `reminder_state` (" +
                "`id` INTEGER NOT NULL, " +
                "`last_active_date` TEXT, " +
                "`last_notification_date` TEXT, " +
                "`inactivity_streak` INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY(`id`))"
            );
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `recent_words` (" +
                "`id` INTEGER NOT NULL, " +
                "`words` TEXT NOT NULL DEFAULT '', " +
                "PRIMARY KEY(`id`))"
            );
        }
    };

    // Incremental upgrade from v6 to v7 (for users already on v6)
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `recent_words` (" +
                "`id` INTEGER NOT NULL, " +
                "`words` TEXT NOT NULL DEFAULT '', " +
                "PRIMARY KEY(`id`))"
            );
        }
    };

    public static DocumentDatabase getDocumentDatabase(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), DocumentDatabase.class, DB_NAME)
                .addMigrations(MIGRATION_5_7, MIGRATION_6_7)
                .fallbackToDestructiveMigration(true)
                .build();
    }
}
