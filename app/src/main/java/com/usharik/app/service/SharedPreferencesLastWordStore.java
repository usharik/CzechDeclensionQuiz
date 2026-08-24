package com.usharik.app.service;

import android.app.Application;
import android.content.SharedPreferences;

import com.usharik.app.fragment.SettingsFragment;

import static android.content.Context.MODE_PRIVATE;

/**
 * {@link LastWordStore} backed by the app's shared {@link SharedPreferences}.
 * The word is saved at the moment it is generated (see the quiz view models) so
 * that a forced close, which may skip lifecycle callbacks, still leaves the
 * currently-shown word persisted.
 */
public class SharedPreferencesLastWordStore implements LastWordStore {

    private static final String KEY_PREFIX = "last_word_";

    private final SharedPreferences prefs;

    public SharedPreferencesLastWordStore(Application application) {
        this.prefs = application.getSharedPreferences(
                SettingsFragment.SHARED_PREFERENCES, MODE_PRIVATE);
    }

    @Override
    public void saveLastWord(String modeKey, String word) {
        prefs.edit().putString(KEY_PREFIX + modeKey, word).apply();
    }

    @Override
    public String getLastWord(String modeKey) {
        return prefs.getString(KEY_PREFIX + modeKey, null);
    }

    @Override
    public void clear(String modeKey) {
        prefs.edit().remove(KEY_PREFIX + modeKey).apply();
    }
}
