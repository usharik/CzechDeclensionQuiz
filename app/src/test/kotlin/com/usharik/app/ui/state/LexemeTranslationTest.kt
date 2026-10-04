package com.usharik.app.ui.state

import com.usharik.database.AdjectiveInfo
import com.usharik.database.WordInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class LexemeTranslationTest {
    private val cases = arrayOf(Array(7) { "" }, Array(7) { "" })
    private val muz = Lexeme.Noun(WordInfo(1, "muž", cases, "мужчи́на, муж", "man, husband", "rod: m. živ.", "muž", "чоловік", "người đàn ông"))
    private val legacy = Lexeme.Noun(WordInfo(2, "pes", cases, "пёс", "dog", "rod: m. živ.", "pán"))
    private val mlady = AdjectiveInfo(1, "mladý", "tvrdé", "молодой", "young", "", "", null, "молодий", "trẻ")

    @Test fun eachUiLanguageGetsItsGloss() {
        assertEquals("чоловік", muz.translationFor("ukr"))
        assertEquals("người đàn ông", muz.translationFor("vie"))
        assertEquals("мужчи́на, муж", muz.translationFor("rus"))
        assertEquals("мужчи́на, муж", muz.translationFor("bel"))
        assertEquals("man, husband", muz.translationFor("eng"))
        assertEquals("man, husband", muz.translationFor("ces"))
        assertEquals("man, husband", muz.translationFor("deu"))
    }

    @Test fun entriesWithoutUkrainianOrVietnameseFallBack() {
        assertEquals("пёс", legacy.translationFor("ukr"))
        assertEquals("dog", legacy.translationFor("vie"))
    }

    @Test fun phraseJoinsTheFirstSensesAndFallsBackWhenAGlossIsMissing() {
        val phrase = Lexeme.Phrase(mlady, muz.info, 0)
        assertEquals("молодий + чоловік", phrase.translationFor("ukr"))
        assertEquals("trẻ + người đàn ông", phrase.translationFor("vie"))
        assertEquals("young + man", phrase.translationFor("eng"))
        val oldNoun = Lexeme.Phrase(mlady, legacy.info, 0)
        assertEquals("молодой + пёс", oldNoun.translationFor("ukr"))
    }
}
