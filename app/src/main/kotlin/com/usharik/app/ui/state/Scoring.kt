package com.usharik.app.ui.state

/**
 * Point values for the micro-reward scoring system, shared by both quiz modes so scoring stays
 * fair and consistent regardless of whether the player is filling the full declension table or
 * answering one case at a time.
 */
object Scoring {
    /** Awarded for every individually correct form placed/answered. */
    const val POINTS_PER_CORRECT_FORM = 3

    /** Bonus awarded once a word is fully completed, on top of its per-form points. */
    const val POINTS_WORD_COMPLETED = 3

    /** Extra bonus for completing a word with zero mistakes along the way. */
    const val POINTS_PERFECT_BONUS = 6

    /** Extra bonus for clearing a word from the mistakes list in review mode. */
    const val POINTS_REVIEW_CLEARED = 10

    /** Deducted for skipping a word before it was completed. */
    const val POINTS_PENALTY = 1

    /** What a full, mistake-free word (7 cases × 2 numbers) earns. */
    const val PERFECT_WORD_POINTS =
        WordQuestionProgress.FORMS_PER_NUMBER * 2 * POINTS_PER_CORRECT_FORM + POINTS_WORD_COMPLETED + POINTS_PERFECT_BONUS
}
