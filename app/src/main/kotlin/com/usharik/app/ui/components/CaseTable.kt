package com.usharik.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.usharik.app.ui.theme.Dimens
import com.usharik.database.WordInfo

/**
 * Static (non-interactive) declension table: the seven case rows share the available height so
 * the whole table is always on screen without scrolling. Used by the handbook and the
 * words-with-errors review page.
 */
@Composable
fun CaseTable(cases: Array<Array<String>>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.spacingContent)) {
        for (i in 0 until 7) {
            RowCase(
                num = i,
                dnd = null,
                singularText = cases[0][i],
                pluralText = cases[1][i],
                fillHeight = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}

/**
 * The word's translation in the app-selected locale (not the device default): Russian for the
 * Russian/Belarusian/Ukrainian UI languages, English otherwise.
 *
 * Note: this reads [LocalConfiguration]'s resolved locale, which reflects the UI language the
 * app applied via [com.usharik.app.UiLanguageManager] (AppCompatDelegate locales), not
 * necessarily the device's default locale.
 */
@Composable
fun localizedTranslation(word: WordInfo): String {
    val lang = LocalConfiguration.current.locales[0].isO3Language
    return if (lang in setOf("rus", "bel", "ukr")) word.translation_ru() else word.translation_en()
}