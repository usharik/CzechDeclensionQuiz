package com.usharik.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for **screen navigation** from the hub and back.
 *
 * The toolbar home button routes through the back dispatcher: on quiz screens it opens the
 * quit-quiz overlay first (handled by [navigateHome]), on other pages it returns directly.
 */
@RunWith(AndroidJUnit4::class)
class NavigationTest : BaseComposeTest() {

    /**
     * Taps "Full declension table quiz", verifies the quiz screen loads a word, then navigates
     * home (via the quit overlay) and confirms the hub is shown again. Repeats once to verify
     * the screen is re-entrant.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun fullQuizNavigationReturnsToHub() {
        composeTestRule.onNodeWithTag(TestTags.BTN_FULL).performClick()

        // Wait for the full quiz screen — it shows the word text node once loaded
        waitForTag(TestTags.FULL_WORD)
        composeTestRule.onNodeWithTag(TestTags.FULL_WORD).assertIsDisplayed()

        navigateHome()

        // Navigate again to verify the screen is re-entrant
        composeTestRule.onNodeWithTag(TestTags.BTN_FULL).performClick()
        waitForTag(TestTags.FULL_WORD)
        navigateHome()
    }

    /** Navigates to "Words with errors" screen and back to hub. */
    @Test
    fun wordsWithErrorsNavigationReturnsToHub() {
        composeTestRule.onNodeWithTag(TestTags.BTN_ERRORS).performClick()
        composeTestRule.waitForIdle()

        // The app bar title switches to the page name
        composeTestRule.onNodeWithTag(TestTags.APP_BAR_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Words with errors").assertIsDisplayed()

        navigateHome()
    }

    /** Navigates to Handbook screen, verifies the paradigm selectors, then returns to hub. */
    @Test
    fun handbookNavigationReturnsToHub() {
        composeTestRule.onNodeWithTag(TestTags.BTN_HANDBOOK).performClick()
        composeTestRule.waitForIdle()

        // Handbook shows the gender and declension-type selectors above the table
        composeTestRule.onNodeWithText("Gender of noun").assertIsDisplayed()
        composeTestRule.onNodeWithText("Type of declension").assertIsDisplayed()

        navigateHome()
    }

    /** Navigates to Settings screen, verifies the section headers, then returns to hub. */
    @Test
    fun settingsNavigationReturnsToHub() {
        composeTestRule.onNodeWithTag(TestTags.BTN_SETTINGS).performClick()
        composeTestRule.waitForIdle()

        // Settings shows the gender filter and app-language sections
        composeTestRule.onNodeWithText("Word filter by gender").assertIsDisplayed()
        composeTestRule.onNodeWithText("App language").assertIsDisplayed()

        navigateHome()
    }

    /** Navigates to About screen, verifies version string, then returns to hub. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aboutNavigationReturnsToHub() {
        composeTestRule.onNodeWithTag(TestTags.BTN_ABOUT).performClick()
        composeTestRule.waitForIdle()

        // About shows a version string starting with "Version "
        waitForText("Version ", substring = true)
        composeTestRule.onNodeWithText("Privacy policy").assertIsDisplayed()

        navigateHome()
    }
}
