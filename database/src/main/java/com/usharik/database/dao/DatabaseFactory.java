package com.usharik.database.dao;

import android.content.Context;
import com.usharik.database.DocumentRepository;

/**
 * Created by macbook on 14/03/2018.
 */

public class DatabaseFactory {

    public static DocumentRepository provideDocumentDb(Context context) {
        return new DocumentRepository(DocumentDatabase.getDocumentDatabase(context));
    }
}
