package com.usharik.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the **Single Case Quiz** screen.
 *
 * In this UI the "Next case" button is always present but stays disabled until an answer
 * is selected (the original enabled/disabled toolbar behaviour).
 */
@RunWith(AndroidJUnit4::class)
class SingleCaseQuizTest : BaseComposeTest() {

    /**
     * Opens the single-case quiz and verifies the initial state:
     * - A word is displayed with the case name, number label and question
     * - Four answer buttons are present
     * - "Next case" button is disabled until an answer is selected
     * - "Next word" button is enabled
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun initialStateIsCorrect() {
        composeTestRule.onNodeWithTag(TestTags.BTN_SINGLE).performClick()

        // Wait for the word to load (async coroutine)
        waitForTag(TestTags.SC_WORD)

        composeTestRule.onNodeWithTag(TestTags.SC_WORD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.SC_CASE_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.SC_NUMBER_LABEL).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.SC_QUESTION).assertIsDisplayed()

        // At least the first answer button exists. The word is random and indeclinable
        // entries (e.g. "café") yield fewer than four unique forms, so only index 0 is
        // guaranteed to be present.
        composeTestRule.onNodeWithTag("${TestTags.SC_ANSWER_PREFIX}0").assertIsDisplayed()

        // "Next case" is shown but disabled before any answer is selected
        composeTestRule.onNodeWithTag(TestTags.SC_NEXT_CASE).assertIsDisplayed().assertIsNotEnabled()

        // "Next word" is always available
        composeTestRule.onNodeWithTag(TestTags.SC_NEXT_WORD).assertIsDisplayed().assertIsEnabled()

        navigateHome()
    }

    /**
     * Opens single-case quiz, taps the first answer, then verifies:
     * - "Next case" becomes enabled
     * - Answer buttons become disabled after selection
     * - Tapping "Next case" advances to the next case (resets answer state)
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun answerInteractionEnablesNextCase() {
        composeTestRule.onNodeWithTag(TestTags.BTN_SINGLE).performClick()
        waitForTag(TestTags.SC_WORD)

        // Before selecting: "Next case" is disabled
        composeTestRule.onNodeWithTag(TestTags.SC_NEXT_CASE).assertIsNotEnabled()

        // Tap the first answer button
        composeTestRule.onNodeWithTag("${TestTags.SC_ANSWER_PREFIX}0").performClick()
        composeTestRule.waitForIdle()

        // After selecting: "Next case" is enabled, answers are locked
        composeTestRule.onNodeWithTag(TestTags.SC_NEXT_CASE).assertIsEnabled()
        composeTestRule.onNodeWithTag("${TestTags.SC_ANSWER_PREFIX}0").assertIsNotEnabled()

        // Tap "Next case" to advance
        composeTestRule.onNodeWithTag(TestTags.SC_NEXT_CASE).performClick()
        composeTestRule.waitForIdle()

        // After advancing: answer state resets — "Next case" disabled again, answers unlocked
        composeTestRule.onNodeWithTag(TestTags.SC_NEXT_CASE).assertIsNotEnabled()
        composeTestRule.onNodeWithTag("${TestTags.SC_ANSWER_PREFIX}0").assertIsDisplayed().assertIsEnabled()

        navigateHome()
    }

    /** "Next word" button skips to a new word immediately, resetting all case progress. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun nextWordButtonSkipsWord() {
        composeTestRule.onNodeWithTag(TestTags.BTN_SINGLE).performClick()
        waitForTag(TestTags.SC_WORD)

        // Tap an answer then next word — state should reset
        composeTestRule.onNodeWithTag("${TestTags.SC_ANSWER_PREFIX}0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.SC_NEXT_WORD).performClick()
        waitForTag(TestTags.SC_WORD, timeoutMillis = 8_000)

        // "Next case" should be disabled again after the word skip
        composeTestRule.onNodeWithTag(TestTags.SC_NEXT_CASE).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TestTags.SC_WORD).assertIsDisplayed()

        navigateHome()
    }
}
