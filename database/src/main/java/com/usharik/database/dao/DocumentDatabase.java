package com.usharik.database.dao;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.usharik.database.WordInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Database(
        entities = {
            DocumentEntity.class
        },
        version = 3
)
@TypeConverters({Converters.class})
public abstract class DocumentDatabase extends RoomDatabase {
    public static final String DB_NAME = "quiz-dictionary-database";

    public abstract DocumentDao documentDao();

    private static Context mContext;

    static DocumentDatabase getDocumentDatabase(Context context) {
        mContext = context;
        return Room.databaseBuilder(context.getApplicationContext(), DocumentDatabase.class, DB_NAME)
                .allowMainThreadQueries()
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .build();
    }

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.i("Migration1_2", "Database structure altering");
            database.execSQL("DELETE FROM `DOCUMENT`;");
            database.execSQL("UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME='DOCUMENT'");
            database.execSQL("ALTER TABLE `DOCUMENT` ADD COLUMN `word` TEXT");
            database.execSQL("ALTER TABLE `DOCUMENT` ADD COLUMN `gender` TEXT");
            database.execSQL("CREATE  INDEX `index_DOCUMENT_word` ON `DOCUMENT` (`word`)");
            database.execSQL("CREATE  INDEX `index_DOCUMENT_gender` ON `DOCUMENT` (`gender`)");
            Log.i("Migration1_2", "Insert new data");
            Gson gson = new Gson();
            try {
                try(InputStream inputStream = mContext.getAssets().open("data.json");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String json;
                    while ((json = reader.readLine()) != null) {
                        WordInfo wordInfo = gson.fromJson(json, WordInfo.class);
                        database.execSQL("insert into DOCUMENT(word_id, word, gender, json) values(?, ?, ?, ?);",
                                new Object[] {wordInfo.wordId, wordInfo.word, wordInfo.gender, json});
                    }
                }
            } catch (IOException e) {
                Log.e("Exception", e.getClass().getName(), e);
            }
            Log.i("Migration1_2", "Migration completed");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.i("Migration2_3", "Removing all data");

            database.execSQL("DELETE FROM `DOCUMENT`;");
            database.execSQL("VACUUM;");

            Log.i("Migration2_3", "Migration completed");
        }
    };

    static void setContext(Context pContext) {
        mContext = pContext;
    }
}
