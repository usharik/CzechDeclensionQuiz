package com.usharik.app.di;

import android.app.Application;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.usharik.app.App;
import com.usharik.database.DocumentRepository;
import com.usharik.database.TrainingStatsRepository;
import com.usharik.database.dao.DatabaseFactory;
import com.usharik.database.dao.DocumentDatabase;
import com.usharik.app.AppState;
import com.usharik.app.ads.AdManager;
import com.usharik.app.ads.AdSessionState;
import com.usharik.app.ads.AdsPolicy;
import com.usharik.app.ads.InterstitialAdPolicy;
import com.usharik.app.ads.RandomProvider;
import com.usharik.app.ads.ThreadLocalRandomProvider;
import com.usharik.app.notification.NotificationHelper;
import com.usharik.app.service.FirebaseAnalyticsService;
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
    DocumentDatabase provideDocumentDatabase(Application application) {
        return DatabaseFactory.provideDocumentDatabase(application);
    }

    @Provides
    @Singleton
    DocumentRepository provideDatabaseManager(DocumentDatabase db) {
        return new DocumentRepository(db);
    }

    @Provides
    @Singleton
    TrainingStatsRepository provideTrainingStatsRepository(DocumentDatabase db) {
        return new TrainingStatsRepository(db);
    }

    @Provides
    @Singleton
    WordService provideWordService(DocumentRepository documentRepository,
                                   AppState appState,
                                   FirebaseAnalyticsService analyticsService) {
        return new WordService(documentRepository, appState, analyticsService);
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

    @Provides
    @Singleton
    RandomProvider provideRandomProvider() {
        return new ThreadLocalRandomProvider();
    }

    @Provides
    @Singleton
    InterstitialAdPolicy provideInterstitialAdPolicy(AdSessionState sessionState, RandomProvider random) {
        return new InterstitialAdPolicy(sessionState, random);
    }

    @Provides
    @Singleton
    AdsPolicy provideAdsPolicy(InterstitialAdPolicy policy) {
        return policy;
    }

    @Provides
    @Singleton
    FirebaseAnalyticsService provideFirebaseAnalyticsService(FirebaseAnalytics firebaseAnalytics) {
        return new FirebaseAnalyticsService(firebaseAnalytics);
    }

    @Provides
    @Singleton
    AppWorkerFactory provideWorkerFactory(
            TrainingStatsRepository statsRepository,
            NotificationHelper notificationHelper) {
        return new AppWorkerFactory(() -> statsRepository, () -> notificationHelper);
    }
}
