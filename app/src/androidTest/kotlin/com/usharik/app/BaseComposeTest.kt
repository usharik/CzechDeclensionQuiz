package com.usharik.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Base class for all Compose UI instrumented tests.
 *
 * Starts [MainActivity] via [composeTestRule] and verifies each test begins on the hub screen.
 * The toolbar home button routes through the back dispatcher, so leaving a quiz screen opens
 * the quit-quiz overlay first; [navigateHome] handles both the direct and the overlay path.
 */
@RunWith(AndroidJUnit4::class)
abstract class BaseComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.HUB_SCREEN).assertIsDisplayed()
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Returns to the hub via the toolbar home button. On the quiz screens the home button
     * opens the quit-quiz overlay, so if it appears we confirm leaving via "Leave quiz".
     */
    @OptIn(ExperimentalTestApi::class)
    protected fun navigateHome() {
        composeTestRule.onNodeWithTag(TestTags.NAV_HOME_BTN).performClick()
        composeTestRule.waitForIdle()
        if (tagExists(TestTags.FULL_QUIT_DIALOG)) {
            composeTestRule.onNodeWithTag(TestTags.FULL_QUIT_LEAVE).performClick()
        }
        composeTestRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.HUB_SCREEN))
        composeTestRule.onNodeWithTag(TestTags.HUB_SCREEN).assertIsDisplayed()
    }

    /** Whether a node with [tag] currently exists in the semantics tree. */
    protected fun tagExists(tag: String): Boolean = try {
        composeTestRule.onNodeWithTag(tag).fetchSemanticsNode()
        true
    } catch (_: AssertionError) {
        false
    }

    /** Waits for a node with [tag] to appear (up to default timeout). */
    @OptIn(ExperimentalTestApi::class)
    protected fun waitForTag(tag: String, timeoutMillis: Long = 10_000) {
        composeTestRule.waitUntilAtLeastOneExists(hasTestTag(tag), timeoutMillis = timeoutMillis)
    }

    /** Waits for a node with [text] to appear (up to default timeout). */
    @OptIn(ExperimentalTestApi::class)
    protected fun waitForText(text: String, substring: Boolean = false) {
        composeTestRule.waitUntilAtLeastOneExists(hasText(text, substring = substring))
    }
}
