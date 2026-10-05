package com.usharik.app.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SupportOfferPolicyTest {
    private val install = LocalDate.of(2026, 10, 1)
    private val firstDay = install.plusDays(SupportOfferPolicy.MIN_INSTALL_DAYS.toLong())
    private val readyOffer = RemoveAdsOffer(formattedPrice = "€2.99", purchasesSynced = true)

    private fun context(
        today: LocalDate = firstDay,
        practiceDays: Int = SupportOfferPolicy.MIN_PRACTICE_DAYS,
        words: Int = SupportOfferPolicy.MIN_WORDS_COMPLETED,
        practicedToday: Boolean = true,
        adFree: Boolean = false,
        offer: RemoveAdsOffer = readyOffer,
    ) = SupportOfferContext(today, install, practiceDays, words, practicedToday, adFree, offer)

    private fun show(state: SupportOfferState, today: LocalDate) =
        SupportOfferPolicy.shouldShow(state, context(today = today))

    @Test fun eligibleRegularLearnerSeesTheOfferAfterPractice() {
        assertTrue(SupportOfferPolicy.shouldShow(SupportOfferState(), context()))
    }

    @Test fun newOrLightUsersAreNotAsked() {
        val state = SupportOfferState()
        assertFalse(SupportOfferPolicy.shouldShow(state, context(today = firstDay.minusDays(1))))
        assertFalse(SupportOfferPolicy.shouldShow(state, context(practiceDays = SupportOfferPolicy.MIN_PRACTICE_DAYS - 1)))
        assertFalse(SupportOfferPolicy.shouldShow(state, context(words = SupportOfferPolicy.MIN_WORDS_COMPLETED - 1)))
        assertFalse(SupportOfferPolicy.shouldShow(state, context(practicedToday = false)))
    }

    @Test fun neverShownWithoutAConfirmedPriceOrWhenBoughtOrPending() {
        val state = SupportOfferState()
        assertFalse(SupportOfferPolicy.shouldShow(state, context(adFree = true)))
        assertFalse(SupportOfferPolicy.shouldShow(state, context(offer = readyOffer.copy(formattedPrice = null))))
        assertFalse(SupportOfferPolicy.shouldShow(state, context(offer = readyOffer.copy(purchasesSynced = false))))
        assertFalse(SupportOfferPolicy.shouldShow(state, context(offer = readyOffer.copy(pending = true))))
    }

    @Test fun anImpressionLastsTheDayAndTheNextWaitsTwoWeeks() {
        val shown = SupportOfferPolicy.onShown(SupportOfferState(), firstDay)
        assertEquals(1, shown.impressions)
        assertEquals(shown, SupportOfferPolicy.onShown(shown, firstDay))
        assertTrue(show(shown, firstDay))
        assertFalse(show(shown, firstDay.plusDays(SupportOfferPolicy.DAYS_BETWEEN_IMPRESSIONS - 1)))
        assertTrue(show(shown, firstDay.plusDays(SupportOfferPolicy.DAYS_BETWEEN_IMPRESSIONS)))
    }

    @Test fun dismissalsBackOffAndTheThirdStopsTheOffer() {
        var state = SupportOfferPolicy.onNotNow(SupportOfferPolicy.onShown(SupportOfferState(), firstDay), firstDay)
        assertFalse(show(state, firstDay))
        assertFalse(show(state, firstDay.plusDays(SupportOfferPolicy.FIRST_DISMISS_SNOOZE_DAYS - 1)))
        val second = firstDay.plusDays(SupportOfferPolicy.FIRST_DISMISS_SNOOZE_DAYS)
        assertTrue(show(state, second))

        state = SupportOfferPolicy.onNotNow(SupportOfferPolicy.onShown(state, second), second)
        assertFalse(show(state, second.plusDays(SupportOfferPolicy.LATER_DISMISS_SNOOZE_DAYS - 1)))
        val third = second.plusDays(SupportOfferPolicy.LATER_DISMISS_SNOOZE_DAYS)
        assertTrue(show(state, third))

        state = SupportOfferPolicy.onNotNow(SupportOfferPolicy.onShown(state, third), third)
        assertTrue(state.optedOut)
        assertFalse(show(state, third.plusYears(1)))
    }

    @Test fun ignoredImpressionsStopAfterTheCap() {
        var state = SupportOfferState()
        var day = firstDay
        repeat(SupportOfferPolicy.MAX_IMPRESSIONS) {
            assertTrue(show(state, day))
            state = SupportOfferPolicy.onShown(state, day)
            day = day.plusDays(SupportOfferPolicy.DAYS_BETWEEN_IMPRESSIONS)
        }
        assertFalse(show(state, day.plusYears(1)))
    }

    @Test fun dontShowAgainIsFinal() {
        val state = SupportOfferPolicy.onNeverShow(SupportOfferPolicy.onShown(SupportOfferState(), firstDay), firstDay)
        assertFalse(show(state, firstDay))
        assertFalse(show(state, firstDay.plusYears(1)))
    }
}
