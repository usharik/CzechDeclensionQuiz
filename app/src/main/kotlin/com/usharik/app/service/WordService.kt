package com.usharik.app.service

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.usharik.app.AppState
import com.usharik.app.Gender
import com.usharik.app.PartOfSpeech
import com.usharik.app.ui.state.Lexeme
import com.usharik.database.AdjectiveInfo
import com.usharik.database.DocumentRepository
import com.usharik.database.WordInfo
import kotlinx.coroutines.CancellationException
import kotlin.random.Random

/**
 * Picks the lexeme (noun, adjective + noun, or verb) for the next quiz round and resolves persisted
 * keys (mistakes list, last word) back into lexemes.
 */
class WordService(
    private val documentRepository: DocumentRepository,
    private val appState: AppState,
    private val analyticsService: FirebaseAnalyticsService,
) {
    suspend fun wordByName(word: String): WordInfo? = documentRepository.wordInfoByWord(word)

    /** The lexeme behind a persisted key (see [PartOfSpeech.key]), or null when it is no longer in the dictionary. */
    suspend fun lexemeByKey(key: String): Lexeme? {
        val word = PartOfSpeech.wordOfKey(key)
        return when (PartOfSpeech.ofKey(key)) {
            PartOfSpeech.NOUN -> documentRepository.wordInfoByWord(word)?.let { Lexeme.Noun(it) }
            PartOfSpeech.ADJECTIVE -> documentRepository.adjectiveByWord(word)?.let { withAgreementNoun(it, null) }
            PartOfSpeech.VERB -> documentRepository.verbByWord(word)?.let { Lexeme.Verb(it) }
            PartOfSpeech.PHRASE -> {
                // "mladý muž": neither adjectives nor nouns contain spaces, so the first space splits the pair.
                val adjective = documentRepository.adjectiveByWord(word.substringBefore(' ')) ?: return null
                val noun = documentRepository.wordInfoByWord(word.substringAfter(' ', "")) ?: return null
                val gender = AdjectiveInfo.genderIndex(noun.gender()) ?: return null
                Lexeme.Phrase(adjective, noun, gender)
            }
        }
    }

    /** The next lexeme of [partOfSpeech] after [current]: half of the time a word from the mistakes list, otherwise a fresh one. */
    suspend fun nextLexeme(partOfSpeech: PartOfSpeech, current: Lexeme?): Lexeme {
        return try {
            val fromErrors = if (Random.nextBoolean()) randomErrorLexeme(partOfSpeech, current) else null
            (fromErrors ?: freshLexeme(partOfSpeech, current)).also {
                Log.i(javaClass.name, "New word is ${it.key}")
                analyticsService.logNextWord(it.key)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(javaClass.name, "Error getting next word", error)
            FirebaseCrashlytics.getInstance().recordException(error)
            throw error
        }
    }

    private suspend fun freshLexeme(partOfSpeech: PartOfSpeech, current: Lexeme?): Lexeme = when (partOfSpeech) {
        PartOfSpeech.NOUN -> {
            val genderFilter = appState.getGenderFilterStr().takeIf { it != Gender.ALL }
            val currentType = (current as? Lexeme.Noun)?.info?.declensionType().orEmpty()
            Lexeme.Noun(documentRepository.randomWordWithAnotherDeclensionType(currentType, genderFilter))
        }
        PartOfSpeech.ADJECTIVE -> {
            val previous = current as? Lexeme.Adjective
            // Alternate hard and soft paradigms so consecutive rounds don't feel identical.
            val kind = when (previous?.info?.kind()) { "tvrdé" -> "měkké"; "měkké" -> "tvrdé"; else -> null }
            withAgreementNoun(documentRepository.randomAdjective(previous?.headword.orEmpty(), kind), previous?.gender)
        }
        PartOfSpeech.PHRASE -> {
            val previous = current as? Lexeme.Phrase
            val kind = when (previous?.adjective?.kind()) { "tvrdé" -> "měkké"; else -> "tvrdé" }
            val adjective = documentRepository.randomAdjective(previous?.adjective?.word().orEmpty(), kind)
            val partner = withAgreementNoun(adjective, previous?.gender)
            Lexeme.Phrase(adjective, partner.noun, partner.gender)
        }
        PartOfSpeech.VERB -> {
            val previous = current as? Lexeme.Verb
            // Prefer a different conjugation group than the previous verb; the DAO falls back to any group.
            val otherClass = previous?.info?.verbClass()?.let { prev -> AGREEMENT_VERB_CLASSES.filter { it != prev }.random() }
            Lexeme.Verb(documentRepository.randomVerb(previous?.headword.orEmpty(), otherClass))
        }
    }

    /**
     * Pairs an adjective with a noun to agree with: a random gender other than [previousGender]
     * and one of the everyday nouns of that gender (all of which exist in the noun dictionary).
     * The gender filter from the settings narrows the choice the same way it narrows noun rounds.
     */
    suspend fun withAgreementNoun(adjective: AdjectiveInfo, previousGender: Int?): Lexeme.Adjective {
        val filter = appState.getGenderFilterStr().takeIf { it != Gender.ALL }?.let(AdjectiveInfo::genderIndex)
        val genders = if (filter != null) listOf(filter) else AdjectiveInfo.GENDERS.indices.filter { it != previousGender }
        val gender = genders.random()
        for (candidate in AGREEMENT_NOUNS.getValue(gender).shuffled()) {
            val noun = documentRepository.wordInfoByWord(candidate) ?: continue
            if (AdjectiveInfo.genderIndex(noun.gender()) == gender) return Lexeme.Adjective(adjective, noun, gender)
        }
        // Unreachable with the bundled dictionary; a synthetic blank noun keeps the quiz running.
        return Lexeme.Adjective(adjective, WordInfo(null, "", Array(2) { Array(7) { "" } }, "", "", AdjectiveInfo.GENDERS[gender], ""), gender)
    }

    /**
     * Next lexeme for review mode: a random key from the mistakes list, avoiding an immediate
     * repeat while other words remain. Null once the list is empty (everything was reviewed).
     */
    suspend fun nextReviewLexeme(current: Lexeme?): Lexeme? {
        val keys = appState.getWordsWithErrors().keys
        val candidates = keys.filter { it != current?.key }.ifEmpty { keys.toList() }.shuffled()
        for (key in candidates) {
            lexemeByKey(key)?.let { return it }
            appState.removeWordFromErrorMap(key)
        }
        return null
    }

    private suspend fun randomErrorLexeme(partOfSpeech: PartOfSpeech, current: Lexeme?): Lexeme? {
        val key = appState.wordsWithErrorsFlow.value.keys.filter { PartOfSpeech.ofKey(it) == partOfSpeech }.randomOrNull() ?: return null
        val lexeme = lexemeByKey(key)
        val genderFilter = appState.getGenderFilterStr().takeIf { it != Gender.ALL }
        return when {
            lexeme == null -> { appState.removeWordFromErrorMap(key); null }
            lexeme.key == current?.key -> null
            lexeme is Lexeme.Noun && genderFilter != null && lexeme.info.gender() != genderFilter -> null
            else -> lexeme
        }
    }

    companion object {
        /**
         * Everyday nouns used as agreement partners for adjectives, by [AdjectiveInfo.GENDERS] index.
         * They cover the main paradigms (pán/muž, hrad/stroj, žena/růže/píseň/kost, město/moře/kuře/stavení).
         */
        val AGREEMENT_NOUNS: Map<Int, List<String>> = mapOf(
            AdjectiveInfo.GENDER_MASCULINE_ANIMATE to listOf("muž", "pán", "student", "kamarád", "kluk", "učitel", "člověk", "pes"),
            AdjectiveInfo.GENDER_MASCULINE_INANIMATE to listOf("dům", "stůl", "hrad", "strom", "obchod", "stroj", "les", "byt"),
            AdjectiveInfo.GENDER_FEMININE to listOf("žena", "kniha", "ulice", "růže", "píseň", "věc", "škola", "holka"),
            AdjectiveInfo.GENDER_NEUTER to listOf("město", "auto", "moře", "kuře", "stavení", "dítě", "místo"),
        )
        private val AGREEMENT_VERB_CLASSES = listOf("dělá", "prosí", "kupuje", "tiskne", "nese", "nepravidelné")
    }
}
