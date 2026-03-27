package com.usharik.app.fragment;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.databinding.Bindable;

import com.usharik.app.CzechCase;
import com.usharik.app.SingleCaseQuizState;
import com.usharik.app.framework.ViewModelObservable;
import com.usharik.app.service.WordService;
import com.usharik.database.TrainingStatsRepository;
import com.usharik.database.WordInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;

import com.usharik.database.dao.DailyTrainingStatsEntity;

public class SingleCaseQuizViewModel extends ViewModelObservable {

    public static final int SINGULAR = 0;
    public static final int PLURAL = 1;

    private static final String TAG = "SingleCaseQuizVM";

    private static final int MAX_RECENT_WORDS = 3;

    private final WordService wordService;
    private final SingleCaseQuizState state;
    private final TrainingStatsRepository statsRepository;
    private final Locale locale;
    private final LinkedHashSet<String> recentWords = new LinkedHashSet<>();

    @Inject
    public SingleCaseQuizViewModel(final WordService wordService,
                                   final TrainingStatsRepository statsRepository,
                                   final Locale locale) {
        this(wordService, statsRepository, locale, new SingleCaseQuizState());
    }

    SingleCaseQuizViewModel(final WordService wordService,
                            final TrainingStatsRepository statsRepository,
                            final Locale locale,
                            final SingleCaseQuizState state) {
        this.wordService = wordService;
        this.statsRepository = statsRepository;
        this.locale = locale;
        this.state = state;
        loadRecentWordsFromDb();
    }

    @SuppressLint("CheckResult")
    private void loadRecentWordsFromDb() {
        if (statsRepository == null) return;
        statsRepository.getRecentWords()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(words -> {
                    recentWords.clear();
                    recentWords.addAll(words); // oldest→newest, preserving insertion order
                }, thr -> Log.w(TAG, "Failed to load recent words", thr));
    }

    public boolean hasCurrentWord() {
        return state.getWordInfo() != null;
    }

    public int getCurrentCaseIndex() {
        return state.getCurrentCase();
    }

    public int getCurrentNumber() {
        return state.isPlural() ? PLURAL : SINGULAR;
    }

    @Bindable
    public String getWord() {
        return state.getWordInfo() != null ? state.getWordInfo().word() : "";
    }

    @Bindable
    public String getGender() {
        return state.getWordInfo() != null ? state.getWordInfo().gender() : "";
    }

    @Bindable
    public String getDeclensionType() {
        return state.getWordInfo() != null ? state.getWordInfo().declensionType() : "";
    }

    @Bindable
    public String getTranslation() {
        WordInfo wordInfo = state.getWordInfo();
        if (wordInfo == null) {
            return "";
        }

        String language = locale.getISO3Language();
        if (DeclensionQuizViewModel.RUS.equals(language)
                || DeclensionQuizViewModel.BEL.equals(language)
                || DeclensionQuizViewModel.UKR.equals(language)) {
            return wordInfo.translation_ru();
        }

        return wordInfo.translation_en();
    }

    @Bindable
    public String getCaseName() {
        return (getCurrentCaseIndex() + 1) + ". " + CzechCase.fromIndex(getCurrentCaseIndex()).name;
    }

    public String getCurrentCaseName() {
        return CzechCase.fromIndex(getCurrentCaseIndex()).name;
    }

    @Bindable
    public String getNumberLabel() {
        return getCurrentNumber() == SINGULAR ? "Singular" : "Plural";
    }

    @Bindable
    public String getCaseQuestion() {
        CzechCase czechCase = CzechCase.fromIndex(getCurrentCaseIndex());
        return czechCase.helperWord.isBlank()
                ? czechCase.question
                : czechCase.helperWord + " - " + czechCase.question;
    }

    public String getCorrectAnswer() {
        return state.getCorrectAnswer();
    }

    public List<String> getAnswers() {
        return state.getAnswers();
    }

    public boolean isAnswered() {
        return state.isAnswered();
    }

    public void markAnswered() {
        state.setAnswered(true);
    }

    private List<String> buildAnswers(WordInfo wordInfo, String correct) {
        if (wordInfo == null) return Collections.emptyList();

        List<String> distractors = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            String sg = wordInfo.cases(SINGULAR, i);
            if (!sg.isEmpty() && !sg.equals(correct)) distractors.add(sg);
            String pl = wordInfo.cases(PLURAL, i);
            if (!pl.isEmpty() && !pl.equals(correct)) distractors.add(pl);
        }

        List<String> unique = new ArrayList<>(new LinkedHashSet<>(distractors));
        Collections.shuffle(unique);

        List<String> answers = new ArrayList<>();
        answers.add(correct);
        for (String s : unique) {
            if (answers.size() >= 4) break;
            answers.add(s);
        }
        Collections.shuffle(answers);
        return answers;
    }

    @SuppressLint("CheckResult")
    public void nextWord(boolean tryAgain) {
        if (tryAgain && state.getWordInfo() != null) {
            resetRound();
            update();
            return;
        }
        wordService.getNextWord(state.getWordInfo())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(wordInfo -> {
                    state.setWordInfo(wordInfo);
                    addRecentWord(wordInfo.word());
                    resetRound();
                    if (statsRepository != null) {
                        statsRepository.incrementWordsCompleted()
                                .subscribe(() -> {}, thr2 -> Log.w(TAG, "Stats error", thr2));
                    }
                    update();
                }, thr -> Log.e(TAG, "Error loading word", thr));
    }

    public void nextStep() {
        // Each answered case = one exercise (visible increment in the quit dialog)
        if (statsRepository != null) {
            statsRepository.incrementExercisesCompleted()
                    .subscribe(() -> {}, thr -> Log.w(TAG, "Stats error", thr));
        }
        if (state.getCurrentCase() < 6) {
            state.setCurrentCase(state.getCurrentCase() + 1);
        } else if (!state.isPlural()) {
            state.setPlural(true);
            state.setCurrentCase(0);
        } else {
            nextWord(false);
            return;
        }
        prepareCurrentQuestion();
        update();
    }

    private void resetRound() {
        state.setCurrentCase(0);
        state.setPlural(false);
        prepareCurrentQuestion();
    }

    private void prepareCurrentQuestion() {
        WordInfo wordInfo = state.getWordInfo();
        if (wordInfo == null) {
            state.setCorrectAnswer("");
            state.setAnswers(Collections.emptyList());
            state.setAnswered(false);
            return;
        }

        String correct = wordInfo.cases(getCurrentNumber(), getCurrentCaseIndex());
        state.setCorrectAnswer(correct);
        state.setAnswers(buildAnswers(wordInfo, state.getCorrectAnswer()));
        state.setAnswered(false);
    }

    public Maybe<DailyTrainingStatsEntity> getTodayStats() {
        return statsRepository != null ? statsRepository.getTodayStats() : Maybe.empty();
    }

    public List<String> getRecentWords() {
        List<String> result = new ArrayList<>(recentWords);
        Collections.reverse(result);
        return result;
    }

    @SuppressLint("CheckResult")
    private void addRecentWord(String word) {
        recentWords.remove(word); // move to end if already present
        recentWords.add(word);
        if (recentWords.size() > MAX_RECENT_WORDS) {
            recentWords.remove(recentWords.iterator().next()); // remove oldest (first)
        }
        if (statsRepository != null) {
            statsRepository.saveRecentWords(new ArrayList<>(recentWords))
                    .subscribe(() -> {}, thr -> Log.w(TAG, "Failed to save recent words", thr));
        }
    }

    private void update() {
        notifyChange();
    }
}
