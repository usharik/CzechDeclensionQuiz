package com.usharik.database.dao;

import android.content.Context;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.google.gson.Gson;
import com.usharik.database.DocumentDb;
import com.usharik.database.WordInfo;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by macbook on 14/03/2018.
 */

public class DatabaseManager {

    private final Context context;
    private DocumentDatabase instance;
    private final Gson gson = new Gson();
    private final DocumentDb documentDb = new DocumentDbImpl();

    public DatabaseManager(Context context) {
        this.context = context;
    }

    private synchronized DocumentDatabase getActiveDbInstance() {
        if (instance == null || !instance.isOpen()) {
            instance = DocumentDatabase.getDocumentDatabase(context);
        }
        return instance;
    }

    public synchronized DocumentDb getDocumentDb() {
        return documentDb;
    }

    public void populateFromJsonStream(InputStream stream) throws IOException {
        DocumentDatabase db = getActiveDbInstance();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            db.runInTransaction(() -> {
                try {
                    String json;
                    while ((json = reader.readLine()) != null) {
                        WordInfo wordInfo = gson.fromJson(json, WordInfo.class);
                        SupportSQLiteStatement stmt =
                                db.compileStatement("insert into DOCUMENT(word_id, word, gender, declension_type, json) values(?, ?, ?, ?, ?);");
                        stmt.bindLong(1, wordInfo.wordId());
                        stmt.bindString(2, wordInfo.word());
                        stmt.bindString(3, wordInfo.gender());
                        stmt.bindString(4, wordInfo.declensionType());
                        stmt.bindString(5, json);
                        stmt.executeInsert();
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to populate database from JSON stream", e);
                }
            });
        }
    }

    private class DocumentDbImpl implements DocumentDb {

        @Override
        public Single<Integer> getCount() {
            return getActiveDbInstance().documentDao().getCount()
                    .subscribeOn(Schedulers.io());
        }

        @Override
        public Maybe<WordInfo> getWordInfoByWord(String word) {
            return getActiveDbInstance().documentDao().getJsonStringByWord(word)
                    .map(json -> gson.fromJson(json, WordInfo.class))
                    .subscribeOn(Schedulers.io());
        }

        @Override
        public Single<WordInfo> getRandomWordWithAnotherDeclensionType(String prevDeclensionType) {
            return getActiveDbInstance().documentDao().getRandomWordWithAnotherDeclensionType(prevDeclensionType)
                    .map(doc -> gson.fromJson(doc.getJson(), WordInfo.class))
                    .subscribeOn(Schedulers.io());
        }
    }
}
