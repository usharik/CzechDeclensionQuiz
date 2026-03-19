package com.usharik.app.fragment;

import androidx.databinding.Bindable;
import com.usharik.app.BR;
import com.usharik.app.framework.ViewModelObservable;
import com.usharik.database.DocumentRepository;

import javax.inject.Inject;

public class WordsWithErrorsViewModel extends ViewModelObservable {

    private String[][] cases;
    private String selectedWord;
    private final DocumentRepository documentRepository;

    @Inject
    public WordsWithErrorsViewModel(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
        this.cases = new String[2][7];
    }

    @Bindable
    public String[][] getCases() {
        return cases;
    }

    public void setSelectedWord(String selectedWord) {
        this.selectedWord = selectedWord;
       documentRepository.getWordInfoByWord(selectedWord)
                .subscribe(wordInfo -> {
                    this.cases = wordInfo.cases();
                    notifyPropertyChanged(BR.cases);
                });
    }

    public String getSelectedWord() {
        return this.selectedWord;
    }
}
