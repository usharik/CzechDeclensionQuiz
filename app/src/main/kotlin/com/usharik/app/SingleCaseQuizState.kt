package com.usharik.app

import com.usharik.database.WordInfo

/** Mutable session state, owned exclusively by SingleCaseQuizViewModel. */
class SingleCaseQuizState {
    private var wordInfo: WordInfo? = null
    private var currentCase = 0
    private var plural = false
    private var answers: List<String> = emptyList()
    private var correctAnswer = ""
    private var answered = false

    fun getWordInfo() = wordInfo
    fun setWordInfo(value: WordInfo?) { wordInfo = value }
    fun getCurrentCase() = currentCase
    fun setCurrentCase(value: Int) { currentCase = value }
    fun isPlural() = plural
    fun setPlural(value: Boolean) { plural = value }
    fun getAnswers() = answers
    fun setAnswers(value: List<String>?) { answers = value ?: emptyList() }
    fun getCorrectAnswer() = correctAnswer
    fun setCorrectAnswer(value: String?) { correctAnswer = value.orEmpty() }
    fun isAnswered() = answered
    fun setAnswered(value: Boolean) { answered = value }
}
