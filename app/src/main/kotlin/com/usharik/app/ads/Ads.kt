package com.usharik.app.ads

import android.app.Activity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

interface AdsPolicy { fun areAdsEnabled(): Boolean }
fun interface RandomProvider { fun nextDouble(): Double }
class ThreadLocalRandomProvider : RandomProvider { override fun nextDouble() = Random.nextDouble() }
class AdSessionState {
    private val words = AtomicInteger(); private val wrongAttempts = AtomicInteger(); private val navigations = AtomicInteger()
    fun nextWord() = words.incrementAndGet(); fun resetWords() = words.set(0)
    fun nextWrongAttempt() = wrongAttempts.incrementAndGet(); fun resetWrongAttempts() = wrongAttempts.set(0)
    fun nextNavigation() = navigations.incrementAndGet(); fun resetNavigations() = navigations.set(0)
}
open class InterstitialAdPolicy(private val state: AdSessionState, private val random: RandomProvider) : AdsPolicy {
    override fun areAdsEnabled() = true
    fun onDeclensionWordCompleted() = if (!areAdsEnabled()) false else state.nextWord().let { if (it >= WORDS_PER_AD) { state.resetWords(); true } else false }
    fun onDeclensionWrongAnswer() = if (!areAdsEnabled()) false else state.nextWrongAttempt().let { if (it >= WRONG_ATTEMPTS_PER_AD) { state.resetWrongAttempts(); true } else false }
    fun onSingleCaseNavigation() = if (!areAdsEnabled()) false else state.nextNavigation().let { if (it >= NAVIGATIONS_PER_AD_ATTEMPT) { state.resetNavigations(); random.nextDouble() < NAVIGATION_AD_PROBABILITY } else false }
    companion object { const val WORDS_PER_AD = 10; const val WRONG_ATTEMPTS_PER_AD = 5; const val NAVIGATIONS_PER_AD_ATTEMPT = 5; const val NAVIGATION_AD_PROBABILITY = .4 }
}
class AdManager {
    private val ads = mutableMapOf<String, InterstitialAd>()
    fun loadAd(activity: Activity, unitId: String) { if (unitId.isBlank() || ads.containsKey(unitId)) return; InterstitialAd.load(activity, unitId, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() { override fun onAdLoaded(ad: InterstitialAd) { ads[unitId] = ad } }) }
    fun showAdIfNeeded(condition: Boolean, activity: Activity, unitId: String, action: () -> Unit) { if (!condition) return action(); val ad = ads.remove(unitId) ?: return action(); ad.fullScreenContentCallback = object : FullScreenContentCallback() { override fun onAdDismissedFullScreenContent() { loadAd(activity, unitId); action() }; override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) { loadAd(activity, unitId); action() } }; ad.show(activity) }
}
