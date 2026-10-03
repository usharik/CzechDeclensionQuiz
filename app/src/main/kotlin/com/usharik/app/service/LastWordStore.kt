package com.usharik.app.service

interface LastWordStore {
    fun saveLastWord(modeKey: String, word: String)
    fun getLastWord(modeKey: String): String?
    companion object {
        const val MODE_FULL_DECLENSION = "full_declension"
        const val MODE_SINGLE_CASE = "single_case"
        const val MODE_REVIEW = "review"
        /** Per-part-of-speech mode keys; nouns keep the legacy keys so an upgrade resumes the saved word. */
        fun modeFullTable(partOfSpeech: com.usharik.app.PartOfSpeech): String =
            if (partOfSpeech == com.usharik.app.PartOfSpeech.NOUN) MODE_FULL_DECLENSION else "$MODE_FULL_DECLENSION:${partOfSpeech.name.lowercase()}"
        fun modeSingleForm(partOfSpeech: com.usharik.app.PartOfSpeech): String =
            if (partOfSpeech == com.usharik.app.PartOfSpeech.NOUN) MODE_SINGLE_CASE else "$MODE_SINGLE_CASE:${partOfSpeech.name.lowercase()}"
        val NO_OP = object : LastWordStore { override fun saveLastWord(modeKey: String, word: String) = Unit; override fun getLastWord(modeKey: String): String? = null }
    }
}
