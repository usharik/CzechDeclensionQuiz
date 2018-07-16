package com.usharik.database.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

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
