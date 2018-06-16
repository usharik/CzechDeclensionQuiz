package com.example.database;

import io.reactivex.Maybe;

public interface DocumentDb {
    Maybe<Long> getCount();

    Maybe<WordInfo> getWordInfoById(long id);

    long addWordInfo(WordInfo wordInfo);
}
