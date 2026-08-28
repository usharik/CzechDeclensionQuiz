package com.usharik.app.ui.state

/**
 * Keeps the per-word scoring invariant for the full-table quiz: each target cell can award its
 * correct-form points at most once. A player may rearrange or return already placed forms without
 * changing the score.
 */
class CorrectPlacementRewards {
    private val rewardedCells = mutableSetOf<Int>()

    /** Returns true only the first time [cellIndex] is correctly filled for this word. */
    fun claim(cellIndex: Int): Boolean = rewardedCells.add(cellIndex)

    /** Starts a fresh scoring scope for a new word (or a deliberate retry). */
    fun reset() = rewardedCells.clear()
}
