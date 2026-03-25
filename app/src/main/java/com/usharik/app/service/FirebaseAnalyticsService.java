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

    public void logButtonClick(String eventName, String buttonName) {
        if (eventName == null || buttonName == null) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString(KEY_BUTTON, buttonName);
        firebaseAnalytics.logEvent(eventName, bundle);
    }

    public void logEvent(String eventName, Bundle bundle) {
        if (eventName == null) {
            return;
        }
        firebaseAnalytics.logEvent(eventName, bundle);
    }
}
