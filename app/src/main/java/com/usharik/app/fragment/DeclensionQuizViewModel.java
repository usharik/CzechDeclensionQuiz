package com.usharik.app.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.databinding.Bindable;
import androidx.databinding.BindingAdapter;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.usharik.app.AppState;
import com.usharik.app.BR;
import com.usharik.app.DeclensionQuizState;
import com.usharik.app.DeclensionQuizState.WordTextModel;
import com.usharik.app.framework.ViewModelObservable;

import java.util.*;

import javax.inject.Inject;

import com.usharik.app.service.WordService;
import com.usharik.app.service.FirebaseAnalyticsService;
import com.usharik.database.WordInfo;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Created by macbook on 07/03/2018.
 */

public class DeclensionQuizViewModel extends ViewModelObservable {
    public static final int SINGULAR = 0;
    public static final int PLURAL = 1;

    public static final String RUS = "rus";
    public static final String BEL = "bel";
    public static final String UKR = "ukr";

    private static final Map<TextView, String> textView2value = new WeakHashMap<>();

    private final AppState appState;
    private final WordService wordService;
    private final FirebaseAnalyticsService analyticsService;
    private final Locale locale;

    private final DeclensionQuizState quizState = new DeclensionQuizState();
    private int errorCount;

    @Inject
    public DeclensionQuizViewModel(final AppState appState,
                                   final WordService wordService,
                                   final FirebaseAnalyticsService analyticsService,
                                   final Locale locale) {
        this.appState = appState;
        this.wordService = wordService;
        this.analyticsService = analyticsService;
        this.locale = locale;
    }

    @Bindable
    public String getWord() {
        WordInfo wordInfo = quizState.getWordInfo();
        return wordInfo != null ? wordInfo.word() : "";
    }

    @Bindable
    public String getGender() {
        WordInfo wordInfo = quizState.getWordInfo();
        return wordInfo != null ? wordInfo.gender() : "";
    }

    @Bindable
    public String getDeclensionType() {
        WordInfo wordInfo = quizState.getWordInfo();
        return wordInfo != null ? wordInfo.declensionType() : "";
    }

    @Bindable
    public String getTranslation() {
        WordInfo wordInfo = quizState.getWordInfo();
        if (wordInfo == null) return "";

        String language = locale.getISO3Language();
        return switch (language) {
            case RUS, BEL, UKR -> wordInfo.translation_ru();
            default -> wordInfo.translation_en();
        };
    }

    @SuppressLint("CheckResult")
    public void nextWord(boolean tryAgain) {
        Single<WordInfo> wordInfoSingle;
        WordInfo currentWordInfo = quizState.getWordInfo();
        if (currentWordInfo == null || !tryAgain) {
            wordInfoSingle = wordService.getNextWord(currentWordInfo);
        } else {
            wordInfoSingle = Single.just(currentWordInfo);
        }
        wordInfoSingle
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(wordInfo -> {
                    quizState.setWordInfo(wordInfo);
                    List<WordTextModel> words = new ArrayList<>();
                    for (int i = 0; i < 7; i++) {
                        String singular = wordInfo.cases(SINGULAR, i);
                        String plural = wordInfo.cases(PLURAL, i);
                        words.add(new WordTextModel(singular, singular.isEmpty() ? View.GONE : View.VISIBLE));
                        words.add(new WordTextModel(plural, plural.isEmpty() ? View.GONE : View.VISIBLE));
                        quizState.getCorrectAnswers()[SINGULAR][i] = singular;
                        quizState.getCorrectAnswers()[PLURAL][i] = plural;
                        quizState.getActualAnswers()[SINGULAR][i] = -1;
                        quizState.getActualAnswers()[PLURAL][i] = -1;
                    }
                    Collections.shuffle(words);
                    quizState.setWordTextModels(words.toArray(new WordTextModel[0]));
                    errorCount = 0;
                    quizState.resetWrongAttempts();
                    update();
                }, thr -> {
                    Log.e("Error", "Error loading next word", thr);
                    FirebaseCrashlytics.getInstance().recordException(thr);
                });
    }

    public void update() {
        notifyPropertyChanged(BR.word);
        notifyPropertyChanged(BR.gender);
        notifyPropertyChanged(BR.declensionType);
        notifyPropertyChanged(BR.translation);
        notifyPropertyChanged(BR.wordTextModels);
        notifyPropertyChanged(BR.caseModels);
    }

    public boolean checkAnswers() {
        Bundle bundle = new Bundle();
        boolean res = true;
        int[][] actualAnswers = quizState.getActualAnswers();
        String[][] correctAnswers = quizState.getCorrectAnswers();
        WordTextModel[] wordTextModels = quizState.getWordTextModels();

        for (int i = 0; i < 7; i++) {
            int actualAnswerSingularIx = actualAnswers[SINGULAR][i];
            if (actualAnswerSingularIx == -1) {
                res &= correctAnswers[SINGULAR][i].isEmpty();
            } else if (!correctAnswers[SINGULAR][i].equals(getWordByIndex(actualAnswerSingularIx))) {
                bundle.putStringArray("SINGULAR_" + i, new String[]{correctAnswers[SINGULAR][i], getWordByIndex(actualAnswerSingularIx)});
                res = false;
                errorCount++;
                wordTextModels[actualAnswerSingularIx].setVisible(View.VISIBLE);
                actualAnswers[SINGULAR][i] = -1;
            }

            int actualAnswerPluralIx = actualAnswers[PLURAL][i];
            if (actualAnswerPluralIx == -1) {
                res &= correctAnswers[PLURAL][i].isEmpty();
            } else if (!correctAnswers[PLURAL][i].equals(getWordByIndex(actualAnswerPluralIx))) {
                bundle.putStringArray("PLURAL_" + i, new String[]{correctAnswers[PLURAL][i], getWordByIndex(actualAnswerPluralIx)});
                res = false;
                errorCount++;
                wordTextModels[actualAnswerPluralIx].setVisible(View.VISIBLE);
                actualAnswers[PLURAL][i] = -1;
            }
        }
        notifyPropertyChanged(BR.wordTextModels);
        notifyPropertyChanged(BR.caseModels);
        if (!bundle.isEmpty()) {
            bundle.putString("WORD", getWord());
            analyticsService.logMistake(bundle);
        }
        return res;
    }

