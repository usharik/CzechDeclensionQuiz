package com.usharik.app

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.usharik.app.ads.AdManager
import com.usharik.app.ads.AdSessionState
import com.usharik.app.ads.InterstitialAdPolicy
import com.usharik.app.ads.RealAdManager
import com.usharik.app.ads.ThreadLocalRandomProvider
import com.usharik.app.notification.DailyReminderWorker
import com.usharik.app.notification.NotificationHelper
import com.usharik.app.service.FirebaseAnalyticsService
import com.usharik.app.service.SharedPreferencesLastWordStore
import com.usharik.app.service.WordService
import com.usharik.database.DocumentRepository
import com.usharik.database.TrainingStatsRepository
import com.usharik.database.dao.DatabaseFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Application-owned dependency graph. It replaces the Dagger Android graph with explicit, typed wiring. */
open class App : Application() {
    lateinit var appState: AppState; private set
    lateinit var gson: Gson; private set
    lateinit var documentRepository: DocumentRepository; private set
    lateinit var statsRepository: TrainingStatsRepository; private set
    lateinit var analyticsService: FirebaseAnalyticsService; private set
    lateinit var notificationHelper: NotificationHelper; private set
    lateinit var adManager: AdManager; private set
    lateinit var adPolicy: InterstitialAdPolicy; private set
    lateinit var wordService: WordService; private set
    lateinit var lastWordStore: SharedPreferencesLastWordStore; private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Completes once the first-launch dictionary import has finished; screens await it before querying words. */
    lateinit var dictionaryReady: Deferred<Unit>; private set

    override fun onCreate() {
        super.onCreate()
        UiLanguageManager.applySavedLanguage(this)
        gson = Gson()
        appState = AppState()
        val database = DatabaseFactory.provideDocumentDatabase(this)
        documentRepository = DocumentRepository(database, gson)
        statsRepository = TrainingStatsRepository(database)
        analyticsService = FirebaseAnalyticsService(FirebaseAnalytics.getInstance(this))
        notificationHelper = NotificationHelper(analyticsService)
        adManager = createAdManager()
        adPolicy = InterstitialAdPolicy(AdSessionState(), ThreadLocalRandomProvider())
        lastWordStore = SharedPreferencesLastWordStore(this)
        wordService = WordService(documentRepository, appState, analyticsService)

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        analyticsService.setCollectionEnabled(!BuildConfig.DEBUG)
        MobileAds.initialize(this) { Log.i("App", "Mobile Ads initialized") }
        notificationHelper.createChannel(this)
        scheduleDailyReminderWorker()
        // The import runs off the main thread so a first launch renders immediately;
        // word-loading screens show their loading state until dictionaryReady completes.
        dictionaryReady = appScope.async {
            if (documentRepository.count() == 0) {
                assets.open("data.jsonl").use { stream -> documentRepository.populateFromJsonStream(stream) }
            }
        }
        restorePreferences()
    }

    /** Overridable so instrumented tests can inject a fake that never shows a real interstitial. */
    open fun createAdManager(): AdManager = RealAdManager()

    private fun restorePreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        appState.setGenderFilterStr(prefs.getString(PREF_GENDER_FILTER, Gender.ALL))
        appState.setSwitchOffAnimation(prefs.getBoolean(PREF_SWITCH_OFF_ANIMATION, false))
        val errorsType = object : TypeToken<HashMap<String, Int>>() {}.type
        appState.setWordsWithErrors(runCatching { gson.fromJson<HashMap<String, Int>>(prefs.getString(PREF_WORDS_WITH_ERRORS, "{}"), errorsType) }.getOrDefault(hashMapOf()))
    }

    fun persistWordsWithErrors() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(PREF_WORDS_WITH_ERRORS, gson.toJson(appState.getWordsWithErrors()))
            .apply()
    }

    private fun scheduleDailyReminderWorker() {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(9, 0)
        if (!now.isBefore(next)) next = next.plusDays(1)
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(DAILY_REMINDER_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    companion object {
        const val PREFS_NAME = "czech_declension_quiz"
        const val PREF_GENDER_FILTER = "genderFilterStr"
        const val PREF_SWITCH_OFF_ANIMATION = "switchOffAnimation"
        const val PREF_WORDS_WITH_ERRORS = "WORDS_WITH_ERRORS"
        private const val DAILY_REMINDER_WORK = "daily_reminder"
    }
}
