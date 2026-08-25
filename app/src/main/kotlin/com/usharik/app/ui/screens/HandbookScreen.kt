package com.usharik.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.usharik.app.App
import com.usharik.app.BuildConfig
import com.usharik.app.R
import com.usharik.app.ui.components.BannerAd
import com.usharik.app.ui.components.CaseTable
import com.usharik.app.ui.theme.AppColors
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
 * Declension handbook. Faithful Compose port of HandbookFragment/ViewModel +
 * handbook_fragment.xml: gender radio row, per-gender paradigm radio row, the "other nouns"
 * hint and a bottom-anchored table of the seven case rows (reusing the row_case cells).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HandbookScreen(app: App) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var gender by remember { mutableStateOf(HandbookGender.MASCULINE) }
    // Each gender group keeps its own checked paradigm, like the three XML RadioGroups.
    var checked by remember { mutableStateOf(mapOf<HandbookGender, String>(HandbookGender.MASCULINE to "pán")) }
    var shownWord by remember { mutableStateOf("pán") }
    var cases by remember { mutableStateOf(Array(2) { Array(7) { "" } }) }

    fun selectWord(word: String) {
        HapticFeedback.light(context)
        checked = checked + (gender to word)
        shownWord = word
        scope.launch { app.dictionaryReady.await(); app.documentRepository.wordInfoByWord(word)?.cases()?.let { cases = it } }
    }

    // Switching the gender re-selects that group's remembered (or default) paradigm so the
    // table and "other nouns" hint always match the visible gender.
    fun selectGender(g: HandbookGender) {
        gender = g
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
            .padding(start = Dimens.spacingXxs, top = Dimens.spacingSm, end = Dimens.spacingXxs),
    ) {
        Text(
            stringResource(R.string.gender_of_noun),
            Modifier.fillMaxWidth().padding(top = Dimens.spacingContent),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = Dimens.textTitle,
            textAlign = TextAlign.Center,
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Dimens.spacingXxs).padding(top = Dimens.spacingXs),
        ) {
            HandbookRadio(stringResource(R.string.masculine), gender == HandbookGender.MASCULINE, Modifier.weight(1f)) {
                selectGender(HandbookGender.MASCULINE)
            }
            HandbookRadio(stringResource(R.string.neuter), gender == HandbookGender.NEUTER, Modifier.weight(1f)) {
                selectGender(HandbookGender.NEUTER)
            }
            HandbookRadio(stringResource(R.string.feminine), gender == HandbookGender.FEMININE, Modifier.weight(1f)) {
                selectGender(HandbookGender.FEMININE)
            }
        }
        Text(
            stringResource(R.string.type_of_declension),
            Modifier.fillMaxWidth().padding(top = Dimens.spacingSm),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = Dimens.textBody,
            textAlign = TextAlign.Center,
        )
        // Natural-width items wrapping onto extra lines so every paradigm word is fully visible.
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = Dimens.spacingXxs).padding(top = Dimens.spacingXs),
            horizontalArrangement = Arrangement.Center,
        ) {
            gender.paradigms.forEach { word ->
                HandbookRadio(word, checked[gender] == word) { selectWord(word) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = Dimens.spacingSm, bottom = Dimens.spacingSm)) {
            Text(
                stringResource(R.string.other_nouns),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = Dimens.textLabel,
            )
            Text(
                otherNouns[shownWord].orEmpty(),
                Modifier.padding(start = Dimens.spacingXxs),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = Dimens.textSmall,
            )
        }
        CaseTable(
            cases,
            Modifier
                .weight(1f)
                .padding(horizontal = Dimens.spacingXxs)
                .padding(top = Dimens.spacingContent, bottom = Dimens.spacingSm),
        )
        BannerAd(
            app,
            BuildConfig.ADMOB_BANNER_AD_UNIT_ID,
            Modifier
                .fillMaxWidth()
                .padding(top = Dimens.spacingSm)
                .heightIn(min = 60.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

/**
 * One selector chip in the original HandbookRadioButton style: no radio circle, rounded-rect
 * background (radio_button_background) — gray fill when checked, outlined surface otherwise.
 */
@Composable
private fun HandbookRadio(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Dimens.cornerLarge)
    Box(
        modifier
            .padding(Dimens.spacingXxs)
            .clip(shape)
            .background(if (selected) AppColors.answerNeutral else MaterialTheme.colorScheme.surface, shape)
            .border(Dimens.strokeThin, AppColors.stroke, shape)
            // selectable (not clickable) so TalkBack announces the radio role and checked state.
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(
                horizontal = Dimens.spacingSm + Dimens.shapeInnerPadding,
                vertical = Dimens.spacingXs + Dimens.shapeInnerPadding,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = Dimens.textBody,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
