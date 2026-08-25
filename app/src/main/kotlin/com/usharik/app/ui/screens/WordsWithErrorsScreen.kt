package com.usharik.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.usharik.app.App
import com.usharik.app.BuildConfig
import com.usharik.app.ui.components.BannerAd
import com.usharik.app.ui.components.RowCase
import com.usharik.app.ui.theme.Dimens
import kotlinx.coroutines.launch

/**
 * Words-with-errors review page. Faithful Compose port of WordsWithErrorsFragment/ViewModel +
 * words_with_errors_fragment.xml: a wrapping single-choice chip list of the words the player got
 * wrong (top third of the page) above the static seven-row declension table for the selected word.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordsWithErrorsScreen(app: App) {
    val scope = rememberCoroutineScope()
    val wordsWithErrors by app.appState.wordsWithErrorsFlow.collectAsState()
    var selectedWord by remember { mutableStateOf<String?>(null) }
    var cases by remember { mutableStateOf(Array(2) { Array(7) { "" } }) }

    fun selectWord(word: String) {
        selectedWord = word
        scope.launch { app.documentRepository.wordInfoByWord(word)?.cases()?.let { cases = it } }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(start = Dimens.spacingXxs, top = Dimens.spacingSm, end = Dimens.spacingXxs),
    ) {
        // RecyclerView with FlexboxLayoutManager, constrained to the top-third guideline.
        FlowRow(
            Modifier
                .fillMaxWidth()
                .weight(0.33f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.spacingXs)
                .padding(top = Dimens.spacingXs, bottom = Dimens.spacingSm),
        ) {
            wordsWithErrors.keys.forEach { word ->
                FilterChip(
                    selected = selectedWord == word,
                    onClick = { selectWord(word) },
                    label = { Text(word) },
                    modifier = Modifier.padding(Dimens.spacingXs).heightIn(min = 40.dp),
                )
            }
        }
        // ScrollView with the packed-to-bottom chain of the seven case rows.
        Column(
            Modifier
                .weight(0.67f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.spacingXxs)
                .padding(top = Dimens.spacingXs, bottom = Dimens.spacingXxs),
            verticalArrangement = Arrangement.Bottom,
        ) {
            for (i in 0 until 7) {
                RowCase(
                    num = i,
                    dnd = null,
                    singularText = cases[0][i],
                    pluralText = cases[1][i],
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.spacingContent, bottom = if (i == 6) Dimens.spacingSmLarge else 0.dp),
                )
            }
        }
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
