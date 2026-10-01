package com.usharik.app.notification

import com.usharik.app.notification.ReminderPlanner.Kind
import com.usharik.app.ui.state.Streak
import com.usharik.database.dao.ReminderStateEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class ReminderPlannerTest {
    private val day = LocalDateTime.of(2026, 10, 1, 0, 0)
    private val today = "2026-10-01"
    private fun at(hour: Int) = day.withHour(hour)
    private fun streak(current: Int, todayCounts: Boolean = false) = Streak.Summary(current, current, todayCounts, if (todayCounts) 0 else Streak.MIN_POINTS)

    private fun plan(
        hour: Int,
        streak: Streak.Summary = streak(0),
        practicedYesterday: Boolean = false,
        reviewCount: Int = 0,
        state: ReminderStateEntity = ReminderStateEntity(),
        preferredHour: Int = 18,
    ) = ReminderPlanner.plan(at(hour), preferredHour, streak, practicedYesterday, reviewCount, state)

    @Test fun waitsForThePreferredHour() {
        val p = plan(hour = 17, streak = streak(3), practicedYesterday = true)
        assertNull(p.kind)
        assertNull(p.state.lastCheckDate)
    }

    @Test fun streakReminderAtThePreferredHour() {
        val p = plan(hour = 18, streak = streak(3), practicedYesterday = true)
        assertEquals(Kind.STREAK, p.kind)
        assertEquals(today, p.state.lastCheckDate)
        assertEquals(today, p.state.lastNotificationDate)
        assertEquals(0, p.state.inactivityStreak)
    }

    @Test fun onlyOneDailyReminderPerDay() {
        val first = plan(hour = 18, streak = streak(3), practicedYesterday = true)
        assertNull(plan(hour = 19, streak = streak(3), practicedYesterday = true, state = first.state).kind)
    }

    @Test fun noReminderWhenTodayAlreadyCounts() {
        val p = plan(hour = 18, streak = streak(4, todayCounts = true), practicedYesterday = true)
        assertNull(p.kind)
        assertEquals(today, p.state.lastCheckDate)
    }

    @Test fun reviewReminderWithoutAStreak() {
        assertEquals(Kind.REVIEW, plan(hour = 18, reviewCount = 5).kind)
    }

    @Test fun comebackRemindersBackOffExponentially() {
        val notified = (1..70).filter { inactive ->
            plan(hour = 18, state = ReminderStateEntity(inactivityStreak = inactive - 1)).kind != null
        }
        assertEquals(listOf(1, 2, 4, 8, 16, 32, 64), notified)
        assertEquals(Kind.COMEBACK, plan(hour = 18, state = ReminderStateEntity(inactivityStreak = 3)).kind)
    }

    @Test fun eveningRescueForAStreakAtRisk() {
        val checked = ReminderStateEntity(lastCheckDate = today, lastNotificationDate = today)
        val p = plan(hour = 20, streak = streak(5), practicedYesterday = true, state = checked)
        assertEquals(Kind.RESCUE, p.kind)
        assertEquals(today, p.state.lastRescueDate)
        assertNull(plan(hour = 21, streak = streak(5), practicedYesterday = true, state = p.state).kind)
    }

    @Test fun noRescueForShortOrSafeStreaks() {
        val checked = ReminderStateEntity(lastCheckDate = today)
        assertNull(plan(hour = 20, streak = streak(1), state = checked).kind)
        assertNull(plan(hour = 20, streak = streak(5, todayCounts = true), state = checked).kind)
    }

    @Test fun preferredHourIsClampedToWakingHours() {
        assertEquals(ReminderPlanner.DEFAULT_HOUR, ReminderPlanner.preferredHour(null))
        assertEquals(9, ReminderPlanner.preferredHour(6))
        assertEquals(19, ReminderPlanner.preferredHour(23))
        assertEquals(14, ReminderPlanner.preferredHour(14))
    }
}
