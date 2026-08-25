package com.usharik.app

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object UiLanguageManager {
    const val UI_LANGUAGE_KEY = "uiLanguage"
    fun hasSavedLanguage(context: Context) = preferences(context).contains(UI_LANGUAGE_KEY)
    fun getSavedLanguage(context: Context) = UiLanguage.fromPreference(preferences(context).getString(UI_LANGUAGE_KEY, null))
    fun getSelectedLanguage(context: Context) = if (hasSavedLanguage(context)) getSavedLanguage(context) else UiLanguage.SYSTEM
    fun getSelectedLanguageLabel(context: Context) = getSelectedLanguage(context).displayName(context)
    fun getAvailableLanguages() = UiLanguage.entries.toTypedArray()
    fun getLanguageLabels(context: Context): Array<CharSequence> = getAvailableLanguages().map { it.displayName(context) }.toTypedArray()
    fun indexOf(target: UiLanguage) = getAvailableLanguages().indexOf(target).coerceAtLeast(0)
    fun applySavedLanguage(context: Context) = hasSavedLanguage(context) && applyLanguage(getSavedLanguage(context))
    fun saveAndApplyLanguage(context: Context, language: UiLanguage): Boolean { preferences(context).edit().putString(UI_LANGUAGE_KEY, language.preferenceValue()).apply(); return applyLanguage(language) }
    fun applyLanguage(language: UiLanguage): Boolean {
        val locales = if (language == UiLanguage.SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(language.languageTags())
        if (AppCompatDelegate.getApplicationLocales() == locales) return false
        AppCompatDelegate.setApplicationLocales(locales); return true
    }
    private fun preferences(context: Context) = context.getSharedPreferences(App.PREFS_NAME, Context.MODE_PRIVATE)
}
