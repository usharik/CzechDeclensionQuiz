package com.usharik.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.usharik.app.App
import com.usharik.app.BuildConfig
import com.usharik.app.PartOfSpeech
import com.usharik.app.R
import com.usharik.app.TestTags
import com.usharik.app.ui.components.BannerAd
import com.usharik.app.ui.components.CaseTable
import com.usharik.app.ui.state.FormTable
import com.usharik.app.ui.state.FormTables
import com.usharik.app.ui.theme.Dimens
import com.usharik.app.utils.HapticFeedback
import com.usharik.database.AdjectiveInfo
import kotlinx.coroutines.launch

private enum class HandbookGender(val paradigms: List<String>) {
    MASCULINE(listOf("pán", "hrad", "muž", "stroj", "předseda", "soudce")),
    NEUTER(listOf("město", "moře", "kuře", "stavení")),
    FEMININE(listOf("žena", "růže", "píseň", "kost")),
}

// Mirrors HandbookViewModel.otherNouns.
private val otherNouns = mapOf(
    "pán" to "syn, pes, doktor",
    "hrad" to "dům, rok, hotel",
    "muž" to "lékař, řidič, strýc",
    "stroj" to "konec, čaj, nůž",
    "předseda" to "děda, Jirka, Honza",
    "soudce" to "poradce",
    "město" to "auto, okno, jablko, zrcadlo",
    "moře" to "pole, nebe",
    "kuře" to "dítě, štěně, kotě, tele",
    "stavení" to "nádraží, náměstí, září, umění",
    "žena" to "kniha, matka, třída, houska",
    "růže" to "večeře, historie",
    "píseň" to "povodeň, pláž, loď",
    "kost" to "radost, starost",
)

/** Model adjective per declension type, with other adjectives of that type and the grammar tip to show. */
private data class AdjectiveModel(val word: String, val others: String, @StringRes val tip: Int)
private val adjectiveModels = listOf(
    AdjectiveModel("mladý", "nový, dobrý, velký, starý, hezký", R.string.tip_adj_hard),
    AdjectiveModel("jarní", "cizí, moderní, poslední, hlavní, další", R.string.tip_adj_soft),
    AdjectiveModel("otcův", "bratrův, dědečkův, kapitánův", R.string.tip_adj_possessive),
    AdjectiveModel("matčin", "sestřin, babiččin", R.string.tip_adj_possessive),
)

/** Model verb per conjugation group (plus the irregular verbs), with other verbs of that group and a tip. */
private data class VerbModel(val word: String, val others: String, @StringRes val tip: Int)
private val verbModels = listOf(
    VerbModel("dělat", "hledat, čekat, znát, dávat, říkat", R.string.tip_verb_dela),
    VerbModel("prosit", "mluvit, vidět, myslet, učit se, umět", R.string.tip_verb_prosi),
    VerbModel("kupovat", "pracovat, potřebovat, studovat, děkovat", R.string.tip_verb_kupuje),
    VerbModel("tisknout", "sednout si, začít, vzpomenout si, zapomenout", R.string.tip_verb_tiskne),
    VerbModel("nést", "číst, psát, pít, jet, brát", R.string.tip_verb_nese),
    VerbModel("být", "", R.string.tip_verb_irregular),
    VerbModel("mít", "", R.string.tip_past_tense),
    VerbModel("jít", "přijít, odejít, najít", R.string.tip_verb_irregular),
    VerbModel("chtít", "", R.string.tip_verb_irregular),
    VerbModel("vědět", "", R.string.tip_verb_irregular),
    VerbModel("jíst", "sníst", R.string.tip_verb_irregular),
    VerbModel("moct", "pomoct", R.string.tip_verb_irregular),
)

