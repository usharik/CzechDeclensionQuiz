package com.usharik.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usharik.app.ui.theme.AppColors
import com.usharik.app.ui.theme.Dimens

/**
 * Replicates Widget.App.Button.Gradient.* — a 16dp rounded, unelevated button with a
 * 135° linear gradient background, bold white 17sp centered text, and an optional leading icon.
 */
@Composable
fun GradientButton(
    text: String,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null,
    fontSize: TextUnit = 17.sp,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(Dimens.cornerButton)
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .alpha(if (enabled) 1f else 0.5f)
            .background(Brush.linearGradient(gradient), shape)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = AppColors.textOnGradient, modifier = Modifier.size(20.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.size(Dimens.spacingContent))
        }
        Text(text, color = AppColors.textOnGradient, fontWeight = FontWeight.Bold, fontSize = fontSize, textAlign = TextAlign.Center)
    }
}

/**
 * Replicates Widget.App.Button.Outlined.Modern — outlined, 2dp stroke and text in the
 * outline-stroke color, bold 17sp, 16dp rounded corners.
 */
@Composable
fun OutlinedModernButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null,
    fontSize: TextUnit = 17.sp,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(Dimens.cornerButton)
    val stroke = AppColors.outlineStroke
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .alpha(if (enabled) 1f else 0.5f)
            .border(BorderStroke(2.dp, stroke), shape)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = stroke, modifier = Modifier.size(20.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.size(Dimens.spacingContent))
        }
        Text(text, color = stroke, fontWeight = FontWeight.Bold, fontSize = fontSize, textAlign = TextAlign.Center)
    }
}

/** Convenience wrapper mirroring the TextButton look used for "Rate application" in the dialog. */
@Composable
fun StrokeTextButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(PaddingValues(horizontal = 12.dp, vertical = 12.dp)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = AppColors.outlineStroke, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
