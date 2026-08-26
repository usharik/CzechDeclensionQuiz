package com.usharik.app.service

interface LastWordStore {
    fun saveLastWord(modeKey: String, word: String)
    fun getLastWord(modeKey: String): String?
    companion object {
        const val MODE_FULL_DECLENSION = "full_declension"
        const val MODE_SINGLE_CASE = "single_case"
        val NO_OP = object : LastWordStore { override fun saveLastWord(modeKey: String, word: String) = Unit; override fun getLastWord(modeKey: String): String? = null }
    }
}
