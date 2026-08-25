package com.usharik.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Application-wide state with Compose-ready observable state. */
class AppState {
    private val _wordsWithErrors = MutableStateFlow<Map<String, Int>>(emptyMap())
    val wordsWithErrorsFlow: StateFlow<Map<String, Int>> = _wordsWithErrors.asStateFlow()

    private val _genderFilter = MutableStateFlow(Gender.ALL)
    val genderFilterFlow: StateFlow<String> = _genderFilter.asStateFlow()

    private val _switchOffAnimation = MutableStateFlow(false)
    val switchOffAnimationFlow: StateFlow<Boolean> = _switchOffAnimation.asStateFlow()

    fun getWordsWithErrors(): Map<String, Int> = _wordsWithErrors.value
    fun getGenderFilterStr(): String = _genderFilter.value
    fun getSwitchOffAnimation(): Boolean = _switchOffAnimation.value
    fun setWordsWithErrors(value: Map<String, Int>?) { _wordsWithErrors.value = value?.toMap().orEmpty() }
    fun setSwitchOffAnimation(value: Boolean) { _switchOffAnimation.value = value }
    fun setGenderFilterStr(value: String?) { _genderFilter.value = value ?: Gender.ALL }
    fun putWordToErrorMap(word: String?, errorCount: Int) {
        if (!word.isNullOrBlank()) _wordsWithErrors.value = _wordsWithErrors.value + (word to errorCount)
    }
    fun removeWordFromErrorMap(word: String?) {
        if (!word.isNullOrBlank()) _wordsWithErrors.value = _wordsWithErrors.value - word
    }
}
