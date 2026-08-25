package com.usharik.app.service

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.usharik.app.AppState
import com.usharik.app.Gender
import com.usharik.database.DocumentRepository
import com.usharik.database.WordInfo
import kotlin.random.Random

class WordService(
    private val documentRepository: DocumentRepository,
    private val appState: AppState,
    private val analyticsService: FirebaseAnalyticsService,
) {
    suspend fun wordByName(word: String): WordInfo? = documentRepository.wordInfoByWord(word)

    suspend fun nextWord(currentWord: WordInfo?): WordInfo {
        return try {
            val previous = currentWord?.word().orEmpty()
            val genderFilter = appState.getGenderFilterStr().takeIf { it != Gender.ALL }
            val fromErrors = if (Random.nextBoolean()) randomErrorWord(previous, genderFilter) else null
            (fromErrors ?: documentRepository.randomWordWithAnotherDeclensionType(currentWord?.declensionType().orEmpty(), genderFilter)).also {
                Log.i(javaClass.name, "New word is ${it.word()}")
                analyticsService.logNextWord(it.word())
            }
        } catch (error: Throwable) {
            Log.e(javaClass.name, "Error getting next word", error)
            FirebaseCrashlytics.getInstance().recordException(error)
            throw error
        }
    }

    private suspend fun randomErrorWord(previous: String, genderFilter: String?): WordInfo? {
        val key = appState.wordsWithErrorsFlow.value.keys.randomOrNull() ?: return null
        val word = documentRepository.wordInfoByWord(key)
        return when {
            word == null -> { appState.removeWordFromErrorMap(key); null }
            word.word() == previous -> null
            genderFilter != null && word.gender() != genderFilter -> null
            else -> word
        }
    }

}
