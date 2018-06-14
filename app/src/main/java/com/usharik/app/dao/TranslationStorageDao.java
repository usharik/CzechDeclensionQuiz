package com.usharik.app.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import com.usharik.app.dao.entity.CasesOfNoun;
import com.usharik.app.dao.entity.FormsOfVerb;
import com.usharik.app.dao.entity.Translation;
import com.usharik.app.dao.entity.Word;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;

@Dao
public abstract class TranslationStorageDao {

    @Query("select count(*) from WORD")
    public abstract Single<Long> getWordCount();

    @Query("select * from WORD")
    public abstract List<Word> getAllWords();

    @Query("select * from WORD where lang = :lang")
    public abstract List<Word> getAllWordsForLang(String lang);

    @Query("select * from WORD where word = :word and lang = :lang")
    public abstract Maybe<Word> getWord(String word, String lang);

    @Query("select info " +
            " from WORD_INFO " +
            "where info like 'rod:%' " +
            "  and word_id = :wordId " +
            "order by word_id " +
            "limit 1")
    public abstract String getWordGender(long wordId);

    @Query("select id from WORD where word = :word and lang = :lang")
    public abstract Long getWordId(String word, String lang);

    @Query("select id from TRANSLATION where translation = :translation and lang = :lang")
    public abstract Long getTranslationId(String translation, String lang);

    @Query("select distinct A.word " +
            " from WORD as A " +
            "inner join WORD_TO_TRANSLATION as B on A.id = B.word_id " +
            "inner join TRANSLATION as C on B.translation_id = C.id " +
            "where A.lang = :langFrom " +
            "  and C.lang = :langTo " +
            "  and (A.word_for_search like :template1 || '%')" +
            "order by " +
            " case when A.word_for_search = :template1 or A.word = :template2 then 1" +
            "      when A.word like :template2 || '%' then 2" +
            "      else 3 end, " +
            " A.word " +
            "limit :limit")
    public abstract Maybe<List<String>> getSuggestions(String template1, String template2, String langFrom, String langTo, int limit);

    @Query("select * from TRANSLATION")
    public abstract List<Translation> getAllTranslation();

    @Query("select A.translation " +
            " from TRANSLATION as A " +
            "inner join WORD_TO_TRANSLATION as B on A.id = B.translation_id " +
            "inner join WORD as C on B.word_id = C.id " +
            "where C.word = :word " +
            "  and C.lang = :langFrom " +
            "  and A.lang = :langTo " +
            "order by A.translation " +
            "limit :limit")
    public abstract Maybe<List<String>> getTranslations(String word, String langFrom, String langTo, int limit);

    @Query("select A.word " +
            " from CASES_OF_NOUN as A " +
            "inner join WORD as B on A.word_id = B.id " +
            "where B.word = :word " +
            "  and A.case_num = :caseNum " +
            "  and A.number = :number")
    public abstract String getCaseOfNoun(String word, Integer caseNum, String number);

    @Query("select A.* " +
            " from CASES_OF_NOUN as A " +
            "inner join WORD as B on A.word_id = B.id " +
            "where B.word = :word " +
            "  and A.number = :number")
    public abstract Maybe<List<CasesOfNoun>> getCasesOfNoun(String word, String number);

    @Query("select A.* " +
            " from FORMS_OF_VERB as A " +
            "inner join WORD as B on A.word_id = B.id " +
            "where B.word = :word " +
            "  and A.number = :number")
    public abstract List<FormsOfVerb> getFormsOfVerb(String word, String number);

    @Query("select A.info " +
            " from WORD_INFO as A " +
            "inner join WORD as B on A.word_id = B.id " +
            "where B.word = :word ")
    public abstract List<String> getWordInfo(String word);
}
