package com.usharik.database;

import com.usharik.database.dao.DailyTrainingStatsEntity;
import com.usharik.database.dao.DocumentDatabase;
import com.usharik.database.dao.RecentWordsEntity;
import com.usharik.database.dao.ReminderStateEntity;
import com.usharik.database.dao.TrainingStatsDao;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Provides access to daily training statistics and reminder state.
 * Increment methods are reactive (Completable). Blocking reads are provided
 * for use inside WorkManager Workers which run on a background thread already.
 */
public class TrainingStatsRepository {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final TrainingStatsDao dao;

    public TrainingStatsRepository(DocumentDatabase db) {
        this.dao = db.trainingStatsDao();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String today() {
        return LocalDate.now().format(DATE_FMT);
    }

    /** Returns today's stats, creating a blank row if none exists yet. */
    private DailyTrainingStatsEntity getOrCreateToday(String date) {
        DailyTrainingStatsEntity entity = dao.getStatsByDate(date).blockingGet();
        if (entity == null) {
            entity = new DailyTrainingStatsEntity();
            entity.date = date;
        }
        return entity;
    }

    // ── reactive increment API (call from ViewModel / IO thread) ─────────────

    public Completable incrementWordsCompleted() {
        return Completable.fromAction(() -> {
            String date = today();
            DailyTrainingStatsEntity e = getOrCreateToday(date);
            e.wordsCompleted++;
            e.updatedAt = System.currentTimeMillis();
            dao.insertOrReplaceStats(e).blockingAwait();
        }).subscribeOn(Schedulers.io());
    }

    public Completable incrementExercisesCompleted() {
        return Completable.fromAction(() -> {
            String date = today();
            DailyTrainingStatsEntity e = getOrCreateToday(date);
            e.exercisesCompleted++;
            e.updatedAt = System.currentTimeMillis();
            dao.insertOrReplaceStats(e).blockingAwait();
        }).subscribeOn(Schedulers.io());
    }

    public Completable incrementErrorsCount() {
        return Completable.fromAction(() -> {
            String date = today();
            DailyTrainingStatsEntity e = getOrCreateToday(date);
            e.errorsCount++;
            e.updatedAt = System.currentTimeMillis();
            dao.insertOrReplaceStats(e).blockingAwait();
        }).subscribeOn(Schedulers.io());
    }

    // ── reactive reads (call from main/IO thread) ─────────────────────────────

    /** Returns today's stats, or completes empty if the user hasn't trained today. */
    public Maybe<DailyTrainingStatsEntity> getTodayStats() {
        return dao.getStatsByDate(today()).subscribeOn(Schedulers.io());
    }

    // ── blocking reads for Worker (already on background thread) ─────────────

    /** Returns today's stats or null if the user hasn't trained today. */
    public DailyTrainingStatsEntity getStatsForDateBlocking(String date) {
        return dao.getStatsByDate(date).blockingGet();
    }

    public Maybe<ReminderStateEntity> getReminderState() {
        return dao.getReminderState().subscribeOn(Schedulers.io());
    }

    public void saveReminderStateBlocking(ReminderStateEntity state) {
        dao.insertOrReplaceReminderState(state).blockingAwait();
    }

    // ── recent words ──────────────────────────────────────────────────────────

    private static final String WORD_SEPARATOR = "|||";

    /**
     * Returns the persisted list of recent words (oldest→newest order),
     * or completes empty if none saved yet.
     */
    public Maybe<List<String>> getRecentWords() {
        return dao.getRecentWords()
                .<List<String>>map(entity -> {
                    if (entity.words == null || entity.words.isEmpty()) {
                        return Collections.emptyList();
                    }
                    return new ArrayList<>(Arrays.asList(entity.words.split(WORD_SEPARATOR, -1)));
                })
                .subscribeOn(Schedulers.io());
    }

    /**
     * Persists the recent words list. Words should be in oldest→newest order.
     */
    public Completable saveRecentWords(List<String> words) {
        return Completable.fromAction(() -> {
            RecentWordsEntity entity = new RecentWordsEntity();
            entity.words = String.join(WORD_SEPARATOR, words);
            dao.insertOrReplaceRecentWords(entity).blockingAwait();
        }).subscribeOn(Schedulers.io());
    }
}

