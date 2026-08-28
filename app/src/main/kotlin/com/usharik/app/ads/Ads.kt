package com.usharik.app.ads

import android.app.Activity
import android.app.ActivityManager
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

interface AdsPolicy { fun areAdsEnabled(): Boolean }
fun interface RandomProvider { fun nextDouble(): Double }
class ThreadLocalRandomProvider : RandomProvider { override fun nextDouble() = Random.nextDouble() }

class AdSessionState {
    private val words = AtomicInteger()
    private val wrongAttempts = AtomicInteger()
    private val navigations = AtomicInteger()

    fun nextWord() = words.incrementAndGet()
    fun resetWords() = words.set(0)

    fun nextWrongAttempt() = wrongAttempts.incrementAndGet()
    fun resetWrongAttempts() = wrongAttempts.set(0)

    fun nextNavigation() = navigations.incrementAndGet()
    fun resetNavigations() = navigations.set(0)
}

open class InterstitialAdPolicy(private val state: AdSessionState, private val random: RandomProvider) : AdsPolicy {
    override fun areAdsEnabled() = true

    fun onDeclensionWordCompleted(): Boolean {
        if (!areAdsEnabled()) return false
        val count = state.nextWord()
        if (count < WORDS_PER_AD) return false
        state.resetWords()
        return true
    }

    fun onDeclensionWrongAnswer(): Boolean {
        if (!areAdsEnabled()) return false
        val count = state.nextWrongAttempt()
        if (count < WRONG_ATTEMPTS_PER_AD) return false
        state.resetWrongAttempts()
        return true
    }

    fun onDeclensionTimeout(): Boolean = areAdsEnabled()

    fun onSingleCaseNavigation(): Boolean {
        if (!areAdsEnabled()) return false
        val count = state.nextNavigation()
        if (count < NAVIGATIONS_PER_AD_ATTEMPT) return false
        state.resetNavigations()
        return random.nextDouble() < NAVIGATION_AD_PROBABILITY
    }

    companion object {
        const val WORDS_PER_AD = 10
        const val WRONG_ATTEMPTS_PER_AD = 5
        const val NAVIGATIONS_PER_AD_ATTEMPT = 5
        const val NAVIGATION_AD_PROBABILITY = .4
    }
}

class AdManager {
    private val ads = mutableMapOf<String, InterstitialAd>()
    // Tracks unit IDs with a load already in flight so a rapid succession of loadAd() calls
    // (e.g. re-entering a quiz screen) doesn't fire redundant concurrent network requests.
    private val loading = mutableSetOf<String>()

    fun loadAd(activity: Activity, unitId: String) {
        // Never load a real ad under instrumentation: an actually-displayed interstitial has no
        // automated way to be dismissed (the test ad's close button isn't tapped), which would
        // hang any test that crosses an ad-policy threshold. With nothing cached, showAdIfNeeded
        // always falls through to its action() callback instead, keeping tests deterministic.
        if (activity.isTestHarness()) return
        if (unitId.isBlank() || ads.containsKey(unitId) || !loading.add(unitId)) return
        InterstitialAd.load(
            activity,
            unitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { loading.remove(unitId); ads[unitId] = ad }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading.remove(unitId)
                    Log.w(TAG, "Failed to load ad $unitId: ${error.message}")
                }
            },
        )
    }

    /**
     * Shows the cached ad for [unitId] if [condition] holds and one is ready, otherwise runs
     * [action] straight away. Either way, a fresh ad is queued afterwards so the next call has
     * one ready, and [action] always fires exactly once regardless of the ad's outcome.
     */
    fun showAdIfNeeded(condition: Boolean, activity: Activity, unitId: String, action: () -> Unit) {
        if (!condition) return action()
        val ad = ads.remove(unitId) ?: return action()
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { loadAd(activity, unitId); action() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Failed to show ad $unitId: ${error.message}")
                loadAd(activity, unitId)
                action()
            }
        }
        ad.show(activity)
    }

    private companion object {
        const val TAG = "AdManager"
    }
}

/** True when the process was launched by an instrumentation test runner (e.g. connectedAndroidTest). */
internal fun Activity.isTestHarness(): Boolean = ActivityManager.isRunningInTestHarness()
