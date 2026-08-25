package com.usharik.app

import android.content.Context

enum class UiLanguage(private val preference: String, private val label: String) {
    SYSTEM("system", ""), ENGLISH("en", "English"), RUSSIAN("ru-RU", "Русский"),
    CZECH("cs", "Čeština"), BELARUSIAN("be", "Беларуская"), UKRAINIAN("uk-UA", "Українська"),
    GERMAN("de", "Deutsch"), VIETNAMESE("vi", "Tiếng Việt");

    fun preferenceValue() = preference
    fun languageTags() = if (this == SYSTEM) "" else preference
    fun displayName(context: Context) = if (this == SYSTEM) context.getString(R.string.use_device_language) else label
    companion object {
        @JvmStatic fun fromPreference(value: String?) = entries.firstOrNull { it.preference.equals(value, true) } ?: SYSTEM
    }
}
