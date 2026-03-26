package com.usharik.app.ads;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Centralises all rules for when an ad should be shown.
 *
 * <p>Fragments call the dedicated method for each event type.  Each method
 * maintains its own counter in {@link AdSessionState} and returns whether
 * an ad should be shown right now.  No ad business logic lives in the UI layer.
 *
 * <p>Probability-based gating (Single-Case Quiz) is isolated behind
 * {@link RandomProvider} so that unit tests can inject a deterministic value.
 *
 * <p>{@link #areAdsEnabled()} is the single point to disable all ads (e.g. after
 * a purchase).  Override this method or replace the singleton binding in the DI
 * graph to suppress ads without touching any fragment code.
 */
@Singleton
public class InterstitialAdPolicy {

    static final int WORDS_PER_AD = 10;
    static final int WRONG_ATTEMPTS_PER_AD = 5;
    static final int NAVIGATIONS_PER_AD_ATTEMPT = 5;
    static final double NAVIGATION_AD_PROBABILITY = 0.4;

    private final AdSessionState sessionState;
    private final RandomProvider random;

    @Inject
    public InterstitialAdPolicy(AdSessionState sessionState, RandomProvider random) {
        this.sessionState = sessionState;
        this.random = random;
    }

    /**
     * @return {@code true} if ads are allowed to be shown.
     *
     * <p>Returns {@code true} by default.  Replace the singleton binding in the DI
     * graph (or subclass and override) to disable all ads after a purchase.
     */
    public boolean areAdsEnabled() {
        return true;
    }

    /**
     * Call when the user completes a word in the Declension Quiz.
     *
     * @return {@code true} if an interstitial should be shown now
     */
    public boolean onDeclensionWordCompleted() {
        int wordCount = sessionState.incrementWordsCount();
        if (wordCount >= WORDS_PER_AD) {
            sessionState.resetWordsCount();
            return true;
        }
        return false;
    }

    /**
     * Call when the user makes a wrong answer in the Declension Quiz.
     *
     * @return {@code true} if an interstitial should be shown now
     */
    public boolean onDeclensionWrongAnswer() {
        int wrongAttempts = sessionState.incrementWrongAttemptsCount();
        if (wrongAttempts >= WRONG_ATTEMPTS_PER_AD) {
            sessionState.resetWrongAttemptsCount();
            return true;
        }
        return false;
    }

    /**
     * Call when the user navigates to the next case or next word in the Single-Case Quiz.
     *
     * @return {@code true} if an interstitial should be shown now
     */
    public boolean onSingleCaseNavigation() {
        int navCount = sessionState.incrementNavigationCount();
        if (navCount >= NAVIGATIONS_PER_AD_ATTEMPT) {
            sessionState.resetNavigationCount();
            return random.nextDouble() < NAVIGATION_AD_PROBABILITY;
        }
        return false;
    }
}
