package com.usharik.app.fragment;

import android.util.Log;

import androidx.databinding.Bindable;
import com.usharik.app.BR;
import com.usharik.app.framework.ViewModelObservable;
import com.usharik.database.DocumentRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class WordsWithErrorsViewModel extends ViewModelObservable {

    private static final String TAG = "WordsWithErrorsVM";

    private String[][] cases;
    private String selectedWord;
    private final DocumentRepository documentRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

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
        disposables.add(
            documentRepository.getWordInfoByWord(selectedWord)
                .subscribe(
                    wordInfo -> {
                        this.cases = wordInfo.cases();
                        notifyPropertyChanged(BR.cases);
                    },
                    thr -> Log.e(TAG, "Error loading word info for: " + selectedWord, thr)
                )
        );
    }

    public String getSelectedWord() {
        return this.selectedWord;
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
