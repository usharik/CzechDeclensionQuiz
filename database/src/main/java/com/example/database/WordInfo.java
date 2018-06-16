package com.example.database;

public class WordInfo {
    public Long wordId;
    public String word;
    public String cases[][];
    public String translation;
    public String gender;

    public WordInfo(Long wordId, String word, String[][] cases, String translation, String gender) {
        this.wordId = wordId;
        this.word = word == null ? "" : word;
        this.cases = cases;
        this.translation = translation == null ? "" : translation;
        this.gender = gender == null ? "" : gender;
    }
}
