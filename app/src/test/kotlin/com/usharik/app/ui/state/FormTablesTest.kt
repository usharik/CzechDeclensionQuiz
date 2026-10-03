package com.usharik.app.ui.state

import com.usharik.app.PartOfSpeech
import com.usharik.database.AdjectiveInfo
import com.usharik.database.VerbInfo
import com.usharik.database.WordInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormTablesTest {
    private val muz = WordInfo(
        1, "muž",
        arrayOf(
            arrayOf("muž", "muže", "muži, mužovi", "muže", "muži", "muži, mužovi", "mužem"),
            arrayOf("muži", "mužů", "mužům", "muže", "muži", "mužích", "muži"),
        ),
        "мужчина", "man", "rod: m. živ.", "muž",
    )
    private val mlady = AdjectiveInfo(
        1, "mladý", "tvrdé", "молодой", "young", "mladší", "nejmladší",
        arrayOf(
            arrayOf(
                arrayOf("mladý", "mladého", "mladému", "mladého", "mladý", "mladém", "mladým"),
                arrayOf("mladý", "mladého", "mladému", "mladý", "mladý", "mladém", "mladým"),
                arrayOf("mladá", "mladé", "mladé", "mladou", "mladá", "mladé", "mladou"),
                arrayOf("mladé", "mladého", "mladému", "mladé", "mladé", "mladém", "mladým"),
            ),
            arrayOf(
                arrayOf("mladí", "mladých", "mladým", "mladé", "mladí", "mladých", "mladými"),
                arrayOf("mladé", "mladých", "mladým", "mladé", "mladé", "mladých", "mladými"),
                arrayOf("mladé", "mladých", "mladým", "mladé", "mladé", "mladých", "mladými"),
                arrayOf("mladá", "mladých", "mladým", "mladá", "mladá", "mladých", "mladými"),
            ),
        ),
    )
    private val delat = VerbInfo(
        1, "dělat", "nedokonavé", "udělat", "dělá", "делать", "to do",
        arrayOf("dělám", "děláš", "dělá", "děláme", "děláte", "dělají"),
        arrayOf("dělal", "dělala", "dělalo", "dělali", "dělaly"),
        arrayOf("dělej", "dělejme", "dělejte"),
        null,
    )
    private val muset = delat.copy(word = "muset", imperative = arrayOf("", "", ""))

    @Test fun nounTableKeepsLegacyLayout() {
        val table = FormTables.noun(muz)
        assertEquals(7, table.rowCount)
        assertEquals(14, table.cells.size)
        assertEquals("0_2", table.cell(0, 2).key)
        assertEquals("muži, mužovi", table.target(0, 2))
        assertEquals("mužů", table.target(1, 1))
        // singular cases first, then plural - the order the one-case quiz always used
        assertEquals(listOf("muž", "muže", "muži, mužovi", "muže", "muži", "muži, mužovi", "mužem"), table.questionOrder.take(7).map { it.target })
        assertEquals(PartOfSpeech.NOUN, table.partOfSpeech)
        assertEquals("muž", table.lexeme.key)
    }

    @Test fun adjectiveTableUsesTheNounGenderAndShowsTheNounAsSuffix() {
        val table = FormTables.adjective(mlady, muz, AdjectiveInfo.GENDER_MASCULINE_ANIMATE)
        assertEquals(7, table.rowCount)
        assertEquals("mladého", table.target(0, 3)) // accusative sg. animate = genitive
        assertEquals("muže", table.cell(0, 3).suffix)
        assertEquals("mladí", table.target(1, 0))
        assertEquals("adj:mladý", table.lexeme.key)
        // alternative noun forms are trimmed to the first one in the hint
        assertEquals("muži", table.cell(0, 2).suffix)
    }

    @Test fun adjectiveParadigmHasNoNounHints() {
        val table = FormTables.adjectiveParadigm(mlady, AdjectiveInfo.GENDER_FEMININE)
        assertEquals("mladou", table.target(0, 3))
        assertTrue(table.cells.all { it.suffix.isEmpty() })
    }

    @Test fun verbTableHasThreeSectionsAndFourteenForms() {
        val table = FormTables.verb(delat)
        assertEquals(8, table.rowCount)
        assertEquals(listOf(SectionTitle.PRESENT, SectionTitle.PAST, SectionTitle.IMPERATIVE), table.sections.map { it.title })
        assertEquals(14, table.targets.size)
        // present: já/my, ty/vy, on/oni
        assertEquals("dělám", table.target(0, 0)); assertEquals("my", table.cell(1, 0).prefix); assertEquals("děláme", table.target(1, 0))
        assertEquals("dělají", table.target(1, 2))
        // past: on/oni, ona/ony, ono/-
        assertEquals("dělal", table.target(0, 3)); assertEquals("dělali", table.target(1, 3))
        assertEquals("dělaly", table.target(1, 4)); assertFalse(table.cell(1, 5).exists)
        // imperative: ty/vy, -/my
        assertEquals("dělej", table.target(0, 6)); assertEquals("dělejte", table.target(1, 6))
        assertFalse(table.cell(0, 7).exists); assertEquals("dělejme", table.target(1, 7))
        assertEquals("verb:dělat", table.lexeme.key)
        // question order walks persons column by column inside each section
        assertEquals(listOf("dělám", "děláš", "dělá", "děláme", "děláte", "dělají", "dělal", "dělala", "dělalo", "dělali", "dělaly", "dělej", "dělejte", "dělejme"), table.questionOrder.map { it.target })
        assertEquals("oni, ony, ona", table.cell(1, 2).question)
    }

    @Test fun verbWithoutImperativeLeavesThoseCellsBlank() {
        val table = FormTables.verb(muset)
        assertEquals(11, table.targets.size)
        assertTrue(table.sections.last().rows.all { row -> row.cells.none { it.exists } })
    }

    @Test fun partOfSpeechKeysRoundTrip() {
        assertEquals(PartOfSpeech.ADJECTIVE, PartOfSpeech.ofKey("adj:mladý"))
        assertEquals("mladý", PartOfSpeech.wordOfKey("adj:mladý"))
        assertEquals(PartOfSpeech.VERB, PartOfSpeech.ofKey("verb:učit se"))
        assertEquals("učit se", PartOfSpeech.wordOfKey("verb:učit se"))
        assertEquals(PartOfSpeech.NOUN, PartOfSpeech.ofKey("muž"))
        assertEquals("muž", PartOfSpeech.wordOfKey("muž"))
    }

    @Test fun phraseTableCombinesAdjectiveAndNounWithCaseHelpers() {
        val table = FormTables.phrase(mlady, muz, AdjectiveInfo.GENDER_MASCULINE_ANIMATE)
        assertEquals("mladý muž", table.lexeme.headword)
        assertEquals("phrase:mladý muž", table.lexeme.key)
        assertEquals("mladého muže", table.target(0, 1))
        assertEquals("bez", table.cell(0, 1).prefix)
        assertEquals("", table.cell(0, 0).prefix)
        // alternative noun forms are cut to the first one
        assertEquals("mladému muži", table.target(0, 2))
        assertEquals("mladými muži", table.target(1, 6))
        assertEquals(14, table.targets.size)
    }
}
