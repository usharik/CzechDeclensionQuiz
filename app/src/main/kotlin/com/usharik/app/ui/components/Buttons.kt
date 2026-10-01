package com.usharik.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usharik.app.ui.theme.AppColors
import com.usharik.app.ui.theme.Dimens

/**
 * Shared frame of the app's custom buttons: a 48dp-min, 16dp-rounded row with an optional leading
 * icon and a bold label, optionally followed by a smaller subtitle. [decoration] paints the
 * background and/or border; [contentColor] tints the icon and text. With a subtitle the content is
 * start-aligned (a card-like mode button), otherwise it is centered.
 */
@Composable
fun ButtonFrame(
    text: String,
    contentColor: Color,
    modifier: Modifier = Modifier,
    decoration: Modifier = Modifier,
    enabled: Boolean = true,
    disabledAlpha: Float = 0.5f,
    icon: Painter? = null,
    subtitle: String? = null,
    fontSize: TextUnit = 17.sp,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(Dimens.cornerButton)
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .alpha(if (enabled) 1f else disabledAlpha)
            .then(decoration)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (subtitle != null) Dimens.spacingMd else 24.dp, vertical = 12.dp),
        horizontalArrangement = if (subtitle != null) Arrangement.Start else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(if (subtitle != null) 28.dp else 20.dp))
            Spacer(Modifier.size(if (subtitle != null) Dimens.spacingMd else Dimens.spacingSm))
        }
        if (subtitle != null) {
            Column(Modifier.weight(1f)) {
                Text(text, color = contentColor, fontWeight = FontWeight.Bold, fontSize = fontSize)
                Text(subtitle, color = contentColor.copy(alpha = 0.85f), fontSize = Dimens.textLabel, lineHeight = 16.sp)
            }
        } else {
            Text(text, color = contentColor, fontWeight = FontWeight.Bold, fontSize = fontSize, textAlign = TextAlign.Center)
        }
    }
}

/** A rounded, unelevated button with a linear gradient background and bold white text. */
@Composable
fun GradientButton(
    text: String,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null,
    subtitle: String? = null,
    fontSize: TextUnit = 17.sp,
    onClick: () -> Unit,
) {
    ButtonFrame(
        text = text,
        contentColor = AppColors.textOnGradient,
        modifier = modifier,
        decoration = Modifier.background(Brush.linearGradient(gradient)),
        enabled = enabled,
        icon = icon,
        subtitle = subtitle,
        fontSize = fontSize,
        onClick = onClick,
    )
}

/** Outlined counterpart of [GradientButton]: 2dp stroke and text in the outline-stroke color. */
@Composable
fun OutlinedModernButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null,
    fontSize: TextUnit = 17.sp,
    onClick: () -> Unit,
) {
    val stroke = AppColors.outlineStroke
    ButtonFrame(
        text = text,
        contentColor = stroke,
        modifier = modifier,
        decoration = Modifier.border(BorderStroke(2.dp, stroke), RoundedCornerShape(Dimens.cornerButton)),
        enabled = enabled,
        icon = icon,
        fontSize = fontSize,
        onClick = onClick,
    )
}

/** Borderless low-emphasis button, e.g. "Rate application" or skipping a word. */
@Composable
fun StrokeTextButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null,
    onClick: () -> Unit,
) {
    ButtonFrame(
        text = text,
        contentColor = AppColors.outlineStroke,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        fontSize = 14.sp,
        onClick = onClick,
    )
}
