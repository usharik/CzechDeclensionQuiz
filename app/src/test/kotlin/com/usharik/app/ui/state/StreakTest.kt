package com.usharik.app.ui.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakTest {
    private val today = LocalDate.of(2026, 10, 1)
    private val enough = Streak.MIN_POINTS

    private fun days(vararg offsetsAndScores: Pair<Long, Int>) =
        offsetsAndScores.map { (offset, score) -> DayScore(today.minusDays(offset), score) }

    @Test fun noHistory() {
        val s = Streak.summarize(emptyList(), today)
        assertEquals(0, s.current); assertEquals(0, s.best)
        assertFalse(s.todayCounts); assertFalse(s.atRisk)
        assertEquals(Streak.MIN_POINTS, s.pointsToKeep)
    }

    @Test fun streakUpToYesterdayIsAliveButAtRisk() {
        val s = Streak.summarize(days(1L to enough, 2L to enough, 3L to enough), today)
        assertEquals(3, s.current)
        assertFalse(s.todayCounts)
        assertTrue(s.atRisk)
    }

    @Test fun todayCountsOnceItReachesTheMinimum() {
        val s = Streak.summarize(days(0L to enough, 1L to enough), today)
        assertEquals(2, s.current)
        assertTrue(s.todayCounts)
        assertFalse(s.atRisk)
        assertEquals(0, s.pointsToKeep)
    }

    @Test fun partialTodayDoesNotCountYet() {
        val s = Streak.summarize(days(0L to enough - 5, 1L to enough), today)
        assertEquals(1, s.current)
        assertFalse(s.todayCounts)
        assertEquals(5, s.pointsToKeep)
    }

    @Test fun gapBreaksTheStreak() {
        val s = Streak.summarize(days(1L to enough, 3L to enough, 4L to enough, 5L to enough), today)
        assertEquals(1, s.current)
        assertEquals(3, s.best)
    }

    @Test fun lowScoreDayBreaksTheStreak() {
        assertEquals(1, Streak.summarize(days(1L to enough, 2L to 3, 3L to enough), today).current)
    }

    @Test fun lastDaysFillsGapsOldestFirst() {
        val week = Streak.lastDays(days(0L to 12, 3L to 40), today)
        assertEquals(7, week.size)
        assertEquals(today.minusDays(6), week.first().date)
        assertEquals(today, week.last().date)
        assertEquals(listOf(0, 0, 0, 40, 0, 0, 12), week.map { it.score })
    }
}
