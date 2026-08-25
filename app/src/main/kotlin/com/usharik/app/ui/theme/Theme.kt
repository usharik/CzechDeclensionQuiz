package com.usharik.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.usharik.app.R

/**
 * Semantic colors that don't map onto a Material3 ColorScheme slot but are used
 * across the quiz screens. Resolved from resources so day/night variants apply.
 */
object AppColors {
    val answerCorrect: Color @Composable get() = colorResource(R.color.colorAnswerCorrectBackground)
    val answerIncorrect: Color @Composable get() = colorResource(R.color.colorAnswerIncorrectBackground)
    val answerNeutral: Color @Composable get() = colorResource(R.color.colorAnswerNeutralBackground)
    val stroke: Color @Composable get() = colorResource(R.color.colorStroke)
    val successText: Color @Composable get() = colorResource(R.color.colorSuccessText)
    val correct: Color @Composable get() = colorResource(R.color.colorCorrect)
    val incorrect: Color @Composable get() = colorResource(R.color.colorIncorrect)
    val outlineStroke: Color @Composable get() = colorResource(R.color.button_outline_stroke)
    val textOnGradient: Color @Composable get() = colorResource(R.color.button_text_on_gradient)

    val gradientPrimary: List<Color>
        @Composable get() = listOf(colorResource(R.color.button_primary_start), colorResource(R.color.button_primary_end))
    val gradientSecondary: List<Color>
        @Composable get() = listOf(colorResource(R.color.button_secondary_start), colorResource(R.color.button_secondary_end))
    val gradientAccent: List<Color>
        @Composable get() = listOf(colorResource(R.color.button_accent_start), colorResource(R.color.button_accent_end))
    val gradientNeutral: List<Color>
        @Composable get() = listOf(colorResource(R.color.button_neutral_start), colorResource(R.color.button_neutral_end))
}

@Composable
private fun appTypography(): Typography {
    val base = Typography()
    return base.copy(
        headlineMedium = base.headlineMedium.copy(fontSize = Dimens.textHeading, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontSize = Dimens.textHeading, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontSize = Dimens.textTitle, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontSize = Dimens.textTitle, fontWeight = FontWeight.Bold),
        bodyLarge = base.bodyLarge.copy(fontSize = Dimens.draggableFontSize),
        bodyMedium = base.bodyMedium.copy(fontSize = Dimens.textBody),
        bodySmall = base.bodySmall.copy(fontSize = Dimens.textLabel),
        labelLarge = base.labelLarge.copy(fontSize = Dimens.textBody),
        labelMedium = base.labelMedium.copy(fontSize = Dimens.textLabel),
        labelSmall = base.labelSmall.copy(fontSize = Dimens.textSmall),
    )
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val colorScheme = base.copy(
        primary = colorResource(R.color.colorPrimary),
        onPrimary = colorResource(R.color.colorOnPrimary),
        primaryContainer = colorResource(R.color.colorAnswerCorrectBackground),
        onPrimaryContainer = colorResource(R.color.colorOnSurface),
        secondary = colorResource(R.color.colorSecondary),
        onSecondary = colorResource(R.color.colorOnSecondary),
        secondaryContainer = colorResource(R.color.colorAnswerNeutralBackground),
        onSecondaryContainer = colorResource(R.color.colorOnSurface),
        tertiary = colorResource(R.color.colorTertiary),
        onTertiary = colorResource(R.color.colorOnTertiary),
        error = colorResource(R.color.colorError),
        onError = colorResource(R.color.colorOnError),
        errorContainer = colorResource(R.color.colorAnswerIncorrectBackground),
        onErrorContainer = colorResource(R.color.colorOnSurface),
        background = colorResource(R.color.colorSurface),
        onBackground = colorResource(R.color.colorOnSurface),
        surface = colorResource(R.color.colorSurface),
        onSurface = colorResource(R.color.colorOnSurface),
        surfaceVariant = colorResource(R.color.colorSurfaceVariant),
        onSurfaceVariant = colorResource(R.color.colorOnSurfaceVariant),
        outline = colorResource(R.color.colorStroke),
    )
    MaterialTheme(colorScheme = colorScheme, typography = appTypography(), content = content)
}
