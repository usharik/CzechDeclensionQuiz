package com.usharik.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.usharik.app.App
import com.usharik.app.BuildConfig
import com.usharik.app.R
import com.usharik.app.TestTags
import com.usharik.app.ui.components.BannerAd
import com.usharik.app.ui.components.CellFeedback
import com.usharik.app.ui.components.DragOverlay
import com.usharik.app.ui.components.DragAndDropState
import com.usharik.app.ui.components.RowCase
import com.usharik.app.ui.components.WordBank
import com.usharik.app.ui.components.WordChip
import com.usharik.app.ui.components.WordModel
import com.usharik.app.ui.theme.Dimens
import com.usharik.database.WordInfo
import java.util.Locale

@Composable
fun DeclensionQuizContent(
    app: App,
    word: WordInfo?,
    models: List<WordModel>,
    dnd: DragAndDropState,
    wordFor: (Int) -> String,
    cellIdx: (Int, Int) -> Int,
    feedback: Map<String, CellFeedback>,
    wrongAttempts: Int,
    actual: List<Int>,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = Dimens.spacingXxs, vertical = Dimens.spacingSm)) {
            if (word == null) {
                Text("…", Modifier.padding(Dimens.spacingMd))
            } else {
                QuizHeader(word, wrongAttempts)
                WordBank(
                    models = models,
                    dnd = dnd,
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                )
                HorizontalDivider()
                // All seven case rows share the remaining height so the whole table
                // is always on screen without scrolling.
                Column(
                    Modifier.fillMaxWidth().weight(2f).padding(vertical = Dimens.spacingXs),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingContent),
                ) {
                    for (i in 0..6) {
                        RowCase(
                            num = i,
                            dnd = dnd,
                            singularText = wordFor(actual[cellIdx(0, i)]),
                            pluralText = wordFor(actual[cellIdx(1, i)]),
                            singularFeedback = feedback["0_$i"],
                            pluralFeedback = feedback["1_$i"],
                            fillHeight = true,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                    }
                }
                BannerAd(app, BuildConfig.ADMOB_BANNER_AD_UNIT_ID)
            }
        }
        DragOverlay(dnd) { WordChip(it) }
    }
}

@Composable
private fun QuizHeader(word: WordInfo, wrongAttempts: Int) {
    val lang = Locale.getDefault().getISO3Language()
    val translation = if (lang in setOf("rus", "bel", "ukr")) word.translation_ru() else word.translation_en()
    Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.spacingXxs), verticalAlignment = Alignment.Bottom) {
        Text(word.word(), Modifier.testTag(TestTags.FULL_WORD), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Text(
            stringResource(R.string.declension_pattern, word.declensionType()),
            Modifier.weight(1f).padding(start = Dimens.spacingXs),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
        )
        Text(word.gender(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.spacingXxs, vertical = Dimens.spacingXs),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(translation, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        ErrorCounter(wrongAttempts)
    }
}

/** "n/5" counter that bounces and turns red once the player has made a mistake. */
@Composable
private fun ErrorCounter(wrongAttempts: Int) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(wrongAttempts) {
        if (wrongAttempts == 0) return@LaunchedEffect
        scale.snapTo(1f)
        scale.animateTo(1.3f, tween(125, easing = FastOutSlowInEasing))
        scale.animateTo(1f, tween(125, easing = FastOutSlowInEasing))
    }
    Text(
        "$wrongAttempts/5",
        Modifier.testTag(TestTags.FULL_ERROR_COUNTER).graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        color = if (wrongAttempts > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
    )
}
