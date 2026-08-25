package com.usharik.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.usharik.app.TestTags
import com.usharik.app.ui.theme.AppColors
import com.usharik.app.ui.theme.Dimens

/** Immutable pool item: one declined form and whether it is still available in the bank. */
data class WordModel(val word: String, val visible: Boolean)

/** Key used to register the word pool as a drop target so placed forms can be returned to it. */
const val POOL_KEY = "POOL"

/**
 * The shuffled word pool, laid out as a wrapping flow of chips (FlexboxLayoutManager in the
 * original). Each visible chip is a drag source tagged with its index in [models]; the whole
 * pool is a drop target so a placed form dropped here returns to the bank.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordBank(models: List<WordModel>, dnd: DragAndDropState, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.dropTarget(dnd, POOL_KEY).padding(Dimens.spacingXs),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXs),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingXs),
    ) {
        models.forEachIndexed { index, model ->
            if (model.visible && model.word.isNotEmpty()) {
                WordChip(
                    text = model.word,
                    modifier = Modifier
                        .testTag("${TestTags.FULL_POOL_WORD_PREFIX}$index")
                        .semantics { contentDescription = model.word }
                        .dragSource(dnd, "$index", model.word),
                )
            }
        }
    }
}

/**
 * A single word chip styled like WordTextEdit: rounded grey background, 17sp text. Reused for the
 * floating drag shadow via [DragOverlay], which applies its own scale/alpha in a graphics layer.
 */
@Composable
fun WordChip(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Dimens.cornerLarge)
    Box(
        modifier
            .clip(shape)
            .background(AppColors.answerNeutral, shape)
            .border(Dimens.strokeThin, AppColors.stroke, shape)
            .padding(horizontal = Dimens.spacingSm, vertical = Dimens.spacingXs),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = Dimens.draggableFontSize,
        )
    }
}
