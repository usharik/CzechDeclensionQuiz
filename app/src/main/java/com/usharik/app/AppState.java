package com.usharik.app;

import androidx.lifecycle.MutableLiveData;

import com.usharik.database.WordInfo;
import com.usharik.app.fragment.DeclensionQuizViewModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by macbook on 07/03/2018.
 * Application state management using LiveData for reactive updates.
 * This class maintains the global state of the quiz application.
 */
public class AppState {
    private final MutableLiveData<WordInfo> wordInfoLiveData = new MutableLiveData<>();
    private final MutableLiveData<DeclensionQuizViewModel.WordTextModel[]> wordTextModelsLiveData;
    private final MutableLiveData<String[][]> correctAnswersLiveData;
    private final MutableLiveData<int[][]> actualAnswersLiveData;
    private final MutableLiveData<Map<String, Integer>> wordsWithErrorsLiveData = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<String> selectedWordLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> selectedWordIdLiveData = new MutableLiveData<>(-1);
    private final MutableLiveData<Integer> selectedGenderLiveData = new MutableLiveData<>(-1);
    private final MutableLiveData<String> genderFilterStrLiveData = new MutableLiveData<>(Gender.ALL);
    private final MutableLiveData<Integer> genderFilterIdLiveData = new MutableLiveData<>(-1);
    private final MutableLiveData<Boolean> switchOffAnimationLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> currentNavigationItemLiveData = new MutableLiveData<>();

    // Constructor to initialize arrays
    public AppState() {
        wordTextModelsLiveData = new MutableLiveData<>(new DeclensionQuizViewModel.WordTextModel[14]);
        correctAnswersLiveData = new MutableLiveData<>(new String[2][7]);

        int[][] initialActualAnswers = new int[2][7];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 7; j++) {
                initialActualAnswers[i][j] = -1;
            }
        }
        actualAnswersLiveData = new MutableLiveData<>(initialActualAnswers);
    }

    public WordInfo getWordInfo() {
        return wordInfoLiveData.getValue();
    }

    public DeclensionQuizViewModel.WordTextModel[] getWordTextModels() {
        return wordTextModelsLiveData.getValue();
    }

    public String[][] getCorrectAnswers() {
        return correctAnswersLiveData.getValue();
    }

    public int[][] getActualAnswers() {
        return actualAnswersLiveData.getValue();
    }

    public Map<String, Integer> getWordsWithErrors() {
        Map<String, Integer> value = wordsWithErrorsLiveData.getValue();
        return value != null ? value : new HashMap<>();
    }

    public String getSelectedWord() {
        return selectedWordLiveData.getValue();
    }

    public int getSelectedWordId() {
        Integer value = selectedWordIdLiveData.getValue();
        return value != null ? value : -1;
    }

    public int getSelectedGender() {
        Integer value = selectedGenderLiveData.getValue();
        return value != null ? value : -1;
    }

    public String getGenderFilterStr() {
        String value = genderFilterStrLiveData.getValue();
        return value != null ? value : Gender.ALL;
    }

    public int getGenderFilterId() {
        Integer value = genderFilterIdLiveData.getValue();
        return value != null ? value : -1;
    }

    public boolean getSwitchOffAnimation() {
        Boolean value = switchOffAnimationLiveData.getValue();
        return value != null ? value : false;
    }

    public Integer getCurrentNavigationItem() {
        return currentNavigationItemLiveData.getValue();
    }

    public void setWordInfo(WordInfo wordInfo) {
        this.wordInfoLiveData.setValue(wordInfo);
    }

    public void setWordTextModels(DeclensionQuizViewModel.WordTextModel[] wordTextModels) {
        this.wordTextModelsLiveData.setValue(wordTextModels);
    }

    public void setWordsWithErrors(Map<String, Integer> wordsWithErrors) {
        this.wordsWithErrorsLiveData.setValue(wordsWithErrors);
    }

    public void setSelectedWord(String selectedWord) {
        this.selectedWordLiveData.setValue(selectedWord);
    }

    public void setSelectedWordId(int selectedWordId) {
        this.selectedWordIdLiveData.setValue(selectedWordId);
    }

    public void setSelectedGender(int selectedGender) {
        this.selectedGenderLiveData.setValue(selectedGender);
    }

    public void setSwitchOffAnimation(boolean switchOffAnimation) {
        this.switchOffAnimationLiveData.setValue(switchOffAnimation);
    }

    public void setCurrentNavigationItem(Integer currentNavigationItem) {
        this.currentNavigationItemLiveData.setValue(currentNavigationItem);
    }

    public void setGenderFilterStr(String genderFilterStr) {
        this.genderFilterStrLiveData.setValue(genderFilterStr);

        int filterId = switch (genderFilterStr) {
            case Gender.ANIMATE_MASCULINE -> R.id.radioAnimateMasculine;
            case Gender.INANIMATE_MASCULINE -> R.id.radioInanimateMasculine;
            case Gender.FEMININE -> R.id.radioFeminine;
            case Gender.NEUTER -> R.id.radioNeuter;
            default -> -1;
        };
        this.genderFilterIdLiveData.setValue(filterId);
    }

    public void putWordToErrorMap(int errorCount) {
        WordInfo currentWordInfo = getWordInfo();
        if (currentWordInfo != null) {
            Map<String, Integer> currentMap = getWordsWithErrors();
            currentMap.put(currentWordInfo.word, errorCount);
            wordsWithErrorsLiveData.setValue(currentMap);
        }
    }

    public void removeWordFromErrorMap() {
        WordInfo currentWordInfo = getWordInfo();
        if (currentWordInfo != null) {
            Map<String, Integer> currentMap = getWordsWithErrors();
            currentMap.remove(currentWordInfo.word);
            wordsWithErrorsLiveData.setValue(currentMap);
        }
    }
}
