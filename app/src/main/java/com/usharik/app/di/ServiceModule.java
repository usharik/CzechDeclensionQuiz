package com.usharik.app.di;

import android.app.Application;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.usharik.app.App;
import com.usharik.database.dao.DatabaseManager;
import com.usharik.app.AppState;
import com.usharik.app.service.WordService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by macbook on 09.02.18.
 */

@Module(includes = {AppModule.class})
class ServiceModule {
    @Provides
    @Singleton
    AppState provideAppState() {
        return new AppState();
    }

    @Provides
    @Singleton
    DatabaseManager provideDatabaseManager(Application application) {
        return new DatabaseManager(application);
    }

    @Provides
    @Singleton
    WordService provideWordService(DatabaseManager databaseManager,
                                   AppState appState,
                                   FirebaseAnalytics firebaseAnalytics) {
        return new WordService(databaseManager, appState, firebaseAnalytics);
    }

    @Provides
    @Singleton
    FirebaseAnalytics provideFirebaseAnalytics(App app) {
        return FirebaseAnalytics.getInstance(app);
    }

    @Provides
    @Singleton
    Gson provideGson() {
        return new Gson();
    }
}
