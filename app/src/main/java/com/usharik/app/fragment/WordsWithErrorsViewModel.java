package com.usharik.app.fragment;

import androidx.databinding.Bindable;
import com.usharik.app.AppState;
import com.usharik.app.BR;
import com.usharik.app.framework.ViewModelObservable;
import com.usharik.database.WordInfo;
import com.usharik.database.dao.DatabaseManager;

import javax.inject.Inject;

public class WordsWithErrorsViewModel extends ViewModelObservable {

    private String[][] cases;
    private String selectedWord;

    private final AppState appState;
    private final DatabaseManager databaseManager;

    @Inject
    public WordsWithErrorsViewModel(DatabaseManager databaseManager,
                                    AppState appState) {
        this.appState = appState;
        this.databaseManager = databaseManager;
        this.cases = new String[2][7];
    }

    @Bindable
    public String[][] getCases() {
        return cases;
    }

    public void setSelectedWord(String selectedWord) {
        this.selectedWord = selectedWord;
        WordInfo wordInfo = databaseManager.getDocumentDb().getWordInfoByWord(selectedWord).blockingGet();
        this.cases = wordInfo.cases;
        notifyPropertyChanged(BR.cases);
    }

    public String getSelectedWord() {
        return this.selectedWord;
    }
}