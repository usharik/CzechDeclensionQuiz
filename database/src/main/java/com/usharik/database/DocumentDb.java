package com.usharik.database;

import io.reactivex.Maybe;

public interface DocumentDb {
    Maybe<Long> getCount();

    Maybe<WordInfo> getWordInfoById(long id);

    Maybe<WordInfo> getWordInfoByWord(String word);

    Maybe<WordInfo> getWordInfoByWordId(long wordId);

    long addWordInfo(WordInfo wordInfo);
}
