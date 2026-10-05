package com.usharik.app.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReviewPromptPolicyTest {
    private val install = LocalDate.of(2026, 10, 1)
    private val firstDay = install.plusDays(ReviewPromptPolicy.MIN_INSTALL_DAYS)

    private fun context(
        today: LocalDate = firstDay,
        practiceDays: Int = ReviewPromptPolicy.MIN_PRACTICE_DAYS,
        goalReached: Boolean = true,
        offerShown: Boolean = false,
    ) = ReviewPromptContext(today, install, practiceDays, goalReached, offerShown)

    @Test fun asksAnEngagedPlayerWhoJustReachedTheGoal() {
        assertTrue(ReviewPromptPolicy.shouldRequest(ReviewPromptState(), context()))
    }

    @Test fun waitsForAHappyMomentAndEnoughHistory() {
        val state = ReviewPromptState()
        assertFalse(ReviewPromptPolicy.shouldRequest(state, context(goalReached = false)))
        assertFalse(ReviewPromptPolicy.shouldRequest(state, context(today = firstDay.minusDays(1))))
        assertFalse(ReviewPromptPolicy.shouldRequest(state, context(practiceDays = ReviewPromptPolicy.MIN_PRACTICE_DAYS - 1)))
    }

    @Test fun neverStacksWithTheSupportOffer() {
        assertFalse(ReviewPromptPolicy.shouldRequest(ReviewPromptState(), context(offerShown = true)))
    }

    @Test fun repeatsRarelyAndStopsAfterTheCap() {
        var state = ReviewPromptPolicy.onRequested(ReviewPromptState(), firstDay)
        assertEquals(1, state.requests)
        assertFalse(ReviewPromptPolicy.shouldRequest(state, context(today = firstDay)))
        assertFalse(ReviewPromptPolicy.shouldRequest(state, context(today = firstDay.plusDays(ReviewPromptPolicy.DAYS_BETWEEN_REQUESTS - 1))))
        var day = firstDay
        repeat(ReviewPromptPolicy.MAX_REQUESTS - 1) {
            day = day.plusDays(ReviewPromptPolicy.DAYS_BETWEEN_REQUESTS)
            assertTrue(ReviewPromptPolicy.shouldRequest(state, context(today = day)))
            state = ReviewPromptPolicy.onRequested(state, day)
        }
        assertFalse(ReviewPromptPolicy.shouldRequest(state, context(today = day.plusYears(1))))
    }
}
