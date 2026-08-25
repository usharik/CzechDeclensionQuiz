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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

private enum class Destination(@StringRes val titleRes: Int) {
    HUB(R.string.hub_title),
    FULL(R.string.quiz_mode_full_table),
    SINGLE(R.string.quiz_mode_one_case),
    ERRORS(R.string.words_with_errors),
    HANDBOOK(R.string.handbook),
    SETTINGS(R.string.settings),
    ABOUT(R.string.about),
}

/** Stable test tag constants used by both production composables and androidTest code. */
object TestTags {
    // App bar
    const val APP_BAR_TITLE = "app_bar_title"
    const val NAV_HOME_BTN = "nav_home"
    // Hub screen
    const val HUB_SCREEN = "hub_screen"
    const val BTN_FULL = "btn_full"
    const val BTN_SINGLE = "btn_single"
    const val BTN_ERRORS = "btn_errors"
    const val BTN_HANDBOOK = "btn_handbook"
    const val BTN_SETTINGS = "btn_settings"
    const val BTN_ABOUT = "btn_about"
    // Single-case quiz
    const val SC_SCREEN = "sc_screen"
    const val SC_WORD = "sc_word"
    const val SC_CASE_NAME = "sc_case_name"
    const val SC_NUMBER_LABEL = "sc_number_label"
    const val SC_QUESTION = "sc_case_question"
    const val SC_ANSWER_PREFIX = "sc_answer_" // append 0..3
    const val SC_NEXT_CASE = "sc_next_case"
    const val SC_NEXT_WORD = "sc_next_word"
    // Full declension quiz
    const val FULL_WORD = "full_word"
    const val FULL_ERROR_COUNTER = "full_error_counter"
    const val FULL_POOL_WORD_PREFIX = "full_pool_word_" // append the shuffled word-model index
    const val FULL_CELL_PREFIX = "full_cell_" // append "<number>_<case>", number: 0 singular / 1 plural
    const val FULL_COMPLETION_DIALOG = "full_completion_dialog"
    const val FULL_DIALOG_NEXT_WORD = "full_dialog_next_word"
    const val FULL_DIALOG_STAY_HERE = "full_dialog_stay_here"
    const val FULL_DIALOG_TRY_AGAIN = "full_dialog_try_again"
    const val FULL_QUIT_DIALOG = "full_quit_dialog"
    const val FULL_QUIT_EXERCISES = "full_quit_exercises"
    const val FULL_QUIT_LEAVE = "full_quit_leave"
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CzechQuizApp(app: App) {
    var destination by remember { mutableStateOf(Destination.HUB) }
    var nextAction by remember { mutableStateOf<(() -> Unit)?>(null) }
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
                Modifier.fillMaxWidth().height(com.usharik.app.ui.theme.Dimens.toolbarHeightDefault),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Routed through the back dispatcher exactly like the original toolbar home
                // button: quizzes intercept it with their quit overlay, pages fall back to the
                // hub, and on the hub itself the activity finishes.
                IconButton(onClick = { backDispatcher?.onBackPressed() }, modifier = Modifier.testTag(TestTags.NAV_HOME_BTN)) {
                    Icon(painterResource(R.drawable.ic_home_black_24dp), contentDescription = null, tint = Color.White)
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
                    IconButton(onClick = action) {
                        Icon(painterResource(R.drawable.ic_arrow_forward_white_18dp), contentDescription = stringResource(R.string.nextCase), tint = Color.White)
                    }
                }
            }
        }
        Box(Modifier.weight(1f).navigationBarsPadding()) {
            when (destination) {
                Destination.HUB -> com.usharik.app.ui.screens.HubScreen(
                    app = app,
                    onOpenFullTable = { destination = Destination.FULL },
                    onOpenOneCase = { destination = Destination.SINGLE },
                    onOpenWordsWithErrors = { destination = Destination.ERRORS },
                    onOpenHandbook = { destination = Destination.HANDBOOK },
                    onOpenSettings = { destination = Destination.SETTINGS },
                    onOpenAbout = { destination = Destination.ABOUT },
                )
                Destination.FULL -> com.usharik.app.ui.screens.DeclensionQuizScreen(
                    app = app,
                    onQuit = { destination = Destination.HUB },
                    registerNext = { nextAction = it },
                )
                Destination.SINGLE -> com.usharik.app.ui.screens.SingleCaseQuizScreen(
                    app = app,
                    onQuit = { destination = Destination.HUB },
                )
                Destination.ERRORS -> com.usharik.app.ui.screens.WordsWithErrorsScreen(app)
                Destination.HANDBOOK -> com.usharik.app.ui.screens.HandbookScreen(app)
                Destination.SETTINGS -> com.usharik.app.ui.screens.SettingsScreen(app)
                Destination.ABOUT -> com.usharik.app.ui.screens.AboutScreen(app)
            }
        }
    }
}
