package com.usharik.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.usharik.app.ui.theme.AppTheme

// AppCompatActivity (not ComponentActivity) so AppCompatDelegate application locales
// are applied on API 28-32 as well.
class MainActivity : AppCompatActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) (application as App).notificationHelper.showWelcomeNotificationIfNeeded(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Honor the review deep link only on a fresh launch from the notification: a recreated
        // activity restores its own page, and Recents replays the original intent.
        val launchedFromHistory = ((intent?.flags ?: 0) and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
        val startInReview = savedInstanceState == null && !launchedFromHistory &&
            intent?.getStringExtra(EXTRA_DESTINATION) == DESTINATION_REVIEW
        setContent {
            AppTheme { CzechQuizApp(application as App, startInReview = startInReview) }
        }
        if (!UiLanguageManager.hasSavedLanguage(this)) {
            // The Compose settings screen also exposes this choice; applying SYSTEM makes the initial state explicit.
            UiLanguageManager.applyLanguage(UiLanguage.SYSTEM)
        }
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        // Keep the full-quiz interstitial warm before the player can reach either its error or
        // timeout boundary. AdManager deduplicates cached and in-flight loads.
        (application as App).adManager.loadAd(this, BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID)
    }

    companion object {
        /** Notification deep link: which screen to open instead of the hub. */
        const val EXTRA_DESTINATION = "destination"
        const val DESTINATION_REVIEW = "review"
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Pre-33 devices (no runtime permission) and already-granted devices get the welcome notification directly.
            (application as App).notificationHelper.showWelcomeNotificationIfNeeded(this)
        }
    }
}
