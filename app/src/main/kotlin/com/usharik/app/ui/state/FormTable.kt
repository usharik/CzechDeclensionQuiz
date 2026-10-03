package com.usharik.app.ui.state

import com.usharik.app.PartOfSpeech
import com.usharik.database.AdjectiveInfo
import com.usharik.database.VerbInfo
import com.usharik.database.WordInfo

/**
 * What one quiz round is about: a noun, an adjective paired with a noun it has to agree with, or
 * a verb. Every lexeme has a persisted [key] (see [PartOfSpeech.key]) and a headword for display.
 */
sealed interface Lexeme {
    val partOfSpeech: PartOfSpeech
    val headword: String
    val translationRu: String
    val translationEn: String
    val key: String get() = partOfSpeech.key(headword)

    data class Noun(val info: WordInfo) : Lexeme {
        override val partOfSpeech get() = PartOfSpeech.NOUN
        override val headword get() = info.word()
        override val translationRu get() = info.translation_ru()
        override val translationEn get() = info.translation_en()
    }

    /**
     * An adjective declined together with [noun] (whose forms are shown as fixed hints), so the
     * player practises agreement: *mladý muž → mladého muže …*. [gender] indexes
     * [AdjectiveInfo.GENDERS] and must match the noun's gender.
     */
    data class Adjective(val info: AdjectiveInfo, val noun: WordInfo, val gender: Int) : Lexeme {
        override val partOfSpeech get() = PartOfSpeech.ADJECTIVE
        override val headword get() = info.word()
        override val translationRu get() = info.translation_ru()
        override val translationEn get() = info.translation_en()
    }

    /**
     * An adjective + noun phrase declined as a whole (*mladý muž, mladého muže, …*), with the case
     * helper words (*bez, ke, vidím, o, s*) as cell prefixes so each cell reads like a mini sentence.
     */
    data class Phrase(val adjective: AdjectiveInfo, val noun: WordInfo, val gender: Int) : Lexeme {
        override val partOfSpeech get() = PartOfSpeech.PHRASE
        /** Shown in agreement (*milé město*, not *milý město*); the persisted [key] keeps both lemmas. */
        override val headword get() = "${adjective.form(0, gender, 0).substringBefore(",").trim().ifEmpty { adjective.word() }} ${noun.word()}"
        override val key get() = partOfSpeech.key("${adjective.word()} ${noun.word()}")
        override val translationRu get() = "${adjective.translation_ru().substringBefore(",").substringBefore(";")} + ${noun.translation_ru().substringBefore(",").substringBefore(";")}"
        override val translationEn get() = "${adjective.translation_en().substringBefore(",").substringBefore(";")} + ${noun.translation_en().substringBefore(",").substringBefore(";")}"
    }

    data class Verb(val info: VerbInfo) : Lexeme {
        override val partOfSpeech get() = PartOfSpeech.VERB
        override val headword get() = info.word()
        override val translationRu get() = info.translation_ru()
        override val translationEn get() = info.translation_en()
    }
}

/**
 * One cell of a [FormTable]. [target] is the form the player must place; an empty target means
 * the cell does not exist for this lexeme (plural-only nouns, verbs without an imperative) and is
 * rendered as a blank. [prefix]/[suffix] are fixed text shown around the form (*já* ___, ___ *muže*).
 * [column] 0/1 and [row] are the cell's position in the table; [question] is the label used by the
 * one-form-at-a-time quiz (for example "já" or "muže").
 */
data class FormCell(
    val row: Int,
    val column: Int,
    val target: String,
    val prefix: String = "",
    val suffix: String = "",
    val question: String = "",
) {
    /** Drag-and-drop key, `"<column>_<row>"`: identical to the legacy noun cell keys. */
    val key: String get() = "${column}_$row"
    val exists: Boolean get() = target.isNotEmpty()
}

/**
 * One row of two cells. [caseIndex] is set for case rows (nouns, adjectives) so the row renders the
 * standard case header; [label] is used instead for verb rows (person) and is empty for case rows.
 */
data class FormRow(val index: Int, val cells: List<FormCell>, val caseIndex: Int? = null, val label: String = "")

/** A titled group of rows: the single section of a declension table, or present / past / imperative for a verb. */
data class FormSection(val title: SectionTitle, val rows: List<FormRow>)

enum class SectionTitle { NONE, PRESENT, PAST, IMPERATIVE }

/** Which two columns the table has: singular/plural (nouns, adjectives) or the verb layout (singular/plural persons). */
enum class ColumnKind { NUMBER, VERB }

/**
 * The full grid the quizzes operate on: rows × 2 columns, split into sections. Built by
 * [FormTables] for every [Lexeme]. The one-form quiz asks the cells in [questionOrder]; the full
 * table quiz pools every existing target.
 */
data class FormTable(val lexeme: Lexeme, val columnKind: ColumnKind, val sections: List<FormSection>) {
    val rows: List<FormRow> = sections.flatMap { it.rows }
    val rowCount: Int get() = rows.size
    val cells: List<FormCell> = rows.flatMap { it.cells }

    fun cell(column: Int, row: Int): FormCell = rows[row].cells[column]
    fun cellIndex(column: Int, row: Int): Int = column * rowCount + row

    /** The target of a cell by its grid index (see [cellIndex]); empty for non-existent cells. */
    fun target(column: Int, row: Int): String = cell(column, row).target

    /**
     * Column-major within each section (all singular forms, then all plural; já ty on then my vy
     * oni), so the one-form quiz walks a noun exactly like before and a verb person by person.
     */
    val questionOrder: List<FormCell> = sections.flatMap { section ->
        (0..1).flatMap { column -> section.rows.map { it.cells[column] } }
    }.filter { it.exists }

    /** Every existing target, in grid order; the full-table quiz's word pool before shuffling. */
    val targets: List<String> get() = cells.filter { it.exists }.map { it.target }

    val partOfSpeech: PartOfSpeech get() = lexeme.partOfSpeech
}
