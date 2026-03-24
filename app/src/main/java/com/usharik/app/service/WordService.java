package com.usharik.app.service;

import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.usharik.app.AppState;
import com.usharik.database.DocumentRepository;
import com.usharik.database.WordInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

public class WordService {

    private final DocumentRepository documentRepository;
    private final AppState appState;
    private final FirebaseAnalytics firebaseAnalytics;
    private final Random rnd = new Random();

    public WordService(final DocumentRepository documentRepository,
                       final AppState appState,
                       final FirebaseAnalytics firebaseAnalytics) {
        this.documentRepository = documentRepository;
        this.appState = appState;
        this.firebaseAnalytics = firebaseAnalytics;
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
        }).doOnError(thr -> {
            Log.e(getClass().getName(), "Error getting next word", thr);
            FirebaseCrashlytics.getInstance().recordException(thr);
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
        return documentRepository.getWordInfoByWord(wordKey).flatMap(doc -> {
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
        return documentRepository.getRandomWordWithAnotherDeclensionType(declensionType);
    }

    /**
     * Returns a list of word forms from a randomly-selected word (with a different declension
     * type than the current word) to use as distractor answers in the single-case quiz.
     * This ensures that even indeclinable words always have enough wrong-answer options.
     */
    public Single<List<String>> getDistractorForms(WordInfo currentWord) {
        String declensionType = (currentWord == null || currentWord.declensionType() == null)
                ? ""
                : currentWord.declensionType();
        return documentRepository.getRandomWordWithAnotherDeclensionType(declensionType)
                .map(distractor -> {
                    List<String> forms = new ArrayList<>();
                    // Collect all forms across both grammatical numbers (singular=0, plural=1)
                    // and all 7 Czech grammatical cases (index 0–6).
                    for (int numberIndex = 0; numberIndex < 2; numberIndex++) {
                        for (int caseIndex = 0; caseIndex < 7; caseIndex++) {
                            String form = distractor.cases(numberIndex, caseIndex);
                            if (form != null && !form.isEmpty()) {
                                forms.add(form);
                            }
                        }
                    }
                    return forms;
                });
    }
}
