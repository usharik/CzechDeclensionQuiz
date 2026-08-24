package com.usharik.app.service;

/**
 * Persists the last word shown in an auto-generating quiz mode so it can be
 * restored after the app is closed (including forced/unexpected termination).
 * The word is stored per mode and reloaded on the next launch instead of
 * generating a new random word.
 */
public interface LastWordStore {

    String MODE_FULL_DECLENSION = "full_declension";
    String MODE_SINGLE_CASE = "single_case";

    void saveLastWord(String modeKey, String word);

    /** @return the saved word for the mode, or {@code null} if none is stored. */
    String getLastWord(String modeKey);

    void clear(String modeKey);

    /** No-op implementation for unit tests and non-persisting contexts. */
    LastWordStore NO_OP = new LastWordStore() {
        @Override
        public void saveLastWord(String modeKey, String word) {
        }

        @Override
        public String getLastWord(String modeKey) {
            return null;
        }

        @Override
        public void clear(String modeKey) {
        }
    };
}
