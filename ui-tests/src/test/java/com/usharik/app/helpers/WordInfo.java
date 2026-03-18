package com.usharik.app.helpers;

/**
 * Data class representing word information from data.jsonl
 * Used for parsing JSON in UI tests
 */
public record WordInfo(
    Long wordId,
    String word,
    String gender,
    String declensionType,
    String translation_ru,
    String translation_en,
    String[][] cases) {

}
