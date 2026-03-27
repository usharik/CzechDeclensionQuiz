package com.usharik.app;

import android.content.Context;

public enum UiLanguage {
    SYSTEM("system", ""),
    ENGLISH("en", "English"),
    RUSSIAN("ru-RU", "Русский"),
    CZECH("cs", "Čeština"),
    BELARUSIAN("be", "Беларуская"),
    UKRAINIAN("uk-UA", "Українська"),
    GERMAN("de", "Deutsch"),
    VIETNAMESE("vi", "Tiếng Việt");

    private final String preferenceValue;
    private final String displayName;

    UiLanguage(String preferenceValue, String displayName) {
        this.preferenceValue = preferenceValue;
        this.displayName = displayName;
    }

    public String preferenceValue() {
        return preferenceValue;
    }

    public String languageTags() {
        return SYSTEM == this ? "" : preferenceValue;
    }

    public String displayName(Context context) {
        return SYSTEM == this ? context.getString(R.string.use_device_language) : displayName;
    }

    public static UiLanguage fromPreference(String value) {
        if (value != null) {
            for (UiLanguage uiLanguage : values()) {
                if (uiLanguage.preferenceValue.equalsIgnoreCase(value)) {
                    return uiLanguage;
                }
            }
        }
        return SYSTEM;
    }
}