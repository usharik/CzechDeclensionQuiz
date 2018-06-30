package com.usharik.app;

import android.app.Activity;
import android.app.Application;

import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.usharik.database.dao.DatabaseManager;
import com.usharik.database.dao.DocumentDatabase;
import com.usharik.app.di.DaggerAppComponent;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.support.HasSupportFragmentInjector;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

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
