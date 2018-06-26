package com.usharik.database;

public class WordInfo {
    public Long wordId;
    public String word;
    public String cases[][];
    public String translation_ru;
    public String gender;

    public WordInfo(Long wordId, String word, String[][] cases, String translation_ru, String gender) {
        this.wordId = wordId;
        this.word = word == null ? "" : word;
        this.cases = cases;
        this.translation_ru = translation_ru == null ? "" : translation_ru;
        this.gender = gender == null ? "" : gender;
    }
}
