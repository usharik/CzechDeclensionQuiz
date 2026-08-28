package com.usharik.app

import android.app.Activity
import com.usharik.app.ads.AdManager

/**
 * [App] variant used by instrumented tests. It injects a fake [AdManager] that never shows a real
 * interstitial (a displayed ad activity has no automated way to be dismissed and would hang any
 * test crossing an ad-policy threshold). [FakeAdManager.showAdIfNeeded] always runs the action
 * straight away, reproducing the "no cached ad" fall-through of the real manager, so tests remain
 * deterministic without any test awareness leaking into production code.
 */
class TestApp : App() {
    override fun createAdManager(): AdManager = FakeAdManager()
}

private class FakeAdManager : AdManager {
    override fun loadAd(activity: Activity, unitId: String) = Unit

    override fun showAdIfNeeded(condition: Boolean, activity: Activity, unitId: String, action: () -> Unit) {
        action()
    }
}
