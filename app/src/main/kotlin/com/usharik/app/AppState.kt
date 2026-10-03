package com.usharik.app

import com.usharik.app.ui.state.DailyGoal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Application-wide state with Compose-ready observable state. */
class AppState {
    private val _wordsWithErrors = MutableStateFlow<Map<String, Int>>(emptyMap())
    val wordsWithErrorsFlow: StateFlow<Map<String, Int>> = _wordsWithErrors.asStateFlow()

    private val _genderFilter = MutableStateFlow(Gender.ALL)
    val genderFilterFlow: StateFlow<String> = _genderFilter.asStateFlow()

    private val _dailyGoal = MutableStateFlow(DailyGoal.DEFAULT.points)
    val dailyGoalFlow: StateFlow<Int> = _dailyGoal.asStateFlow()

    private val _switchOffAnimation = MutableStateFlow(false)
    val switchOffAnimationFlow: StateFlow<Boolean> = _switchOffAnimation.asStateFlow()

    // Which word class the hub's quiz modes drill (nouns / adjectives / verbs); persisted in preferences.
    private val _partOfSpeech = MutableStateFlow(PartOfSpeech.NOUN)
    val partOfSpeechFlow: StateFlow<PartOfSpeech> = _partOfSpeech.asStateFlow()

    // Handbook tab (nouns / adjectives / verbs) and the selection remembered inside each tab.
    private val _handbookPartOfSpeech = MutableStateFlow(PartOfSpeech.NOUN)
    val handbookPartOfSpeechFlow: StateFlow<PartOfSpeech> = _handbookPartOfSpeech.asStateFlow()
    private val _handbookAdjective = MutableStateFlow(DEFAULT_HANDBOOK_ADJECTIVE)
    val handbookAdjectiveFlow: StateFlow<String> = _handbookAdjective.asStateFlow()
    private val _handbookAdjectiveGender = MutableStateFlow(0)
    val handbookAdjectiveGenderFlow: StateFlow<Int> = _handbookAdjectiveGender.asStateFlow()
    private val _handbookVerb = MutableStateFlow(DEFAULT_HANDBOOK_VERB)
    val handbookVerbFlow: StateFlow<String> = _handbookVerb.asStateFlow()

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
    fun getDailyGoal(): Int = _dailyGoal.value
    fun setDailyGoal(points: Int) { _dailyGoal.value = DailyGoal.Level.fromPoints(points).points }
    fun getHandbookGender(): String = _handbookGender.value
    fun getPartOfSpeech(): PartOfSpeech = _partOfSpeech.value
    fun setPartOfSpeech(value: PartOfSpeech) { _partOfSpeech.value = value }
    fun getHandbookPartOfSpeech(): PartOfSpeech = _handbookPartOfSpeech.value
    fun setHandbookPartOfSpeech(value: PartOfSpeech) { _handbookPartOfSpeech.value = value }
    fun getHandbookAdjective(): String = _handbookAdjective.value
    fun setHandbookAdjective(value: String) { _handbookAdjective.value = value }
    fun getHandbookAdjectiveGender(): Int = _handbookAdjectiveGender.value
    fun setHandbookAdjectiveGender(value: Int) { _handbookAdjectiveGender.value = value }
    fun getHandbookVerb(): String = _handbookVerb.value
    fun setHandbookVerb(value: String) { _handbookVerb.value = value }
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
        const val DEFAULT_HANDBOOK_ADJECTIVE = "mladý"
        const val DEFAULT_HANDBOOK_VERB = "dělat"
    }
}
