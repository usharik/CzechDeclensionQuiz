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
import com.google.gson.JsonParser
import com.usharik.app.ads.AdManager
import com.usharik.app.ads.AdSessionState
import com.usharik.app.ads.InterstitialAdPolicy
import com.usharik.app.ads.RealAdManager
import com.usharik.app.ads.ThreadLocalRandomProvider
import com.usharik.app.billing.AdFreeEntitlement
import com.usharik.app.billing.PlayPurchaseManager
import com.usharik.app.billing.PurchaseManager
import com.usharik.app.billing.SupportOfferStore
import com.usharik.app.notification.DailyReminderWorker
import com.usharik.app.notification.NotificationHelper
import com.usharik.app.service.FirebaseAnalyticsService
import com.usharik.app.service.SharedPreferencesLastWordStore
import com.usharik.app.service.WordService
import com.usharik.app.ui.state.DailyGoal
import com.usharik.database.DocumentRepository
import com.usharik.database.TrainingStatsRepository
import com.usharik.database.dao.DatabaseFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    lateinit var adFree: AdFreeEntitlement; private set
    lateinit var purchaseManager: PurchaseManager; private set
    lateinit var supportOfferStore: SupportOfferStore; private set
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
        documentRepository = DocumentRepository(database)
        statsRepository = TrainingStatsRepository(database)
        analyticsService = FirebaseAnalyticsService(FirebaseAnalytics.getInstance(this))
        notificationHelper = NotificationHelper(analyticsService)
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        adFree = AdFreeEntitlement(prefs.getBoolean(PREF_AD_FREE, false)) { prefs.edit().putBoolean(PREF_AD_FREE, it).apply() }
        adManager = createAdManager()
        adPolicy = InterstitialAdPolicy(AdSessionState(), { !adFree.isAdFree() }, ThreadLocalRandomProvider())
        purchaseManager = createPurchaseManager()
        supportOfferStore = SupportOfferStore(prefs)
        lastWordStore = SharedPreferencesLastWordStore(this)
        wordService = WordService(documentRepository, appState, analyticsService)

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        analyticsService.setCollectionEnabled(!BuildConfig.DEBUG)
        if (!adFree.isAdFree()) MobileAds.initialize(this) { Log.i("App", "Mobile Ads initialized") }
        purchaseManager.refresh()
        notificationHelper.createChannel(this)
        scheduleDailyReminderWorker()
        // The import runs off the main thread so a first launch renders immediately;
        // word-loading screens show their loading state until dictionaryReady completes.
        dictionaryReady = appScope.async {
            // Installs whose rows predate the bundled dictionary (e.g. without the uk/vi glosses) reload it;
            // the version is stored only after a complete import, so an interrupted one is retried.
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val dictionaryOutdated = prefs.getInt(PREF_DICTIONARY_VERSION, 0) < DICTIONARY_VERSION
            if (dictionaryOutdated) documentRepository.clearDictionaries()
            if (documentRepository.count() == 0) {
                assets.open("data.jsonl").use { stream -> documentRepository.populateFromJsonStream(stream) }
            }
            // Adjectives and verbs arrived in a later version: existing installs get the tables from the
            // 10→11 migration and fill them here on their first start after the update.
            if (documentRepository.adjectiveCount() == 0) {
                assets.open("adjectives.jsonl").use { stream -> documentRepository.populateAdjectivesFromJsonStream(stream) }
            }
            if (documentRepository.verbCount() == 0) {
                assets.open("verbs.jsonl").use { stream -> documentRepository.populateVerbsFromJsonStream(stream) }
            }
            if (dictionaryOutdated) prefs.edit().putInt(PREF_DICTIONARY_VERSION, DICTIONARY_VERSION).apply()
        }
        restorePreferences()
    }

    /** Overridable so instrumented tests can inject a fake that never shows a real interstitial. */
    open fun createAdManager(): AdManager = RealAdManager { !adFree.isAdFree() }

    /** First install day; a reinstall starts the support-offer waiting period again. */
    val installDate: LocalDate by lazy {
        val installed = runCatching { packageManager.getPackageInfo(packageName, 0).firstInstallTime }.getOrDefault(System.currentTimeMillis())
        Instant.ofEpochMilli(installed).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    /** Overridable so instrumented tests never talk to Google Play. */
    open fun createPurchaseManager(): PurchaseManager = PlayPurchaseManager(this, adFree) { analyticsService.logEvent(it) }

    private fun restorePreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        appState.setGenderFilterStr(prefs.getString(PREF_GENDER_FILTER, Gender.ALL))
        appState.setSwitchOffAnimation(prefs.getBoolean(PREF_SWITCH_OFF_ANIMATION, false))
        appState.setDailyGoal(prefs.getInt(PREF_DAILY_GOAL, DailyGoal.DEFAULT.points))
        appState.setPartOfSpeech(PartOfSpeech.fromName(prefs.getString(PREF_PART_OF_SPEECH, null)))
        // Parsed by hand rather than through a TypeToken, which relies on R8 keeping generic signatures.
        appState.setWordsWithErrors(
            runCatching {
                JsonParser.parseString(prefs.getString(PREF_WORDS_WITH_ERRORS, null) ?: "{}").asJsonObject
                    .entrySet().associate { (word, count) -> word to count.asInt }
            }.getOrDefault(emptyMap()),
        )
    }

    fun persistPartOfSpeech(partOfSpeech: PartOfSpeech) {
        appState.setPartOfSpeech(partOfSpeech)
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(PREF_PART_OF_SPEECH, partOfSpeech.name).apply()
    }

    fun persistWordsWithErrors() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(PREF_WORDS_WITH_ERRORS, gson.toJson(appState.getWordsWithErrors()))
            .apply()
    }

    /**
     * Hourly check: [DailyReminderWorker] picks the player's usual practice hour and the evening
     * streak-rescue slot itself, so the schedule no longer pins every reminder to 9:00.
     */
    private fun scheduleDailyReminderWorker() {
        val workManager = WorkManager.getInstance(this)
        workManager.cancelUniqueWork(LEGACY_DAILY_REMINDER_WORK)
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(REMINDER_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    companion object {
        const val PREFS_NAME = "czech_declension_quiz"
        const val PREF_GENDER_FILTER = "genderFilterStr"
        const val PREF_SWITCH_OFF_ANIMATION = "switchOffAnimation"
        const val PREF_WORDS_WITH_ERRORS = "WORDS_WITH_ERRORS"
        const val PREF_DAILY_GOAL = "dailyGoalPoints"
        const val PREF_PART_OF_SPEECH = "partOfSpeech"
        const val PREF_DICTIONARY_VERSION = "dictionaryVersion"
        const val PREF_AD_FREE = "adFree"
        /** Bump whenever the bundled JSONL dictionaries change so existing installs re-import them. */
        const val DICTIONARY_VERSION = 1
        private const val LEGACY_DAILY_REMINDER_WORK = "daily_reminder"
        private const val REMINDER_WORK = "hourly_reminder"
    }
}
