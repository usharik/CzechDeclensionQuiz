package com.usharik.database.dao;

import androidx.room.testing.MigrationTestHelper;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class DatabaseMigrationTest {

    private static final String TEST_DB = "migration-test";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            DocumentDatabase.class
    );

    @Test
    public void migrate5To7_validatesSchema() throws IOException {
        helper.createDatabase(TEST_DB, 5).close();

        helper.runMigrationsAndValidate(
                TEST_DB,
                7,
                true,
                DocumentDatabase.MIGRATION_5_7
        );
    }

    @Test
    public void migrate6To7_validatesSchema() throws IOException {
        helper.createDatabase(TEST_DB, 6).close();

        helper.runMigrationsAndValidate(
                TEST_DB,
                7,
                true,
                DocumentDatabase.MIGRATION_6_7
        );
    }
}
