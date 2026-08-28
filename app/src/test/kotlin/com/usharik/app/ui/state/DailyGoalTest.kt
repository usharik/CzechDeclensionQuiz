package com.usharik.app.ui.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyGoalTest {
    @Test fun noProgressYet() {
        val p = DailyGoal.Progress(completed = 0)
        assertEquals(DailyGoal.TARGET_POINTS, p.remaining)
        assertFalse(p.isReached)
        assertFalse(p.isOneWordAway)
        assertEquals(0f, p.fraction)
    }

    @Test fun oneWordAwayFromGoal() {
        val p = DailyGoal.Progress(completed = DailyGoal.TARGET_POINTS - Scoring.POINTS_PER_CORRECT_FORM)
        assertEquals(Scoring.POINTS_PER_CORRECT_FORM, p.remaining)
        assertTrue(p.isOneWordAway)
        assertFalse(p.isReached)
    }

    @Test fun goalReachedExactly() {
        val p = DailyGoal.Progress(completed = DailyGoal.TARGET_POINTS)
        assertTrue(p.isReached)
        assertFalse(p.isOneWordAway)
        assertEquals(0, p.remaining)
        assertEquals(1f, p.fraction)
    }

    @Test fun goalExceeded() {
        val p = DailyGoal.Progress(completed = DailyGoal.TARGET_POINTS + 3)
        assertTrue(p.isReached)
        assertEquals(0, p.remaining)
        assertEquals(1f, p.fraction)
    }

    @Test fun fractionIsClampedAndProportional() {
        val p = DailyGoal.Progress(completed = 2, target = 4)
        assertEquals(0.5f, p.fraction)
    }
}
