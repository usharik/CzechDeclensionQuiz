package com.usharik.database.dao

import androidx.room.Dao
import androidx.room.Query

/** Adjective and verb dictionary tables (added in database version 11 next to the noun `DOCUMENT` table). */
@Dao
interface LexiconDao {
    @Query("select count(*) from ADJECTIVE")
    suspend fun adjectiveCount(): Int

    @Query("select json from ADJECTIVE where word = :word")
    suspend fun adjectiveJson(word: String): String?

    @Query("""
        select * from ADJECTIVE
        where word != :excludingWord
          and (:kind is null or kind = :kind)
        order by random() limit 1
    """)
    suspend fun randomAdjective(excludingWord: String, kind: String?): AdjectiveEntity?

    @Query("select count(*) from VERB")
    suspend fun verbCount(): Int

    @Query("select json from VERB where word = :word")
    suspend fun verbJson(word: String): String?

    @Query("""
        select * from VERB
        where word != :excludingWord
          and (:verbClass is null or verb_class = :verbClass)
        order by random() limit 1
    """)
    suspend fun randomVerb(excludingWord: String, verbClass: String?): VerbEntity?
}
