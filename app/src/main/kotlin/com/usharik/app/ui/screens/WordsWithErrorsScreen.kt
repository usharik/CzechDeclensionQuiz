package com.usharik.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.usharik.app.App
import com.usharik.app.BuildConfig
import com.usharik.app.PartOfSpeech
import com.usharik.app.R
import com.usharik.app.TestTags
import com.usharik.app.ui.components.BannerAd
import com.usharik.app.ui.components.CaseTable
import com.usharik.app.ui.components.GradientButton
import com.usharik.app.ui.components.lexemeSubtitle
import com.usharik.app.ui.state.FormTable
import com.usharik.app.ui.state.FormTables
import com.usharik.app.ui.theme.AppColors
import com.usharik.app.ui.theme.Dimens

/**
 * Words-with-errors page: a review-mode shortcut, the chips of the words the player got wrong
 * (nouns, adjectives and verbs, tagged by class) and the form table of the selected one (the
 * first one until another is picked).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordsWithErrorsScreen(app: App, onStartReview: () -> Unit) {
    val wordsWithErrors by app.appState.wordsWithErrorsFlow.collectAsState()
    var selectedKey by remember { mutableStateOf<String?>(null) }
    var table by remember { mutableStateOf<FormTable?>(null) }

    fun select(key: String) {
        selectedKey = key
    }

    LaunchedEffect(wordsWithErrors.keys) {
        if (selectedKey !in wordsWithErrors) wordsWithErrors.keys.firstOrNull()?.let(::select) ?: run { selectedKey = null; table = null }
    }
    LaunchedEffect(selectedKey) {
        table = null
        val key = selectedKey ?: return@LaunchedEffect
        app.dictionaryReady.await()
        table = app.wordService.lexemeByKey(key)?.let(FormTables::of)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.spacingSm)
            .padding(top = Dimens.spacingXs),
    ) {
        if (wordsWithErrors.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().weight(1f).padding(Dimens.spacingLg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("✨", fontSize = 48.sp)
                Text(
                    stringResource(R.string.errors_empty),
                    Modifier.padding(top = Dimens.spacingMd),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = Dimens.textBody,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            GradientButton(
                text = pluralStringResource(R.plurals.progress_review_button, wordsWithErrors.size, wordsWithErrors.size),
                gradient = AppColors.gradientAccent,
                icon = painterResource(R.drawable.ic_star),
                fontSize = Dimens.textBody,
                onClick = onStartReview,
                modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingXs).testTag(TestTags.ERRORS_REVIEW),
            )
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .weight(0.33f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Dimens.spacingXs),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm),
            ) {
                wordsWithErrors.keys.forEach { key ->
                    val tag = when (PartOfSpeech.ofKey(key)) {
                        PartOfSpeech.NOUN, PartOfSpeech.PHRASE -> ""
                        PartOfSpeech.ADJECTIVE -> " · " + stringResource(R.string.pos_tag_adjective)
                        PartOfSpeech.VERB -> " · " + stringResource(R.string.pos_tag_verb)
                    }
                    FilterChip(
                        selected = selectedKey == key,
                        onClick = { select(key) },
                        label = { Text(PartOfSpeech.wordOfKey(key) + tag) },
                    )
                }
            }
            table?.takeIf { it.lexeme.key == selectedKey }?.let {
                Text(
                    lexemeSubtitle(it.lexeme),
                    Modifier.padding(start = Dimens.spacingXs),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = Dimens.textLabel,
                    maxLines = 1,
                )
                CaseTable(
                    it,
                    Modifier
                        .weight(0.67f)
                        .padding(top = Dimens.spacingXs, bottom = Dimens.spacingSm),
                )
            } ?: Spacer(Modifier.weight(0.67f))
        }
        BannerAd(app, BuildConfig.ADMOB_BANNER_AD_UNIT_ID)
    }
}
