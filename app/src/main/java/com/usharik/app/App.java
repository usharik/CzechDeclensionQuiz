package com.usharik.app;

import android.app.Application;

import android.content.SharedPreferences;

import android.util.Log;

import com.google.android.gms.ads.MobileAds;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.usharik.app.service.FirebaseAnalyticsService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.usharik.database.DocumentRepository;
import com.usharik.app.di.DaggerAppComponent;

import java.lang.reflect.Type;
import java.util.HashMap;

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

    @Override
    public void onCreate() {
        super.onCreate();
        DaggerAppComponent.factory().create(this).inject(this);

        Log.i(getClass().getName(), "Application start!!!");

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG);
        analyticsService.setCollectionEnabled(!BuildConfig.DEBUG);

        // Initialize Mobile Ads SDK
        Log.i(getClass().getName(), "Initializing Mobile Ads SDK!!!");
        MobileAds.initialize(this, initializationStatus -> {
            Log.i(getClass().getName(), "Mobile Ads SDK initialized");
        });

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
}
