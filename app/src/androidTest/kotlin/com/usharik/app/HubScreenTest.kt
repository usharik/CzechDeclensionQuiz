package com.usharik.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the **Hub screen** (quiz mode selection).
 */
@RunWith(AndroidJUnit4::class)
class HubScreenTest : BaseComposeTest() {

    /** Verifies that the progress card and all six navigation buttons are on the hub screen. */
    @Test
    fun showsAllNavigationButtons() {
        // Hub screen root is visible
        composeTestRule.onNodeWithTag(TestTags.HUB_SCREEN).assertIsDisplayed()
        composeTestRule.waitUntil(5_000) { composeTestRule.onAllNodesWithTag(TestTags.HUB_PROGRESS_CARD).fetchSemanticsNodes().isNotEmpty() }
        composeTestRule.onNodeWithTag(TestTags.HUB_STREAK).assertIsDisplayed()

        // Verify each navigation button is displayed by both tag and label text
        composeTestRule.onNodeWithTag(TestTags.BTN_FULL).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Full declension table quiz").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.BTN_SINGLE).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("One case at a time quiz").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.BTN_ERRORS).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Words with errors").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.BTN_HANDBOOK).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Handbook").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.BTN_SETTINGS).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.BTN_ABOUT).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("About").performScrollTo().assertIsDisplayed()
    }

    /**
     * Verifies the app bar title on the hub screen is "Czech Declension Quiz".
     * The hub is the root destination, so the app bar shows no back button there.
     */
    @Test
    fun appBarShowsBrandTitle() {
        composeTestRule.onNodeWithTag(TestTags.APP_BAR_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Czech Declension Quiz").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NAV_HOME_BTN).assertDoesNotExist()
    }
}
