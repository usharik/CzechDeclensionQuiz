package com.usharik.app.ads;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Holds ad-related session counters used by {@link InterstitialAdPolicy}.
 * Extracted from AppState so that ad state is owned exclusively by the ad subsystem.
 */
@Singleton
public class AdSessionState {

    private final AtomicInteger wordsCount = new AtomicInteger(0);
    private final AtomicInteger wrongAttemptsCount = new AtomicInteger(0);
    private final AtomicInteger navigationCount = new AtomicInteger(0);

    @Inject
    public AdSessionState() {
    }

    public int incrementWordsCount() {
        return wordsCount.incrementAndGet();
    }

    public void resetWordsCount() {
        wordsCount.set(0);
    }

    public int incrementWrongAttemptsCount() {
        return wrongAttemptsCount.incrementAndGet();
    }

    public void resetWrongAttemptsCount() {
        wrongAttemptsCount.set(0);
    }

    public int incrementNavigationCount() {
        return navigationCount.incrementAndGet();
    }

    public void resetNavigationCount() {
        navigationCount.set(0);
    }
}
