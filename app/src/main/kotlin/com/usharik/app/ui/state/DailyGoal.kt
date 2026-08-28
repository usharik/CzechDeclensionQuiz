package com.usharik.app.ui.state

/**
 * Lightweight daily points goal used to nudge players to finish "just one more" instead of
 * quitting. Kept as a plain data holder (no Compose/Android deps) so the nudge logic is trivially
 * unit-testable independent of [com.usharik.app.ui.components.QuitQuizDialog].
 *
 * Points-based (rather than words-based) so every correct form gives frequent, small positive
 * feedback instead of a single reward at the end of a word — micro-rewards keep short sessions
 * motivating even when a player only has time for a couple of forms.
 */
object DailyGoal {
    /** Points/day target: several words' worth of practice (~3*14 + 3 + 6 = 51 pts/perfect word). */
    const val TARGET_POINTS = 1000

    /** Progress of today's collected points towards [TARGET_POINTS]. */
    data class Progress(val completed: Int, val target: Int = TARGET_POINTS) {
        val remaining: Int get() = (target - completed).coerceAtLeast(0)
        val isReached: Boolean get() = completed >= target
        val isOneWordAway: Boolean get() = !isReached && remaining <= Scoring.POINTS_PER_CORRECT_FORM
        val fraction: Float get() = if (target <= 0) 1f else (completed.toFloat() / target).coerceIn(0f, 1f)
    }
}
