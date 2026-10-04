package com.usharik.database

/**
 * Immutable adjective entry (one line of `adjectives.jsonl`).
 *
 * [cases] is indexed `[number][gender][case]`: number 0 = singular, 1 = plural; gender in the
 * order of [GENDERS] (masculine animate, masculine inanimate, feminine, neuter); case 0..6 in
 * the usual Czech order (nominative … instrumental). Alternative forms are comma-separated,
 * exactly like the noun corpus.
 */
data class AdjectiveInfo(
    val wordId: Long?,
    val word: String?,
    /** `tvrdé` (mladý), `měkké` (jarní) or `přivlastňovací` (otcův, matčin). */
    val kind: String?,
    val translation_ru: String?,
    val translation_en: String?,
    val comparative: String?,
    val superlative: String?,
    val cases: Array<Array<Array<String>>>?,
    val translation_uk: String? = null,
    val translation_vi: String? = null,
) {
    fun word() = word.orEmpty()
    fun kind() = kind.orEmpty()
    fun translation_ru() = translation_ru.orEmpty()
    fun translation_en() = translation_en.orEmpty()
    fun translation_uk() = translation_uk.orEmpty()
    fun translation_vi() = translation_vi.orEmpty()
    fun comparative() = comparative.orEmpty()
    fun superlative() = superlative.orEmpty()

    /** The form for [number] (0 singular / 1 plural), [gender] (index into [GENDERS]) and [grammaticalCase] 0..6. */
    fun form(number: Int, gender: Int, grammaticalCase: Int): String =
        cases?.getOrNull(number)?.getOrNull(gender)?.getOrNull(grammaticalCase).orEmpty()

    /** Every distinct form of the adjective, in table order; handy for building distractors. */
    fun allForms(): List<String> = cases?.flatMap { number -> number.flatMap { gender -> gender.asList() } }?.distinct().orEmpty()

    companion object {
        const val GENDER_MASCULINE_ANIMATE = 0
        const val GENDER_MASCULINE_INANIMATE = 1
        const val GENDER_FEMININE = 2
        const val GENDER_NEUTER = 3
        /** Gender labels in the corpus' own notation, matching the noun corpus' `gender` field. */
        @JvmField val GENDERS = listOf("rod: m. živ.", "rod: m. neživ.", "rod: ž.", "rod: s.")

        /** Index into [GENDERS] for a noun-corpus gender string, or null for an unknown value. */
        fun genderIndex(nounGender: String): Int? = GENDERS.indexOf(nounGender).takeIf { it >= 0 }
    }
}
