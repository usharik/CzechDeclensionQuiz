package com.usharik.app.fragment;

import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.view.View;
import android.widget.TextView;

import com.usharik.app.AppState;
import com.usharik.app.BR;
import com.usharik.app.framework.ViewModelObservable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.usharik.app.service.WordService;

/**
 * Created by macbook on 07/03/2018.
 */

public class DeclensionQuizViewModel extends ViewModelObservable {
    public static final int SINGULAR = 0;
    public static final int PLURAL = 1;

    private final AppState appState;
    private WordService wordService;
    private static Map<TextView, String> textView2value = new HashMap<>();

    @Inject
    public DeclensionQuizViewModel(final AppState appState,
                                   final WordService wordService) {
        this.appState = appState;
        this.wordService = wordService;
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
        return appState.wordInfo.translation_ru;
    }

    public void nextWord() {
        appState.wordInfo = wordService.getNextWord();
        List<WordTextModel> words = new ArrayList<>();
        for (int i=0; i<7; i++) {
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
        update();
    }

    public void update() {
        notifyPropertyChanged(BR.word);
        notifyPropertyChanged(BR.gender);
        notifyPropertyChanged(BR.translation);
        notifyPropertyChanged(BR.wordTextModels);
        notifyPropertyChanged(BR.caseModels);
    }

    public boolean checkAnswers() {
        boolean res=true;
        for (int i=0; i<7; i++) {
            int actualAnswerSingularIx = appState.actualAnswers[SINGULAR][i];
            if (actualAnswerSingularIx == -1) {
                res &= appState.correctAnswers[SINGULAR][i].isEmpty();
            } else if (!appState.correctAnswers[SINGULAR][i].equals(getWordByIndex(actualAnswerSingularIx))) {
                res = false;
                appState.wordTextModels[actualAnswerSingularIx].visible = View.VISIBLE;
                appState.actualAnswers[SINGULAR][i] = -1;
            }

            int actualAnswerPluralIx = appState.actualAnswers[PLURAL][i];
            if (actualAnswerPluralIx == -1) {
                res &= appState.correctAnswers[PLURAL][i].isEmpty();
            } else if (!appState.correctAnswers[PLURAL][i].equals(getWordByIndex(actualAnswerPluralIx))) {
                res = false;
                appState.wordTextModels[actualAnswerPluralIx].visible = View.VISIBLE;
                appState.actualAnswers[PLURAL][i] = -1;
            }
        }
        notifyPropertyChanged(BR.wordTextModels);
        notifyPropertyChanged(BR.caseModels);
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
            textView.animate()
                    .rotationX(360)
                    .setDuration(500)
                    .start();
        }
        if (!textView.getText().toString().equals(prevText)) {
            textView2value.put(textView, text);
        }
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
