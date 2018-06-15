package com.usharik.app.service;

import com.usharik.app.dao.DatabaseManager;
import com.usharik.app.dao.TranslationStorageDao;
import io.reactivex.Maybe;
import io.reactivex.schedulers.Schedulers;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.usharik.app.MainViewModel.PLURAL;
import static com.usharik.app.MainViewModel.SINGULAR;

public class WordService {

    private final DatabaseManager databaseManager;
    private final List<Long> wordList;
    private final Random random = new Random();

    public WordService(final DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        wordList = databaseManager.getActiveDbInstance().translationStorageDao()
                .getWordList()
                .subscribeOn(Schedulers.io())
                .blockingGet(Collections.emptyList());
    }

    public WordInfo getNextWord() {
        TranslationStorageDao dao = databaseManager.getActiveDbInstance().translationStorageDao();
        long wordId = wordList.get(random.nextInt(wordList.size()));

        return Maybe.zip(
                dao.getCasesOfNoun(wordId, "nS").defaultIfEmpty(Collections.emptyList()),
                dao.getCasesOfNoun(wordId, "nP").defaultIfEmpty(Collections.emptyList()),
                dao.getTranslation(wordId, "cz", "ru", 100).defaultIfEmpty(Collections.emptyList()),
                dao.getWordGender(wordId).defaultIfEmpty(""),
                (singular, plural, translations, gender) -> {
                    String cases[][] = new String[2][7];
                    for (int i=0; i<7; i++) {
                        cases[SINGULAR][i] = singular.get(i);
                        cases[PLURAL][i] = plural.get(i);
                    }
                    StringBuilder sb = new StringBuilder();
                    for (String str : translations) {
                        sb.append(str).append(", ");
                    }
                    return new WordInfo(cases[0][0], cases, sb.toString(), gender);
                })
                .subscribeOn(Schedulers.io())
                .blockingGet();
    }

    public class WordInfo {
        public String word;
        public String cases[][];
        public String translation;
        public String gender;

        public WordInfo(String word, String[][] cases, String translation, String gender) {
            this.word = word;
            this.cases = cases;
            this.translation = translation;
            this.gender = gender;
        }
    }
}
