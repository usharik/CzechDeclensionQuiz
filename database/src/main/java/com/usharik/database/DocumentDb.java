package com.usharik.database;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

public interface DocumentDb {
    Single<Integer> getCount();

    Maybe<WordInfo> getWordInfoByWord(String word);

    Single<WordInfo> getRandomWordWithAnotherDeclensionType(String prevDeclensionType);
}
