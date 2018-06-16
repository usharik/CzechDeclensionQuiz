package com.usharik.app;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.MutableDocument;
import com.usharik.app.dao.DatabaseManager;
import com.usharik.app.di.DaggerAppComponent;

import javax.inject.Inject;

import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;

/**
 * Created by macbook on 08.02.18.
 */

public class App extends Application implements HasActivityInjector {

    @Inject
    DispatchingAndroidInjector<Activity> dispatchingAndroidInjector;

//    @Inject
//    DatabaseManager databaseManager;

    @Override
    public void onCreate() {
        super.onCreate();
        DaggerAppComponent.builder().create(this).inject(this);

//        try {
//            DatabaseConfiguration config = new DatabaseConfiguration(getApplicationContext());
//            Database database = new Database("mydb", config);
//
//            MutableDocument mutableDoc = new MutableDocument()
//                    .setFloat("version", 2.0F)
//                    .setString("type", "SDK");
//
//            database.save(mutableDoc);
//        } catch (Exception ex) {
//            Log.e(getClass().getName(), "Exception", ex);
//        }
    }

    @Override
    public DispatchingAndroidInjector<Activity> activityInjector() {
        return dispatchingAndroidInjector;
    }
}
