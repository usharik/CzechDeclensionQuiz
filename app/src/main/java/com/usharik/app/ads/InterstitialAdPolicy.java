package com.usharik.app.ads;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Centralises all rules for when an interstitial ad should be shown.
 *
 * <p>Fragments report events via {@link #shouldShowInterstitial(AdEvent)};
 * this class maintains counters in {@link AdSessionState} and returns whether
 * an ad should be shown right now.  No ad business logic lives in the UI layer.
 *
 * <p>Probability-based gating (Single-Case Quiz) is isolated behind
 * {@link RandomProvider} so that unit tests can inject a deterministic value.
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
     * Evaluate whether an interstitial ad should be shown for the given event.
     * Internal counters are updated (and reset when a show is triggered) inside this call.
     *
     * @param event the ad event that just occurred
     * @return {@code true} if an interstitial should be shown now
     */
    public boolean shouldShowInterstitial(AdEvent event) {
        switch (event) {
            case DECLENSION_WORD_COMPLETED: {
                int wordCount = sessionState.incrementWordsCount();
                if (wordCount >= WORDS_PER_AD) {
                    sessionState.resetWordsCount();
                    return true;
                }
                return false;
            }
            case DECLENSION_WRONG_ANSWER: {
                int wrongAttempts = sessionState.incrementWrongAttemptsCount();
                if (wrongAttempts >= WRONG_ATTEMPTS_PER_AD) {
                    sessionState.resetWrongAttemptsCount();
                    return true;
                }
                return false;
            }
            case SINGLE_CASE_NAVIGATION: {
                int navCount = sessionState.incrementNavigationCount();
                if (navCount >= NAVIGATIONS_PER_AD_ATTEMPT) {
                    sessionState.resetNavigationCount();
                    return random.nextDouble() < NAVIGATION_AD_PROBABILITY;
                }
                return false;
            }
            default:
                return false;
        }
    }
}
