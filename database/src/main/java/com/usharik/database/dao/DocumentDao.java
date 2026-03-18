package com.usharik.database.dao;

import androidx.room.Dao;
import androidx.room.Query;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface DocumentDao {

    @Query("select count(*) from DOCUMENT")
    Single<Integer> getCount();

    @Query("select json from DOCUMENT where word = :word")
    Maybe<String> getJsonStringByWord(String word);

    @Query( """
            select *
            from DOCUMENT
            where declension_type != :declensionType
            order by random() limit 1
            """)
    Single<DocumentEntity> getRandomWordWithAnotherDeclensionType(String declensionType);
}
