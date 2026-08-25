package com.usharik.app.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterstitialAdPolicyTest {
    @Test fun countersReachThresholdAndReset() {
        val policy = InterstitialAdPolicy(AdSessionState()) { 0.0 }
        repeat(InterstitialAdPolicy.WORDS_PER_AD - 1) { assertFalse(policy.onDeclensionWordCompleted()) }
        assertTrue(policy.onDeclensionWordCompleted())
        repeat(InterstitialAdPolicy.WORDS_PER_AD - 1) { assertFalse(policy.onDeclensionWordCompleted()) }
    }
    @Test fun navigationRespectsProbability() {
        val policy = InterstitialAdPolicy(AdSessionState()) { 1.0 }
        repeat(InterstitialAdPolicy.NAVIGATIONS_PER_AD_ATTEMPT) { assertFalse(policy.onSingleCaseNavigation()) }
    }
}
