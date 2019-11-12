package com.usharik.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.Maybe;

@Dao
public interface DocumentDao {

    @Query("select count(*) from DOCUMENT")
    Maybe<Long> getCount();

    @Query("select json from DOCUMENT where id = :id")
    Maybe<String> getJsonString(long id);

    @Query("select json from DOCUMENT where word = :word")
    Maybe<String> getJsonStringByWord(String word);

    @Query("select json from DOCUMENT where word_id = :wordId")
    Maybe<String> getJsonStringByWordId(long wordId);

    @Query("select * from DOCUMENT")
    List<DocumentEntity> getAllDocuments();

    @Insert
    long insertDocument(DocumentEntity entity);

    @Update
    void updateDocument(DocumentEntity entity);
}
