package com.usharik.app.ui.state

import com.usharik.app.CzechCase
import com.usharik.database.AdjectiveInfo
import com.usharik.database.VerbInfo
import com.usharik.database.WordInfo

/** Builders turning dictionary entries into the generic [FormTable] grid both quizzes and the handbook render. */
object FormTables {
    const val CASES = 7

    /** The classic 7 × 2 declension table of a noun. */
    fun noun(info: WordInfo): FormTable = noun(Lexeme.Noun(info))

    private fun noun(lexeme: Lexeme.Noun): FormTable {
        val rows = (0 until CASES).map { case ->
            FormRow(
                index = case,
                caseIndex = case,
                cells = (0..1).map { number -> FormCell(row = case, column = number, target = lexeme.info.cases(number, case)) },
            )
        }
        return FormTable(lexeme, ColumnKind.NUMBER, listOf(FormSection(SectionTitle.NONE, rows)))
    }

    /**
     * The adjective's 7 × 2 table for the gender of [noun]; each cell shows the noun's own form as a
     * suffix hint (*___ muže*), so the player places *mladého* and reads the whole phrase. Cells
     * whose noun form is missing (plural-only nouns) are still asked: agreement does not depend on it.
     */
    fun adjective(info: AdjectiveInfo, noun: WordInfo, gender: Int): FormTable = adjective(Lexeme.Adjective(info, noun, gender))

    private fun adjective(lexeme: Lexeme.Adjective): FormTable {
        val rows = (0 until CASES).map { case ->
            FormRow(
                index = case,
                caseIndex = case,
                cells = (0..1).map { number ->
                    val nounForm = lexeme.noun.cases(number, case).substringBefore(",").trim()
                    FormCell(
                        row = case,
                        column = number,
                        target = lexeme.info.form(number, lexeme.gender, case),
                        suffix = nounForm,
                        question = nounForm,
                    )
                },
            )
        }
        return FormTable(lexeme, ColumnKind.NUMBER, listOf(FormSection(SectionTitle.NONE, rows)))
    }

    /** The adjective's own 7 × 2 table for one gender without a noun (handbook). */
    fun adjectiveParadigm(info: AdjectiveInfo, gender: Int): FormTable {
        val blank = WordInfo(null, "", Array(2) { Array(CASES) { "" } }, "", "", AdjectiveInfo.GENDERS[gender], "")
        return adjective(Lexeme.Adjective(info, blank, gender))
    }

    /** Present-tense persons: singular column, then plural column. Short form (cell prefix) / full form (question label). */
    val PRESENT_PERSONS = listOf(listOf("já" to "já", "ty" to "ty", "on" to "on, ona, ono"), listOf("my" to "my", "vy" to "vy", "oni" to "oni, ony, ona"))
    /** Past participle subjects: (on, ona, ono) in the singular column, (oni, ony) in the plural column. */
    val PAST_PERSONS = listOf(listOf("on" to "on", "ona" to "ona", "ono" to "ono"), listOf("oni" to "oni", "ony" to "ony", "" to ""))
    /** Imperative persons: ty / vy in the first row, my alone in the second. */
    val IMPERATIVE_PERSONS = listOf(listOf("ty!" to "ty", "" to ""), listOf("vy!" to "vy", "my!" to "my"))

    /**
     * A verb as an 8 × 2 grid in three sections: present tense (já … oni), the past participle
     * by gender (on, ona, ono / oni, ony) and the imperative (ty, vy / my). That is 14 forms, the
     * same count as a noun table, so scoring and the per-word time budget stay comparable.
     */
    fun verb(info: VerbInfo): FormTable {
        val lexeme = Lexeme.Verb(info)
        var rowIndex = 0
        fun section(title: SectionTitle, persons: List<List<Pair<String, String>>>, forms: (column: Int, position: Int) -> String): FormSection {
            val rows = persons[0].indices.map { position ->
                val row = rowIndex++
                FormRow(
                    index = row,
                    label = persons[0][position].second,
                    cells = (0..1).map { column ->
                        val (short, full) = persons[column][position]
                        val target = if (full.isEmpty()) "" else forms(column, position)
                        FormCell(row = row, column = column, target = target, prefix = short, question = full)
                    },
                )
            }
            return FormSection(title, rows)
        }
        val present = section(SectionTitle.PRESENT, PRESENT_PERSONS) { column, position -> info.present(column * 3 + position) }
        val past = section(SectionTitle.PAST, PAST_PERSONS) { column, position -> info.past(column * 3 + position) }
        // imperative order in the corpus: ty, my, vy
        val imperative = section(SectionTitle.IMPERATIVE, IMPERATIVE_PERSONS) { column, position ->
            when (column to position) { 0 to 0 -> info.imperative(0); 1 to 0 -> info.imperative(2); 1 to 1 -> info.imperative(1); else -> "" }
        }
        return FormTable(lexeme, ColumnKind.VERB, listOf(present, past, imperative))
    }

    /**
     * A whole adjective + noun phrase per cell (*mladého muže*), introduced by the case helper word
     * (*bez*, *ke*, *vidím*, *o*, *s*). Nouns with alternative forms contribute their first one so
     * the chips stay short. Cells whose noun form is missing (plural-only nouns) do not exist.
     */
    fun phrase(adjective: AdjectiveInfo, noun: WordInfo, gender: Int): FormTable = phrase(Lexeme.Phrase(adjective, noun, gender))

    private fun phrase(lexeme: Lexeme.Phrase): FormTable {
        val rows = (0 until CASES).map { case ->
            FormRow(
                index = case,
                caseIndex = case,
                cells = (0..1).map { number ->
                    val nounForm = lexeme.noun.cases(number, case).substringBefore(",").trim()
                    val adjForm = lexeme.adjective.form(number, lexeme.gender, case).substringBefore(",").trim()
                    val helper = CzechCase.fromIndex(case).helperWord
                    FormCell(
                        row = case,
                        column = number,
                        target = if (nounForm.isEmpty() || adjForm.isEmpty()) "" else "$adjForm $nounForm",
                        prefix = helper,
                        question = helper,
                    )
                },
            )
        }
        return FormTable(lexeme, ColumnKind.NUMBER, listOf(FormSection(SectionTitle.NONE, rows)))
    }

    fun of(lexeme: Lexeme): FormTable = when (lexeme) {
        is Lexeme.Noun -> noun(lexeme)
        is Lexeme.Adjective -> adjective(lexeme)
        is Lexeme.Verb -> verb(lexeme.info)
        is Lexeme.Phrase -> phrase(lexeme)
    }
}
