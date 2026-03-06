package com.usharik.database;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

public interface DocumentDb {
    Single<Integer> getCount();

    Maybe<WordInfo> getWordInfoById(long id);

    Maybe<WordInfo> getWordInfoByWord(String word);

    Maybe<WordInfo> getWordInfoByWordId(long wordId);

    long addWordInfo(WordInfo wordInfo);
}
