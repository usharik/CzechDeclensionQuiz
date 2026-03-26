package com.usharik.app.ads;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InterstitialAdPolicyTest {

    /** RandomProvider that always returns a value below the probability threshold. */
    private static final RandomProvider ALWAYS_SHOW = () -> 0.0;

    /** RandomProvider that always returns a value above the probability threshold. */
    private static final RandomProvider NEVER_SHOW = () -> 1.0;

    private InterstitialAdPolicy buildPolicy(RandomProvider randomProvider) {
        return new InterstitialAdPolicy(new AdSessionState(), randomProvider);
    }

    // ─── DECLENSION_WORD_COMPLETED ────────────────────────────────────────────

    @Test
    public void wordCompleted_doesNotShowBeforeThreshold() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        for (int i = 1; i < InterstitialAdPolicy.WORDS_PER_AD; i++) {
            assertFalse("Should not show at word count " + i,
                    policy.shouldShowInterstitial(AdEvent.DECLENSION_WORD_COMPLETED));
        }
    }

    @Test
    public void wordCompleted_showsAtThreshold() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        for (int i = 1; i < InterstitialAdPolicy.WORDS_PER_AD; i++) {
            policy.shouldShowInterstitial(AdEvent.DECLENSION_WORD_COMPLETED);
        }
        assertTrue(policy.shouldShowInterstitial(AdEvent.DECLENSION_WORD_COMPLETED));
    }

    @Test
    public void wordCompleted_resetsCounterAfterShow() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        // Reach threshold once
        for (int i = 0; i < InterstitialAdPolicy.WORDS_PER_AD; i++) {
            policy.shouldShowInterstitial(AdEvent.DECLENSION_WORD_COMPLETED);
        }
        // Counter should now be reset; next (WORDS_PER_AD - 1) calls must not show
        for (int i = 1; i < InterstitialAdPolicy.WORDS_PER_AD; i++) {
            assertFalse("Should not show after reset at count " + i,
                    policy.shouldShowInterstitial(AdEvent.DECLENSION_WORD_COMPLETED));
        }
    }

    // ─── DECLENSION_WRONG_ANSWER ──────────────────────────────────────────────

    @Test
    public void wrongAnswer_doesNotShowBeforeThreshold() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        for (int i = 1; i < InterstitialAdPolicy.WRONG_ATTEMPTS_PER_AD; i++) {
            assertFalse("Should not show at wrong-answer count " + i,
                    policy.shouldShowInterstitial(AdEvent.DECLENSION_WRONG_ANSWER));
        }
    }

    @Test
    public void wrongAnswer_showsAtThreshold() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        for (int i = 1; i < InterstitialAdPolicy.WRONG_ATTEMPTS_PER_AD; i++) {
            policy.shouldShowInterstitial(AdEvent.DECLENSION_WRONG_ANSWER);
        }
        assertTrue(policy.shouldShowInterstitial(AdEvent.DECLENSION_WRONG_ANSWER));
    }

    @Test
    public void wrongAnswer_resetsCounterAfterShow() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        for (int i = 0; i < InterstitialAdPolicy.WRONG_ATTEMPTS_PER_AD; i++) {
            policy.shouldShowInterstitial(AdEvent.DECLENSION_WRONG_ANSWER);
        }
        for (int i = 1; i < InterstitialAdPolicy.WRONG_ATTEMPTS_PER_AD; i++) {
            assertFalse("Should not show after reset at wrong count " + i,
                    policy.shouldShowInterstitial(AdEvent.DECLENSION_WRONG_ANSWER));
        }
    }

    // ─── SINGLE_CASE_NAVIGATION ───────────────────────────────────────────────

    @Test
    public void singleCaseNavigation_doesNotShowBeforeAttemptThreshold() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        for (int i = 1; i < InterstitialAdPolicy.NAVIGATIONS_PER_AD_ATTEMPT; i++) {
            assertFalse("Should not show at nav count " + i,
                    policy.shouldShowInterstitial(AdEvent.SINGLE_CASE_NAVIGATION));
        }
    }

    @Test
    public void singleCaseNavigation_showsAtThresholdWhenRandomAllows() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        for (int i = 1; i < InterstitialAdPolicy.NAVIGATIONS_PER_AD_ATTEMPT; i++) {
            policy.shouldShowInterstitial(AdEvent.SINGLE_CASE_NAVIGATION);
        }
        assertTrue(policy.shouldShowInterstitial(AdEvent.SINGLE_CASE_NAVIGATION));
    }

    @Test
    public void singleCaseNavigation_doesNotShowWhenRandomBlocks() {
        InterstitialAdPolicy policy = buildPolicy(NEVER_SHOW);
        for (int i = 0; i < InterstitialAdPolicy.NAVIGATIONS_PER_AD_ATTEMPT; i++) {
            assertFalse(policy.shouldShowInterstitial(AdEvent.SINGLE_CASE_NAVIGATION));
        }
    }

    @Test
    public void singleCaseNavigation_resetsCounterAfterThresholdReached() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        // Reach threshold once (ad shown)
        for (int i = 0; i < InterstitialAdPolicy.NAVIGATIONS_PER_AD_ATTEMPT; i++) {
            policy.shouldShowInterstitial(AdEvent.SINGLE_CASE_NAVIGATION);
        }
        // Counter should be reset; next (N-1) calls must not show
        for (int i = 1; i < InterstitialAdPolicy.NAVIGATIONS_PER_AD_ATTEMPT; i++) {
            assertFalse("Should not show after reset at nav count " + i,
                    policy.shouldShowInterstitial(AdEvent.SINGLE_CASE_NAVIGATION));
        }
    }

    @Test
    public void singleCaseNavigation_countersAreIndependentOfOtherEvents() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        // Fire other events many times so their counters cycle several times
        for (int i = 0; i < 50; i++) {
            policy.shouldShowInterstitial(AdEvent.DECLENSION_WORD_COMPLETED);
            policy.shouldShowInterstitial(AdEvent.DECLENSION_WRONG_ANSWER);
        }
        // Navigation counter should still be at 0; first (N-1) nav events must not show
        for (int i = 1; i < InterstitialAdPolicy.NAVIGATIONS_PER_AD_ATTEMPT; i++) {
            assertFalse("Nav counter should be independent at i=" + i,
                    policy.shouldShowInterstitial(AdEvent.SINGLE_CASE_NAVIGATION));
        }
    }
}
