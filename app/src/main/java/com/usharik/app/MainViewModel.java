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
    private final AppState appState;
    private DatabaseManager databaseManager;
    private String text;

    private WordTextModel[] wordTextModels = new WordTextModel[14];
    private CaseModel[] caseModels = new CaseModel[7];

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
            caseModels[i] = new CaseModel();
            caseModels[i].correctAnswerSingular = singular.get(i).getWord();
            caseModels[i].correctAnswerPlural = plural.get(i).getWord();
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
    public CaseModel[] getCaseModels() {
        return caseModels;
    }

    public void updateWordTextModel(int num, int visible) {
        wordTextModels[num].visible = visible;
        notifyPropertyChanged(BR.wordTextModels);
    }

    public void updateCaseModelSingular(int num, int wordIx) {
        caseModels[num].singularIx = wordIx;
        notifyPropertyChanged(BR.caseModels);
    }

    public void updateCaseModelPlural(int num, int wordIx) {
        caseModels[num].pluralIx = wordIx;
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

    public class CaseModel {
        String correctAnswerSingular;
        int singularIx = -1;
        String correctAnswerPlural;
        int pluralIx = -1;

        public String getActualAnswerSingular() {
            if (singularIx == -1) {
                return "";
            }
            return wordTextModels[singularIx].getWord();
        }

        public String getActualAnswerPlural() {
            if (pluralIx == -1) {
                return "";
            }
            return wordTextModels[pluralIx].getWord();
        }

        boolean isCorrectAnswerSingular() {
            return correctAnswerSingular.equals(wordTextModels[singularIx].getWord());
        }

        boolean isCorrectAnswerPlural() {
            return correctAnswerPlural.equals(wordTextModels[pluralIx].getWord());
        }
    }
}
