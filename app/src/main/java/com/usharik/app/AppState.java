package com.usharik.app;

import com.usharik.database.WordInfo;
import com.usharik.app.fragment.DeclensionQuizViewModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by macbook on 07/03/2018.
 */

public class AppState {
    public WordInfo wordInfo;
    public DeclensionQuizViewModel.WordTextModel[] wordTextModels = new DeclensionQuizViewModel.WordTextModel[14];
    public String[][] correctAnswers = new String[2][7];
    public int[][] actualAnswers = new int[2][7];
    public Map<String, Integer> wordsWithErrors = new HashMap<>();

    public String selectedWord;
    public int selectedWordId = -1;
    public int selectedGender = -1;

    public String genderFilterStr = Gender.ALL;
    public int genderFilterId = -1;
    public boolean switchOffAnimation = false;

    public void setGenderFilterStr(String genderFilterStr) {
        this.genderFilterStr = genderFilterStr;
        switch (genderFilterStr) {
            case Gender.ANIMATE_MASCULINE:
                this.genderFilterId = R.id.radioAnimateMasculine;
                break;
            case Gender.INANIMATE_MASCULINE:
                this.genderFilterId = R.id.radioInanimateMasculine;
                break;
            case Gender.FEMININE:
                this.genderFilterId = R.id.radioFeminine;
                break;
            case Gender.NEUTER:
                this.genderFilterId = R.id.radioNeuter;
                break;
        }
    }

    public void putWordToErrorMap(int errorCount) {
        if (wordInfo != null) {
            wordsWithErrors.put(wordInfo.word, errorCount);
        }
    }

    public void removeWordFromErrorMap() {
        if (wordInfo != null) {
            wordsWithErrors.remove(wordInfo.word);
        }
    }
}
