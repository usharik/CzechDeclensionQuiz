package com.usharik.app.di;

import android.app.Application;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.usharik.app.App;
import com.usharik.database.DocumentRepository;
import com.usharik.database.dao.DatabaseFactory;
import com.usharik.app.AppState;
import com.usharik.app.ads.AdManager;
import com.usharik.app.service.WordService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import java.util.Locale;

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
    DocumentRepository provideDatabaseManager(Application application) {
        return DatabaseFactory.provideDocumentDb(application);
    }

    @Provides
    @Singleton
    WordService provideWordService(DocumentRepository documentRepository,
                                   AppState appState,
                                   FirebaseAnalytics firebaseAnalytics) {
        return new WordService(documentRepository, appState, firebaseAnalytics);
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

    @Provides
    @Singleton
    Locale provideLocale(Application application) {
        return application.getResources().getConfiguration().getLocales().get(0);
    }

    @Provides
    @Singleton
    AdManager provideAdManager() {
        return new AdManager();
    }
}
