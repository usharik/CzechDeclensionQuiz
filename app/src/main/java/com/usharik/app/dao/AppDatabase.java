package com.usharik.app.dao;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

import com.usharik.app.dao.entity.CasesOfNoun;
import com.usharik.app.dao.entity.FormsOfVerb;
import com.usharik.app.dao.entity.Translation;
import com.usharik.app.dao.entity.Word;
import com.usharik.app.dao.entity.WordInfo;
import com.usharik.app.dao.entity.WordToTranslation;

@Database(
        entities = {
                Word.class,
                Translation.class,
                WordToTranslation.class,
                CasesOfNoun.class,
                FormsOfVerb.class,
                WordInfo.class
        },
        version = 7
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public static final String DB_NAME = "slovnik-database";

    public abstract TranslationStorageDao translationStorageDao();

    static AppDatabase getAppDatabase(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DB_NAME)
                .build();
    }
}
