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

    // Handbook selection, kept here (rather than remembered inside HandbookScreen) so it
    // survives the screen being torn down and recreated - e.g. swiping the quiz's handbook
    // overlay closed and reopening it, or navigating away from the standalone Handbook
    // destination and back - instead of resetting to the masculine "pán" default each time.
    private val _handbookGender = MutableStateFlow(DEFAULT_HANDBOOK_GENDER)
    val handbookGenderFlow: StateFlow<String> = _handbookGender.asStateFlow()
    private val _handbookParadigmByGender = MutableStateFlow(mapOf(DEFAULT_HANDBOOK_GENDER to DEFAULT_HANDBOOK_PARADIGM))
    val handbookParadigmByGenderFlow: StateFlow<Map<String, String>> = _handbookParadigmByGender.asStateFlow()

    fun getWordsWithErrors(): Map<String, Int> = _wordsWithErrors.value
    fun getGenderFilterStr(): String = _genderFilter.value
    fun getSwitchOffAnimation(): Boolean = _switchOffAnimation.value
    fun getHandbookGender(): String = _handbookGender.value
    fun getHandbookParadigmByGender(): Map<String, String> = _handbookParadigmByGender.value
    fun setWordsWithErrors(value: Map<String, Int>?) { _wordsWithErrors.value = value?.toMap().orEmpty() }
    fun setSwitchOffAnimation(value: Boolean) { _switchOffAnimation.value = value }
    fun setGenderFilterStr(value: String?) { _genderFilter.value = value ?: Gender.ALL }
    fun setHandbookGender(value: String) { _handbookGender.value = value }
    fun setHandbookParadigm(gender: String, paradigm: String) {
        _handbookParadigmByGender.value = _handbookParadigmByGender.value + (gender to paradigm)
    }
    fun putWordToErrorMap(word: String?, errorCount: Int) {
        if (!word.isNullOrBlank()) _wordsWithErrors.value = _wordsWithErrors.value + (word to errorCount)
    }
    fun removeWordFromErrorMap(word: String?) {
        if (!word.isNullOrBlank()) _wordsWithErrors.value = _wordsWithErrors.value - word
    }

    companion object {
        const val DEFAULT_HANDBOOK_GENDER = "MASCULINE"
        const val DEFAULT_HANDBOOK_PARADIGM = "pán"
    }
}
