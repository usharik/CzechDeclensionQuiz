package com.usharik.database

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Reads dictionary entries (one JSONL line of `data.jsonl`, `adjectives.jsonl` or `verbs.jsonl`,
 * also stored as-is in the database) field by field.
 *
 * Deliberately no reflection: Gson's reflective binding depends on R8 keeping the entry classes,
 * their fields and constructors intact, and a release build without the right keep rules crashed
 * on start ("Abstract classes can't be instantiated"). Missing keys read as null, so rows written
 * before a field existed (e.g. the uk/vi glosses) still load.
 */
object DictionaryJson {

    fun wordInfo(json: String): WordInfo = parse(json).run {
        WordInfo(
            wordId = long("wordId"),
            word = string("word"),
            cases = get("cases").stringArray2(),
            translation_ru = string("translation_ru"),
            translation_en = string("translation_en"),
            gender = string("gender"),
            declensionType = string("declensionType"),
            translation_uk = string("translation_uk"),
            translation_vi = string("translation_vi"),
        )
    }

    fun adjectiveInfo(json: String): AdjectiveInfo = parse(json).run {
        AdjectiveInfo(
            wordId = long("wordId"),
            word = string("word"),
            kind = string("kind"),
            translation_ru = string("translation_ru"),
            translation_en = string("translation_en"),
            comparative = string("comparative"),
            superlative = string("superlative"),
            cases = (get("cases") as? JsonArray)?.map { it.stringArray2() ?: emptyArray() }?.toTypedArray(),
            translation_uk = string("translation_uk"),
            translation_vi = string("translation_vi"),
        )
    }

    fun verbInfo(json: String): VerbInfo = parse(json).run {
        VerbInfo(
            wordId = long("wordId"),
            word = string("word"),
            aspect = string("aspect"),
            pair = string("pair"),
            verbClass = string("verbClass"),
            translation_ru = string("translation_ru"),
            translation_en = string("translation_en"),
            present = get("present").stringArray(),
            past = get("past").stringArray(),
            imperative = get("imperative").stringArray(),
            future = get("future").stringArray(),
            translation_uk = string("translation_uk"),
            translation_vi = string("translation_vi"),
        )
    }

    private fun parse(json: String): JsonObject = JsonParser.parseString(json).asJsonObject

    private fun JsonObject.string(key: String): String? = get(key)?.takeUnless { it.isJsonNull }?.asString

    private fun JsonObject.long(key: String): Long? = get(key)?.takeUnless { it.isJsonNull }?.asLong

    private fun JsonElement?.stringArray(): Array<String>? =
        (this as? JsonArray)?.map { if (it.isJsonNull) "" else it.asString }?.toTypedArray()

    private fun JsonElement?.stringArray2(): Array<Array<String>>? =
        (this as? JsonArray)?.map { it.stringArray() ?: emptyArray() }?.toTypedArray()
}
