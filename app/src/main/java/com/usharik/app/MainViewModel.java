package com.usharik.app;

import android.databinding.Bindable;
import android.view.View;

import com.usharik.app.dao.DatabaseManager;
import com.usharik.app.dao.TranslationStorageDao;
import com.usharik.app.dao.entity.CasesOfNoun;
import com.usharik.app.framework.ViewModelObservable;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.schedulers.Schedulers;

/**
 * Created by macbook on 07/03/2018.
 */

public class MainViewModel extends ViewModelObservable {
    public static final int SINGULAR = 0;
    public static final int PLURAL = 1;

    private final AppState appState;
    private DatabaseManager databaseManager;
    private String text;

    private WordTextModel[] wordTextModels = new WordTextModel[14];

    private String[][] correctAnswers = new String[2][7];
    private int[][] actualAnswers = new int[2][7];

    @Inject
    public MainViewModel(final AppState appState,
                         final DatabaseManager databaseManager) {
        this.appState = appState;
        this.databaseManager = databaseManager;
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
        TranslationStorageDao dao = databaseManager.getActiveDbInstance().translationStorageDao();

        List<CasesOfNoun> singular = dao.getCasesOfNoun("dům", "nS")
                .subscribeOn(Schedulers.io())
                .blockingGet(Collections.EMPTY_LIST);
        List<CasesOfNoun> plural = dao.getCasesOfNoun("dům", "nP")
                .subscribeOn(Schedulers.io())
                .blockingGet(Collections.EMPTY_LIST);
        for (int i=0; i<7; i++) {
            wordTextModels[i] = new WordTextModel(singular.get(i).getWord(), View.VISIBLE);
            wordTextModels[i+7] = new WordTextModel(plural.get(i).getWord(), View.VISIBLE);
            correctAnswers[SINGULAR][i] = singular.get(i).getWord();
            correctAnswers[PLURAL][i] = plural.get(i).getWord();
            actualAnswers[SINGULAR][i] = -1;
            actualAnswers[PLURAL][i] = -1;
        }
    }

    public boolean checkAnswers() {
        boolean res=true;
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
