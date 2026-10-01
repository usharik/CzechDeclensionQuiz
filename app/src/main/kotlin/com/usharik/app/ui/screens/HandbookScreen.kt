package com.usharik.app.ui.screens

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
import androidx.compose.ui.res.stringResource
import com.usharik.app.App
import com.usharik.app.BuildConfig
import com.usharik.app.R
import com.usharik.app.ui.components.BannerAd
import com.usharik.app.ui.components.CaseTable
import com.usharik.app.ui.theme.Dimens
import com.usharik.app.utils.HapticFeedback
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

/**
 * Declension handbook: a gender switch, the per-gender paradigm chips, the "other nouns" hint
 * and a bottom-anchored table of the seven case rows.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HandbookScreen(app: App) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Selection is persisted in AppState (not just `remember`ed) so it survives this screen
    // being torn down and recreated - e.g. swiping the quiz's handbook overlay closed and
    // reopening it - instead of resetting to the masculine "pán" default every time.
    var gender by remember { mutableStateOf(HandbookGender.valueOf(app.appState.getHandbookGender())) }
    // Each gender group keeps its own checked paradigm.
    var checked by remember {
        mutableStateOf(app.appState.getHandbookParadigmByGender().mapKeys { (g, _) -> HandbookGender.valueOf(g) })
    }
    var shownWord by remember { mutableStateOf(checked[gender] ?: gender.paradigms.first()) }
    var cases by remember { mutableStateOf(Array(2) { Array(7) { "" } }) }

    fun selectWord(word: String) {
        HapticFeedback.light(context)
        checked = checked + (gender to word)
        shownWord = word
        app.appState.setHandbookParadigm(gender.name, word)
        scope.launch { app.dictionaryReady.await(); app.documentRepository.wordInfoByWord(word)?.cases()?.let { cases = it } }
    }

    // Switching the gender re-selects that group's remembered (or default) paradigm so the
    // table and "other nouns" hint always match the visible gender.
    fun selectGender(g: HandbookGender) {
        gender = g
        app.appState.setHandbookGender(g.name)
        selectWord(checked[g] ?: g.paradigms.first())
    }

    LaunchedEffect(Unit) {
        app.analyticsService.logHandbookOpen()
        app.dictionaryReady.await()
        app.documentRepository.wordInfoByWord(shownWord)?.cases()?.let { cases = it }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.spacingSm)
            .padding(top = Dimens.spacingXs),
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
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
        Text(
            stringResource(R.string.type_of_declension),
            Modifier.padding(top = Dimens.spacingSmLarge, start = Dimens.spacingXs),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
        // Natural-width chips wrapping onto extra lines so every paradigm word is fully visible.
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm),
        ) {
            gender.paradigms.forEach { word ->
                FilterChip(
                    selected = checked[gender] == word,
                    onClick = { selectWord(word) },
                    label = { Text(word, fontSize = Dimens.textBody) },
                )
            }
        }
        Text(
            "${stringResource(R.string.other_nouns)} ${otherNouns[shownWord].orEmpty()}",
            Modifier.padding(start = Dimens.spacingXs, bottom = Dimens.spacingXs),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = Dimens.textLabel,
        )
        CaseTable(
            cases,
            Modifier
                .weight(1f)
                .padding(top = Dimens.spacingXs, bottom = Dimens.spacingSm),
        )
        BannerAd(app, BuildConfig.ADMOB_BANNER_AD_UNIT_ID)
    }
}
