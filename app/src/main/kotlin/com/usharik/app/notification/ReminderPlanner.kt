package com.usharik.app.notification

import com.usharik.app.ui.state.Streak
import com.usharik.database.dao.ReminderStateEntity
import java.time.LocalDateTime

/**
 * Decides which reminder (if any) the hourly worker posts. Pure so the rules are unit-testable:
 *  - one daily reminder at the player's usual practice hour, skipped once today already counts,
 *    and backed off exponentially (1, 2, 4 … 32 days, then monthly) while the player stays away;
 *  - one evening "streak at risk" reminder when a streak of 2+ days would end at midnight.
 */
object ReminderPlanner {
    const val DEFAULT_HOUR = 18
    const val EARLIEST_HOUR = 9
    const val LATEST_HOUR = 20
    const val RESCUE_HOUR = 20
    const val MIN_RESCUE_STREAK = 2
    private const val MAX_BACKOFF_DAYS = 32

    enum class Kind { STREAK, REVIEW, COMEBACK, RESCUE }

    data class Plan(val kind: Kind?, val state: ReminderStateEntity)

    /** The hour the player last practiced at (clamped to waking hours), or [DEFAULT_HOUR]. */
    fun preferredHour(lastPracticeHour: Int?): Int = lastPracticeHour?.coerceIn(EARLIEST_HOUR, LATEST_HOUR - 1) ?: DEFAULT_HOUR

    fun plan(
        now: LocalDateTime,
        preferredHour: Int,
        streak: Streak.Summary,
        practicedYesterday: Boolean,
        reviewCount: Int,
        state: ReminderStateEntity,
    ): Plan {
        val today = now.toLocalDate().toString()
        if (state.lastCheckDate != today && now.hour >= preferredHour) {
            val inactivity = if (practicedYesterday) 0 else state.inactivityStreak + 1
            val next = state.copy(
                lastCheckDate = today,
                inactivityStreak = inactivity,
                lastActiveDate = if (practicedYesterday) now.toLocalDate().minusDays(1).toString() else state.lastActiveDate,
            )
            val notify = !streak.todayCounts && (practicedYesterday || isBackoffDay(inactivity))
            if (!notify) return Plan(null, next)
            val kind = when {
                streak.current > 0 -> Kind.STREAK
                reviewCount > 0 -> Kind.REVIEW
                else -> Kind.COMEBACK
            }
            return Plan(kind, next.copy(lastNotificationDate = today))
        }
        val rescue = state.lastCheckDate == today && state.lastRescueDate != today &&
            now.hour >= RESCUE_HOUR &&
            streak.atRisk && streak.current >= MIN_RESCUE_STREAK
        return if (rescue) Plan(Kind.RESCUE, state.copy(lastRescueDate = today)) else Plan(null, state)
    }

    private fun isBackoffDay(inactiveDays: Int): Boolean = when {
        inactiveDays <= 0 -> false
        inactiveDays >= MAX_BACKOFF_DAYS -> inactiveDays % MAX_BACKOFF_DAYS == 0
        else -> inactiveDays and (inactiveDays - 1) == 0
    }
}