    @Bindable
    public WordTextModel[] getWordTextModels() {
        return quizState.getWordTextModels();
    }

    @Bindable
    public int[][] getCaseModels() {
        return quizState.getActualAnswers();
    }

    public String getWordByIndex(int ix) {
        WordTextModel[] wordTextModels = quizState.getWordTextModels();
        return (ix == -1 || wordTextModels[ix] == null) ? "" : wordTextModels[ix].getWord();
    }

    public void updateWordTextModel(int num, int visible) {
        quizState.getWordTextModels()[num].setVisible(visible);
        notifyPropertyChanged(BR.wordTextModels);
    }

    public void updateCaseModel(int caseNum, int number, int wordIx) {
        int[][] actualAnswers = quizState.getActualAnswers();
        if (actualAnswers[number][caseNum] != -1) {
            quizState.getWordTextModels()[actualAnswers[number][caseNum]].setVisible(View.VISIBLE);
            notifyPropertyChanged(BR.wordTextModels);
        }
        actualAnswers[number][caseNum] = wordIx;
        notifyPropertyChanged(BR.caseModels);
    }

    public void swapCaseModels(int caseNum1, int number1, int caseNum2, int number2) {
        int[][] actualAnswers = quizState.getActualAnswers();
        int tmp = actualAnswers[number1][caseNum1];
        actualAnswers[number1][caseNum1] = actualAnswers[number2][caseNum2];
        actualAnswers[number2][caseNum2] = tmp;
        notifyPropertyChanged(BR.caseModels);
    }

    public boolean getSwitchOffAnimation() {
        return appState.getSwitchOffAnimation();
    }

    @BindingAdapter("animateView")
    public static void setAnimateView(TextView textView, boolean animateView) {
        String prevText = textView2value.get(textView);
        String text = textView.getText().toString();
        if (animateView && (prevText != null && !prevText.isEmpty()) && text.isEmpty()) {
            textView.setRotationX(0);
            // textView.setCameraDistance(10); // probable reason of disappearing on some phones
            textView.animate()
                    .rotationX(360)
                    .setDuration(500)
                    .start();
        }
        if (!textView.getText().toString().equals(prevText)) {
            textView2value.put(textView, text);
        }
    }

    public int getErrorCount() {
        return errorCount;
    }

    /**
     * Check if a single answer is correct when dropped
     * @param caseNum case number (0-6)
     * @param numberCode 0 for singular, 1 for plural
     * @param wordIx index of the word in wordTextModels
     * @return true if correct, false if incorrect
     */
    public boolean checkSingleAnswer(int caseNum, int numberCode, int wordIx) {
        String[][] correctAnswers = quizState.getCorrectAnswers();
        String correctAnswer = correctAnswers[numberCode][caseNum];
        String actualAnswer = getWordByIndex(wordIx);
        return correctAnswer.equals(actualAnswer);
    }

    /**
     * Check if the quiz is complete (all non-empty cells are filled correctly)
     * @return true if all answers are correct and placed
     */
    public boolean isQuizComplete() {
        int[][] actualAnswers = quizState.getActualAnswers();
        String[][] correctAnswers = quizState.getCorrectAnswers();

        for (int i = 0; i < 7; i++) {
            // Check singular
            if (!correctAnswers[SINGULAR][i].isEmpty()) {
                int actualAnswerIx = actualAnswers[SINGULAR][i];
                if (actualAnswerIx == -1 || !correctAnswers[SINGULAR][i].equals(getWordByIndex(actualAnswerIx))) {
                    return false;
                }
            }

            // Check plural
            if (!correctAnswers[PLURAL][i].isEmpty()) {
                int actualAnswerIx = actualAnswers[PLURAL][i];
                if (actualAnswerIx == -1 || !correctAnswers[PLURAL][i].equals(getWordByIndex(actualAnswerIx))) {
                    return false;
                }
            }
        }

        return true;
    }

    public int getWrongAttempts() {
        return quizState.getWrongAttempts();
    }

    public void incrementWrongAttempts() {
        quizState.incrementWrongAttempts();
        notifyPropertyChanged(BR.wrongAttemptsCounter);
    }

    public void resetWrongAttempts() {
        quizState.resetWrongAttempts();
        notifyPropertyChanged(BR.wrongAttemptsCounter);
    }

    @Bindable
    public String getWrongAttemptsCounter() {
        return quizState.getWrongAttempts() + "/5";
    }
}
