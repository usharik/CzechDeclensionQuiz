package com.usharik.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the **Hub screen** (quiz mode selection).
 */
@RunWith(AndroidJUnit4::class)
class HubScreenTest : BaseComposeTest() {

    /** Verifies that all six navigation buttons are visible on the hub screen. */
    @Test
    fun showsAllNavigationButtons() {
        // Hub screen root is visible
        composeTestRule.onNodeWithTag(TestTags.HUB_SCREEN).assertIsDisplayed()

        // Verify each navigation button is displayed by both tag and label text
        composeTestRule.onNodeWithTag(TestTags.BTN_FULL).assertIsDisplayed()
        composeTestRule.onNodeWithText("Full declension table quiz").assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.BTN_SINGLE).assertIsDisplayed()
        composeTestRule.onNodeWithText("One case at a time quiz").assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.BTN_ERRORS).assertIsDisplayed()
        composeTestRule.onNodeWithText("Words with errors").assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.BTN_HANDBOOK).assertIsDisplayed()
        composeTestRule.onNodeWithText("Handbook").assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.BTN_SETTINGS).assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.BTN_ABOUT).assertIsDisplayed()
        composeTestRule.onNodeWithText("About").assertIsDisplayed()
    }

    /**
     * Verifies the app bar title on the hub screen is "Czech Declension Quiz".
     * The toolbar home button is part of the persistent app bar and is always present.
     */
    @Test
    fun appBarShowsBrandTitle() {
        composeTestRule.onNodeWithTag(TestTags.APP_BAR_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Czech Declension Quiz").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NAV_HOME_BTN).assertIsDisplayed()
    }
}
