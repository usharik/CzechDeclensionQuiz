package com.usharik.app.fragment;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.databinding.Bindable;

import com.usharik.app.AppState;
import com.usharik.app.BR;
import com.usharik.app.CzechCase;
import com.usharik.app.framework.ViewModelObservable;
import com.usharik.app.service.WordService;
import com.usharik.database.WordInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SingleCaseQuizViewModel extends ViewModelObservable {

    public static final int SINGULAR = 0;
    public static final int PLURAL = 1;

    private static final String TAG = "SingleCaseQuizVM";

    private final AppState appState;
    private final WordService wordService;

    private int currentStepIndex = 0;

    @Inject
    public SingleCaseQuizViewModel(final AppState appState, final WordService wordService) {
        this.appState = appState;
        this.wordService = wordService;
    }

    public int getCurrentCaseIndex() {
        return currentStepIndex < 7 ? currentStepIndex : currentStepIndex - 7;
    }

    public int getCurrentNumber() {
        return currentStepIndex < 7 ? SINGULAR : PLURAL;
    }

    @Bindable
    public String getWord() {
        WordInfo wi = appState.getWordInfo();
        return wi != null ? wi.word() : "";
    }

    @Bindable
    public String getGender() {
        WordInfo wi = appState.getWordInfo();
        return wi != null ? wi.gender() : "";
    }

    @Bindable
    public String getDeclensionType() {
        WordInfo wi = appState.getWordInfo();
        return wi != null ? wi.declensionType() : "";
    }

    @Bindable
    public String getCaseName() {
        return CzechCase.fromIndex(getCurrentCaseIndex()).name;
    }

    @Bindable
    public String getNumberLabel() {
        return getCurrentNumber() == SINGULAR ? "Singular" : "Plural";
    }

    @Bindable
    public String getCaseQuestion() {
        return CzechCase.fromIndex(getCurrentCaseIndex()).question;
    }

    public String getCorrectAnswer() {
        WordInfo wi = appState.getWordInfo();
        if (wi == null) return "";
        return wi.cases(getCurrentNumber(), getCurrentCaseIndex());
    }

    public List<String> buildAnswers() {
        WordInfo wi = appState.getWordInfo();
        if (wi == null) return Collections.emptyList();

        String correct = getCorrectAnswer();

        List<String> distractors = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            String sg = wi.cases(SINGULAR, i);
            if (!sg.isEmpty() && !sg.equals(correct)) distractors.add(sg);
            String pl = wi.cases(PLURAL, i);
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
        WordInfo current = appState.getWordInfo();
        if (tryAgain && current != null) {
            currentStepIndex = 0;
            update();
            return;
        }
        wordService.getNextWord()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(wordInfo -> {
                    appState.setWordInfo(wordInfo);
                    currentStepIndex = 0;
                    update();
                }, thr -> Log.e(TAG, "Error loading word", thr));
    }

    public void nextStep() {
        if (currentStepIndex < 13) {
            currentStepIndex++;
            update();
        } else {
            nextWord(false);
        }
    }

    private void update() {
        notifyChange();
    }
}
