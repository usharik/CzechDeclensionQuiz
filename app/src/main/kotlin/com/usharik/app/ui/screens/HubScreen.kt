package com.usharik.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.usharik.app.App
import com.usharik.app.BuildConfig
import com.usharik.app.R
import com.usharik.app.TestTags
import com.usharik.app.ui.components.BannerAd
import com.usharik.app.ui.components.GradientButton
import com.usharik.app.ui.components.OutlinedModernButton
import com.usharik.app.ui.theme.AppColors
import com.usharik.app.ui.theme.Dimens
import com.usharik.app.utils.HapticFeedback

/**
 * Quiz-mode selection hub. Faithful Compose port of QuizModeSelectionFragment +
 * fragment_quiz_mode_selection.xml: a vertically-centered scrollable column with the title,
 * three gradient quiz buttons, a divider, three outlined page buttons and the banner ad slot.
 */
@Composable
fun HubScreen(
    app: App,
    onOpenFullTable: () -> Unit,
    onOpenOneCase: () -> Unit,
    onOpenWordsWithErrors: () -> Unit,
    onOpenHandbook: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val context = LocalContext.current
    fun click(buttonName: String, action: () -> Unit) {
        HapticFeedback.light(context)
        app.analyticsService.logButtonClick("HUB_BUTTON_CLICK", buttonName)
        action()
    }

    // NestedScrollView(fillViewport) + LinearLayout(gravity=center_vertical): content is centered
    // when it fits the viewport, and scrolls normally when it does not.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .testTag(TestTags.HUB_SCREEN)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = viewportHeight)
                    .padding(Dimens.spacingMd),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(R.string.quiz_mode_title),
                    Modifier.fillMaxWidth().padding(bottom = Dimens.spacingXl),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = Dimens.textHeading,
                    textAlign = TextAlign.Center,
                )
                GradientButton(
                    text = stringResource(R.string.quiz_mode_full_table),
                    gradient = AppColors.gradientPrimary,
                    icon = painterResource(R.drawable.ic_quiz),
                    fontSize = Dimens.textBody,
                    onClick = { click("FULL_TABLE", onOpenFullTable) },
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.BTN_FULL).padding(bottom = Dimens.spacingMd),
                )
                GradientButton(
                    text = stringResource(R.string.quiz_mode_one_case),
                    gradient = AppColors.gradientSecondary,
                    icon = painterResource(R.drawable.ic_quiz),
                    fontSize = Dimens.textBody,
                    onClick = { click("ONE_CASE", onOpenOneCase) },
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.BTN_SINGLE).padding(bottom = Dimens.spacingMd),
                )
                GradientButton(
                    text = stringResource(R.string.words_with_errors),
                    gradient = AppColors.gradientAccent,
                    icon = painterResource(R.drawable.ic_star),
                    fontSize = Dimens.textBody,
                    onClick = { click("WORDS_WITH_ERRORS", onOpenWordsWithErrors) },
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.BTN_ERRORS).padding(bottom = Dimens.spacingXl),
                )
                // Divider between primary and secondary actions.
                HorizontalDivider(
                    Modifier.fillMaxWidth().padding(bottom = Dimens.spacingMd),
                    thickness = Dimens.strokeThin,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                )
                OutlinedModernButton(
                    text = stringResource(R.string.handbook),
                    icon = painterResource(R.drawable.ic_book),
                    fontSize = Dimens.textBody,
                    onClick = { click("HANDBOOK", onOpenHandbook) },
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.BTN_HANDBOOK).padding(bottom = Dimens.spacingSm),
                )
                OutlinedModernButton(
                    text = stringResource(R.string.settings),
                    icon = painterResource(R.drawable.ic_settings_black_24dp),
                    fontSize = Dimens.textBody,
                    onClick = { click("SETTINGS", onOpenSettings) },
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.BTN_SETTINGS).padding(bottom = Dimens.spacingSm),
                )
                OutlinedModernButton(
                    text = stringResource(R.string.about),
                    icon = painterResource(R.drawable.ic_info),
                    fontSize = Dimens.textBody,
                    onClick = { click("ABOUT", onOpenAbout) },
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.BTN_ABOUT).padding(bottom = Dimens.spacingMd),
                )
                BannerAd(
                    app,
                    BuildConfig.ADMOB_HUB_BANNER_AD_UNIT_ID,
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
        }
    }
}
