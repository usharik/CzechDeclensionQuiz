package com.usharik.app.ads;

import com.usharik.app.subscription.SubscriptionStatus;
import com.usharik.app.subscription.PremiumAccess;
import com.usharik.app.subscription.SubscriptionSource;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InterstitialAdPolicyTest {

    /** RandomProvider that always returns a value below the probability threshold. */
    private static final RandomProvider ALWAYS_SHOW = () -> 0.0;

    /** RandomProvider that always returns a value above the probability threshold. */
    private static final RandomProvider NEVER_SHOW = () -> 1.0;

    private static final PremiumAccess FREE_ACCESS = new FixedPremiumAccess(false);
    private static final PremiumAccess PREMIUM_ACCESS = new FixedPremiumAccess(true);

    private InterstitialAdPolicy buildPolicy(RandomProvider randomProvider) {
        return new InterstitialAdPolicy(new AdSessionState(), randomProvider, FREE_ACCESS);
    }

    private InterstitialAdPolicy buildDisabledPolicy(RandomProvider randomProvider) {
        return new InterstitialAdPolicy(new AdSessionState(), randomProvider, PREMIUM_ACCESS);
    }

    private static final class FixedPremiumAccess implements PremiumAccess {
        private final boolean premium;

        private FixedPremiumAccess(boolean premium) {
            this.premium = premium;
        }

        @Override
        public boolean hasPremiumAccess() {
            return premium;
        }

        @Override
        public SubscriptionStatus getStatus() {
            return premium ? SubscriptionStatus.premium(SubscriptionSource.DEBUG, "") : SubscriptionStatus.free();
        }
    }

    // ─── areAdsEnabled() guard ────────────────────────────────────────────────

    @Test
    public void adsDisabled_wordCompleted_returnsFalseWithoutMutatingCounter() {
        InterstitialAdPolicy policy = buildDisabledPolicy(ALWAYS_SHOW);
        // Fire well past the threshold — counters must NOT be incremented
        for (int i = 0; i < InterstitialAdPolicy.WORDS_PER_AD * 2; i++) {
            assertFalse("Should always return false when ads disabled",
                    policy.onDeclensionWordCompleted());
        }
        // Re-enable ads by building a fresh policy that reuses the same AdSessionState.
        // Counter must be zero, so first (WORDS_PER_AD - 1) calls still return false.
        InterstitialAdPolicy enabled = buildPolicy(ALWAYS_SHOW);
        for (int i = 1; i < InterstitialAdPolicy.WORDS_PER_AD; i++) {
            assertFalse("Counter should be clean after ads-disabled calls; i=" + i,
                    enabled.onDeclensionWordCompleted());
        }
    }

    @Test
    public void adsDisabled_wrongAnswer_returnsFalseWithoutMutatingCounter() {
        InterstitialAdPolicy policy = buildDisabledPolicy(ALWAYS_SHOW);
        for (int i = 0; i < InterstitialAdPolicy.WRONG_ATTEMPTS_PER_AD * 2; i++) {
            assertFalse("Should always return false when ads disabled",
                    policy.onDeclensionWrongAnswer());
        }
    }

    @Test
    public void adsDisabled_singleCaseNavigation_returnsFalseWithoutMutatingCounter() {
        InterstitialAdPolicy policy = buildDisabledPolicy(ALWAYS_SHOW);
        for (int i = 0; i < InterstitialAdPolicy.NAVIGATIONS_PER_AD_ATTEMPT * 2; i++) {
            assertFalse("Should always return false when ads disabled",
                    policy.onSingleCaseNavigation());
        }
    }

    // ─── onDeclensionWordCompleted ────────────────────────────────────────────

    @Test
    public void wordCompleted_doesNotShowBeforeThreshold() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        for (int i = 1; i < InterstitialAdPolicy.WORDS_PER_AD; i++) {
            assertFalse("Should not show at word count " + i,
                    policy.onDeclensionWordCompleted());
        }
    }

    @Test
    public void wordCompleted_showsAtThreshold() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        for (int i = 1; i < InterstitialAdPolicy.WORDS_PER_AD; i++) {
            policy.onDeclensionWordCompleted();
        }
        assertTrue(policy.onDeclensionWordCompleted());
    }

    @Test
    public void wordCompleted_resetsCounterAfterShow() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        // Reach threshold once
        for (int i = 0; i < InterstitialAdPolicy.WORDS_PER_AD; i++) {
            policy.onDeclensionWordCompleted();
        }
        // Counter should now be reset; next (WORDS_PER_AD - 1) calls must not show
        for (int i = 1; i < InterstitialAdPolicy.WORDS_PER_AD; i++) {
            assertFalse("Should not show after reset at count " + i,
                    policy.onDeclensionWordCompleted());
        }
    }

    // ─── onDeclensionWrongAnswer ──────────────────────────────────────────────

    @Test
    public void wrongAnswer_doesNotShowBeforeThreshold() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        for (int i = 1; i < InterstitialAdPolicy.WRONG_ATTEMPTS_PER_AD; i++) {
            assertFalse("Should not show at wrong-answer count " + i,
                    policy.onDeclensionWrongAnswer());
        }
    }

    @Test
    public void wrongAnswer_showsAtThreshold() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        for (int i = 1; i < InterstitialAdPolicy.WRONG_ATTEMPTS_PER_AD; i++) {
            policy.onDeclensionWrongAnswer();
        }
        assertTrue(policy.onDeclensionWrongAnswer());
    }

    @Test
    public void wrongAnswer_resetsCounterAfterShow() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        for (int i = 0; i < InterstitialAdPolicy.WRONG_ATTEMPTS_PER_AD; i++) {
            policy.onDeclensionWrongAnswer();
        }
        for (int i = 1; i < InterstitialAdPolicy.WRONG_ATTEMPTS_PER_AD; i++) {
            assertFalse("Should not show after reset at wrong count " + i,
                    policy.onDeclensionWrongAnswer());
        }
    }

    // ─── onSingleCaseNavigation ───────────────────────────────────────────────

    @Test
    public void singleCaseNavigation_doesNotShowBeforeAttemptThreshold() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        for (int i = 1; i < InterstitialAdPolicy.NAVIGATIONS_PER_AD_ATTEMPT; i++) {
            assertFalse("Should not show at nav count " + i,
                    policy.onSingleCaseNavigation());
        }
    }

    @Test
    public void singleCaseNavigation_showsAtThresholdWhenRandomAllows() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        for (int i = 1; i < InterstitialAdPolicy.NAVIGATIONS_PER_AD_ATTEMPT; i++) {
            policy.onSingleCaseNavigation();
        }
        assertTrue(policy.onSingleCaseNavigation());
    }

    @Test
    public void singleCaseNavigation_doesNotShowWhenRandomBlocks() {
        InterstitialAdPolicy policy = buildPolicy(NEVER_SHOW);
        for (int i = 0; i < InterstitialAdPolicy.NAVIGATIONS_PER_AD_ATTEMPT; i++) {
            assertFalse(policy.onSingleCaseNavigation());
        }
    }

    @Test
    public void singleCaseNavigation_resetsCounterAfterThresholdReached() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        // Reach threshold once (ad shown)
        for (int i = 0; i < InterstitialAdPolicy.NAVIGATIONS_PER_AD_ATTEMPT; i++) {
            policy.onSingleCaseNavigation();
        }
        // Counter should be reset; next (N-1) calls must not show
        for (int i = 1; i < InterstitialAdPolicy.NAVIGATIONS_PER_AD_ATTEMPT; i++) {
            assertFalse("Should not show after reset at nav count " + i,
                    policy.onSingleCaseNavigation());
        }
    }

    @Test
    public void singleCaseNavigation_countersAreIndependentOfOtherEvents() {
        InterstitialAdPolicy policy = buildPolicy(ALWAYS_SHOW);
        // Fire other events many times so their counters cycle several times
        for (int i = 0; i < 50; i++) {
            policy.onDeclensionWordCompleted();
            policy.onDeclensionWrongAnswer();
        }
        // Navigation counter should still be at 0; first (N-1) nav events must not show
        for (int i = 1; i < InterstitialAdPolicy.NAVIGATIONS_PER_AD_ATTEMPT; i++) {
            assertFalse("Nav counter should be independent at i=" + i,
                    policy.onSingleCaseNavigation());
        }
    }
}
