package com.usharik.app;

import androidx.lifecycle.MutableLiveData;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by macbook on 07/03/2018.
 * Application state management using LiveData for reactive updates.
 * This class maintains the global state of the quiz application.
 */
public class AppState {
    private final MutableLiveData<Map<String, Integer>> wordsWithErrorsLiveData = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<String> genderFilterStrLiveData = new MutableLiveData<>(Gender.ALL);
    private final MutableLiveData<Integer> genderFilterIdLiveData = new MutableLiveData<>(-1);
    private final MutableLiveData<Boolean> switchOffAnimationLiveData = new MutableLiveData<>(false);

    public Map<String, Integer> getWordsWithErrors() {
        Map<String, Integer> value = wordsWithErrorsLiveData.getValue();
        return value != null ? value : new HashMap<>();
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

    public void setWordsWithErrors(Map<String, Integer> wordsWithErrors) {
        this.wordsWithErrorsLiveData.setValue(wordsWithErrors);
    }

    public void setSwitchOffAnimation(boolean switchOffAnimation) {
        this.switchOffAnimationLiveData.setValue(switchOffAnimation);
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

    public void putWordToErrorMap(String word, int errorCount) {
        if (word != null && !word.isEmpty()) {
            Map<String, Integer> newMap = new HashMap<>(getWordsWithErrors());
            newMap.put(word, errorCount);
            wordsWithErrorsLiveData.setValue(newMap);
        }
    }

    public void removeWordFromErrorMap(String word) {
        if (word != null && !word.isEmpty()) {
            Map<String, Integer> newMap = new HashMap<>(getWordsWithErrors());
            newMap.remove(word);
            wordsWithErrorsLiveData.setValue(newMap);
        }
    }
}
