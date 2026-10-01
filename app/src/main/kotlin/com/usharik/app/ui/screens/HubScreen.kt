package com.usharik.app.ui.screens

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.usharik.app.App
import com.usharik.app.BuildConfig
import com.usharik.app.R
import com.usharik.app.TestTags
import com.usharik.app.ui.components.BannerAd
import com.usharik.app.ui.components.GradientButton
import com.usharik.app.ui.components.ProgressCard
import com.usharik.app.ui.state.ProgressOverview
import com.usharik.app.ui.state.progressOverview
import com.usharik.app.ui.theme.AppColors
import com.usharik.app.ui.theme.Dimens
import com.usharik.app.utils.HapticFeedback

/**
 * Quiz-mode selection hub: the progress card, three gradient quiz-mode cards, tiles for the
 * secondary pages and a banner pinned to the bottom.
 */
@Composable
fun HubScreen(
    app: App,
    onOpenFullTable: () -> Unit,
    onOpenOneCase: () -> Unit,
    onOpenWordsWithErrors: () -> Unit,
    onStartReview: () -> Unit,
    onOpenHandbook: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val context = LocalContext.current
    val goalTarget by app.appState.dailyGoalFlow.collectAsState()
    val wordsWithErrors by app.appState.wordsWithErrorsFlow.collectAsState()
    var overview by remember { mutableStateOf<ProgressOverview?>(null) }
    LaunchedEffect(goalTarget, wordsWithErrors.size) {
        overview = app.statsRepository.progressOverview(goalTarget, wordsWithErrors.size).also {
            app.analyticsService.logEvent("hub_progress_shown", Bundle().apply {
                putInt("streak", it.streak.current)
                putInt("today_points", it.goal.completed)
                putInt("review_count", it.reviewCount)
            })
        }
    }
    fun click(buttonName: String, action: () -> Unit) {
        HapticFeedback.light(context)
        app.analyticsService.logButtonClick("HUB_BUTTON_CLICK", buttonName)
        action()
    }

    // Content scrolls on small screens while the banner stays pinned under it, always visible.
    Column(Modifier.fillMaxSize().testTag(TestTags.HUB_SCREEN)) {
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.spacingMd)
                .padding(top = Dimens.spacingXs, bottom = Dimens.spacingMd),
        ) {
            overview?.let {
                ProgressCard(it, onReview = { click("REVIEW", onStartReview) }, Modifier.padding(bottom = Dimens.spacingMdLarge))
            }
            Text(
                stringResource(R.string.quiz_mode_title),
                Modifier.padding(bottom = Dimens.spacingSmLarge),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            GradientButton(
                text = stringResource(R.string.quiz_mode_full_table),
                subtitle = stringResource(R.string.quiz_mode_full_table_desc),
                gradient = AppColors.gradientPrimary,
                icon = painterResource(R.drawable.ic_grid),
                fontSize = Dimens.textTitle,
                onClick = { click("FULL_TABLE", onOpenFullTable) },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.BTN_FULL),
            )
            GradientButton(
                text = stringResource(R.string.quiz_mode_one_case),
                subtitle = stringResource(R.string.quiz_mode_one_case_desc),
                gradient = AppColors.gradientSecondary,
                icon = painterResource(R.drawable.ic_quiz),
                fontSize = Dimens.textTitle,
                onClick = { click("ONE_CASE", onOpenOneCase) },
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingSmLarge).testTag(TestTags.BTN_SINGLE),
            )
            GradientButton(
                text = stringResource(R.string.words_with_errors),
                subtitle = stringResource(R.string.words_with_errors_desc),
                gradient = AppColors.gradientAccent,
                icon = painterResource(R.drawable.ic_star),
                fontSize = Dimens.textTitle,
                onClick = { click("WORDS_WITH_ERRORS", onOpenWordsWithErrors) },
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingSmLarge).testTag(TestTags.BTN_ERRORS),
            )
            // Secondary pages as a compact row of tonal tiles rather than three more full-width buttons.
            Row(
                Modifier.fillMaxWidth().padding(top = Dimens.spacingMdLarge),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmLarge),
            ) {
                HubTile(stringResource(R.string.handbook), R.drawable.ic_book, Modifier.weight(1f).testTag(TestTags.BTN_HANDBOOK)) { click("HANDBOOK", onOpenHandbook) }
                HubTile(stringResource(R.string.settings), R.drawable.ic_settings_black_24dp, Modifier.weight(1f).testTag(TestTags.BTN_SETTINGS)) { click("SETTINGS", onOpenSettings) }
                HubTile(stringResource(R.string.about), R.drawable.ic_info, Modifier.weight(1f).testTag(TestTags.BTN_ABOUT)) { click("ABOUT", onOpenAbout) }
            }
        }
        BannerAd(
            app,
            BuildConfig.ADMOB_HUB_BANNER_AD_UNIT_ID,
            Modifier.fillMaxWidth().padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingXs),
            widthFraction = 1f,
        )
    }
}

/** Tonal tile with an icon above a short label, for the hub's secondary pages. */
@Composable
private fun HubTile(label: String, @DrawableRes icon: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.cornerButton),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            Modifier.padding(vertical = Dimens.spacingSmLarge, horizontal = Dimens.spacingXs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Text(
                label,
                Modifier.padding(top = Dimens.spacingXs),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}
