package com.usharik.app.ui.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyGoalTest {
    private val target = DailyGoal.DEFAULT.points

    @Test fun noProgressYet() {
        val p = DailyGoal.Progress(completed = 0)
        assertEquals(target, p.remaining)
        assertFalse(p.isReached)
        assertFalse(p.isOneWordAway)
        assertEquals(0f, p.fraction)
    }

    @Test fun onePerfectWordAwayFromGoal() {
        val p = DailyGoal.Progress(completed = target - Scoring.PERFECT_WORD_POINTS)
        assertEquals(Scoring.PERFECT_WORD_POINTS, p.remaining)
        assertTrue(p.isOneWordAway)
        assertFalse(p.isReached)
    }

    @Test fun moreThanOneWordAway() {
        assertFalse(DailyGoal.Progress(completed = target - Scoring.PERFECT_WORD_POINTS - 1).isOneWordAway)
    }

    @Test fun goalReachedExactly() {
        val p = DailyGoal.Progress(completed = target)
        assertTrue(p.isReached)
        assertFalse(p.isOneWordAway)
        assertEquals(0, p.remaining)
        assertEquals(1f, p.fraction)
    }

    @Test fun goalExceeded() {
        val p = DailyGoal.Progress(completed = target + 3)
        assertTrue(p.isReached)
        assertEquals(0, p.remaining)
        assertEquals(1f, p.fraction)
    }

    @Test fun fractionIsClampedAndProportional() {
        val p = DailyGoal.Progress(completed = 2, target = 4)
        assertEquals(0.5f, p.fraction)
    }

    @Test fun perfectWordIsWorthFiftyOnePoints() {
        assertEquals(51, Scoring.PERFECT_WORD_POINTS)
    }

    @Test fun unknownStoredGoalFallsBackToDefault() {
        assertEquals(DailyGoal.Level.LIGHT, DailyGoal.Level.fromPoints(100))
        assertEquals(DailyGoal.DEFAULT, DailyGoal.Level.fromPoints(1000))
    }
}
