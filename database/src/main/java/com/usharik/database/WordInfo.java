package com.usharik.database;

public record WordInfo(
    Long wordId,
    String word,
    String[][] cases,
    String translation_ru,
    String translation_en,
    String gender,
    String declensionType
) {
    public WordInfo(Long wordId, String word, String[][] cases, String translation_ru, String translation_en, String gender, String declensionType) {
        this.wordId = wordId;
        this.word = word == null ? "" : word;
        this.cases = cases;
        this.translation_ru = translation_ru == null ? "" : translation_ru;
        this.translation_en = translation_en == null ? "" : translation_en;
        this.gender = gender == null ? "" : gender;
        this.declensionType = declensionType == null ? "" : declensionType;
    }

    public String cases(int i, int j) {
        return this.cases[i][j];
    }
}
