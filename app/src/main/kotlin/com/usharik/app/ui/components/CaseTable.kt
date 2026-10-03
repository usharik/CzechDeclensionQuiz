package com.usharik.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.usharik.app.R
import com.usharik.app.ui.state.FormTable
import com.usharik.app.ui.state.Lexeme
import com.usharik.app.ui.state.SectionTitle
import com.usharik.app.ui.theme.Dimens
import com.usharik.database.VerbInfo

/**
 * Static (non-interactive) form table: the rows share the available height so the whole table
 * is always on screen without scrolling. Used by the handbook and the words-with-errors page for
 * nouns, adjectives and verbs alike.
 */
@Composable
fun CaseTable(table: FormTable, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.spacingContent)) {
        CaseColumnsHeader(table.columnKind, Modifier.fillMaxWidth())
        // A section none of whose cells exist (no imperative for modal verbs) is left out entirely.
        table.sections.filter { s -> s.rows.any { r -> r.cells.any { it.exists } } }.forEach { section ->
            if (section.title != SectionTitle.NONE) SectionHeader(section.title)
            section.rows.forEach { row ->
                RowCase(
                    row = row,
                    dnd = null,
                    text = { it.target },
                    fillHeight = true,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }
}

/**
 * The lexeme's translation in the app-selected locale (not the device default): Russian for the
 * Russian/Belarusian/Ukrainian UI languages, English otherwise.
 *
 * Note: this reads [LocalConfiguration]'s resolved locale, which reflects the UI language the
 * app applied via [com.usharik.app.UiLanguageManager] (AppCompatDelegate locales), not
 * necessarily the device's default locale.
 */
@Composable
fun localizedTranslation(lexeme: Lexeme): String {
    val lang = LocalConfiguration.current.locales[0].isO3Language
    return if (lang in setOf("rus", "bel", "ukr")) lexeme.translationRu else lexeme.translationEn
}

/**
 * The grammar line under a headword: declension pattern and gender for a noun; adjective type and
 * the noun it agrees with; aspect, conjugation group and aspect partner for a verb.
 */
@Composable
fun lexemeSubtitle(lexeme: Lexeme): String = when (lexeme) {
    is Lexeme.Noun -> "${stringResource(R.string.declension_pattern, lexeme.info.declensionType())} · ${lexeme.info.gender()}"
    is Lexeme.Adjective -> buildString {
        append(stringResource(adjectiveKindRes(lexeme.info.kind())))
        if (lexeme.noun.word().isNotEmpty()) append(" · ").append(stringResource(R.string.with_noun, lexeme.noun.word())).append(" (").append(lexeme.noun.gender()).append(')')
    }
    is Lexeme.Phrase -> "${stringResource(adjectiveKindRes(lexeme.adjective.kind()))} + ${stringResource(R.string.declension_pattern, lexeme.noun.declensionType())} · ${lexeme.noun.gender()}"
    is Lexeme.Verb -> buildString {
        append(stringResource(aspectRes(lexeme.info.aspect())))
        append(" · ")
        append(if (lexeme.info.verbClass() == "nepravidelné") stringResource(R.string.verb_group_irregular) else stringResource(R.string.declension_pattern, lexeme.info.verbClass()))
        if (lexeme.info.pair().isNotEmpty()) append(" · ").append(stringResource(R.string.aspect_pair, lexeme.info.pair()))
    }
}

fun adjectiveKindRes(kind: String): Int = when (kind) {
    "měkké" -> R.string.adjective_kind_soft
    "přivlastňovací" -> R.string.adjective_kind_possessive
    else -> R.string.adjective_kind_hard
}

fun aspectRes(aspect: String): Int = when (aspect) {
    VerbInfo.ASPECT_PERFECTIVE -> R.string.aspect_perfective
    VerbInfo.ASPECT_IMPERFECTIVE -> R.string.aspect_imperfective
    else -> R.string.aspect_biaspectual
}
