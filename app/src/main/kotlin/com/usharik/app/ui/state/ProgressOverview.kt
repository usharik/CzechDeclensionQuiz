package com.usharik.app.ui.state

import com.usharik.database.TrainingStatsRepository
import java.time.LocalDate

/** Everything the hub progress card shows, computed from the per-day stats history. */
data class ProgressOverview(
    val goal: DailyGoal.Progress,
    val streak: Streak.Summary,
    val week: List<DayScore>,
    val reviewCount: Int,
)

suspend fun TrainingStatsRepository.dayScores(): List<DayScore> =
    allStats().mapNotNull { row -> runCatching { DayScore(LocalDate.parse(row.date), row.score) }.getOrNull() }

suspend fun TrainingStatsRepository.progressOverview(goalTarget: Int, reviewCount: Int, today: LocalDate = LocalDate.now()): ProgressOverview {
    val days = dayScores()
    val todayScore = days.firstOrNull { it.date == today }?.score ?: 0
    return ProgressOverview(
        goal = DailyGoal.Progress(todayScore, goalTarget),
        streak = Streak.summarize(days, today),
        week = Streak.lastDays(days, today),
        reviewCount = reviewCount,
    )
}

/** Lifetime practice figures the support offer waits for. */
data class PracticeTotals(val practiceDays: Int, val wordsCompleted: Int, val practicedToday: Boolean)

suspend fun TrainingStatsRepository.practiceTotals(today: LocalDate = LocalDate.now()): PracticeTotals {
    val rows = allStats()
    return PracticeTotals(
        practiceDays = rows.count { Streak.isPracticeDay(it.score) },
        wordsCompleted = rows.sumOf { it.wordsCompleted },
        practicedToday = rows.any { it.date == today.toString() && Streak.isPracticeDay(it.score) },
    )
}
