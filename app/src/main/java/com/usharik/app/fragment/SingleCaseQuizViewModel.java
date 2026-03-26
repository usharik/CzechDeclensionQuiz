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

    private final WordService wordService;
    private final SingleCaseQuizState state;
    private final TrainingStatsRepository statsRepository;

    @Inject
    public SingleCaseQuizViewModel(final WordService wordService,
                                   final TrainingStatsRepository statsRepository) {
        this(wordService, statsRepository, new SingleCaseQuizState());
    }

    SingleCaseQuizViewModel(final WordService wordService,
                            final TrainingStatsRepository statsRepository,
                            final SingleCaseQuizState state) {
        this.wordService = wordService;
        this.statsRepository = statsRepository;
        this.state = state;
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
        
        // Check if system language is Russian
        String systemLanguage = Locale.getDefault().getLanguage();
        if ("ru".equals(systemLanguage)) {
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
                    resetRound();
                    if (statsRepository != null) {
                        statsRepository.incrementWordsCompleted()
                                .subscribe(() -> {}, thr2 -> Log.w(TAG, "Stats error", thr2));
                    }
                    update();
                }, thr -> Log.e(TAG, "Error loading word", thr));
    }

    public void nextStep() {
        if (state.getCurrentCase() < 6) {
            state.setCurrentCase(state.getCurrentCase() + 1);
        } else if (!state.isPlural()) {
            state.setPlural(true);
            state.setCurrentCase(0);
        } else {
            // Completed all cases (singular + plural) for this word → count as one exercise
            if (statsRepository != null) {
                statsRepository.incrementExercisesCompleted()
                        .subscribe(() -> {}, thr -> Log.w(TAG, "Stats error", thr));
            }
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

    private void update() {
        notifyChange();
    }
}
