package com.usharik.app.helpers;

/**
 * Data class representing word information from data.json
 * Used for parsing JSON in UI tests
 */
public class WordInfo {
    public Long wordId;
    public String word;
    public String gender;
    public String declensionType;
    public String translation_ru;
    public String translation_en;
    public String[][] cases;

    // No-arg constructor for GSON
    public WordInfo() {
    }
}

