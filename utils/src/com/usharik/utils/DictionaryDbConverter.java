package com.usharik.utils;

import com.google.gson.Gson;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Generate Json for Declension Quiz Database
 * */
public class DictionaryDbConverter {

    private static Gson gson = new Gson();

    public static List<WordInfo> getWords(Connection conn) throws Exception {
        List<WordInfo> res = new ArrayList<>();
        ResultSet resultSet = conn.prepareStatement("select id, word\n" +
                "  from WORD A \n" +
                " where exists(select 1 from CASES_OF_NOUN where word_id = A.id)\n" +
                "   and A.lang = 'cz'" +
                " order by id").executeQuery();
        while (resultSet.next()) {
            WordInfo wordInfo = new WordInfo();
            wordInfo.wordId = resultSet.getLong(1);
            wordInfo.word = resultSet.getString(2);
            res.add(wordInfo);
        }
        return res;
    }

    public static void fillTranslations(Connection conn, List<WordInfo> wordInfos) throws Exception {
        PreparedStatement preparedStatement = conn.prepareStatement(
                "select B.translation \n" +
                        "  from WORD_TO_TRANSLATION A\n" +
                        " inner join TRANSLATION B on A.translation_id = B.id\n" +
                        " where A.word_id = ? and B.lang = ?");
        for (WordInfo wordInfo : wordInfos) {
            preparedStatement.setLong(1, wordInfo.wordId);
            preparedStatement.setString(2, "ru");
            ResultSet resultSet = preparedStatement.executeQuery();
            StringBuilder sb = new StringBuilder();
            while (resultSet.next()) {
                sb.append(resultSet.getString(1)).append(", ");
            }
            if (sb.length() > 2) {
                wordInfo.translation_ru = sb.substring(0,sb.length() - 2);
            } else {
                wordInfo.translation_ru = "";
            }
        }
    }

    public static void fillGender(Connection conn, List<WordInfo> wordInfos) throws Exception {
        PreparedStatement preparedStatement = conn.prepareStatement(
                "select info from WORD_INFO\n" +
                        "where info like 'rod:%'\n" +
                        "  and word_id = ?");
        for (WordInfo wordInfo : wordInfos) {
            preparedStatement.setLong(1, wordInfo.wordId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                wordInfo.gender = resultSet.getString(1);
            }
        }
    }

    public static void fillCases(Connection conn, List<WordInfo> wordInfos) throws Exception {
        PreparedStatement preparedStatement = conn.prepareStatement(
                "select case_num, number, word\n" +
                        "  from CASES_OF_NOUN\n" +
                        " where word_id = ?");
        for (WordInfo wordInfo : wordInfos) {
            preparedStatement.setLong(1, wordInfo.wordId);
            ResultSet resultSet = preparedStatement.executeQuery();
            wordInfo.cases = new String[2][7];
            while (resultSet.next()) {
                if (resultSet.getString(2).equals("nS")) {
                    wordInfo.cases[0][resultSet.getInt(1)-1] = resultSet.getString(3).replaceAll("[0-9]", "");
                } else {
                    wordInfo.cases[1][resultSet.getInt(1)-1] = resultSet.getString(3).replaceAll("[0-9]", "");
                }
            }
            wordInfo.cases[0][0] = wordInfo.word;
        }
    }

    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String pathToDatabase = "";
        String url = "jdbc:sqlite:" + pathToDatabase;
        Connection conn = DriverManager.getConnection(url);
        List<WordInfo> words = getWords(conn);
        fillTranslations(conn, words);
        fillGender(conn, words);
        fillCases(conn, words);
        words.stream()
                .map(wi -> gson.toJson(wi))
                .forEach(System.out::println);
    }

    public static class WordInfo {
        public Long wordId;
        public String word;
        public String gender;
        public String translation_ru;
        public String cases[][];
    }
}
