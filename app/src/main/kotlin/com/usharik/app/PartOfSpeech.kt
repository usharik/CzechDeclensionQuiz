package com.usharik.app

import androidx.annotation.StringRes

/**
 * The three word classes the quizzes can drill. Nouns are the original content; adjectives and
 * verbs were added later and reuse the same quiz modes over a generic [com.usharik.app.ui.state.FormTable].
 *
 * [keyPrefix] namespaces persisted word keys (the mistakes list, "last word" per mode) so that
 * `mladý` the adjective never collides with a noun of the same spelling. Nouns keep an empty prefix
 * for backward compatibility with keys saved by earlier versions.
 */
enum class PartOfSpeech(val keyPrefix: String, @StringRes val titleRes: Int, @StringRes val shortTitleRes: Int) {
    NOUN("", R.string.pos_nouns, R.string.pos_nouns_short),
    ADJECTIVE("adj:", R.string.pos_adjectives, R.string.pos_adjectives_short),
    VERB("verb:", R.string.pos_verbs, R.string.pos_verbs_short),
    /** Adjective + noun phrases declined together (*mladý muž → bez mladého muže*); drawn from the two dictionaries above. */
    PHRASE("phrase:", R.string.pos_phrases, R.string.pos_phrases_short);

    /** Whether the handbook has a tab for this class (phrases reuse the adjective and noun tabs). */
    val hasHandbook: Boolean get() = this != PHRASE

    /** The persisted key of a lexeme of this class. */
    fun key(word: String): String = keyPrefix + word

    companion object {
        /** The part of speech a persisted key belongs to. Unprefixed keys are nouns. */
        fun ofKey(key: String): PartOfSpeech = entries.filter { it.keyPrefix.isNotEmpty() }.firstOrNull { key.startsWith(it.keyPrefix) } ?: NOUN

        /** The bare headword of a persisted key. */
        fun wordOfKey(key: String): String = key.removePrefix(ofKey(key).keyPrefix)

        fun fromName(name: String?): PartOfSpeech = entries.firstOrNull { it.name == name } ?: NOUN
    }
}
