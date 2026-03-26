package com.usharik.database.dao;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Singleton row (id=1) that stores the last N recently studied words
 * as a comma-separated string, ordered oldest→newest.
 */
@Entity(tableName = "recent_words")
public class RecentWordsEntity {

    @PrimaryKey
    public int id = 1;

    /** Comma-separated list of recent words, oldest first. Empty string means no history. */
    @ColumnInfo(name = "words")
    public String words = "";
}

