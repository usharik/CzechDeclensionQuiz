package com.usharik.database.dao;

import android.content.Context;
import com.usharik.database.DocumentRepository;

/**
 * Created by macbook on 14/03/2018.
 */

public class DatabaseFactory {

    /** Returns the shared {@link DocumentDatabase} instance. */
    public static DocumentDatabase provideDocumentDatabase(Context context) {
        return DocumentDatabase.getDocumentDatabase(context);
    }

    /** @deprecated Use {@link #provideDocumentDatabase(Context)} to share one DB instance. */
    @Deprecated
    public static DocumentRepository provideDocumentDb(Context context) {
        return new DocumentRepository(DocumentDatabase.getDocumentDatabase(context));
    }
}
