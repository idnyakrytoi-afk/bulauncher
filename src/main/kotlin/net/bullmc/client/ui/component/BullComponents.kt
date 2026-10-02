package net.bullmc.client.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bullmc.client.AnimDurations
import net.bullmc.client.theme.BullColors
import net.bullmc.client.theme.LocalBullColors

/** Единая скруглённая поверхность (карточка) с мягкой тенью и границей. */
@Composable
fun BullCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    colors: BullColors = LocalBullColors.current,
    background: Color = colors.surface,
    border: Color = colors.border,
    hoverable: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = if (hoverable && hovered) colors.surfaceHover else background,
        animationSpec = tween(AnimDurations.NORMAL),
        label = "bullCardBg"
    )

    val clickModifier = if (onClick != null) {
        Modifier.clickable(interactionSource = interaction, indication = null) { onClick() }
    } else Modifier

    Column(
        modifier = modifier
            .shadow(10.dp, shape, ambientColor = Color.Black.copy(alpha = 0.4f), spotColor = Color.Black.copy(alpha = 0.5f))
            .clip(shape)
            .background(bg)
            .border(1.dp, if (hoverable && hovered) colors.borderStrong else border, shape)
            .then(clickModifier)
            .padding(contentPadding),
        content = content
    )
}

/** Заголовок секции с акцентной чертой слева. */
@Composable
fun BullSectionTitle(
    title: String,
    subtitle: String? = null,
    colors: BullColors = LocalBullColors.current,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(colors.primaryGradient))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = colors.textMuted)
            }
        }
    }
}

/** Крупная кнопка с градиентом акцента и свечением при наведении. */
@Composable
fun BullPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: BullColors = LocalBullColors.current,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    leading: (@Composable () -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            pressed -> 0.98f
            hovered -> 1.02f
            else -> 1f
        },
        animationSpec = tween(AnimDurations.FAST, easing = FastOutSlowInEasing),
        label = "bullBtnScale"
    )

    val gradient = if (enabled) colors.primaryGradient
    else listOf(colors.borderStrong, colors.border)
    val contentColor = if (enabled) colors.onPrimary else colors.textMuted

    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation = if (enabled && hovered) 16.dp else 8.dp,
                shape = shape,
                ambientColor = colors.primary.copy(alpha = 0.5f),
                spotColor = colors.primary.copy(alpha = 0.6f)
            )
            .clip(shape)
            .background(Brush.horizontalGradient(gradient), shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/** Второстепенная кнопка/чип на поверхности. */
@Composable
fun BullSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: BullColors = LocalBullColors.current,
    enabled: Boolean = true,
    height: Dp = 40.dp,
    shape: Shape = RoundedCornerShape(10.dp),
    accent: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = when {
            !enabled -> colors.surfaceSunken
            accent -> colors.primary.copy(alpha = if (hovered) 0.24f else 0.15f)
            hovered -> colors.surfaceHover
            else -> colors.surfaceSunken
        },
        animationSpec = tween(AnimDurations.NORMAL),
        label = "bullSecondaryBg"
    )
    val textColor = when {
        !enabled -> colors.textMuted
        accent -> colors.primary
        else -> colors.textSecondary
    }

    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(bg)
            .border(1.dp, if (accent) colors.primary.copy(alpha = 0.35f) else colors.border, shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, color = textColor, fontWeight = FontWeight.SemiBold)
    }
}

/** Поле ввода в едином стиле. */
@Composable
fun BullTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    colors: BullColors = LocalBullColors.current,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    fontSize: Int = 14
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceSunken)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = colors.textMuted, fontSize = fontSize.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            enabled = enabled,
            textStyle = TextStyle(color = colors.textPrimary, fontSize = fontSize.sp),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Тумблер (switch) в стиле клиента. */
@Composable
fun BullToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    colors: BullColors = LocalBullColors.current
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.primary else colors.borderStrong,
        animationSpec = tween(AnimDurations.NORMAL),
        label = "toggleTrack"
    )
    val knobOffset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(AnimDurations.NORMAL, easing = FastOutSlowInEasing),
        label = "toggleKnob"
    )

    Box(
        modifier = modifier
            .width(48.dp)
            .height(26.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = 3.dp)
                .offset(x = (20 * knobOffset).dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(if (checked) colors.onPrimary else Color.White)
        )
    }
}

/** Чип-тег с состоянием выбора. */
@Composable
fun BullChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    colors: BullColors = LocalBullColors.current,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (selected) colors.primary.copy(alpha = 0.18f) else colors.surfaceSunken,
        animationSpec = tween(AnimDurations.NORMAL),
        label = "chipBg"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(
                1.dp,
                if (selected) colors.primary.copy(alpha = 0.5f) else colors.border,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            fontSize = 12.sp,
            color = if (selected) colors.primary else colors.textSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/** Метка/бейдж (лоадер, статус, категория). */
@Composable
fun BullBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
    }
}
