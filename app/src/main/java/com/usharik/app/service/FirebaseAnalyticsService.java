package com.usharik.app.service;

import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FirebaseAnalyticsService {

    public static final String KEY_BUTTON = "BUTTON";

    private final FirebaseAnalytics firebaseAnalytics;

    @Inject
    public FirebaseAnalyticsService(FirebaseAnalytics firebaseAnalytics) {
        this.firebaseAnalytics = firebaseAnalytics;
    }

    public void setCollectionEnabled(boolean enabled) {
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled);
    }

    public void logButtonClick(String eventName, String buttonName) {
        if (eventName == null || buttonName == null) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString(KEY_BUTTON, buttonName);
        firebaseAnalytics.logEvent(eventName, bundle);
    }

    public void logNextWord(String word) {
        Bundle bundle = new Bundle();
        bundle.putString("WORD", word);
        firebaseAnalytics.logEvent("NEXT_WORD", bundle);
    }

    public void logNextWordAction(String actionName) {
        Bundle bundle = new Bundle();
        bundle.putString("NEXT_WORD_ACTION", actionName);
        firebaseAnalytics.logEvent("NEXT_WORD_ACTION", bundle);
    }

    public void logMistake(Bundle bundle) {
        if (bundle == null || bundle.isEmpty()) {
            return;
        }
        firebaseAnalytics.logEvent("MISTAKE", bundle);
    }

    public void logSettings(boolean switchOffAnimation) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("SWITCH_OFF_ANIMATION", switchOffAnimation);
        firebaseAnalytics.logEvent("SETTINGS", bundle);
    }

    public void logSingleCaseAnswer(boolean isCorrect, String selected, String correct, String word, String caseName) {
        Bundle bundle = new Bundle();
        bundle.putString("RESULT", isCorrect ? "CORRECT" : "INCORRECT");
        bundle.putString("SELECTED_ANSWER", selected);
        bundle.putString("CORRECT_ANSWER", correct);
        bundle.putString("WORD", word);
        bundle.putString("CASE", caseName);
        firebaseAnalytics.logEvent("SINGLE_CASE_ANSWER", bundle);
    }

    public void logSingleCaseNavigation(String buttonName, String word) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_BUTTON, buttonName);
        bundle.putString("WORD", word);
        firebaseAnalytics.logEvent("SINGLE_CASE_NAVIGATION", bundle);
    }

    public void logHandbookOpen() {
        Bundle bundle = new Bundle();
        bundle.putString("HANDBOOK_FRAGMENT", "OPEN");
        firebaseAnalytics.logEvent("HANDBOOK_FRAGMENT", bundle);
    }

    public void logEvent(String eventName, Bundle bundle) {
        if (eventName == null) {
            return;
        }
        firebaseAnalytics.logEvent(eventName, bundle);
    }
}
