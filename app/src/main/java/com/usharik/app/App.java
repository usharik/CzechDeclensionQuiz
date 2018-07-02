package com.usharik.app;

import android.app.Activity;
import android.app.Application;

import android.content.SharedPreferences;
import android.support.v4.app.Fragment;

import com.usharik.database.dao.DatabaseManager;
import com.usharik.app.di.DaggerAppComponent;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.support.HasSupportFragmentInjector;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

import static com.usharik.app.fragment.SettingsFragment.GENDER_FILTER_KEY;
import static com.usharik.app.fragment.SettingsFragment.SHARED_PREFERENCES;
import static com.usharik.app.fragment.SettingsFragment.SWITCH_OFF_ANIMATION;

/**
 * Created by macbook on 08.02.18.
 */

public class App extends Application implements HasActivityInjector, HasSupportFragmentInjector {

    @Inject
    DispatchingAndroidInjector<Activity> dispatchingAndroidActivityInjector;

    @Inject
    DispatchingAndroidInjector<Fragment> dispatchingAndroidFragmentInjector;

    @Inject
    DatabaseManager databaseManager;

    @Inject
    AppState appState;

    @Override
    public void onCreate() {
        super.onCreate();
        DaggerAppComponent.builder().create(this).inject(this);

        databaseManager.getDocumentDb().getCount()
                .flatMapCompletable(cnt -> {
                    if (cnt == 0) {
                        databaseManager.populateFromJsonStream(getAssets().open("data.json"));
                    }
                    return Completable.complete();
                })
                .subscribeOn(Schedulers.io())
                .blockingAwait();

        SharedPreferences prefs = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        appState.setGenderFilterStr(prefs.getString(GENDER_FILTER_KEY, Gender.ALL));
        appState.switchOffAnimation = prefs.getBoolean(SWITCH_OFF_ANIMATION, false);
    }

    @Override
    public DispatchingAndroidInjector<Activity> activityInjector() {
        return dispatchingAndroidActivityInjector;
    }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return dispatchingAndroidFragmentInjector;
    }
}
