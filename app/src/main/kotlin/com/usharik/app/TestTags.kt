package com.usharik.app

/** Stable test tag constants used by both production composables and androidTest code. */
object TestTags {
    // App bar
    const val APP_BAR_TITLE = "app_bar_title"
    const val NAV_HOME_BTN = "nav_home"
    const val NAV_NEXT_BTN = "nav_next"
    // Hub screen
    const val HUB_SCREEN = "hub_screen"
    const val BTN_FULL = "btn_full"
    const val BTN_SINGLE = "btn_single"
    const val BTN_ERRORS = "btn_errors"
    const val BTN_HANDBOOK = "btn_handbook"
    const val BTN_SETTINGS = "btn_settings"
    const val BTN_ABOUT = "btn_about"
    // Single-case quiz
    const val SC_SCREEN = "sc_screen"
    const val SC_WORD = "sc_word"
    const val SC_CASE_NAME = "sc_case_name"
    const val SC_NUMBER_LABEL = "sc_number_label"
    const val SC_QUESTION = "sc_case_question"
    const val SC_ANSWER_PREFIX = "sc_answer_" // append 0..3
    const val SC_NEXT_CASE = "sc_next_case"
    const val SC_NEXT_WORD = "sc_next_word"
    // Full declension quiz
    const val FULL_WORD = "full_word"
    const val FULL_ERROR_COUNTER = "full_error_counter"
    const val FULL_TIMER = "full_timer"
    const val FULL_POOL_WORD_PREFIX = "full_pool_word_" // append the shuffled word-model index
    const val FULL_CELL_PREFIX = "full_cell_" // append "<number>_<case>", number: 0 singular / 1 plural
    const val FULL_COMPLETION_DIALOG = "full_completion_dialog"
    const val FULL_DIALOG_NEXT_WORD = "full_dialog_next_word"
    const val FULL_DIALOG_STAY_HERE = "full_dialog_stay_here"
    const val FULL_DIALOG_TRY_AGAIN = "full_dialog_try_again"
    const val FULL_QUIT_DIALOG = "full_quit_dialog"
    const val FULL_QUIT_EXERCISES = "full_quit_exercises"
    const val FULL_QUIT_LEAVE = "full_quit_leave"
    const val FULL_HANDBOOK_OVERLAY = "full_handbook_overlay"
    const val FULL_QUIZ_ROOT = "full_quiz_root" // swipe gesture surface; spans the whole screen
}