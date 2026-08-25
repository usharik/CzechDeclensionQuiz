package com.usharik.database.dao

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), DocumentDatabase::class.java)

    @Test fun migrate5To8_validatesSchema() {
        helper.createDatabase("migration-5-8", 5).close()
        helper.runMigrationsAndValidate("migration-5-8", 8, true, DocumentDatabase.MIGRATION_5_7, DocumentDatabase.MIGRATION_7_8)
    }

    @Test fun migrate6To8_validatesSchema() {
        helper.createDatabase("migration-6-8", 6).close()
        helper.runMigrationsAndValidate("migration-6-8", 8, true, DocumentDatabase.MIGRATION_6_7, DocumentDatabase.MIGRATION_7_8)
    }

    @Test fun migrate7To8_correctsDictionaryWithoutChangingStats() {
        val database: SupportSQLiteDatabase = helper.createDatabase("migration-7-8", 7)
        database.execSQL("INSERT INTO DOCUMENT (id, word_id, word, gender, declension_type, json) VALUES (1, 257, 'Ázerbájdžánec', 'rod: m. neživ.', 'hrad', '{}')")
        database.execSQL("INSERT INTO daily_training_stats (date, words_completed, exercises_completed, errors_count, updated_at) VALUES ('2026-01-01', 4, 9, 2, 1)")
        database.close()
        val migrated = helper.runMigrationsAndValidate("migration-7-8", 8, true, DocumentDatabase.MIGRATION_7_8)
        migrated.query("SELECT gender, declension_type, json FROM DOCUMENT WHERE word_id = 257").use { cursor ->
            cursor.moveToFirst()
            assertEquals("rod: m. živ.", cursor.getString(0)); assertEquals("muž", cursor.getString(1)); assertEquals(true, cursor.getString(2).contains("Ázerbájdžánce"))
        }
        migrated.query("SELECT words_completed, exercises_completed, errors_count FROM daily_training_stats WHERE date = '2026-01-01'").use { cursor ->
            cursor.moveToFirst(); assertEquals(4, cursor.getInt(0)); assertEquals(9, cursor.getInt(1)); assertEquals(2, cursor.getInt(2))
        }
        migrated.close()
    }
}
