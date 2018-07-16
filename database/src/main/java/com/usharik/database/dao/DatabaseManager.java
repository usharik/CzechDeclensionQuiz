package com.usharik.database.dao;

import android.content.Context;
import android.os.Environment;

import com.usharik.database.DocumentDb;
import com.usharik.database.WordInfo;
import com.google.gson.Gson;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;

import io.reactivex.Maybe;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by macbook on 14/03/2018.
 */

public class DatabaseManager {

    private static final String BACKUP_FOLDER = "/Declination-Quiz/";

    private Context context;
    private DocumentDatabase instance;
    private Gson gson = new Gson();
    private DocumentDb documentDb = new DocumentDbImpl();

    public DatabaseManager(Context context) {
        this.context = context;
    }

    private synchronized DocumentDatabase getActiveDbInstance() {
        if (instance == null || !instance.isOpen()) {
            instance = DocumentDatabase.getDocumentDatabase(context);
        }
        return instance;
    }

    public synchronized DocumentDb getDocumentDb() {
        return documentDb;
    }

    public void close() {
        if (instance == null) {
            return;
        }
        instance.close();
    }

    private boolean createBackupFolderIfNotExists() {
        File folder = new File(Environment.getExternalStorageDirectory() + BACKUP_FOLDER);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        return success;
    }

    public void backup() throws IOException {
        close();
        if (!createBackupFolderIfNotExists()) {
            throw new RuntimeException("Can't create folder for backup");
        }
        String sourcePath = getDatabasePath();
        String destPath = Environment.getExternalStorageDirectory() + BACKUP_FOLDER;
        copyFile(sourcePath, DocumentDatabase.DB_NAME, destPath, DocumentDatabase.DB_NAME);
    }

    public void restore() throws IOException {
        close();
        String sourcePath = Environment.getExternalStorageDirectory() + BACKUP_FOLDER;
        String destPath = getDatabasePath();
        copyFile(sourcePath, DocumentDatabase.DB_NAME, destPath, DocumentDatabase.DB_NAME);
    }

    public void restore(InputStream stream) throws IOException {
        close();
        OutputStream output = new BufferedOutputStream(new FileOutputStream(getDatabasePath() + DocumentDatabase.DB_NAME));

        byte data[] = new byte[1024];
        int count;

        while ((count = stream.read(data)) != -1) {
            output.write(data, 0, count);
        }

        output.flush();
        output.close();
        stream.close();
    }

    public void populateFromJsonStream(InputStream stream) throws IOException {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String json;
            while ((json = reader.readLine()) != null) {
                WordInfo wordInfo = gson.fromJson(json, WordInfo.class);
                getActiveDbInstance()
                        .compileStatement(String.format("insert into DOCUMENT(word_id, word, gender, json) values(%d, '%s', '%s', '%s');",
                        wordInfo.wordId, wordInfo.word, wordInfo.gender, json)).executeInsert();
            }
        }
    }

    private HttpURLConnection getHttpURLConnection(String link) throws IOException {
        URL url = new URL(link);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            conn.disconnect();
            throw new RuntimeException("Dictionary not found at URL.");
        }
        return conn;
    }

    public void restoreFromUrl(String link) throws IOException {
        close();
        HttpURLConnection conn = getHttpURLConnection(link);
        String fileName = BACKUP_FOLDER + DocumentDatabase.DB_NAME;
        String destFileName = Environment.getExternalStorageDirectory() + fileName;
        File destFile = new File(Environment.getExternalStorageDirectory(), fileName);
        if (!createBackupFolderIfNotExists()) {
            throw new RuntimeException("Can't create folder for backup");
        }
        if (destFile.exists()) {
            destFile.delete();
        }
        destFile.createNewFile();
        int loadedCount = 0;
        int fileLength;
        try (InputStream input = conn.getInputStream();
             OutputStream output = new FileOutputStream(destFileName)) {
            fileLength = conn.getContentLength();
            byte buffer[] = new byte[16384];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
                loadedCount += count;
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        if (loadedCount != fileLength) {
            throw new RuntimeException("Incorrect dictionary file length.");
        }
        restore();
    }

    private String getDatabasePath() {
        return Environment.getDataDirectory() + "/data/" + context.getPackageName() + "/databases/";
    }

    private void copyFile(String sourcePath,
                          String sourceFileName,
                          String destPath,
                          String detFileName) throws IOException {
        File sourceFile = new File(sourcePath, sourceFileName);
        File destFile = new File(destPath, detFileName);
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileChannel source = new FileInputStream(sourceFile).getChannel();
        FileChannel destination = new FileOutputStream(destFile).getChannel();
        destination.transferFrom(source, 0, source.size());
        source.close();
        destination.close();
    }

    private class DocumentDbImpl implements DocumentDb {

        @Override
        public Maybe<Long> getCount() {
            return getActiveDbInstance().documentDao().getCount()
                    .subscribeOn(Schedulers.io());
        }

        @Override
        public Maybe<WordInfo> getWordInfoById(long id) {
            return getActiveDbInstance().documentDao().getJsonString(id)
                    .flatMap(json -> Maybe.just(gson.fromJson(json, WordInfo.class)))
                    .subscribeOn(Schedulers.io());
        }

        @Override
        public Maybe<WordInfo> getWordInfoByWord(String word) {
            return getActiveDbInstance().documentDao().getJsonStringByWord(word)
                    .flatMap(json -> Maybe.just(gson.fromJson(json, WordInfo.class)))
                    .subscribeOn(Schedulers.io());
        }

        @Override
        public Maybe<WordInfo> getWordInfoByWordId(long wordId) {
            return getActiveDbInstance().documentDao().getJsonStringByWordId(wordId)
                    .flatMap(json -> Maybe.just(gson.fromJson(json, WordInfo.class)))
                    .subscribeOn(Schedulers.io());
        }

        @Override
        public long addWordInfo(WordInfo wordInfo) {
            return getActiveDbInstance()
                    .documentDao()
                    .insertDocument(new DocumentEntity(wordInfo.wordId, wordInfo.word, wordInfo.gender, gson.toJson(wordInfo)));
        }
    }
}