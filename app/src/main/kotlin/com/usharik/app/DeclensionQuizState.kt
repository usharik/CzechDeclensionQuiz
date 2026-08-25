package com.usharik.app

import com.usharik.database.WordInfo

/** Mutable session state, owned exclusively by DeclensionQuizViewModel. */
class DeclensionQuizState {
    private var wordInfo: WordInfo? = null
    private var wordTextModels = arrayOfNulls<WordTextModel>(14)
    private val correctAnswers = Array(2) { arrayOfNulls<String>(7) }
    private val actualAnswers = Array(2) { IntArray(7) { -1 } }
    private var wrongAttempts = 0

    fun getWordInfo() = wordInfo
    fun setWordInfo(value: WordInfo?) { wordInfo = value }
    fun getWordTextModels(): Array<WordTextModel?> = wordTextModels
    fun setWordTextModels(value: Array<WordTextModel?>) { wordTextModels = value }
    fun getCorrectAnswers(): Array<Array<String?>> = correctAnswers
    fun getActualAnswers(): Array<IntArray> = actualAnswers
    fun getWrongAttempts() = wrongAttempts
    fun incrementWrongAttempts() { wrongAttempts++ }
    fun resetWrongAttempts() { wrongAttempts = 0 }

    class WordTextModel(@JvmField val word: String, @JvmField var visible: Int) {
        fun getWord() = word
        fun getVisible() = visible
        fun setVisible(value: Int) { visible = value }
    }
}
