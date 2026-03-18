package com.usharik.app;

import com.usharik.database.WordInfo;

/**
 * Holds the mutable state for a single full-declension quiz session.
 * Owned exclusively by DeclensionQuizViewModel.
 */
public class DeclensionQuizState {
    private WordInfo wordInfo;
    private WordTextModel[] wordTextModels;
    private String[][] correctAnswers;
    private int[][] actualAnswers;

    public DeclensionQuizState() {
        wordTextModels = new WordTextModel[14];
        correctAnswers = new String[2][7];
        actualAnswers = new int[2][7];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 7; j++) {
                actualAnswers[i][j] = -1;
            }
        }
    }

    public WordInfo getWordInfo() {
        return wordInfo;
    }

    public void setWordInfo(WordInfo wordInfo) {
        this.wordInfo = wordInfo;
    }

    public WordTextModel[] getWordTextModels() {
        return wordTextModels;
    }

    public void setWordTextModels(WordTextModel[] wordTextModels) {
        this.wordTextModels = wordTextModels;
    }

    public String[][] getCorrectAnswers() {
        return correctAnswers;
    }

    public int[][] getActualAnswers() {
        return actualAnswers;
    }

    public static class WordTextModel {
        private final String word;
        private int visible;

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

        public void setVisible(int visible) {
            this.visible = visible;
        }
    }
}
