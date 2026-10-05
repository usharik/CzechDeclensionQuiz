package com.usharik.app.billing

import android.content.SharedPreferences
import java.time.LocalDate

/**
 * Persisted history of the hub's "remove ads / support the developer" card. Days are epoch days.
 */
data class SupportOfferState(
    val impressions: Int = 0,
    val dismissals: Int = 0,
    val lastShownDay: Long? = null,
    /** The card was dismissed on this day, so that day's impression is over. */
    val dismissedDay: Long? = null,
    /** No new impression before this day. */
    val snoozedUntilDay: Long = 0,
    val optedOut: Boolean = false,
)

/** Everything besides [SupportOfferState] that decides whether the card may appear. */
data class SupportOfferContext(
    val today: LocalDate,
    val installDate: LocalDate,
    /** Days that counted for the streak. */
    val practiceDays: Int,
    val wordsCompleted: Int,
    /** Today already counts for the streak, i.e. the player is back on the hub after real practice. */
    val practicedToday: Boolean,
    val adFree: Boolean,
    val offer: RemoveAdsOffer,
)

/**
 * When to invite the player to buy [REMOVE_ADS_PRODUCT_ID]: only after they have found the app useful,
 * only on the hub after a practice, rarely, and never again once they say so. The card is the only
 * automatic reminder; notifications stay about learning, as Play policy expects.
 */
object SupportOfferPolicy {
    const val MIN_INSTALL_DAYS = 7
    const val MIN_PRACTICE_DAYS = 3
    const val MIN_WORDS_COMPLETED = 20
    const val MAX_IMPRESSIONS = 3
    const val DAYS_BETWEEN_IMPRESSIONS = 14L
    const val FIRST_DISMISS_SNOOZE_DAYS = 30L
    const val LATER_DISMISS_SNOOZE_DAYS = 60L
    const val MAX_DISMISSALS = 3

    fun shouldShow(state: SupportOfferState, context: SupportOfferContext): Boolean {
        val offer = context.offer
        // Without a confirmed purchase check a previous buyer (e.g. after a reinstall) could be asked again.
        if (state.optedOut || context.adFree || offer.pending || offer.formattedPrice == null || !offer.purchasesSynced) return false
        if (!context.practicedToday) return false
        val today = context.today.toEpochDay()
        // The day's impression stays on the hub until it is dismissed.
        if (state.lastShownDay == today) return state.dismissedDay != today
        return context.today >= context.installDate.plusDays(MIN_INSTALL_DAYS.toLong()) &&
            context.practiceDays >= MIN_PRACTICE_DAYS &&
            context.wordsCompleted >= MIN_WORDS_COMPLETED &&
            state.impressions < MAX_IMPRESSIONS &&
            today >= state.snoozedUntilDay
    }

    /** Records an impression; a no-op while the same day's card is still on screen. */
    fun onShown(state: SupportOfferState, today: LocalDate): SupportOfferState {
        val day = today.toEpochDay()
        if (state.lastShownDay == day) return state
        return state.copy(
            impressions = state.impressions + 1,
            lastShownDay = day,
            snoozedUntilDay = maxOf(state.snoozedUntilDay, day + DAYS_BETWEEN_IMPRESSIONS),
        )
    }

    fun onNotNow(state: SupportOfferState, today: LocalDate): SupportOfferState {
        val day = today.toEpochDay()
        val dismissals = state.dismissals + 1
        val snooze = if (dismissals == 1) FIRST_DISMISS_SNOOZE_DAYS else LATER_DISMISS_SNOOZE_DAYS
        return state.copy(
            dismissals = dismissals,
            dismissedDay = day,
            snoozedUntilDay = maxOf(state.snoozedUntilDay, day + snooze),
            optedOut = state.optedOut || dismissals >= MAX_DISMISSALS,
        )
    }

    fun onNeverShow(state: SupportOfferState, today: LocalDate): SupportOfferState =
        state.copy(optedOut = true, dismissedDay = today.toEpochDay())
}

class SupportOfferStore(private val prefs: SharedPreferences) {
    fun load() = SupportOfferState(
        impressions = prefs.getInt(IMPRESSIONS, 0),
        dismissals = prefs.getInt(DISMISSALS, 0),
        lastShownDay = prefs.getLong(LAST_SHOWN_DAY, NONE).takeIf { it != NONE },
        dismissedDay = prefs.getLong(DISMISSED_DAY, NONE).takeIf { it != NONE },
        snoozedUntilDay = prefs.getLong(SNOOZED_UNTIL_DAY, 0),
        optedOut = prefs.getBoolean(OPTED_OUT, false),
    )

    fun save(state: SupportOfferState) {
        prefs.edit()
            .putInt(IMPRESSIONS, state.impressions)
            .putInt(DISMISSALS, state.dismissals)
            .putLong(LAST_SHOWN_DAY, state.lastShownDay ?: NONE)
            .putLong(DISMISSED_DAY, state.dismissedDay ?: NONE)
            .putLong(SNOOZED_UNTIL_DAY, state.snoozedUntilDay)
            .putBoolean(OPTED_OUT, state.optedOut)
            .apply()
    }

    private companion object {
        const val NONE = Long.MIN_VALUE
        const val IMPRESSIONS = "supportOfferImpressions"
        const val DISMISSALS = "supportOfferDismissals"
        const val LAST_SHOWN_DAY = "supportOfferLastShownDay"
        const val DISMISSED_DAY = "supportOfferDismissedDay"
        const val SNOOZED_UNTIL_DAY = "supportOfferSnoozedUntilDay"
        const val OPTED_OUT = "supportOfferOptedOut"
    }
}
