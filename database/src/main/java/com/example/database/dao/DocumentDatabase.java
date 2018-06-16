package com.example.database.dao;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

@Database(
        entities = {
            DocumentEntity.class
        },
        version = 1
)
@TypeConverters({Converters.class})
public abstract class DocumentDatabase extends RoomDatabase {
    public static final String DB_NAME = "quiz-dictionary-database";

    public abstract DocumentDao documentDao();

    static DocumentDatabase getDocumentDatabase(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), DocumentDatabase.class, DB_NAME)
                .allowMainThreadQueries()
                .build();
    }
}
