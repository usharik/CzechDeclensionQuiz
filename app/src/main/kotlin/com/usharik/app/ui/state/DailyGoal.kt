package com.usharik.app.ui.state

/**
 * Daily points goal used to nudge players to finish "just one more" instead of quitting. Kept as
 * a plain data holder (no Compose/Android deps) so the nudge logic is trivially unit-testable.
 *
 * Points-based (rather than words-based) so every correct form gives frequent, small positive
 * feedback instead of a single reward at the end of a word — micro-rewards keep short sessions
 * motivating even when a player only has time for a couple of forms.
 */
object DailyGoal {
    /**
     * Selectable goal sizes. A perfect word is worth [Scoring.PERFECT_WORD_POINTS] (~51) points, so
     * the levels are roughly 2, 5 and 10 words a day: reachable in one short sitting by default.
     */
    enum class Level(val points: Int) {
        LIGHT(100),
        REGULAR(250),
        INTENSE(500);

        companion object {
            fun fromPoints(points: Int): Level = entries.firstOrNull { it.points == points } ?: DEFAULT
        }
    }

    val DEFAULT = Level.REGULAR

    /** Progress of today's collected points towards [target]. */
    data class Progress(val completed: Int, val target: Int = DEFAULT.points) {
        val remaining: Int get() = (target - completed).coerceAtLeast(0)
        val isReached: Boolean get() = completed >= target
        /** One more perfect word would reach the goal. */
        val isOneWordAway: Boolean get() = !isReached && remaining <= Scoring.PERFECT_WORD_POINTS
        val fraction: Float get() = if (target <= 0) 1f else (completed.toFloat() / target).coerceIn(0f, 1f)
    }
}
