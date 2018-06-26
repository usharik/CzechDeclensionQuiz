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
        version = 2
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
                .build();
    }

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("DELETE FROM `DOCUMENT`;");
            database.execSQL("ALTER TABLE `DOCUMENT` ADD COLUMN `word` TEXT");
            database.execSQL("CREATE  INDEX `index_DOCUMENT_word` ON `DOCUMENT` (`word`)");
            Gson gson = new Gson();
            InputStream inputStream;
            try {
                inputStream = mContext.getAssets().open("data.json");
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String json;
                    while ((json = reader.readLine()) != null) {
                        WordInfo wordInfo = gson.fromJson(json, WordInfo.class);
                        database.execSQL("insert into DOCUMENT(word_id, word, json) values(?, ?, ?);",
                                new Object[] {wordInfo.wordId, wordInfo.word, json});
                    }
                }
            } catch (IOException e) {
                Log.e("Exception", e.getClass().getName(), e);
            }
        }
    };

    static void setContext(Context pContext) {
        mContext = pContext;
    }
}
