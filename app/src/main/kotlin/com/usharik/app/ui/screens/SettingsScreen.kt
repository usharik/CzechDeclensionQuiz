package com.usharik.app.ui.screens

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.usharik.app.App
import com.usharik.app.Gender
import com.usharik.app.R
import com.usharik.app.TestTags
import com.usharik.app.UiLanguageManager
import com.usharik.app.ui.state.DailyGoal
import com.usharik.app.ui.theme.Dimens

/**
 * Settings page: daily goal, the gender word filter, the app language (single-choice dialog) and
 * the "turn off animation" switch, plus the one-time "remove ads" purchase. Changes are persisted
 * to SharedPreferences immediately.
 */
@Composable
fun SettingsScreen(app: App) {
    val context = LocalContext.current
    val genderFilter by app.appState.genderFilterFlow.collectAsState()
    val switchOffAnimation by app.appState.switchOffAnimationFlow.collectAsState()
    val dailyGoal by app.appState.dailyGoalFlow.collectAsState()
    val adFree by app.adFree.adFree.collectAsState()
    val removeAdsOffer by app.purchaseManager.offer.collectAsState()
    var languageLabel by remember { mutableStateOf(UiLanguageManager.getSelectedLanguageLabel(context)) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    fun persist() {
        context.getSharedPreferences(App.PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(App.PREF_GENDER_FILTER, app.appState.getGenderFilterStr())
            .putBoolean(App.PREF_SWITCH_OFF_ANIMATION, app.appState.getSwitchOffAnimation())
            .putInt(App.PREF_DAILY_GOAL, app.appState.getDailyGoal())
            .apply()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.spacingMd)
            .padding(top = Dimens.spacingXs, bottom = Dimens.spacingMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm),
    ) {
        SettingsSection(stringResource(R.string.remove_ads_section)) {
            if (adFree) {
                Text(
                    stringResource(R.string.ads_removed),
                    Modifier.padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSmLarge),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                val price = removeAdsOffer.formattedPrice
                Row(
                    Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.BTN_REMOVE_ADS)
                        .clickable(enabled = price != null && !removeAdsOffer.pending, role = Role.Button) {
                            app.analyticsService.logButtonClick("SETTINGS_BUTTON_CLICK", "REMOVE_ADS")
                            (context as? Activity)?.let { app.purchaseManager.launchRemoveAdsPurchase(it) }
                        }
                        .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSmLarge),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.remove_ads), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            when {
                                removeAdsOffer.pending -> stringResource(R.string.remove_ads_pending)
                                price != null -> stringResource(R.string.remove_ads_price, price)
                                !removeAdsOffer.billingChecked -> stringResource(R.string.remove_ads_loading)
                                else -> stringResource(R.string.remove_ads_unavailable)
                            },
                            color = if (price != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (price != null) Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        SettingsSection(stringResource(R.string.daily_goal)) {
            listOf(
                DailyGoal.Level.LIGHT to R.string.daily_goal_light,
                DailyGoal.Level.REGULAR to R.string.daily_goal_regular,
                DailyGoal.Level.INTENSE to R.string.daily_goal_intense,
            ).forEach { (level, label) ->
                SettingsRadioRow(stringResource(label, level.points), dailyGoal == level.points) {
                    app.appState.setDailyGoal(level.points)
                    app.analyticsService.logEvent("daily_goal_changed", android.os.Bundle().apply { putInt("points", level.points) })
                    persist()
                }
            }
        }
        SettingsSection(stringResource(R.string.word_filter)) {
            listOf(
                Gender.ALL to stringResource(R.string.all_words),
                Gender.ANIMATE_MASCULINE to stringResource(R.string.animate_masculine),
                Gender.INANIMATE_MASCULINE to stringResource(R.string.inanimate_masculine),
                Gender.FEMININE to stringResource(R.string.feminine),
                Gender.NEUTER to stringResource(R.string.neuter),
            ).forEach { (value, label) ->
                SettingsRadioRow(label, genderFilter == value) {
                    app.appState.setGenderFilterStr(value)
                    persist()
                }
            }
        }
        SettingsSection(stringResource(R.string.additional_settings)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { showLanguageDialog = true }
                    .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSmLarge),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.ui_language), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                    Text(languageLabel, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                }
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(value = switchOffAnimation, role = Role.Switch) { newValue ->
                        app.appState.setSwitchOffAnimation(newValue)
                        app.analyticsService.logSettings(newValue)
                        persist()
                    }
                    .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.turn_off_animation), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                Switch(checked = switchOffAnimation, onCheckedChange = null)
            }
        }
    }

    if (showLanguageDialog) {
        val options = UiLanguageManager.getAvailableLanguages()
        val currentIndex = UiLanguageManager.indexOf(UiLanguageManager.getSelectedLanguage(context))
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.choose_app_language)) },
            text = {
                Column {
                    options.forEachIndexed { index, language ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    UiLanguageManager.saveAndApplyLanguage(context, language)
                                    languageLabel = UiLanguageManager.getSelectedLanguageLabel(context)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = Dimens.spacingXs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = index == currentIndex, onClick = null)
                            Text(language.displayName(context), Modifier.padding(start = Dimens.spacingSm))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

/** A titled group of settings rows on a tonal card. */
@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        Modifier.padding(start = Dimens.spacingXs, top = Dimens.spacingSm),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
    )
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cornerButton),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(vertical = Dimens.spacingXs)) { content() }
    }
}

/** One single-choice row: the whole row is the touch target and announces the radio role. */
@Composable
private fun SettingsRadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = Dimens.spacingSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(Dimens.spacingSmLarge))
        Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
    }
}
