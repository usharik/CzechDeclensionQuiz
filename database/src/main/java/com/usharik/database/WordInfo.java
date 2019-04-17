package com.usharik.database;

public class WordInfo {
    public Long wordId;
    public String word;
    public String cases[][];
    public String translation_ru;
    public String translation_en;
    public String gender;

    public WordInfo(Long wordId, String word, String[][] cases, String translation_ru, String translation_en, String gender) {
        this.wordId = wordId;
        this.word = word == null ? "" : word;
        this.cases = cases;
        this.translation_ru = translation_ru == null ? "" : translation_ru;
        this.translation_en = translation_en == null ? "" : translation_en;
        this.gender = gender == null ? "" : gender;
    }
}
