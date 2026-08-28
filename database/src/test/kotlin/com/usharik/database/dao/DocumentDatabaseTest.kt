package com.usharik.database.dao

import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentDatabaseTest {
    @Test fun databaseNameIsStableForInstalledUsers() {
        assertEquals("quiz-dictionary-database", DocumentDatabase.DB_NAME)
    }

    @Test fun correctionMigrationTargetsVersionEight() {
        assertEquals(7, DocumentDatabase.MIGRATION_7_8.startVersion)
        assertEquals(8, DocumentDatabase.MIGRATION_7_8.endVersion)
    }

    @Test fun scoreMigrationTargetsVersionNine() {
        assertEquals(8, DocumentDatabase.MIGRATION_8_9.startVersion)
        assertEquals(9, DocumentDatabase.MIGRATION_8_9.endVersion)
    }
}
