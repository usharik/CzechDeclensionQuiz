package com.usharik.app.service

import android.app.Application
import com.usharik.app.App

class SharedPreferencesLastWordStore(application: Application) : LastWordStore {
    private val preferences = application.getSharedPreferences(App.PREFS_NAME, 0)
    override fun saveLastWord(modeKey: String, word: String) { preferences.edit().putString("last_word_$modeKey", word).apply() }
    override fun getLastWord(modeKey: String): String? = preferences.getString("last_word_$modeKey", null)
}
