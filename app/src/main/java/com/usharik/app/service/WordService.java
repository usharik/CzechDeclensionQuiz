package com.usharik.app.service;

import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.usharik.app.AppState;
import com.usharik.database.WordInfo;
import com.usharik.database.dao.DatabaseManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
                ? getRandomWordWithErrorAsync(null)
                : Maybe.empty()
        ).switchIfEmpty(
                getRandomWordAsync(null)
        ).doOnSuccess(doc -> {
            Log.i(getClass().getName(), "New word is " + doc.word());
            Bundle bundle = new Bundle();
            bundle.putString("WORD", doc.word());
            firebaseAnalytics.logEvent("NEXT_WORD", bundle);
        });
    }

    public Single<WordInfo> getNextWord(WordInfo currentWord) {
        return Maybe.defer(() -> rnd.nextBoolean()
                ? getRandomWordWithErrorAsync(currentWord)
                : Maybe.empty()
        ).switchIfEmpty(
                getRandomWordAsync(currentWord)
        ).doOnSuccess(doc -> {
            Log.i(getClass().getName(), "New word is " + doc.word());
            Bundle bundle = new Bundle();
            bundle.putString("WORD", doc.word());
            firebaseAnalytics.logEvent("NEXT_WORD", bundle);
        });
    }

    private Maybe<WordInfo> getRandomWordWithErrorAsync(WordInfo currentWord) {
        Map<String, Integer> wordsWithErrors = appState.getWordsWithErrors();
        int size = wordsWithErrors.size();
        if (size == 0) {
            return Maybe.empty();
        }
        String prevWord = currentWord == null || currentWord.gender() == null ? "" : currentWord.word();

        List<String> keys = new ArrayList<>(wordsWithErrors.keySet());
        String wordKey = keys.get(rnd.nextInt(size));
        return databaseManager.getDocumentDb().getWordInfoByWord(wordKey).flatMap(doc -> {
            if (doc == null) {
                appState.removeWordFromErrorMap(wordKey);
                return Maybe.empty();
            } else if (prevWord.equals(doc.word())) {
                return Maybe.empty();
            }
            return Maybe.just(doc);
        });
    }

    private Single<WordInfo> getRandomWordAsync(WordInfo currentWord) {
        String declensionType = (currentWord == null || currentWord.declensionType() == null)
                ? ""
                : currentWord.declensionType();
        return databaseManager.getDocumentDb().getRandomWordWithAnotherDeclensionType(declensionType);
    }
}
