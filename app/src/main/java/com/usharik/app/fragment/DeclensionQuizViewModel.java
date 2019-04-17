package com.usharik.app.fragment;

import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.usharik.app.AppState;
import com.usharik.app.BR;
import com.usharik.app.framework.ViewModelObservable;

import java.util.*;

import javax.inject.Inject;

import com.usharik.app.service.WordService;
import com.usharik.database.WordInfo;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by macbook on 07/03/2018.
 */

public class DeclensionQuizViewModel extends ViewModelObservable {
    public static final int SINGULAR = 0;
    public static final int PLURAL = 1;

    public static final String RUS = "rus";
    public static final String BEL = "bel";
    public static final String UKR = "ukr";
    public static final String ENG = "eng";

    private static final Map<TextView, String> textView2value = new HashMap<>();

    private final AppState appState;
    private final WordService wordService;
    private final FirebaseAnalytics firebaseAnalytics;
    private final Locale locale;

    private int errorCount;

    @Inject
    public DeclensionQuizViewModel(final AppState appState,
                                   final WordService wordService,
                                   final FirebaseAnalytics firebaseAnalytics,
                                   final Locale locale) {
        this.appState = appState;
        this.wordService = wordService;
        this.firebaseAnalytics = firebaseAnalytics;
        this.locale = locale;
    }

    @Bindable
    public String getWord() {
        return appState.wordInfo.word;
    }

    @Bindable
    public String getGender() {
        return appState.wordInfo.gender;
    }

    @Bindable
    public String getTranslation() {
        String language = locale.getISO3Language();

        switch (language) {
            case RUS:
            case BEL:
            case UKR:
                return appState.wordInfo.translation_ru;
            case ENG:
                return appState.wordInfo.translation_en;
            default:
                return appState.wordInfo.translation_en;
        }
    }

    public void nextWord(boolean tryAgain) {
        Single<WordInfo> wordInfoSingle;
        if (appState.wordInfo == null || !tryAgain) {
            wordInfoSingle = wordService.getNextWord();
        } else {
            wordInfoSingle = Single.just(appState.wordInfo);
        }
        wordInfoSingle
                .subscribeOn(Schedulers.io())
                .subscribe(wordInfo -> {
                    appState.wordInfo = wordInfo;
                    List<WordTextModel> words = new ArrayList<>();
                    for (int i = 0; i < 7; i++) {
                        String singular = appState.wordInfo.cases[SINGULAR][i];
                        String plural = appState.wordInfo.cases[PLURAL][i];
                        words.add(new WordTextModel(singular, singular.isEmpty() ? View.GONE : View.VISIBLE));
                        words.add(new WordTextModel(plural, plural.isEmpty() ? View.GONE : View.VISIBLE));
                        appState.correctAnswers[SINGULAR][i] = singular;
                        appState.correctAnswers[PLURAL][i] = plural;
                        appState.actualAnswers[SINGULAR][i] = -1;
                        appState.actualAnswers[PLURAL][i] = -1;
                    }
                    Collections.shuffle(words);
                    appState.wordTextModels = words.toArray(new WordTextModel[words.size()]);
                    errorCount = 0;
                    update();
                });
    }

    public void update() {
        notifyPropertyChanged(BR.word);
        notifyPropertyChanged(BR.gender);
        notifyPropertyChanged(BR.translation);
        notifyPropertyChanged(BR.wordTextModels);
        notifyPropertyChanged(BR.caseModels);
    }

    public boolean checkAnswers() {
        Bundle bundle = new Bundle();
        boolean res = true;
        for (int i = 0; i < 7; i++) {
            int actualAnswerSingularIx = appState.actualAnswers[SINGULAR][i];
            if (actualAnswerSingularIx == -1) {
                res &= appState.correctAnswers[SINGULAR][i].isEmpty();
            } else if (!appState.correctAnswers[SINGULAR][i].equals(getWordByIndex(actualAnswerSingularIx))) {
                bundle.putStringArray("SINGULAR_" + i, new String[]{appState.correctAnswers[SINGULAR][i], getWordByIndex(actualAnswerSingularIx)});
                res = false;
                errorCount++;
                appState.wordTextModels[actualAnswerSingularIx].visible = View.VISIBLE;
                appState.actualAnswers[SINGULAR][i] = -1;
            }

            int actualAnswerPluralIx = appState.actualAnswers[PLURAL][i];
            if (actualAnswerPluralIx == -1) {
                res &= appState.correctAnswers[PLURAL][i].isEmpty();
            } else if (!appState.correctAnswers[PLURAL][i].equals(getWordByIndex(actualAnswerPluralIx))) {
                bundle.putStringArray("PLURAL_" + i, new String[]{appState.correctAnswers[PLURAL][i], getWordByIndex(actualAnswerPluralIx)});
                res = false;
                errorCount++;
                appState.wordTextModels[actualAnswerPluralIx].visible = View.VISIBLE;
                appState.actualAnswers[PLURAL][i] = -1;
            }
        }
        notifyPropertyChanged(BR.wordTextModels);
        notifyPropertyChanged(BR.caseModels);
        if (!bundle.isEmpty()) {
            bundle.putString("WORD", getWord());
            firebaseAnalytics.logEvent("MISTAKE", bundle);
        }
        return res;
    }

    @Bindable
    public WordTextModel[] getWordTextModels() {
        return appState.wordTextModels;
    }

    @Bindable
    public int[][] getCaseModels() {
        return appState.actualAnswers;
    }

    public String getWordByIndex(int ix) {
        if (ix == -1) {
            return "";
        }
        return appState.wordTextModels[ix].getWord();
    }

    public void updateWordTextModel(int num, int visible) {
        appState.wordTextModels[num].visible = visible;
        notifyPropertyChanged(BR.wordTextModels);
    }

    public void updateCaseModel(int caseNum, int number, int wordIx) {
        if (appState.actualAnswers[number][caseNum] != -1) {
            appState.wordTextModels[appState.actualAnswers[number][caseNum]].visible = View.VISIBLE;
            notifyPropertyChanged(BR.wordTextModels);
        }
        appState.actualAnswers[number][caseNum] = wordIx;
        notifyPropertyChanged(BR.caseModels);
    }

    public void swapCaseModels(int caseNum1, int number1, int caseNum2, int number2) {
        int tmp = appState.actualAnswers[number1][caseNum1];
        appState.actualAnswers[number1][caseNum1] = appState.actualAnswers[number2][caseNum2];
        appState.actualAnswers[number2][caseNum2] = tmp;
        notifyPropertyChanged(BR.caseModels);
    }

    public boolean getSwitchOffAnimation() {
        return appState.switchOffAnimation;
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

    public class WordTextModel {
        String word;
        int visible;

        public WordTextModel(String word, int visible) {
            this.word = word;
            this.visible = visible;
        }

        public String getWord() {
            return word;
        }

        public int getVisible() {
            return visible;
        }
    }
}
