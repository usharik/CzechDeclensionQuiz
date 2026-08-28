package com.usharik.app

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usharik.app.ui.screens.AboutScreen
import com.usharik.app.ui.screens.DeclensionQuizScreen
import com.usharik.app.ui.screens.HandbookScreen
import com.usharik.app.ui.screens.HubScreen
import com.usharik.app.ui.screens.SettingsScreen
import com.usharik.app.ui.screens.SingleCaseQuizScreen
import com.usharik.app.ui.screens.WordsWithErrorsScreen
import com.usharik.app.ui.theme.Dimens

private enum class Destination(@StringRes val titleRes: Int) {
    HUB(R.string.hub_title),
    FULL(R.string.quiz_mode_full_table),
    SINGLE(R.string.quiz_mode_one_case),
    ERRORS(R.string.words_with_errors),
    HANDBOOK(R.string.handbook),
    SETTINGS(R.string.settings),
    ABOUT(R.string.about),
}

/** A screen-owned action rendered in the app bar. */
data class ToolbarAction(val onClick: () -> Unit, val enabled: Boolean = true)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CzechQuizApp(app: App) {
    // Saveable so a configuration change (rotation, locale switch) keeps the current page.
    var destination by rememberSaveable { mutableStateOf(Destination.HUB) }
    var nextAction by remember { mutableStateOf<ToolbarAction?>(null) }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    // FULL and SINGLE own their back press (quit-quiz overlay); other pages return to the hub.
    BackHandler(enabled = destination != Destination.HUB && destination != Destination.FULL && destination != Destination.SINGLE) { destination = Destination.HUB }
    // testTagsAsResourceId exposes test tags as resource-ids so external Appium/UiAutomator2 tests can find them.
    Column(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
        // Port of main_activity.xml: AppBarLayout (colorPrimary, 4dp elevation, 68dp min height,
        // fills behind the status bar) hosting a 56dp bottom-aligned MaterialToolbar with the
        // white home navigation icon and, on the full quiz, the "Next case" menu action.
        Box(
            Modifier
                .fillMaxWidth()
                .shadow(4.dp)
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .heightIn(min = 68.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            Row(
                Modifier.fillMaxWidth().height(Dimens.toolbarHeightDefault),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Routed through the back dispatcher exactly like the original toolbar home
                // button: quizzes intercept it with their quit overlay, pages fall back to the
                // hub, and on the hub itself the activity finishes.
                IconButton(onClick = { backDispatcher?.onBackPressed() }, modifier = Modifier.testTag(TestTags.NAV_HOME_BTN)) {
                    Icon(painterResource(R.drawable.ic_home_black_24dp), contentDescription = stringResource(R.string.nav_home), tint = Color.White)
                }
                Text(
                    stringResource(destination.titleRes),
                    Modifier.weight(1f).testTag(TestTags.APP_BAR_TITLE),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                nextAction?.let { action ->
                    IconButton(onClick = action.onClick, enabled = action.enabled, modifier = Modifier.testTag(TestTags.NAV_NEXT_BTN)) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_forward_white_18dp),
                            contentDescription = stringResource(R.string.nextCase),
                            tint = Color.White,
                            modifier = Modifier.size(Dimens.nextActionIconSize),
                        )
                    }
                }
            }
        }
        Box(Modifier.weight(1f).navigationBarsPadding()) {
            when (destination) {
                Destination.HUB -> HubScreen(
                    app = app,
                    onOpenFullTable = { destination = Destination.FULL },
                    onOpenOneCase = { destination = Destination.SINGLE },
                    onOpenWordsWithErrors = { destination = Destination.ERRORS },
                    onOpenHandbook = { destination = Destination.HANDBOOK },
                    onOpenSettings = { destination = Destination.SETTINGS },
                    onOpenAbout = { destination = Destination.ABOUT },
                )
                Destination.FULL -> DeclensionQuizScreen(
                    app = app,
                    onQuit = { destination = Destination.HUB },
                    registerNext = { nextAction = it },
                )
                Destination.SINGLE -> SingleCaseQuizScreen(
                    app = app,
                    onQuit = { destination = Destination.HUB },
                )
                Destination.ERRORS -> WordsWithErrorsScreen(app)
                Destination.HANDBOOK -> HandbookScreen(app)
                Destination.SETTINGS -> SettingsScreen(app)
                Destination.ABOUT -> AboutScreen(app)
            }
        }
    }
}
