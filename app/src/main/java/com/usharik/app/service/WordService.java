package com.usharik.app.service;

import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.usharik.app.AppState;
import com.usharik.app.Gender;
import com.usharik.database.WordInfo;
import com.usharik.database.dao.DatabaseManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WordService {

    private final DatabaseManager databaseManager;
    private final AppState appState;
    private final FirebaseAnalytics firebaseAnalytics;
    private final Random random = new Random();

    public WordService(final DatabaseManager databaseManager,
                       final AppState appState,
                       final FirebaseAnalytics firebaseAnalytics) {
        this.databaseManager = databaseManager;
        this.appState = appState;
        this.firebaseAnalytics = firebaseAnalytics;
    }

    public WordInfo getNextWord() {
        Log.i(getClass().getName(), "New word generation");
        WordInfo doc = null;
        int id;
        long wordCount = databaseManager.getDocumentDb().getCount().blockingGet();

        int size = appState.wordsWithErrors.size();
        String prevWord = appState.wordInfo == null || appState.wordInfo.gender == null ? "" : appState.wordInfo.word;
        if (random.nextBoolean() && size > 0) {
            List<String> keys = new ArrayList<>(appState.wordsWithErrors.keySet());
            String wordKey = keys.get(random.nextInt(size));
            doc = databaseManager.getDocumentDb().getWordInfoByWord(wordKey).blockingGet();
            if (doc == null) {
                appState.removeWordFromErrorMap();
            } else if (prevWord.equals(doc.word)) {
                doc = null;
            }
        }
        if (doc == null) {
            String prevGender = appState.wordInfo == null || appState.wordInfo.gender == null ? "" : appState.wordInfo.gender;
            while (doc == null) {
                id = random.nextInt((int) wordCount);
                doc = databaseManager.getDocumentDb().getWordInfoById(id).blockingGet();
                String gender = appState.genderFilterStr;
                if (gender != null && !gender.equals(Gender.ALL) && doc != null) {
                    doc = doc.gender == null || doc.gender.equals(gender) ? doc : null;
                } else if (doc != null) {
                    doc = prevGender.equals(doc.gender) ? null : doc;
                }
            }
        }
        Log.i(getClass().getName(), "New word is " + doc.word);
        Bundle bundle = new Bundle();
        bundle.putString("WORD", doc.word);
        firebaseAnalytics.logEvent("NEXT_WORD", bundle);
        return doc;
    }
}
