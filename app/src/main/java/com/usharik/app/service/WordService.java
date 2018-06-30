package com.usharik.app.service;

import com.usharik.database.WordInfo;
import com.usharik.database.dao.DatabaseManager;

import java.util.Random;

public class WordService {

    private final DatabaseManager databaseManager;
    private final Random random = new Random();

    public WordService(final DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public WordInfo getNextWord() {
        WordInfo doc = null;
        int id;
        long wordCount = databaseManager.getDocumentDb().getCount().blockingGet();

        while (doc == null) {
            id = random.nextInt((int) wordCount);
            doc = databaseManager.getDocumentDb().getWordInfoById(id).blockingGet();
        }
        return doc;
    }
}
