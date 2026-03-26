package com.usharik.app.ads;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Holds ad-related session counters used by {@link InterstitialAdPolicy}.
 * Extracted from AppState so that ad state is owned exclusively by the ad subsystem.
 */
@Singleton
public class AdSessionState {

    private int wordsCount = 0;
    private int wrongAttemptsCount = 0;
    private int navigationCount = 0;

    @Inject
    public AdSessionState() {
    }

    public int incrementWordsCount() {
        return ++wordsCount;
    }

    public void resetWordsCount() {
        wordsCount = 0;
    }

    public int incrementWrongAttemptsCount() {
        return ++wrongAttemptsCount;
    }

    public void resetWrongAttemptsCount() {
        wrongAttemptsCount = 0;
    }

    public int incrementNavigationCount() {
        return ++navigationCount;
    }

    public void resetNavigationCount() {
        navigationCount = 0;
    }
}
