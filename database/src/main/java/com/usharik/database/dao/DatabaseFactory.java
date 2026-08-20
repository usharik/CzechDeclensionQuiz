package com.usharik.database.dao;

import android.content.Context;

/**
 * Created by macbook on 14/03/2018.
 */

public class DatabaseFactory {

    /** Returns the shared {@link DocumentDatabase} instance. */
    public static DocumentDatabase provideDocumentDatabase(Context context) {
        return DocumentDatabase.getDocumentDatabase(context);
    }
}
