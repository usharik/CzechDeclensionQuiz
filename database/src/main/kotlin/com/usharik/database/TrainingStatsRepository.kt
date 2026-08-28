package com.usharik.database

import com.usharik.database.dao.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TrainingStatsRepository(db: DocumentDatabase) {
    private val dao = db.trainingStatsDao()
    private fun today() = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    suspend fun incrementWordsCompleted() = dao.incrementWordsCompleted(today(), System.currentTimeMillis())
    suspend fun incrementExercisesCompleted() = dao.incrementExercisesCompleted(today(), System.currentTimeMillis())
    suspend fun incrementErrorsCount() = dao.incrementErrorsCount(today(), System.currentTimeMillis())
    suspend fun addScorePoints(points: Int) = dao.addScorePoints(today(), points, System.currentTimeMillis())
    suspend fun todayStats(): DailyTrainingStatsEntity? = dao.statsByDate(today())
    suspend fun statsForDate(date: String): DailyTrainingStatsEntity? = dao.statsByDate(date)
    suspend fun reminderState(): ReminderStateEntity? = dao.reminderState()
    suspend fun saveReminderState(state: ReminderStateEntity) = dao.upsertReminderState(state)
    suspend fun recentWords(): List<String> = dao.recentWords()?.words.orEmpty().split(WORD_SEPARATOR).filter { it.isNotEmpty() }
    suspend fun saveRecentWords(words: List<String>) { dao.upsertRecentWords(RecentWordsEntity().also { it.words = words.joinToString(WORD_SEPARATOR) }) }
    private companion object { const val WORD_SEPARATOR = "|||" }

}
