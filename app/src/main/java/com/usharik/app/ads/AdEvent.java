package com.usharik.app.ads;

/**
 * Events that can trigger interstitial ad display.
 * Fragments report these events to {@link InterstitialAdPolicy} which decides whether to show an ad.
 */
public enum AdEvent {
    /** User completed a word in the Declension Quiz. */
    DECLENSION_WORD_COMPLETED,
    /** User made a wrong answer in the Declension Quiz. */
    DECLENSION_WRONG_ANSWER,
    /** User navigated to the next case or next word in the Single-Case Quiz. */
    SINGLE_CASE_NAVIGATION
}
