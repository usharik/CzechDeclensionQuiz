package com.usharik.app;

import android.databinding.Bindable;
import android.view.View;

import com.usharik.app.framework.ViewModelObservable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.usharik.app.service.WordService;

/**
 * Created by macbook on 07/03/2018.
 */

public class MainViewModel extends ViewModelObservable {
    public static final int SINGULAR = 0;
    public static final int PLURAL = 1;

    private final AppState appState;
    private WordService wordService;
    private String text;

    private WordTextModel[] wordTextModels = new WordTextModel[14];

    private String[][] correctAnswers = new String[2][7];
    private int[][] actualAnswers = new int[2][7];

    @Inject
    public MainViewModel(final AppState appState,
                         final WordService wordService) {
        this.appState = appState;
        this.wordService = wordService;
    }

    @Bindable
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        notifyPropertyChanged(BR.text);
    }

    public void getWordDeclension() {
        WordService.WordInfo nextWord = wordService.getNextWord();

        List<WordTextModel> words = new ArrayList<>();
        for (int i=0; i<7; i++) {
            words.add(new WordTextModel(nextWord.cases[SINGULAR][i], View.VISIBLE));
            words.add(new WordTextModel(nextWord.cases[PLURAL][i], View.VISIBLE));

            correctAnswers[SINGULAR][i] = nextWord.cases[SINGULAR][i];
            correctAnswers[PLURAL][i] = nextWord.cases[PLURAL][i];

            actualAnswers[SINGULAR][i] = -1;
            actualAnswers[PLURAL][i] = -1;
        }
        Collections.shuffle(words);
        wordTextModels = words.toArray(new WordTextModel[words.size()]);
    }

    public boolean checkAnswers() {
        boolean res=true;
        for (int i=0; i<7; i++) {
            int actualAnswerSingularIx = actualAnswers[SINGULAR][i];
            if (!correctAnswers[SINGULAR][i].equals(getWordByIndex(actualAnswerSingularIx))) {
                res = false;
                wordTextModels[actualAnswerSingularIx].visible = View.VISIBLE;
                actualAnswers[SINGULAR][i] = -1;
            }

            int actualAnswerPluralIx = actualAnswers[PLURAL][i];
            if (!correctAnswers[PLURAL][i].equals(getWordByIndex(actualAnswerPluralIx))) {
                res = false;
                wordTextModels[actualAnswerPluralIx].visible = View.VISIBLE;
                actualAnswers[PLURAL][i] = -1;
            }
        }
        notifyPropertyChanged(BR.wordTextModels);
        notifyPropertyChanged(BR.caseModels);
        return res;
    }

    @Bindable
    public WordTextModel[] getWordTextModels() {
        return wordTextModels;
    }

    @Bindable
    public int[][] getCaseModels() {
        return actualAnswers;
    }

    public String getWordByIndex(int ix) {
        if (ix == -1) {
            return "";
        }
        return wordTextModels[ix].getWord();
    }

    public void updateWordTextModel(int num, int visible) {
        wordTextModels[num].visible = visible;
        notifyPropertyChanged(BR.wordTextModels);
    }

    public void updateCaseModel(int caseNum, int number, int wordIx) {
        if (actualAnswers[number][caseNum] != -1) {
            wordTextModels[actualAnswers[number][caseNum]].visible = View.VISIBLE;
            notifyPropertyChanged(BR.wordTextModels);
        }
        actualAnswers[number][caseNum] = wordIx;
        notifyPropertyChanged(BR.caseModels);
    }

    public void swapCaseModels(int caseNum1, int number1, int caseNum2, int number2) {
        int tmp = actualAnswers[number1][caseNum1];
        actualAnswers[number1][caseNum1] = actualAnswers[number2][caseNum2];
        actualAnswers[number2][caseNum2] = tmp;
        notifyPropertyChanged(BR.caseModels);
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
