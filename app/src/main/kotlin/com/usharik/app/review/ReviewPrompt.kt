package com.usharik.app.review

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import java.time.LocalDate

/** Persisted history of automatic review requests. Days are epoch days. */
data class ReviewPromptState(val requests: Int = 0, val lastRequestDay: Long? = null)

data class ReviewPromptContext(
    val today: LocalDate,
    val installDate: LocalDate,
    /** Days that counted for the streak. */
    val practiceDays: Int,
    /** Today's daily goal is reached: the player is back on the hub after a good session. */
    val goalReachedToday: Boolean,
    /** The support-offer card is on the hub today; never stack two requests on one visit. */
    val supportOfferShownToday: Boolean,
)

/**
 * When to open Google Play's in-app review sheet. Play itself caps how often the sheet really
 * appears and never says whether the player rated, so the app asks rarely, only at a happy moment,
 * and stops after [MAX_REQUESTS]. Play policy forbids asking about the player's opinion first, so
 * there is no "Do you like the app?" pre-dialog.
 */
object ReviewPromptPolicy {
    const val MIN_INSTALL_DAYS = 3L
    const val MIN_PRACTICE_DAYS = 3
    const val DAYS_BETWEEN_REQUESTS = 60L
    const val MAX_REQUESTS = 4

    fun shouldRequest(state: ReviewPromptState, context: ReviewPromptContext): Boolean {
        if (!context.goalReachedToday || context.supportOfferShownToday) return false
        if (state.requests >= MAX_REQUESTS) return false
        val today = context.today.toEpochDay()
        val last = state.lastRequestDay
        return context.today >= context.installDate.plusDays(MIN_INSTALL_DAYS) &&
            context.practiceDays >= MIN_PRACTICE_DAYS &&
            (last == null || today >= last + DAYS_BETWEEN_REQUESTS)
    }

    fun onRequested(state: ReviewPromptState, today: LocalDate) =
        ReviewPromptState(requests = state.requests + 1, lastRequestDay = today.toEpochDay())
}

class ReviewPromptStore(private val prefs: SharedPreferences) {
    fun load() = ReviewPromptState(
        requests = prefs.getInt(REQUESTS, 0),
        lastRequestDay = prefs.getLong(LAST_REQUEST_DAY, NONE).takeIf { it != NONE },
    )

    fun save(state: ReviewPromptState) {
        prefs.edit().putInt(REQUESTS, state.requests).putLong(LAST_REQUEST_DAY, state.lastRequestDay ?: NONE).apply()
    }

    private companion object {
        const val NONE = Long.MIN_VALUE
        const val REQUESTS = "reviewPromptRequests"
        const val LAST_REQUEST_DAY = "reviewPromptLastRequestDay"
    }
}

fun interface ReviewPrompter {
    /** Asks Play to show its review sheet; Play may silently skip it (quota, already rated, no Play). */
    fun request(activity: Activity)
}

class PlayReviewPrompter(context: Context) : ReviewPrompter {
    private val manager = ReviewManagerFactory.create(context.applicationContext)

    override fun request(activity: Activity) {
        manager.requestReviewFlow().addOnCompleteListener { task ->
            when {
                !task.isSuccessful -> Log.w(TAG, "Review flow unavailable", task.exception)
                !activity.isFinishing && !activity.isDestroyed -> manager.launchReviewFlow(activity, task.result)
            }
        }
    }

    private companion object {
        const val TAG = "PlayReviewPrompter"
    }
}
