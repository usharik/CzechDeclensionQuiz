package com.usharik.database;

import androidx.sqlite.db.SupportSQLiteStatement;
import com.google.gson.Gson;
import com.usharik.database.dao.DocumentDatabase;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DocumentRepository {

    private final DocumentDatabase db;

    private final Gson gson = new Gson();

    public DocumentRepository(DocumentDatabase db) {
        this.db = db;
    }

    public Single<Integer> getCount() {
        return db.documentDao().getCount()
                .subscribeOn(Schedulers.io());
    }

    public Maybe<WordInfo> getWordInfoByWord(String word) {
        return db.documentDao().getJsonStringByWord(word)
                .map(json -> gson.fromJson(json, WordInfo.class))
                .subscribeOn(Schedulers.io());
    }

    public Single<WordInfo> getRandomWordWithAnotherDeclensionType(String prevDeclensionType) {
        return db.documentDao().getRandomWordWithAnotherDeclensionType(prevDeclensionType)
                .map(doc -> gson.fromJson(doc.getJson(), WordInfo.class))
                .subscribeOn(Schedulers.io());
    }

    public void populateFromJsonStream(InputStream stream) throws IOException {
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
}
