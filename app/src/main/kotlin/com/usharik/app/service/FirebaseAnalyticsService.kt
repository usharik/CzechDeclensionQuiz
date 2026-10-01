package com.usharik.app.service

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseAnalyticsService(private val analytics: FirebaseAnalytics) {
    fun setCollectionEnabled(enabled: Boolean) = analytics.setAnalyticsCollectionEnabled(enabled)
    fun logButtonClick(event: String, button: String) = logEvent(event, Bundle().apply { putString(KEY_BUTTON, button) })
    fun logNextWord(word: String) = logEvent("NEXT_WORD", Bundle().apply { putString("WORD", word) })
    fun logNextWordAction(action: String) = logEvent("NEXT_WORD_ACTION", Bundle().apply { putString("NEXT_WORD_ACTION", action) })
    fun logMistake(bundle: Bundle?) { if (bundle?.isEmpty == false) analytics.logEvent("MISTAKE", bundle) }
    fun logSettings(switchOffAnimation: Boolean) = logEvent("SETTINGS", Bundle().apply { putBoolean("SWITCH_OFF_ANIMATION", switchOffAnimation) })
    fun logSingleCaseAnswer(correct: Boolean, selected: String, answer: String, word: String, caseName: String) = logEvent("SINGLE_CASE_ANSWER", Bundle().apply { putString("RESULT", if (correct) "CORRECT" else "INCORRECT"); putString("SELECTED_ANSWER", selected); putString("CORRECT_ANSWER", answer); putString("WORD", word); putString("CASE", caseName) })
    fun logSingleCaseNavigation(button: String, word: String) = logEvent("SINGLE_CASE_NAVIGATION", Bundle().apply { putString(KEY_BUTTON, button); putString("WORD", word) })
    fun logHandbookOpen() = logEvent("HANDBOOK_FRAGMENT", Bundle().apply { putString("HANDBOOK_FRAGMENT", "OPEN") })
    fun logReminderShown(kind: String, streak: Int, inactivityStreak: Int, reviewCount: Int) = logEvent("daily_reminder_shown", Bundle().apply { putString("kind", kind); putInt("streak", streak); putInt("inactivity_streak", inactivityStreak); putInt("review_count", reviewCount) })
    fun logEvent(event: String, bundle: Bundle? = null) { analytics.logEvent(event, bundle) }
    companion object { const val KEY_BUTTON = "BUTTON" }
}
