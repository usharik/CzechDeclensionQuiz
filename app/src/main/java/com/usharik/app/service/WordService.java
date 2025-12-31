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
import java.util.NoSuchElementException;
import java.util.Random;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

public class WordService {

    private final DatabaseManager databaseManager;
    private final AppState appState;
    private final FirebaseAnalytics firebaseAnalytics;
    private final Random rnd = new Random();

    public WordService(final DatabaseManager databaseManager,
                       final AppState appState,
                       final FirebaseAnalytics firebaseAnalytics) {
        this.databaseManager = databaseManager;
        this.appState = appState;
        this.firebaseAnalytics = firebaseAnalytics;
    }

    public Single<WordInfo> getNextWord() {
        return Maybe.defer(() -> rnd.nextBoolean()
                ? getRandomWordWithErrorAsync()
                : Maybe.empty()
        ).switchIfEmpty(
                databaseManager.getDocumentDb().getCount().flatMap(this::getRandomWordAsync)
        ).doOnSuccess(doc -> {
            Log.i(getClass().getName(), "New word is " + doc.word);
            Bundle bundle = new Bundle();
            bundle.putString("WORD", doc.word);
            firebaseAnalytics.logEvent("NEXT_WORD", bundle);
        });
    }

    private Maybe<WordInfo> getRandomWordWithErrorAsync() {
        int size = appState.wordsWithErrors.size();
        if (size == 0) {
            return Maybe.empty();
        }
        String prevWord = appState.wordInfo == null || appState.wordInfo.gender == null ? "" : appState.wordInfo.word;

        List<String> keys = new ArrayList<>(appState.wordsWithErrors.keySet());
        String wordKey = keys.get(rnd.nextInt(size));
        return databaseManager.getDocumentDb().getWordInfoByWord(wordKey).flatMap(doc -> {
            if (doc == null) {
                appState.removeWordFromErrorMap();
                return Maybe.empty();
            } else if (prevWord.equals(doc.word)) {
                return Maybe.empty();
            }
            return Maybe.just(doc);
        });
    }

    private Single<WordInfo> getRandomWordAsync(int wordCount) {
        String prevGender = (appState.wordInfo == null || appState.wordInfo.gender == null)
                ? ""
                : appState.wordInfo.gender;

        return Maybe.defer(() -> {
                    int id = rnd.nextInt(wordCount);
                    return databaseManager.getDocumentDb().getWordInfoById(id);
                })
                .flatMap(doc -> {
                    WordInfo filtered = applyFilters(doc, prevGender);
                    if (filtered == null) {
                        return Maybe.error(new NoSuchElementException("Filtered out"));
                    }
                    return Maybe.just(filtered);
                })
                .retry()
                .toSingle();
    }

    private WordInfo applyFilters(WordInfo doc, String prevGender) {
        if (doc == null) {
            return null;
        }
        String genderFilter = appState.genderFilterStr;
        if (genderFilter != null && !genderFilter.equals(Gender.ALL)) {
            return (doc.gender == null || doc.gender.equals(genderFilter)) ? doc : null;
        } else {
            return prevGender.equals(doc.gender) ? null : doc;
        }
    }
}
