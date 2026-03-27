package com.usharik.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.usharik.app.fragment.SettingsFragment;

public final class UiLanguageManager {
    public static final String UI_LANGUAGE_KEY = "uiLanguage";

    private UiLanguageManager() {
    }

    public static boolean hasSavedLanguage(Context context) {
        return getPreferences(context).contains(UI_LANGUAGE_KEY);
    }

    public static UiLanguage getSavedLanguage(Context context) {
        return UiLanguage.fromPreference(getPreferences(context).getString(UI_LANGUAGE_KEY, null));
    }

    public static UiLanguage getSelectedLanguage(Context context) {
        return hasSavedLanguage(context) ? getSavedLanguage(context) : UiLanguage.SYSTEM;
    }

    public static String getSelectedLanguageLabel(Context context) {
        return getSelectedLanguage(context).displayName(context);
    }

    public static UiLanguage[] getAvailableLanguages() {
        return UiLanguage.values();
    }

    public static CharSequence[] getLanguageLabels(Context context) {
        UiLanguage[] options = getAvailableLanguages();
        CharSequence[] labels = new CharSequence[options.length];
        for (int i = 0; i < options.length; i++) {
            labels[i] = options[i].displayName(context);
        }
        return labels;
    }

    public static int indexOf(UiLanguage target) {
        UiLanguage[] options = getAvailableLanguages();
        for (int i = 0; i < options.length; i++) {
            if (options[i] == target) {
                return i;
            }
        }
        return 0;
    }

    public static boolean applySavedLanguage(Context context) {
        if (!hasSavedLanguage(context)) {
            return false;
        }
        return applyLanguage(getSavedLanguage(context));
    }

    public static boolean saveAndApplyLanguage(Context context, UiLanguage uiLanguage) {
        getPreferences(context).edit().putString(UI_LANGUAGE_KEY, uiLanguage.preferenceValue()).apply();
        return applyLanguage(uiLanguage);
    }

    public static boolean applyLanguage(UiLanguage uiLanguage) {
        LocaleListCompat newLocales = toLocaleList(uiLanguage);
        LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
        if (currentLocales.equals(newLocales)) {
            return false;
        }
        AppCompatDelegate.setApplicationLocales(newLocales);
        return true;
    }

    private static LocaleListCompat toLocaleList(UiLanguage uiLanguage) {
        return uiLanguage == UiLanguage.SYSTEM
                ? LocaleListCompat.getEmptyLocaleList()
                : LocaleListCompat.forLanguageTags(uiLanguage.languageTags());
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(SettingsFragment.SHARED_PREFERENCES, Context.MODE_PRIVATE);
    }
}