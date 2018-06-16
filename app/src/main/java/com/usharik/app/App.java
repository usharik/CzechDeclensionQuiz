package com.usharik.app;

import android.app.Activity;
import android.app.Application;

import com.example.database.dao.DatabaseManager;
import com.example.database.dao.DocumentDatabase;
import com.usharik.app.di.DaggerAppComponent;

import javax.inject.Inject;

import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by macbook on 08.02.18.
 */

public class App extends Application implements HasActivityInjector {

    @Inject
    DispatchingAndroidInjector<Activity> dispatchingAndroidInjector;

    @Inject
    DatabaseManager databaseManager;

    @Override
    public void onCreate() {
        super.onCreate();
        DaggerAppComponent.builder().create(this).inject(this);

        databaseManager.getDocumentDb().getCount()
                .flatMapCompletable(cnt -> {
                    if (cnt == 0) {
                        databaseManager.restore(getAssets().open(DocumentDatabase.DB_NAME));
                    }
                    return Completable.complete();
                })
                .subscribeOn(Schedulers.io())
                .blockingAwait();
    }

    @Override
    public DispatchingAndroidInjector<Activity> activityInjector() {
        return dispatchingAndroidInjector;
    }
}
