package com.usharik.app.di;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;

import com.usharik.app.notification.DailyReminderWorker;
import com.usharik.app.notification.NotificationHelper;
import com.usharik.database.TrainingStatsRepository;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Custom WorkerFactory to support dependency injection in Workers.
 * Provides instances of Workers with their dependencies injected.
 */
@Singleton
public class AppWorkerFactory extends WorkerFactory {

    private final Provider<TrainingStatsRepository> statsRepositoryProvider;
    private final Provider<NotificationHelper> notificationHelperProvider;

    @Inject
    public AppWorkerFactory(
            Provider<TrainingStatsRepository> statsRepositoryProvider,
            Provider<NotificationHelper> notificationHelperProvider) {
        this.statsRepositoryProvider = statsRepositoryProvider;
        this.notificationHelperProvider = notificationHelperProvider;
    }

    @Override
    public ListenableWorker createWorker(
            @NonNull Context appContext,
            @NonNull String workerClassName,
            @NonNull WorkerParameters workerParameters) {

        if (workerClassName.equals(DailyReminderWorker.class.getName())) {
            return new DailyReminderWorker(
                    appContext,
                    workerParameters,
                    statsRepositoryProvider.get(),
                    notificationHelperProvider.get()
            );
        }

        // Return null to delegate to the default WorkerFactory
        return null;
    }
}
