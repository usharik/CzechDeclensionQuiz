package com.usharik.database

/**
 * Immutable verb entry (one line of `verbs.jsonl`).
 *
 * - [present]: já, ty, on/ona/ono, my, vy, oni/ony/ona (6 forms).
 * - [past]: the l-participle by gender and number: on, ona, ono, oni, ony (5 forms; the rarely
 *   used neuter plural is left out so a verb fills a 14-cell table like a noun does).
 * - [imperative]: ty, my, vy (3 forms; empty for modal verbs such as *muset* and *moct*).
 * - [future]: only present for *být* (budu … budou); other imperfective verbs form the future
 *   with *budu* + infinitive and perfective verbs use their present forms.
 *
 * Alternative forms are comma-separated (`mohu, můžu`), like in the noun corpus.
 */
data class VerbInfo(
    val wordId: Long?,
    val word: String?,
    /** `nedokonavé` (imperfective), `dokonavé` (perfective) or `obouvidové`. */
    val aspect: String?,
    /** The aspect partner (udělat for dělat), or empty when none is recorded. */
    val pair: String?,
    /** Conjugation group named after its model 3rd person singular: dělá, prosí, kupuje, tiskne, nese, nepravidelné. */
    val verbClass: String?,
    val translation_ru: String?,
    val translation_en: String?,
    val present: Array<String>?,
    val past: Array<String>?,
    val imperative: Array<String>?,
    val future: Array<String>?,
    val translation_uk: String? = null,
    val translation_vi: String? = null,
) {
    fun word() = word.orEmpty()
    fun aspect() = aspect.orEmpty()
    fun pair() = pair.orEmpty()
    fun verbClass() = verbClass.orEmpty()
    fun translation_ru() = translation_ru.orEmpty()
    fun translation_en() = translation_en.orEmpty()
    fun translation_uk() = translation_uk.orEmpty()
    fun translation_vi() = translation_vi.orEmpty()
    fun present(index: Int): String = present?.getOrNull(index).orEmpty()
    fun past(index: Int): String = past?.getOrNull(index).orEmpty()
    fun imperative(index: Int): String = imperative?.getOrNull(index).orEmpty()
    fun isReflexive(): Boolean = word().endsWith(" se") || word().endsWith(" si")

    /** Every distinct form (present, past, imperative), handy for building distractors. */
    fun allForms(): List<String> =
        (present.orEmpty().asList() + past.orEmpty().asList() + imperative.orEmpty().asList()).filter { it.isNotEmpty() }.distinct()

    companion object {
        const val PRESENT_FORMS = 6
        const val PAST_FORMS = 5
        const val IMPERATIVE_FORMS = 3
        @JvmField val CLASSES = listOf("dělá", "prosí", "kupuje", "tiskne", "nese", "nepravidelné")
        const val ASPECT_IMPERFECTIVE = "nedokonavé"
        const val ASPECT_PERFECTIVE = "dokonavé"
    }
}
