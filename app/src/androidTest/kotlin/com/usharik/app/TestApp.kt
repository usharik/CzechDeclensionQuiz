package com.usharik.app

import android.app.Activity
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import com.usharik.app.ads.AdManager
import com.usharik.app.billing.PurchaseManager
import com.usharik.app.billing.RemoveAdsOffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * [App] variant used by instrumented tests. It injects a fake [AdManager] that never shows a real
 * interstitial (a displayed ad activity has no automated way to be dismissed and would hang any
 * test crossing an ad-policy threshold). [FakeAdManager.showAdIfNeeded] always runs the action
 * straight away, reproducing the "no cached ad" fall-through of the real manager, so tests remain
 * deterministic without any test awareness leaking into production code.
 *
 * It also pins the UI language to English before [App.onCreate] applies the saved language, since
 * the tests match English labels and must not depend on the device locale. On API 33+ the framework
 * [LocaleManager] is set directly: AppCompat's setter is a no-op until an activity exists.
 */
class TestApp : App() {
    override fun onCreate() {
        // The tests drive the noun quizzes: pin the hub's word class, since a manual session on the
        // same device may have left adjectives, verbs or phrases selected.
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putString(UiLanguageManager.UI_LANGUAGE_KEY, UiLanguage.ENGLISH.preferenceValue())
            .putString(PREF_PART_OF_SPEECH, PartOfSpeech.NOUN.name)
            .putBoolean(PREF_AD_FREE, false)
            .commit()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(UiLanguage.ENGLISH.languageTags())
        }
        super.onCreate()
    }

    override fun createAdManager(): AdManager = FakeAdManager()

    override fun createPurchaseManager(): PurchaseManager = FakePurchaseManager()
}

/** Never contacts Google Play; the ad-free entitlement keeps whatever the test preferences hold. */
private class FakePurchaseManager : PurchaseManager {
    override val offer: StateFlow<RemoveAdsOffer> = MutableStateFlow(RemoveAdsOffer())
    override fun refresh() = Unit
    override fun launchRemoveAdsPurchase(activity: Activity) = false
}

private class FakeAdManager : AdManager {
    override fun loadAd(activity: Activity, unitId: String) = Unit

    override fun showAdIfNeeded(condition: Boolean, activity: Activity, unitId: String, action: () -> Unit) {
        action()
    }
}
