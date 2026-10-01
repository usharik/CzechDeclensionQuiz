package com.usharik.app.ui.state

import java.time.LocalDate

/** Points scored on one calendar day. */
data class DayScore(val date: LocalDate, val score: Int)

/**
 * Consecutive-practice-days streak. A day counts once it collects [MIN_POINTS] — about ten
 * correct forms — which is deliberately far below any daily goal: the streak rewards showing up,
 * the goal rewards effort.
 */
object Streak {
    const val MIN_POINTS = 30

    fun isPracticeDay(score: Int): Boolean = score >= MIN_POINTS

    data class Summary(
        /** Streak length, including today only once today counts. */
        val current: Int,
        val best: Int,
        val todayCounts: Boolean,
        /** Points still needed today for it to count. */
        val pointsToKeep: Int,
    ) {
        /** A running streak that ends at midnight unless today counts. */
        val atRisk: Boolean get() = current > 0 && !todayCounts
    }

    fun summarize(days: List<DayScore>, today: LocalDate): Summary {
        val practiced = days.filter { isPracticeDay(it.score) }.map { it.date }.toSortedSet()
        val todayScore = days.firstOrNull { it.date == today }?.score ?: 0
        val todayCounts = today in practiced
        var current = 0
        var day = if (todayCounts) today else today.minusDays(1)
        while (day in practiced) { current++; day = day.minusDays(1) }
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (date in practiced) {
            if (date.isAfter(today)) break
            run = if (previous != null && previous.plusDays(1) == date) run + 1 else 1
            best = maxOf(best, run)
            previous = date
        }
        return Summary(current, maxOf(best, current), todayCounts, (MIN_POINTS - todayScore).coerceAtLeast(0))
    }

    /** The [count] days ending today, oldest first, with zero for days without a record. */
    fun lastDays(days: List<DayScore>, today: LocalDate, count: Int = 7): List<DayScore> {
        val byDate = days.associate { it.date to it.score }
        return (count - 1 downTo 0).map { offset -> today.minusDays(offset.toLong()).let { DayScore(it, byDate[it] ?: 0) } }
    }
}
