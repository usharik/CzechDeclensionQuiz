package com.usharik.database.dao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DocumentDatabaseTest {

    @Test
    public void shouldRecreateAfterMigrationFailure_detectsSchemaValidationError() {
        RuntimeException exception = new RuntimeException(
                new IllegalStateException("Migration didn't properly handle: recent_words"));

        assertTrue(DocumentDatabase.shouldRecreateAfterMigrationFailure(exception));
    }

    @Test
    public void shouldRecreateAfterMigrationFailure_detectsMissingMigrationPath() {
        IllegalStateException exception = new IllegalStateException(
                "A migration from 6 to 7 was required but not found.");

        assertTrue(DocumentDatabase.shouldRecreateAfterMigrationFailure(exception));
    }

    @Test
    public void shouldRecreateAfterMigrationFailure_ignoresUnrelatedErrors() {
        RuntimeException exception = new RuntimeException("Disk I/O error");

        assertFalse(DocumentDatabase.shouldRecreateAfterMigrationFailure(exception));
    }
}