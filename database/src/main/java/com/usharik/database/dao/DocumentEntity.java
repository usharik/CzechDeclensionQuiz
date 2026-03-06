package com.usharik.database.dao;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "DOCUMENT",
        indices = {@Index(value = "word"), @Index(value = "gender"), @Index(value = "declension_type")})
public class DocumentEntity {
    @PrimaryKey(autoGenerate = true)
    private Long id;

    @ColumnInfo(name = "word_id")
    private Long wordId;

    @ColumnInfo(name = "word")
    private String word;

    @ColumnInfo(name = "gender")
    private String gender;

    @ColumnInfo(name = "declension_type")
    private String declensionType;

    @ColumnInfo(name = "json")
    private String json;

    public DocumentEntity(Long wordId, String word, String gender, String declensionType, String json) {
        this.wordId = wordId;
        this.word = word;
        this.gender = gender;
        this.declensionType = declensionType;
        this.json = json;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWordId() {
        return wordId;
    }

    public void setWordId(Long wordId) {
        this.wordId = wordId;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDeclensionType() {
        return declensionType;
    }

    public void setDeclensionType(String declensionType) {
        this.declensionType = declensionType;
    }
}
