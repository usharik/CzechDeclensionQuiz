package com.usharik.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usharik.app.App
import com.usharik.app.Gender
import com.usharik.app.R
import com.usharik.app.UiLanguageManager
import com.usharik.app.ui.theme.Dimens

/**
 * Settings page. Faithful Compose port of SettingsFragment/ViewModel + settings_fragment.xml:
 * the gender word-filter radio group, the app-language button with its single-choice dialog and
 * the "turn off animation" checkbox. Changes are persisted to SharedPreferences immediately
 * (the fragment saved them in onPause).
 */
@Composable
fun SettingsScreen(app: App) {
    val context = LocalContext.current
    val genderFilter by app.appState.genderFilterFlow.collectAsState()
    val switchOffAnimation by app.appState.switchOffAnimationFlow.collectAsState()
    var languageLabel by remember { mutableStateOf(UiLanguageManager.getSelectedLanguageLabel(context)) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    fun persist() {
        context.getSharedPreferences(App.PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(App.PREF_GENDER_FILTER, app.appState.getGenderFilterStr())
            .putBoolean(App.PREF_SWITCH_OFF_ANIMATION, app.appState.getSwitchOffAnimation())
            .apply()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = Dimens.spacingMd, start = Dimens.spacingMd, end = Dimens.spacingMd),
    ) {
        SectionHeader(stringResource(R.string.word_filter))
        Column(Modifier.padding(start = Dimens.spacingMd)) {
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
        SectionHeader(stringResource(R.string.ui_language), Modifier.padding(top = Dimens.spacingSm))
        Button(
            onClick = { showLanguageDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacingMd)
                .heightIn(min = 48.dp),
        ) {
            Text(languageLabel)
        }
        SectionHeader(stringResource(R.string.additional_settings), Modifier.padding(top = Dimens.spacingSm))
        Row(
            Modifier
                .padding(start = Dimens.spacingMd)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    val newValue = !switchOffAnimation
                    app.appState.setSwitchOffAnimation(newValue)
                    app.analyticsService.logSettings(newValue)
                    persist()
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = switchOffAnimation,
                onCheckedChange = { newValue ->
                    app.appState.setSwitchOffAnimation(newValue)
                    app.analyticsService.logSettings(newValue)
                    persist()
                },
            )
            Text(stringResource(R.string.turn_off_animation), color = MaterialTheme.colorScheme.onSurface, fontSize = Dimens.textBody)
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

/** Section title in the textAppearanceSubtitle1 style (16sp, onSurface) with the 8dp inset. */
@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.padding(start = Dimens.spacingSm),
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 16.sp,
    )
}

/** One MaterialRadioButton row of the gender-filter group. */
@Composable
private fun SettingsRadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = Dimens.textBody)
    }
}
