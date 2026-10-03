package com.usharik.app

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import com.usharik.app.ui.screens.AboutScreen
import com.usharik.app.ui.screens.DeclensionQuizScreen
import com.usharik.app.ui.screens.HandbookScreen
import com.usharik.app.ui.screens.HubScreen
import com.usharik.app.ui.screens.SettingsScreen
import com.usharik.app.ui.screens.SingleCaseQuizScreen
import com.usharik.app.ui.screens.WordsWithErrorsScreen
import com.usharik.app.ui.screens.quizTitleRes

private enum class Destination(@StringRes val titleRes: Int) {
    HUB(R.string.hub_title),
    FULL(R.string.quiz_mode_full_table),
    SINGLE(R.string.quiz_mode_one_case),
    ERRORS(R.string.words_with_errors),
    REVIEW(R.string.review_title),
    HANDBOOK(R.string.handbook),
    SETTINGS(R.string.settings),
    ABOUT(R.string.about),
}

/** A screen-owned action rendered in the app bar. */
data class ToolbarAction(val onClick: () -> Unit, val enabled: Boolean = true)

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CzechQuizApp(app: App, startInReview: Boolean = false) {
    // Saveable so a configuration change (rotation, locale switch) keeps the current page.
    var destination by rememberSaveable { mutableStateOf(if (startInReview) Destination.REVIEW else Destination.HUB) }
    fun startReview() {
        app.analyticsService.logEvent("review_started")
        destination = Destination.REVIEW
    }
    var nextAction by remember { mutableStateOf<ToolbarAction?>(null) }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    // The hub's nouns / adjectives / verbs choice; quiz screens capture it when they are created.
    val partOfSpeech by app.appState.partOfSpeechFlow.collectAsState()
    // Quizzes own their back press (quit-quiz overlay); other pages return to the hub.
    BackHandler(enabled = destination !in setOf(Destination.HUB, Destination.FULL, Destination.SINGLE, Destination.REVIEW)) { destination = Destination.HUB }
    val isHub = destination == Destination.HUB
    // testTagsAsResourceId exposes test tags as resource-ids so external Appium/UiAutomator2 tests can find them.
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).semantics { testTagsAsResourceId = true }) {
        // Flat Material 3 app bar on the page surface: the hub shows the brand title, every other
        // page a back arrow. The arrow is routed through the back dispatcher, so quizzes still
        // intercept it with their quit overlay and pages fall back to the hub.
        TopAppBar(
            title = {
                Text(
                    stringResource(
                        when (destination) {
                            Destination.FULL -> quizTitleRes(partOfSpeech, full = true)
                            Destination.SINGLE -> quizTitleRes(partOfSpeech, full = false)
                            else -> destination.titleRes
                        },
                    ),
                    Modifier.testTag(TestTags.APP_BAR_TITLE),
                    style = if (isHub) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                if (!isHub) {
                    IconButton(onClick = { backDispatcher?.onBackPressed() }, modifier = Modifier.testTag(TestTags.NAV_HOME_BTN)) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.nav_back))
                    }
                }
            },
            actions = {
                nextAction?.let { action ->
                    // A tinted circle makes "next word" read as the screen's action, not a media control.
                    FilledTonalIconButton(
                        onClick = action.onClick,
                        enabled = action.enabled,
                        modifier = Modifier.padding(end = 8.dp).testTag(TestTags.NAV_NEXT_BTN),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(painterResource(R.drawable.ic_arrow_forward), contentDescription = stringResource(R.string.next_word))
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.primary,
            ),
        )
        Box(Modifier.weight(1f).navigationBarsPadding()) {
            when (destination) {
                Destination.HUB -> HubScreen(
                    app = app,
                    onOpenFullTable = { destination = Destination.FULL },
                    onOpenOneCase = { destination = Destination.SINGLE },
                    onOpenWordsWithErrors = { destination = Destination.ERRORS },
                    onStartReview = ::startReview,
                    onOpenHandbook = { destination = Destination.HANDBOOK },
                    onOpenSettings = { destination = Destination.SETTINGS },
                    onOpenAbout = { destination = Destination.ABOUT },
                )
                Destination.FULL -> DeclensionQuizScreen(
                    app = app,
                    partOfSpeech = partOfSpeech,
                    onQuit = { destination = Destination.HUB },
                    registerNext = { nextAction = it },
                )
                Destination.REVIEW -> DeclensionQuizScreen(
                    app = app,
                    partOfSpeech = partOfSpeech,
                    onQuit = { destination = Destination.HUB },
                    registerNext = { nextAction = it },
                    review = true,
                )
                Destination.SINGLE -> SingleCaseQuizScreen(
                    app = app,
                    partOfSpeech = partOfSpeech,
                    onQuit = { destination = Destination.HUB },
                )
                Destination.ERRORS -> WordsWithErrorsScreen(app, onStartReview = ::startReview)
                Destination.HANDBOOK -> HandbookScreen(app)
                Destination.SETTINGS -> SettingsScreen(app)
                Destination.ABOUT -> AboutScreen(app)
            }
        }
    }
}
