package com.usharik.database.dao;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;

@Database(
        entities = {
            DocumentEntity.class
        },
        version = 5
)
@TypeConverters({Converters.class})
public abstract class DocumentDatabase extends RoomDatabase {
    public static final String DB_NAME = "quiz-dictionary-database";

    public abstract DocumentDao documentDao();

    public static DocumentDatabase getDocumentDatabase(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), DocumentDatabase.class, DB_NAME)
                .fallbackToDestructiveMigration(true)
                .build();
    }
}
