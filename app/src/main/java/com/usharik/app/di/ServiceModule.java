package com.usharik.app.di;

import android.app.Application;

import com.example.database.dao.DatabaseManager;
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
    WordService provideWordService(DatabaseManager databaseManager) {
        return new WordService(databaseManager);
    }
}
