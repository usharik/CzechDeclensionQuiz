package com.usharik.app;

/**
 * Compose testTag values used by the smoke tests. The app exposes them as
 * resource-ids via {@code testTagsAsResourceId} (see TestTags in CzechQuizApp.kt).
 */
public final class UiConstants {

    static final String APP_PACKAGE = "com.usharik.app";

    // App bar
    static final String TAG_APP_BAR_TITLE = "app_bar_title";

    // Hub screen
    static final String TAG_HUB_SCREEN = "hub_screen";
    static final String TAG_BTN_FULL = "btn_full";
    static final String TAG_BTN_SINGLE = "btn_single";
    static final String TAG_BTN_ERRORS = "btn_errors";
    static final String TAG_BTN_HANDBOOK = "btn_handbook";
    static final String TAG_BTN_SETTINGS = "btn_settings";
    static final String TAG_BTN_ABOUT = "btn_about";

    // Single-case quiz
    static final String TAG_SC_WORD = "sc_word";
    static final String TAG_SC_QUESTION = "sc_case_question";
    static final String TAG_SC_ANSWER_0 = "sc_answer_0";
    static final String TAG_SC_NEXT_CASE = "sc_next_case";

    // Full declension quiz
    static final String TAG_FULL_WORD = "full_word";
    static final String TAG_FULL_POOL_WORD_PREFIX = "full_pool_word_";

    // Quit-quiz dialog (shared by both quizzes)
    static final String TAG_QUIT_DIALOG = "full_quit_dialog";
    static final String TAG_QUIT_LEAVE = "full_quit_leave";
}
