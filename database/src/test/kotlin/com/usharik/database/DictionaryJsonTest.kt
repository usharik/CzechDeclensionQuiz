package com.usharik.database

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DictionaryJsonTest {
    /** Unit tests run with the module directory as the working directory. */
    private fun asset(name: String): List<String> = File("src/main/assets/$name").readLines().filter { it.isNotBlank() }

    private fun assertGlosses(word: String, vararg glosses: String) =
        glosses.forEach { assertTrue("missing gloss for $word", it.isNotBlank()) }

    @Test fun everyBundledNounParses() {
        val words = asset("data.jsonl").map(DictionaryJson::wordInfo)
        assertEquals(913, words.size)
        words.forEach { w ->
            assertTrue(w.word().isNotEmpty() && w.wordId != null && w.gender().isNotEmpty())
            assertEquals(w.word(), 2, w.cases!!.size)
            w.cases!!.forEach { assertEquals(w.word(), 7, it.size) }
            assertGlosses(w.word(), w.translation_ru(), w.translation_en(), w.translation_uk(), w.translation_vi())
        }
    }

    @Test fun everyBundledAdjectiveParses() {
        val adjectives = asset("adjectives.jsonl").map(DictionaryJson::adjectiveInfo)
        assertEquals(506, adjectives.size)
        adjectives.forEach { a ->
            assertTrue(a.word().isNotEmpty() && a.kind().isNotEmpty())
            assertTrue(a.word(), a.form(0, AdjectiveInfo.GENDER_MASCULINE_ANIMATE, 0).isNotEmpty())
            assertEquals(a.word(), 2, a.cases!!.size)
            a.cases!!.forEach { number -> assertEquals(4, number.size); number.forEach { assertEquals(7, it.size) } }
            assertGlosses(a.word(), a.translation_ru(), a.translation_en(), a.translation_uk(), a.translation_vi())
        }
    }

    @Test fun everyBundledVerbParses() {
        val verbs = asset("verbs.jsonl").map(DictionaryJson::verbInfo)
        assertEquals(581, verbs.size)
        verbs.forEach { v ->
            assertTrue(v.word().isNotEmpty() && v.aspect().isNotEmpty() && v.verbClass().isNotEmpty())
            assertEquals(v.word(), VerbInfo.PRESENT_FORMS, v.present!!.size)
            assertEquals(v.word(), VerbInfo.PAST_FORMS, v.past!!.size)
            assertEquals(v.word(), VerbInfo.IMPERATIVE_FORMS, v.imperative!!.size)
            assertGlosses(v.word(), v.translation_ru(), v.translation_en(), v.translation_uk(), v.translation_vi())
        }
        assertArrayEquals(arrayOf("budu", "budeš", "bude", "budeme", "budete", "budou"), verbs.first { it.word == "být" }.future)
        assertNull(verbs.first { it.word == "dělat" }.future)
    }

    /** Rows stored by older versions have no uk/vi keys and must still load. */
    @Test fun rowsWithoutNewerFieldsStillParse() {
        val w = DictionaryJson.wordInfo(
            """{"wordId":576,"word":"schůdky","gender":"rod: m. neživ.","declensionType":"pomnožné","translation_ru":"ле́сенка","translation_en":"steps","cases":[["schůdky","","","","","",""],["schůdky","schůdků","schůdkům","schůdky","schůdky","schůdcích, schůdkách","schůdky"]]}""",
        )
        assertEquals(576L, w.wordId)
        assertEquals("schůdků", w.cases(1, 1))
        assertEquals("", w.translation_uk())
        assertNull(w.translation_vi)
    }
}
