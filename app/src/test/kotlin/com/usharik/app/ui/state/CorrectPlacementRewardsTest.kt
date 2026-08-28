package com.usharik.app.ui.state

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectPlacementRewardsTest {
    @Test fun cellCanOnlyClaimCorrectFormPointsOncePerWord() {
        val rewards = CorrectPlacementRewards()

        assertTrue(rewards.claim(cellIndex = 3))
        assertFalse(rewards.claim(cellIndex = 3))
    }

    @Test fun resetStartsANewWordScoringScope() {
        val rewards = CorrectPlacementRewards()
        assertTrue(rewards.claim(cellIndex = 3))

        rewards.reset()

        assertTrue(rewards.claim(cellIndex = 3))
    }
}
