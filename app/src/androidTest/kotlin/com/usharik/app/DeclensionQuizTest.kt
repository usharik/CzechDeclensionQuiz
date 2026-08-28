package com.usharik.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
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

    /** A solved cell may be rearranged, but it must never earn its correct-form points twice. */
    @Test
    fun revisitingCorrectCellDoesNotAwardExtraPoints() {
        openQuizAndWaitForWord()
        val word = loadedWord()
        val expectedScore = openQuitDialogAndReadScore() + 3
        val (poolIndex, _) = poolFormForTarget(word, number = 0, caseIndex = 0)

        dragPoolWordToCell(poolIndex, number = 0, caseIndex = 0)
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            !tagExists("${TestTags.FULL_POOL_WORD_PREFIX}$poolIndex")
        }
        assertEquals(expectedScore, openQuitDialogAndReadScore())

        dragCellToCell(
            sourceNumber = 0,
            sourceCaseIndex = 0,
            targetNumber = 0,
            targetCaseIndex = 0,
        )
        assertEquals(expectedScore, openQuitDialogAndReadScore())
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

    /**
     * Regression test: selecting a paradigm in the handbook overlay, closing it with a
     * swipe-left, and reopening it with a swipe-right must keep the selection instead of
     * resetting to the masculine "pán" default.
     */
    @Test
    fun handbookSelectionSurvivesSwipeCloseAndReopen() {
        openQuizAndWaitForWord()

        composeTestRule.onNodeWithTag(TestTags.FULL_QUIZ_ROOT).performTouchInput {
            swipe(start = centerLeft, end = centerRight, durationMillis = 300)
        }
        waitForTag(TestTags.FULL_HANDBOOK_OVERLAY)

        waitForText("Feminine")
        composeTestRule.onNodeWithText("Feminine").performClick()
        waitForText("růže")
        composeTestRule.onNodeWithText("růže").performClick()

        composeTestRule.onNodeWithTag(TestTags.FULL_QUIZ_ROOT).performTouchInput {
            swipe(start = centerRight, end = centerLeft, durationMillis = 300)
        }
        composeTestRule.waitUntil(timeoutMillis = 3_000) { !tagExists(TestTags.FULL_HANDBOOK_OVERLAY) }

        composeTestRule.onNodeWithTag(TestTags.FULL_QUIZ_ROOT).performTouchInput {
            swipe(start = centerLeft, end = centerRight, durationMillis = 300)
        }
        waitForTag(TestTags.FULL_HANDBOOK_OVERLAY)

        waitForText("Feminine")
        composeTestRule.onNodeWithText("Feminine").assertIsDisplayed()
        // "růže" also appears in the case table itself, so just confirm the paradigm chip
        // (and thus the whole selection) is still there rather than picking a unique node.
        composeTestRule.onAllNodesWithText("růže").onFirst().assertIsDisplayed()
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

    /**
     * End-to-end scoring scenario: a wrong placement earns nothing, every correct placement
     * immediately awards +3 points, and completing the table awards the +3 completion bonus but
     * *not* the +6 perfect bonus once a mistake was made along the way. Reads the running total
     * off the quit-overlay's score stat after each action, so it verifies against the real
     * persisted `DailyTrainingStatsEntity.score` rather than in-memory state alone.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scoringSystemAwardsAndAccumulatesPointsCorrectly() {
        openQuizAndWaitForWord()
        val word = loadedWord()

        var expectedScore = openQuitDialogAndReadScore()

        // A wrong placement returns to the bank and must not change the score.
        val (wrongPoolIndex, _) = poolFormNotMatchingTarget(word, number = 0, caseIndex = 0)
        dragPoolWordToCell(wrongPoolIndex, number = 0, caseIndex = 0)
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            tagExists("${TestTags.FULL_POOL_WORD_PREFIX}$wrongPoolIndex")
        }
        assertEquals(expectedScore, openQuitDialogAndReadScore())

        // Every correct placement immediately awards +3 points, one form at a time.
        for (caseIndex in 0..6) {
            for (number in 0..1) {
                if (word.cases(number, caseIndex).isNotEmpty()) {
                    val (poolIndex, _) = poolFormForTarget(word, number, caseIndex)
                    dragPoolWordToCell(poolIndex, number, caseIndex)
                    composeTestRule.waitUntil(timeoutMillis = 3_000) {
                        !tagExists("${TestTags.FULL_POOL_WORD_PREFIX}$poolIndex")
                    }
                    expectedScore += 3
                    assertEquals(expectedScore, openQuitDialogAndReadScore())
                }
            }
        }

        // The table is now complete. The completion (and any perfect) bonus is only awarded once
        // the player actually leaves the word via "Next word" - not merely by finishing the
        // table - so the earlier mistake means only the +3 completion bonus applies here.
        composeTestRule.waitUntilAtLeastOneExists(
            hasTestTag(TestTags.FULL_COMPLETION_DIALOG),
            timeoutMillis = 5_000,
        )
        composeTestRule.onNodeWithTag(TestTags.FULL_DIALOG_NEXT_WORD).performClick()
        waitForTag(TestTags.FULL_WORD)
        expectedScore += 3
        assertEquals(expectedScore, openQuitDialogAndReadScore())
    }

    /**
     * Completing a table without any mistakes awards both the completion bonus and the perfect
     * bonus (+3 and +6), on top of the +3 earned per correct placement.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun perfectWordCompletionAwardsPerfectBonus() {
        openQuizAndWaitForWord()
        val word = loadedWord()

        var expectedScore = openQuitDialogAndReadScore()

        for (caseIndex in 0..6) {
            for (number in 0..1) {
                if (word.cases(number, caseIndex).isNotEmpty()) {
                    val (poolIndex, _) = poolFormForTarget(word, number, caseIndex)
                    dragPoolWordToCell(poolIndex, number, caseIndex)
                    composeTestRule.waitUntil(timeoutMillis = 3_000) {
                        !tagExists("${TestTags.FULL_POOL_WORD_PREFIX}$poolIndex")
                    }
                    expectedScore += 3
                }
            }
        }

        composeTestRule.waitUntilAtLeastOneExists(
            hasTestTag(TestTags.FULL_COMPLETION_DIALOG),
            timeoutMillis = 5_000,
        )
        composeTestRule.onNodeWithTag(TestTags.FULL_DIALOG_NEXT_WORD).performClick()
        waitForTag(TestTags.FULL_WORD)
        expectedScore += 3 + 6 // completion bonus + perfect bonus
        assertEquals(expectedScore, openQuitDialogAndReadScore())
    }

    /**
     * Advancing to a new word without finishing the current table (toolbar "Next" action) must
     * dock a 1-point penalty instead of awarding the word/perfect bonuses.
     */
    @Test
    fun skippingWordBeforeCompletionDeductsPenaltyPoint() {
        openQuizAndWaitForWord()
        val word = loadedWord()

        // Earn a couple of points first so the penalty has something to subtract from (the score
        // is clamped at 0, which would otherwise mask the deduction).
        val (poolIndex, _) = poolFormForTarget(word, number = 0, caseIndex = 0)
        dragPoolWordToCell(poolIndex, number = 0, caseIndex = 0)
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            !tagExists("${TestTags.FULL_POOL_WORD_PREFIX}$poolIndex")
        }
        val expectedScore = openQuitDialogAndReadScore() - 1

        composeTestRule.onNodeWithTag(TestTags.NAV_NEXT_BTN).performClick()
        waitForTag(TestTags.FULL_WORD)

        assertEquals(expectedScore, openQuitDialogAndReadScore())
    }

    /** Rapid repeated navigation must still score the current word/penalty exactly once. */
    @Test
    fun doubleTappingNextWordOnlyAppliesOnePenalty() {
        openQuizAndWaitForWord()
        val word = loadedWord()
        val (poolIndex, _) = poolFormForTarget(word, number = 0, caseIndex = 0)
        dragPoolWordToCell(poolIndex, number = 0, caseIndex = 0)
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            !tagExists("${TestTags.FULL_POOL_WORD_PREFIX}$poolIndex")
        }
        val expectedScore = openQuitDialogAndReadScore() - 1

        // Two pointer-up events are sent in one gesture, before Compose can recompose the app
        // bar for the next word. This mirrors an actual double tap rather than two deliberate
        // navigations made after the new word is already visible.
        composeTestRule.onNodeWithTag(TestTags.NAV_NEXT_BTN).performTouchInput {
            down(center)
            up()
            down(center)
            up()
        }
        waitForTag(TestTags.FULL_WORD)

        assertEquals(expectedScore, openQuitDialogAndReadScore())
    }

    /**
     * Five wrong placements reach the same limit shown by the error badge, trigger the
     * interstitial policy, and reset the badge once the ad flow completes.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun fiveMistakesDeductPenaltyPoint() {
        openQuizAndWaitForWord()
        val word = loadedWord()
        val expectedScore = openQuitDialogAndReadScore() - 1

        repeat(5) {
            val (poolIndex, _) = poolFormNotMatchingTarget(word, number = 0, caseIndex = 0)
            dragPoolWordToCell(poolIndex, number = 0, caseIndex = 0)
            // The 5th mistake may briefly show a real interstitial ad (if one happened to load),
            // pausing the activity, so give this a longer timeout than a plain wrong-drop bounce.
            composeTestRule.waitUntil(timeoutMillis = 8_000) {
                tagExists("${TestTags.FULL_POOL_WORD_PREFIX}$poolIndex")
            }
        }

        // Wait through the real ad-flow callback rather than blocking the UI thread. Blocking it
        // would prevent the delayed threshold handler itself from applying the penalty/reset.
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            taggedText(TestTags.FULL_ERROR_COUNTER) == "0/5"
        }
        assertEquals(expectedScore, openQuitDialogAndReadScore())
    }

    /**
     * Opens the quit overlay via the toolbar home button, reads the persisted score off the
     * [TestTags.FULL_QUIT_SCORE] stat column, then dismisses the overlay with "Keep going" so the
     * quiz resumes exactly where it was left.
     */
    @OptIn(ExperimentalTestApi::class)
    private fun openQuitDialogAndReadScore(): Int {
        composeTestRule.onNodeWithTag(TestTags.NAV_HOME_BTN).performClick()
        waitForTag(TestTags.FULL_QUIT_DIALOG)
        val score = composeTestRule.onNodeWithTag(TestTags.FULL_QUIT_SCORE)
            .fetchSemanticsNode().children.first().config[SemanticsProperties.Text].single().text.toInt()
        composeTestRule.onNodeWithText("Keep going").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) { !tagExists(TestTags.FULL_QUIT_DIALOG) }
        return score
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

    private fun dragCellToCell(sourceNumber: Int, sourceCaseIndex: Int, targetNumber: Int, targetCaseIndex: Int) {
        val source = composeTestRule.onNodeWithTag("${TestTags.FULL_CELL_PREFIX}${sourceNumber}_$sourceCaseIndex")
        val target = composeTestRule.onNodeWithTag("${TestTags.FULL_CELL_PREFIX}${targetNumber}_$targetCaseIndex")
        val sourceBounds = source.fetchSemanticsNode().boundsInRoot
        val targetBounds = target.fetchSemanticsNode().boundsInRoot
        val sourceOrigin = sourceBounds.topLeft
        val targetPoint = if (sourceNumber == targetNumber && sourceCaseIndex == targetCaseIndex) {
            // Move far enough to exceed touch-slop but remain inside the same target cell.
            targetBounds.topLeft + androidx.compose.ui.geometry.Offset(16f, 16f)
        } else {
            targetBounds.center
        }

        source.performTouchInput {
            swipe(
                start = sourceBounds.center - sourceOrigin,
                end = targetPoint - sourceOrigin,
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
