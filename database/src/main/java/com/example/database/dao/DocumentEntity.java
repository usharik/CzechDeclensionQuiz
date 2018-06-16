package com.example.database.dao;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "DOCUMENT")
public class DocumentEntity {
    @PrimaryKey(autoGenerate = true)
    private Long id;

    @ColumnInfo(name = "word_id")
    private Long wordId;

    @ColumnInfo(name = "json")
    private String json;

    public DocumentEntity(Long wordId, String json) {
        this.wordId = wordId;
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
}
