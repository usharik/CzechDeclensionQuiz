package com.usharik.app;

import com.usharik.database.WordInfo;

import java.util.Collections;
import java.util.List;

/**
 * Holds the mutable state for a single-case quiz session.
 * Owned exclusively by SingleCaseQuizViewModel.
 */
public class SingleCaseQuizState {
    private WordInfo wordInfo;
    private int currentCase;
    private boolean isPlural;
    private List<String> answers = Collections.emptyList();
    private String correctAnswer = "";
    private boolean answered;
    private List<String> distractorForms = Collections.emptyList();

    public WordInfo getWordInfo() {
        return wordInfo;
    }

    public void setWordInfo(WordInfo wordInfo) {
        this.wordInfo = wordInfo;
    }

    public int getCurrentCase() {
        return currentCase;
    }

    public void setCurrentCase(int currentCase) {
        this.currentCase = currentCase;
    }

    public boolean isPlural() {
        return isPlural;
    }

    public void setPlural(boolean plural) {
        isPlural = plural;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public void setAnswers(List<String> answers) {
        this.answers = answers == null ? Collections.emptyList() : answers;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer == null ? "" : correctAnswer;
    }

    public boolean isAnswered() {
        return answered;
    }

    public void setAnswered(boolean answered) {
        this.answered = answered;
    }

    public List<String> getDistractorForms() {
        return distractorForms;
    }

    public void setDistractorForms(List<String> distractorForms) {
        this.distractorForms = distractorForms == null ? Collections.emptyList() : distractorForms;
    }
}