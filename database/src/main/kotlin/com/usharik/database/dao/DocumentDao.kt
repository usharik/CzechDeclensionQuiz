package com.usharik.database.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DocumentDao {
    @Query("select count(*) from DOCUMENT")
    suspend fun count(): Int

    @Query("select json from DOCUMENT where word = :word")
    suspend fun jsonForWord(word: String): String?

    @Query("""
        select * from DOCUMENT
        where declension_type != :declensionType
          and (:gender is null or gender = :gender)
        order by random() limit 1
    """)
    suspend fun randomWordWithAnotherDeclensionType(declensionType: String, gender: String?): DocumentEntity?
}
