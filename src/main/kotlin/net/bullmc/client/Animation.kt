package net.bullmc.client

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Единые длительности, чтобы все экраны анимировались в одном ритме. */
object AnimDurations {
    const val FAST = 140
    const val NORMAL = 260
    const val SLOW = 420
    const val STAGGER_STEP = 55
}

/**
 * Анимированный переход между экранами.
 *
 * Если [screenOrder] содержит оба экрана, направление слайда зависит от порядка
 * пунктов (вперёд — справа, назад — слева). Иначе контент просто плавно перетекает.
 */
@Composable
fun ScreenTransition(
    targetState: String,
    modifier: Modifier = Modifier,
    screenOrder: List<String> = emptyList(),
    content: @Composable (String) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            val from = screenOrder.indexOf(initialState)
            val to = screenOrder.indexOf(targetState)
            val forward = from < 0 || to < 0 || to >= from
            val distance = { width: Int -> width / 5 }

            if (forward) {
                (slideInHorizontally(tween(AnimDurations.SLOW, easing = FastOutSlowInEasing)) { distance(it) } +
                        fadeIn(tween(AnimDurations.NORMAL))) togetherWith
                        (slideOutHorizontally(tween(AnimDurations.SLOW, easing = FastOutSlowInEasing)) { -distance(it) } +
                                fadeOut(tween(AnimDurations.NORMAL)))
            } else {
                (slideInHorizontally(tween(AnimDurations.SLOW, easing = FastOutSlowInEasing)) { -distance(it) } +
                        fadeIn(tween(AnimDurations.NORMAL))) togetherWith
                        (slideOutHorizontally(tween(AnimDurations.SLOW, easing = FastOutSlowInEasing)) { distance(it) } +
                                fadeOut(tween(AnimDurations.NORMAL)))
            }
        },
        label = "screenTransition"
    ) { screen ->
        content(screen)
    }
}

/**
 * Масштаб при наведении и нажатии.
 * Применяется к любой кликабельной карточке/кнопке.
 */
fun Modifier.interactiveScale(
    interactionSource: MutableInteractionSource,
    hoverScale: Float = 1.02f,
    pressScale: Float = 0.975f
): Modifier = composed {
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> pressScale
            hovered -> hoverScale
            else -> 1f
        },
        animationSpec = tween(AnimDurations.FAST, easing = FastOutSlowInEasing),
        label = "interactiveScale"
    )

    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Плавное изменение цвета фона/текста при наведении. */
@Composable
fun rememberAnimatedColor(target: Color, durationMs: Int = AnimDurations.NORMAL): Color {
    return animateColorAsStateCompat(target, durationMs)
}

@Composable
private fun animateColorAsStateCompat(target: Color, durationMs: Int): Color {
    val animated by androidx.compose.animation.animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        label = "animatedColor"
    )
    return animated
}

/**
 * Появление одного элемента: fade + сдвиг снизу.
 * [index] задаёт задержку для «каскадного» появления списка.
 */
@Composable
fun AnimatedEntry(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    index: Int = 0,
    delayPerItem: Int = AnimDurations.STAGGER_STEP,
    offsetX: Float = 0f,
    offsetY: Float = 18f,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val density = LocalDensity.current

    LaunchedEffect(visible, index) {
        if (!visible) {
            progress.snapTo(0f)
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        delay(index.toLong() * delayPerItem)
        progress.animateTo(1f, tween(AnimDurations.SLOW, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            translationX = (1f - progress.value) * offsetX * density.density
            translationY = (1f - progress.value) * offsetY * density.density
        }
    ) {
        content()
    }
}

/** Пульсирующая точка статуса (онлайн / загрузка / работа). */
@Composable
fun PulsingDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
    pulseScale: Float = 1.8f,
    periodMs: Int = 1400
) {
    val transition = rememberInfiniteTransition(label = "pulsingDot")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    val scale = 1f + (pulseScale - 1f) * pulse
                    scaleX = scale
                    scaleY = scale
                    alpha = (1f - pulse) * 0.45f
                }
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/** «Дыхание» — мягкое периодическое масштабирование (для кнопки PLAY). */
@Composable
fun rememberBreathingScale(enabled: Boolean, min: Float = 1f, max: Float = 1.035f, periodMs: Int = 1600): State<Float> {
    val transition = rememberInfiniteTransition(label = "breathing")
    val value by transition.animateFloat(
        initialValue = min,
        targetValue = max,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )
    return remember { derivedStateOf { if (enabled) value else min } }
}

/** Анимированная полоса прогресса с градиентом. */
@Composable
fun AnimatedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color,
    backgroundColor: Color = Color(0xFF21262D),
    height: Dp = 5.dp,
    corner: Dp = 3.dp
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(AnimDurations.NORMAL, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(shape)
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), color)))
        )
    }
}

/** Заглушка-скелетон с бегущим бликом (пока данные грузятся). */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    baseColor: Color = Color(0xFF161B22),
    highlightColor: Color = Color(0xFF2A3038),
    corner: Dp = 8.dp,
    periodMs: Int = 1200
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerShift"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(baseColor)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        highlightColor.copy(alpha = 0f),
                        highlightColor.copy(alpha = 0.8f),
                        highlightColor.copy(alpha = 0f)
                    ),
                    startX = shift * 220f,
                    endX = shift * 220f + 240f
                )
            )
    )
}

/** Появление/скрытие боковой панели (панель друзей, консоль логов). */
@Composable
fun SidePanelVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInHorizontally(tween(AnimDurations.SLOW, easing = FastOutSlowInEasing)) { it } +
                fadeIn(tween(AnimDurations.NORMAL)),
        exit = slideOutHorizontally(tween(AnimDurations.SLOW, easing = FastOutSlowInEasing)) { it } +
                fadeOut(tween(AnimDurations.NORMAL))
    ) {
        content()
    }
}

/** Появление/скрытие снизу (консоль логов, всплывающие подсказки). */
@Composable
fun BottomPanelVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(tween(AnimDurations.SLOW, easing = FastOutSlowInEasing)) { it } +
                fadeIn(tween(AnimDurations.NORMAL)),
        exit = slideOutVertically(tween(AnimDurations.NORMAL, easing = FastOutSlowInEasing)) { it } +
                fadeOut(tween(AnimDurations.FAST))
    ) {
        content()
    }
}

/** Однократный «поп» при изменении [trigger] (клик, смена состояния). */
@Composable
fun rememberPopScale(trigger: Any?, strength: Float = 1.12f): Float {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(trigger) {
        if (trigger == null) return@LaunchedEffect
        scope.launch {
            scale.animateTo(strength, tween(90, easing = FastOutSlowInEasing))
            scale.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
        }
    }
    return scale.value
}

/** Плавное появление всего контента экрана при первой отрисовке. */
@Composable
fun rememberScreenEntryAlpha(durationMs: Int = AnimDurations.SLOW): Float {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(durationMs, easing = FastOutSlowInEasing))
    }
    return alpha.value
}
