package com.usharik.app;

import android.app.Application;

import android.content.SharedPreferences;

import android.util.Log;

import androidx.work.Configuration;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.ads.MobileAds;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.usharik.app.di.AppWorkerFactory;
import com.usharik.app.notification.DailyReminderWorker;
import com.usharik.app.notification.NotificationHelper;
import com.usharik.app.service.FirebaseAnalyticsService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.usharik.database.DocumentRepository;
import com.usharik.app.di.DaggerAppComponent;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static com.usharik.app.fragment.DeclensionQuizFragment.WORDS_WITH_ERRORS;
import static com.usharik.app.fragment.SettingsFragment.GENDER_FILTER_KEY;
import static com.usharik.app.fragment.SettingsFragment.SHARED_PREFERENCES;
import static com.usharik.app.fragment.SettingsFragment.SWITCH_OFF_ANIMATION;

/**
 * Created by macbook on 08.02.18.
 */

public class App extends Application implements HasAndroidInjector {

    @Inject
    DispatchingAndroidInjector<Object> dispatchingAndroidInjector;

    @Inject
    DocumentRepository documentRepository;

    @Inject
    AppState appState;

    @Inject
    Gson gson;

    @Inject
    FirebaseAnalyticsService analyticsService;

    @Inject
    AppWorkerFactory workerFactory;

    @Inject
    NotificationHelper notificationHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        DaggerAppComponent.factory().create(this).inject(this);

        // Configure WorkManager with custom WorkerFactory for DI support
        WorkManager.initialize(
                this,
                new Configuration.Builder()
                        .setWorkerFactory(workerFactory)
                        .build()
        );

        Log.i(getClass().getName(), "Application start!!!");

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG);
        analyticsService.setCollectionEnabled(!BuildConfig.DEBUG);

        // Initialize Mobile Ads SDK
        Log.i(getClass().getName(), "Initializing Mobile Ads SDK!!!");
        MobileAds.initialize(this, initializationStatus -> {
            Log.i(getClass().getName(), "Mobile Ads SDK initialized");
        });

        // Set up the daily reminder notification channel and schedule the worker
        notificationHelper.createChannel(this);
        scheduleDailyReminderWorker();

        documentRepository.getCount()
                .flatMapCompletable(cnt -> {
                    if (cnt == 0) {
                        Log.i(getClass().getName(), "Populating empty database!!!");
                        documentRepository.populateFromJsonStream(getAssets().open("data.jsonl"));
                    }
                    return Completable.complete();
                })
                .subscribeOn(Schedulers.io())
                .blockingAwait();

        Log.i(getClass().getName(), "Loading preferences!!!");
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        appState.setGenderFilterStr(prefs.getString(GENDER_FILTER_KEY, Gender.ALL));
        appState.setSwitchOffAnimation(prefs.getBoolean(SWITCH_OFF_ANIMATION, false));
        String str = prefs.getString(WORDS_WITH_ERRORS, "{}");
        try {
            Type type = new TypeToken<HashMap<String, Integer>>() {
            }.getType();
            HashMap<String, Integer> wordsWithErrors = gson.fromJson(str, type);
            appState.setWordsWithErrors(wordsWithErrors);
        } catch (Exception ex) {
            Log.e(getClass().getName(), "Exception", ex);
            FirebaseCrashlytics.getInstance().recordException(ex);
            appState.setWordsWithErrors(new HashMap<>());
        }
    }

    @Override
    public AndroidInjector<Object> androidInjector() {
        return dispatchingAndroidInjector;
    }

    private void scheduleDailyReminderWorker() {
        // Calculate how many hours until the next 9:00 AM
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next9am = now.toLocalDate().atTime(9, 0);
        if (!now.isBefore(next9am)) {
            next9am = next9am.plusDays(1);
        }
        long initialDelayMinutes = java.time.Duration.between(now, next9am).toMinutes();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                DailyReminderWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "daily_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                request);

        Log.i(getClass().getName(), "Daily reminder worker scheduled, first run in ~"
                + initialDelayMinutes + " minutes");
    }
}
