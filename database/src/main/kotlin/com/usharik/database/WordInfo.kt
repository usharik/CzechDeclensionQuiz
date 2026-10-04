package com.usharik.database


/**
 * Immutable dictionary entry shared by the data layer and both quiz modes.
 *
 * The component-style accessors preserve Java-call compatibility while the
 * rest of the app is migrated incrementally to Kotlin properties.
 */
data class WordInfo(
    val wordId: Long?,
    val word: String?,
    val cases: Array<Array<String>>?,
    val translation_ru: String?,
    val translation_en: String?,
    val gender: String?,
    val declensionType: String?,
    // Last and defaulted so positional constructor calls stay valid; Gson leaves them null in old rows.
    val translation_uk: String? = null,
    val translation_vi: String? = null,
) {
    fun wordId() = wordId
    fun word() = word.orEmpty()
    fun cases() = cases
    fun translation_ru() = translation_ru.orEmpty()
    fun translation_en() = translation_en.orEmpty()
    fun translation_uk() = translation_uk.orEmpty()
    fun translation_vi() = translation_vi.orEmpty()
    fun gender() = gender.orEmpty()
    fun declensionType() = declensionType.orEmpty()
    fun cases(number: Int, grammaticalCase: Int) = cases!![number][grammaticalCase]
}
