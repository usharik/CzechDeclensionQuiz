package com.usharik.database.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DocumentEntity::class, DailyTrainingStatsEntity::class, ReminderStateEntity::class, RecentWordsEntity::class],
    version = 9,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class DocumentDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun trainingStatsDao(): TrainingStatsDao

    companion object {
        const val DB_NAME = "quiz-dictionary-database"

        /** Creates the statistics and reminder tables introduced after version 5. */
        @JvmField val MIGRATION_5_7 = object : Migration(5, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `daily_training_stats` (`date` TEXT NOT NULL, `words_completed` INTEGER NOT NULL DEFAULT 0, `exercises_completed` INTEGER NOT NULL DEFAULT 0, `errors_count` INTEGER NOT NULL DEFAULT 0, `updated_at` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`date`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `reminder_state` (`id` INTEGER NOT NULL, `last_active_date` TEXT, `last_notification_date` TEXT, `inactivity_streak` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `recent_words` (`id` INTEGER NOT NULL, `words` TEXT, PRIMARY KEY(`id`))")
            }
        }

        /** Creates the recent-word history table for users upgrading from version 6. */
        @JvmField val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `recent_words` (`id` INTEGER NOT NULL, `words` TEXT, PRIMARY KEY(`id`))")
            }
        }

        /**
         * Brings existing version-7 installations in line with the corrected bundled dictionary.
         * This deliberately updates only dictionary rows; user training progress remains intact.
         */
        @JvmField val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                updateDictionaryEntry(
                    db, 257L, "rod: m. živ.", "muž",
                    "{\"wordId\":257,\"word\":\"Ázerbájdžánec\",\"gender\":\"rod: m. živ.\",\"declensionType\":\"muž\",\"translation_ru\":\"азербайджа́нец\",\"translation_en\":\"Azerbaijani(an), Azeri\",\"cases\":[[\"Ázerbájdžánec\",\"Ázerbájdžánce\",\"Ázerbájdžánci, Ázerbájdžáncovi\",\"Ázerbájdžánce\",\"Ázerbájdžánče\",\"Ázerbájdžánci, Ázerbájdžáncovi\",\"Ázerbájdžáncem\"],[\"Ázerbájdžánci\",\"Ázerbájdžánců\",\"Ázerbájdžáncům\",\"Ázerbájdžánce\",\"Ázerbájdžánci\",\"Ázerbájdžáncích\",\"Ázerbájdžánci\"]]}"
                )
                updateDictionaryEntry(
                    db, 576L, "rod: m. neživ.", "pomnožné",
                    "{\"wordId\":576,\"word\":\"schůdky\",\"gender\":\"rod: m. neživ.\",\"declensionType\":\"pomnožné\",\"translation_ru\":\"ле́сенка, трап\",\"translation_en\":\"stepladder, steps\",\"cases\":[[\"schůdky\",\"\",\"\",\"\",\"\",\"\",\"\"],[\"schůdky\",\"schůdků\",\"schůdkům\",\"schůdky\",\"schůdky\",\"schůdcích, schůdkách\",\"schůdky\"]]}"
                )
            }

            private fun updateDictionaryEntry(database: SupportSQLiteDatabase, wordId: Long, gender: String, declensionType: String, json: String) {
                database.execSQL(
                    "UPDATE `DOCUMENT` SET `gender` = ?, `declension_type` = ?, `json` = ? WHERE `word_id` = ?",
                    arrayOf<Any>(gender, declensionType, json, wordId),
                )
            }
        }

        /** Adds the points-based score column backing the daily goal introduced after version 8. */
        @JvmField val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `daily_training_stats` ADD COLUMN `score` INTEGER NOT NULL DEFAULT 0")
            }
        }

        @JvmStatic
        fun getDocumentDatabase(context: Context): DocumentDatabase =
            Room.databaseBuilder(context.applicationContext, DocumentDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_5_7, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                .fallbackToDestructiveMigration(true)
                .build()
    }
}

object DatabaseFactory {
    @JvmStatic
    fun provideDocumentDatabase(context: Context): DocumentDatabase = DocumentDatabase.getDocumentDatabase(context)
}
