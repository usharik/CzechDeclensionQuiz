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
    suspend fun randomWordWithAnotherDeclensionType(type: String, gender: String? = null): WordInfo {
        val dao = db.documentDao()
        // Fall back to any declension type within the gender, then to any gender, so a
        // narrow filter can never leave the quiz without a word.
        val entity = dao.randomWordWithAnotherDeclensionType(type, gender)
            ?: dao.randomWordWithAnotherDeclensionType("", gender)
            ?: requireNotNull(dao.randomWordWithAnotherDeclensionType(type, null)) { "Dictionary is empty" }
        return gson.fromJson(entity.json, WordInfo::class.java)
    }
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

    // ---- adjectives -------------------------------------------------------------------------

    suspend fun adjectiveCount(): Int = db.lexiconDao().adjectiveCount()
    suspend fun adjectiveByWord(word: String): AdjectiveInfo? = db.lexiconDao().adjectiveJson(word)?.let { gson.fromJson(it, AdjectiveInfo::class.java) }

    /** A random adjective other than [excludingWord], preferring [kind] (tvrdé/měkké/přivlastňovací) when given. */
    suspend fun randomAdjective(excludingWord: String = "", kind: String? = null): AdjectiveInfo {
        val dao = db.lexiconDao()
        val entity = dao.randomAdjective(excludingWord, kind)
            ?: requireNotNull(dao.randomAdjective(excludingWord, null)) { "Adjective dictionary is empty" }
        return gson.fromJson(entity.json, AdjectiveInfo::class.java)
    }

    suspend fun populateAdjectivesFromJsonStream(stream: InputStream) {
        BufferedReader(InputStreamReader(stream)).use { reader ->
            db.runInTransaction {
                val statement = db.compileStatement("insert into ADJECTIVE(word_id, word, kind, json) values(?, ?, ?, ?)")
                generateSequence { reader.readLine() }.forEach { json ->
                    val adjective = gson.fromJson(json, AdjectiveInfo::class.java)
                    statement.bindLong(1, adjective.wordId!!)
                    statement.bindString(2, adjective.word())
                    statement.bindString(3, adjective.kind())
                    statement.bindString(4, json)
                    statement.executeInsert()
                    statement.clearBindings()
                }
            }
        }
    }

    // ---- verbs -------------------------------------------------------------------------------

    suspend fun verbCount(): Int = db.lexiconDao().verbCount()
    suspend fun verbByWord(word: String): VerbInfo? = db.lexiconDao().verbJson(word)?.let { gson.fromJson(it, VerbInfo::class.java) }

    /** A random verb other than [excludingWord], preferring [verbClass] (dělá, prosí, …) when given. */
    suspend fun randomVerb(excludingWord: String = "", verbClass: String? = null): VerbInfo {
        val dao = db.lexiconDao()
        val entity = dao.randomVerb(excludingWord, verbClass)
            ?: requireNotNull(dao.randomVerb(excludingWord, null)) { "Verb dictionary is empty" }
        return gson.fromJson(entity.json, VerbInfo::class.java)
    }

    suspend fun populateVerbsFromJsonStream(stream: InputStream) {
        BufferedReader(InputStreamReader(stream)).use { reader ->
            db.runInTransaction {
                val statement = db.compileStatement("insert into VERB(word_id, word, aspect, verb_class, json) values(?, ?, ?, ?, ?)")
                generateSequence { reader.readLine() }.forEach { json ->
                    val verb = gson.fromJson(json, VerbInfo::class.java)
                    statement.bindLong(1, verb.wordId!!)
                    statement.bindString(2, verb.word())
                    statement.bindString(3, verb.aspect())
                    statement.bindString(4, verb.verbClass())
                    statement.bindString(5, json)
                    statement.executeInsert()
                    statement.clearBindings()
                }
            }
        }
    }

}
