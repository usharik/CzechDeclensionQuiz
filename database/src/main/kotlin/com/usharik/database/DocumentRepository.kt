package com.usharik.database

import androidx.sqlite.db.SupportSQLiteStatement
import com.google.gson.Gson
import com.usharik.database.dao.DocumentDatabase
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class DocumentRepository @JvmOverloads constructor(private val db: DocumentDatabase, private val gson: Gson = Gson()) {
    suspend fun count(): Int = db.documentDao().count()
    suspend fun wordInfoByWord(word: String): WordInfo? = db.documentDao().jsonForWord(word)?.let { gson.fromJson(it, WordInfo::class.java) }
    suspend fun randomWordWithAnotherDeclensionType(type: String): WordInfo = gson.fromJson(db.documentDao().randomWordWithAnotherDeclensionType(type).json, WordInfo::class.java)
    suspend fun populateFromJsonStream(stream: InputStream) {
        BufferedReader(InputStreamReader(stream)).use { reader ->
            db.runInTransaction {
                generateSequence { reader.readLine() }.forEach { json ->
                    val word = gson.fromJson(json, WordInfo::class.java)
                    val statement: SupportSQLiteStatement = db.compileStatement("insert into DOCUMENT(word_id, word, gender, declension_type, json) values(?, ?, ?, ?, ?)")
                    statement.bindLong(1, word.wordId()!!)
                    statement.bindString(2, word.word())
                    statement.bindString(3, word.gender())
                    statement.bindString(4, word.declensionType())
                    statement.bindString(5, json)
                    statement.executeInsert()
                }
            }
        }
    }

}
