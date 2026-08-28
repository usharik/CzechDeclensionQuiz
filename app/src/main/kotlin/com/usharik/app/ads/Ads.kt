package com.usharik.app.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

interface AdsPolicy { fun areAdsEnabled(): Boolean }
fun interface RandomProvider { fun nextDouble(): Double }
class ThreadLocalRandomProvider : RandomProvider { override fun nextDouble() = Random.nextDouble() }

class AdSessionState {
    private val words = AtomicInteger()
    private val navigations = AtomicInteger()

    fun nextWord() = words.incrementAndGet()
    fun resetWords() = words.set(0)

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

    /** The quiz session already establishes the visible per-word error limit. */
    fun onDeclensionErrorLimitReached(): Boolean = areAdsEnabled()

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
        const val NAVIGATIONS_PER_AD_ATTEMPT = 5
        const val NAVIGATION_AD_PROBABILITY = .4
    }
}

/**
 * Manages interstitial ads. Injected via [App.createAdManager] so instrumented tests can supply a
 * fake that never shows a real ad (a displayed interstitial has no automated way to be dismissed),
 * keeping production code free of any test awareness.
 */
interface AdManager {
    fun loadAd(activity: Activity, unitId: String)
    fun showAdIfNeeded(condition: Boolean, activity: Activity, unitId: String, action: () -> Unit)
}

class RealAdManager : AdManager {
    private val ads = mutableMapOf<String, InterstitialAd>()
    // Tracks unit IDs with a load already in flight so a rapid succession of loadAd() calls
    // (e.g. re-entering a quiz screen) doesn't fire redundant concurrent network requests.
    private val loading = mutableSetOf<String>()

    override fun loadAd(activity: Activity, unitId: String) {
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
     * [action] straight away. If the cache is empty, queue a load before continuing so a slow
     * initial load or transient load failure affects only this event, never all later ones.
     * [action] always fires exactly once regardless of the ad's outcome.
     */
    override fun showAdIfNeeded(condition: Boolean, activity: Activity, unitId: String, action: () -> Unit) {
        if (!condition) return action()
        val ad = ads.remove(unitId) ?: run {
            loadAd(activity, unitId)
            return action()
        }
        val actionDelivered = AtomicBoolean(false)
        fun finish() {
            if (actionDelivered.compareAndSet(false, true)) action()
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { loadAd(activity, unitId); finish() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Failed to show ad $unitId: ${error.message}")
                loadAd(activity, unitId)
                finish()
            }
        }
        runCatching { ad.show(activity) }
            .onFailure {
                Log.w(TAG, "Failed to start ad $unitId", it)
                loadAd(activity, unitId)
                finish()
            }
    }

    private companion object {
        const val TAG = "RealAdManager"
    }
}
