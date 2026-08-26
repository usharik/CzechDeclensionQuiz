package com.usharik.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.usharik.database.WordInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the **Full Declension Quiz** screen.
 *
 * The tests exercise the actual pointer-driven drag-and-drop implementation. They use the word
 * loaded by the app to identify an unambiguous source/target pair, rather than relying on a
 * fixture or duplicating the production shuffle order.
 */
@RunWith(AndroidJUnit4::class)
class DeclensionQuizTest : BaseComposeTest() {

    /**
     * Smoke test: navigating to the full declension quiz shows a word and the error counter.
     * Verifies the screen loads correctly before any interaction.
     */
    @Test
    fun fullQuizScreenLoadsWord() {
        openQuizAndWaitForWord()

        composeTestRule.onNodeWithTag(TestTags.FULL_WORD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.FULL_ERROR_COUNTER).assertIsDisplayed()

        // Error counter starts at 0/5
        assertEquals("0/5", taggedText(TestTags.FULL_ERROR_COUNTER))
    }

    /**
     * Verifies that tapping "Home" from the full quiz opens the quit overlay and its
     * "Leave quiz" action returns to the hub.
     */
    @Test
    fun homeButtonReturnsToHub() {
        openQuizAndWaitForWord()
        navigateHome()
    }

    /** Verifies that the app bar title changes to the quiz name inside the full quiz. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun appBarTitleChangesOnFullQuiz() {
        openQuizAndWaitForWord()

        waitForText("Full declension table quiz")
        composeTestRule.onNodeWithText("Full declension table quiz").assertIsDisplayed()

        navigateHome()
    }

    /**
     * Dragging a form to its matching case/number cell removes it from the bank and leaves it
     * in the table.
     */
    @Test
    fun correctPlacementStaysInMatchingCell() {
        openQuizAndWaitForWord()
        val word = loadedWord()
        val (poolIndex, text) = poolFormForTarget(word, number = 0, caseIndex = 0)

        dragPoolWordToCell(poolIndex, number = 0, caseIndex = 0)

        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            !tagExists("${TestTags.FULL_POOL_WORD_PREFIX}$poolIndex")
        }
        assertEquals(text, cellText(number = 0, caseIndex = 0))
        assertEquals("0/5", taggedText(TestTags.FULL_ERROR_COUNTER))
    }

    /**
     * An invalid drop increments the error counter and returns the form to the word bank after
     * its shake animation.
     */
    @Test
    fun incorrectPlacementReturnsFormToBankAndIncrementsErrorCounter() {
        openQuizAndWaitForWord()
        val word = loadedWord()
        val (poolIndex, _) = poolFormNotMatchingTarget(word, number = 0, caseIndex = 0)

        dragPoolWordToCell(poolIndex, number = 0, caseIndex = 0)

        assertEquals("1/5", taggedText(TestTags.FULL_ERROR_COUNTER))
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            tagExists("${TestTags.FULL_POOL_WORD_PREFIX}$poolIndex")
        }
        assertEquals("", cellText(number = 0, caseIndex = 0))
    }

    /**
     * Completes the full 7×2 table through real drag gestures and verifies the success dialog.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun completingAllCorrectPlacementsShowsSuccessDialog() {
        openQuizAndWaitForWord()
        val word = loadedWord()

        for (caseIndex in 0..6) {
            for (number in 0..1) {
                if (word.cases(number, caseIndex).isNotEmpty()) {
                    val (poolIndex, _) = poolFormForTarget(word, number, caseIndex)
                    dragPoolWordToCell(poolIndex, number, caseIndex)
                    composeTestRule.waitUntil(timeoutMillis = 3_000) {
                        !tagExists("${TestTags.FULL_POOL_WORD_PREFIX}$poolIndex")
                    }
                }
            }
        }

        composeTestRule.waitUntilAtLeastOneExists(
            hasTestTag(TestTags.FULL_COMPLETION_DIALOG),
            timeoutMillis = 5_000,
        )
        composeTestRule.onNodeWithTag(TestTags.FULL_DIALOG_NEXT_WORD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.FULL_DIALOG_STAY_HERE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.FULL_DIALOG_TRY_AGAIN).assertIsDisplayed()
    }

    /**
     * Regression test for the bug where the error counter stayed stuck after the 90s timeout
     * ad: any external advance to a new word (the toolbar "Next" action wired via `registerNext`,
     * the same mechanism the timeout-ad callback uses) must reset the error counter for the
     * freshly loaded word.
     */
    @Test
    fun advancingToNextWordResetsErrorCounter() {
        openQuizAndWaitForWord()
        val word = loadedWord()
        val (poolIndex, _) = poolFormNotMatchingTarget(word, number = 0, caseIndex = 0)
        dragPoolWordToCell(poolIndex, number = 0, caseIndex = 0)
        assertEquals("1/5", taggedText(TestTags.FULL_ERROR_COUNTER))

        composeTestRule.onNodeWithTag(TestTags.NAV_NEXT_BTN).performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) { taggedText(TestTags.FULL_ERROR_COUNTER) == "0/5" }
        assertEquals("0/5", taggedText(TestTags.FULL_ERROR_COUNTER))
    }

    /**
     * Swiping right on the quiz reveals the handbook overlay (finger-tracked panel), and
     * swiping left on it hides the overlay again, returning to the quiz underneath.
     */
    @Test
    fun swipeRightOpensHandbookAndSwipeLeftCloses() {
        openQuizAndWaitForWord()

        composeTestRule.onNodeWithTag(TestTags.FULL_QUIZ_ROOT).performTouchInput {
            swipe(start = centerLeft, end = centerRight, durationMillis = 300)
        }
        waitForTag(TestTags.FULL_HANDBOOK_OVERLAY)
        composeTestRule.onNodeWithTag(TestTags.FULL_HANDBOOK_OVERLAY).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.FULL_QUIZ_ROOT).performTouchInput {
            swipe(start = centerRight, end = centerLeft, durationMillis = 300)
        }
        composeTestRule.waitUntil(timeoutMillis = 3_000) { !tagExists(TestTags.FULL_HANDBOOK_OVERLAY) }
        composeTestRule.onNodeWithTag(TestTags.FULL_WORD).assertIsDisplayed()
    }

    /** The system back action opens the full-quiz exit dialog and its Leave action returns home. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun backShowsQuitDialogWithExerciseStatistic() {
        openQuizAndWaitForWord()

        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        waitForTag(TestTags.FULL_QUIT_DIALOG)
        composeTestRule.onNodeWithTag(TestTags.FULL_QUIT_EXERCISES).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.FULL_QUIT_LEAVE).performClick()
        waitForTag(TestTags.HUB_SCREEN)
    }

    private fun openQuizAndWaitForWord() {
        composeTestRule.onNodeWithTag(TestTags.BTN_FULL).performClick()
        waitForTag(TestTags.FULL_WORD)
    }

    private fun loadedWord(): WordInfo {
        val text = composeTestRule.onNodeWithTag(TestTags.FULL_WORD)
            .fetchSemanticsNode().config[SemanticsProperties.Text].single().text
        return requireNotNull(runBlocking {
            (composeTestRule.activity.application as App).wordService.wordByName(text)
        }) { "The displayed word '$text' was not found in the app database." }
    }

    private fun poolFormForTarget(word: WordInfo, number: Int, caseIndex: Int): Pair<Int, String> {
        val targetText = word.cases(number, caseIndex)
        return visiblePoolForms().firstOrNull { it.second == targetText }
            ?: error("No visible pool form for $number/$caseIndex ('$targetText').")
    }

    private fun poolFormNotMatchingTarget(word: WordInfo, number: Int, caseIndex: Int): Pair<Int, String> {
        val targetText = word.cases(number, caseIndex)
        return visiblePoolForms().firstOrNull { it.second != targetText }
            ?: error("Every visible form matched $number/$caseIndex ('$targetText').")
    }

    private fun visiblePoolForms(): List<Pair<Int, String>> = (0 until 14).mapNotNull { index ->
        val tag = "${TestTags.FULL_POOL_WORD_PREFIX}$index"
        try {
            val text = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription].single()
            index to text
        } catch (_: AssertionError) {
            null
        }
    }

    /**
     * Drags a pool chip onto a table cell with a real swipe gesture. The whole quiz fits on a
     * single screen (no scrolling), so both nodes are always visible.
     */
    private fun dragPoolWordToCell(poolIndex: Int, number: Int, caseIndex: Int) {
        val source = composeTestRule.onNodeWithTag("${TestTags.FULL_POOL_WORD_PREFIX}$poolIndex")
        val target = composeTestRule.onNodeWithTag("${TestTags.FULL_CELL_PREFIX}${number}_$caseIndex")
        val sourceBounds = source.fetchSemanticsNode().boundsInRoot
        val targetBounds = target.fetchSemanticsNode().boundsInRoot
        val sourceOrigin = sourceBounds.topLeft

        source.performTouchInput {
            swipe(
                start = sourceBounds.center - sourceOrigin,
                end = targetBounds.center - sourceOrigin,
                durationMillis = 400,
            )
        }
    }

    private fun cellText(number: Int, caseIndex: Int): String =
        composeTestRule.onNodeWithTag("${TestTags.FULL_CELL_PREFIX}${number}_$caseIndex")
            .fetchSemanticsNode().children.single().config[SemanticsProperties.Text].single().text

    private fun taggedText(tag: String): String =
        composeTestRule.onNodeWithTag(tag).fetchSemanticsNode()
            .config[SemanticsProperties.Text].single().text
}
