
package com.usharik.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Instrumentation runner that swaps the production [App] for [TestApp], so instrumented tests run
 * against a fake [com.usharik.app.ads.AdManager] instead of one that would try to display a real,
 * undismissable interstitial. Wired via `testInstrumentationRunner` in build.gradle.
 */
class TestAppRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, TestApp::class.java.name, context)
}
