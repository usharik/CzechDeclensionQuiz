package com.usharik.app.di;

import android.app.Application;

import com.usharik.app.AppState;
import com.usharik.app.service.WordService;
import com.usharik.app.dao.DatabaseManager;

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
    WordService provideWordService(DatabaseManager databaseManager) {
        return new WordService(databaseManager);
    }
}