/**
 * Grammar handbook with three tabs. Nouns: gender switch, paradigm chips, "other nouns" and the
 * declension table. Adjectives: the hard / soft / possessive model words, a gender switch and the
 * adjective's table for that gender. Verbs: a model verb per conjugation group plus the irregular
 * verbs, each with its present / past / imperative table and a short grammar tip.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HandbookScreen(app: App) {
    val context = LocalContext.current
    // Tab selection is persisted in AppState (not just `remember`ed) so it survives this screen
    // being torn down and recreated - e.g. swiping the quiz's handbook overlay closed and reopening it.
    var partOfSpeech by remember { mutableStateOf(app.appState.getHandbookPartOfSpeech()) }

    LaunchedEffect(Unit) { app.analyticsService.logHandbookOpen() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.spacingSm)
            .padding(top = Dimens.spacingXs),
    ) {
        val tabs = PartOfSpeech.entries.filter { it.hasHandbook }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, pos ->
                SegmentedButton(
                    selected = partOfSpeech == pos,
                    onClick = { HapticFeedback.light(context); partOfSpeech = pos; app.appState.setHandbookPartOfSpeech(pos) },
                    shape = SegmentedButtonDefaults.itemShape(index, tabs.size),
                    label = { Text(stringResource(pos.shortTitleRes), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag(TestTags.HANDBOOK_POS_PREFIX + pos.name.lowercase()),
                )
            }
        }
        when (partOfSpeech) {
            PartOfSpeech.NOUN -> NounHandbook(app, Modifier.weight(1f))
            PartOfSpeech.ADJECTIVE -> AdjectiveHandbook(app, Modifier.weight(1f))
            PartOfSpeech.VERB -> VerbHandbook(app, Modifier.weight(1f))
            PartOfSpeech.PHRASE -> AdjectiveHandbook(app, Modifier.weight(1f))
        }
        BannerAd(app, BuildConfig.ADMOB_BANNER_AD_UNIT_ID)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun NounHandbook(app: App, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var gender by remember { mutableStateOf(HandbookGender.valueOf(app.appState.getHandbookGender())) }
    // Each gender group keeps its own checked paradigm.
    var checked by remember {
        mutableStateOf(app.appState.getHandbookParadigmByGender().mapKeys { (g, _) -> HandbookGender.valueOf(g) })
    }
    var shownWord by remember { mutableStateOf(checked[gender] ?: gender.paradigms.first()) }
    var table by remember { mutableStateOf<FormTable?>(null) }

    fun selectWord(word: String) {
        HapticFeedback.light(context)
        checked = checked + (gender to word)
        shownWord = word
        app.appState.setHandbookParadigm(gender.name, word)
        scope.launch { app.dictionaryReady.await(); app.documentRepository.wordInfoByWord(word)?.let { table = FormTables.noun(it) } }
    }

    // Switching the gender re-selects that group's remembered (or default) paradigm so the
    // table and "other nouns" hint always match the visible gender.
    fun selectGender(g: HandbookGender) {
        gender = g
        app.appState.setHandbookGender(g.name)
        selectWord(checked[g] ?: g.paradigms.first())
    }

    LaunchedEffect(Unit) {
        app.dictionaryReady.await()
        app.documentRepository.wordInfoByWord(shownWord)?.let { table = FormTables.noun(it) }
    }

    Column(modifier) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = Dimens.spacingSm)) {
            val genders = listOf(
                HandbookGender.MASCULINE to R.string.masculine,
                HandbookGender.NEUTER to R.string.neuter,
                HandbookGender.FEMININE to R.string.feminine,
            )
            genders.forEachIndexed { index, (g, label) ->
                SegmentedButton(
                    selected = gender == g,
                    onClick = { selectGender(g) },
                    shape = SegmentedButtonDefaults.itemShape(index, genders.size),
                    label = { Text(stringResource(label), maxLines = 1) },
                )
            }
        }
        ChipRow(stringResource(R.string.type_of_declension), gender.paradigms, checked[gender], ::selectWord)
        Hint("${stringResource(R.string.other_nouns)} ${otherNouns[shownWord].orEmpty()}")
        table?.let { CaseTable(it, Modifier.weight(1f).padding(top = Dimens.spacingXs, bottom = Dimens.spacingSm)) }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AdjectiveHandbook(app: App, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var word by remember { mutableStateOf(app.appState.getHandbookAdjective()) }
    var gender by remember { mutableStateOf(app.appState.getHandbookAdjectiveGender()) }
    var info by remember { mutableStateOf<AdjectiveInfo?>(null) }
    val model = adjectiveModels.firstOrNull { it.word == word } ?: adjectiveModels.first()

    fun load(w: String) {
        scope.launch { app.dictionaryReady.await(); info = app.documentRepository.adjectiveByWord(w) }
    }
    fun selectWord(w: String) {
        HapticFeedback.light(context)
        word = w
        app.appState.setHandbookAdjective(w)
        load(w)
    }
    LaunchedEffect(Unit) { load(word) }

    Column(modifier) {
        ChipRow(stringResource(R.string.type_of_adjective), adjectiveModels.map { it.word }, word, ::selectWord)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = Dimens.spacingXs)) {
            val labels = listOf(
                R.string.gender_masculine_animate_short, R.string.gender_masculine_inanimate_short,
                R.string.gender_feminine_short, R.string.gender_neuter_short,
            )
            labels.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = gender == index,
                    onClick = { HapticFeedback.light(context); gender = index; app.appState.setHandbookAdjectiveGender(index) },
                    shape = SegmentedButtonDefaults.itemShape(index, labels.size),
                    label = { Text(stringResource(label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
        Hint("${stringResource(R.string.other_adjectives)} ${model.others}")
        Hint(stringResource(model.tip))
        info?.let { CaseTable(FormTables.adjectiveParadigm(it, gender), Modifier.weight(1f).padding(top = Dimens.spacingXs, bottom = Dimens.spacingSm)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VerbHandbook(app: App, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var word by remember { mutableStateOf(app.appState.getHandbookVerb()) }
    var table by remember { mutableStateOf<FormTable?>(null) }
    val model = verbModels.firstOrNull { it.word == word } ?: verbModels.first()

    fun load(w: String) {
        scope.launch { app.dictionaryReady.await(); app.documentRepository.verbByWord(w)?.let { table = FormTables.verb(it) } }
    }
    fun selectWord(w: String) {
        HapticFeedback.light(context)
        word = w
        app.appState.setHandbookVerb(w)
        load(w)
    }
    LaunchedEffect(Unit) { load(word) }

    Column(modifier) {
        ChipRow(stringResource(R.string.type_of_conjugation), verbModels.map { it.word }, word, ::selectWord)
        if (model.others.isNotEmpty()) Hint("${stringResource(R.string.other_verbs)} ${model.others}")
        Hint(stringResource(model.tip))
        table?.let { CaseTable(it, Modifier.weight(1f).padding(top = Dimens.spacingXs, bottom = Dimens.spacingSm)) }
    }
}

/** A labelled row of natural-width chips wrapping onto extra lines so every model word is fully visible. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(label: String, words: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Text(
        label,
        Modifier.padding(top = Dimens.spacingSmLarge, start = Dimens.spacingXs),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
    )
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm),
    ) {
        words.forEach { word ->
            FilterChip(
                selected = selected == word,
                onClick = { onSelect(word) },
                label = { Text(word, fontSize = Dimens.textBody) },
            )
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        Modifier.padding(start = Dimens.spacingXs, top = Dimens.spacingXxs, bottom = Dimens.spacingXs),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = Dimens.textLabel,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}
