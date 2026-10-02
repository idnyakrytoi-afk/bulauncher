package net.bullmc.client.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.bullmc.client.theme.LocalBullColors

private const val SPLASH_TOTAL_MS = 2400L

@Composable
fun SplashScreen(
    onReady: () -> Unit
) {
    val colors = LocalBullColors.current
    val hasLogo = remember {
        Thread.currentThread().contextClassLoader?.getResource("bull.png") != null
    }

    val logoScale = remember { Animatable(0.7f) }
    val logoAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing)) }
        launch { logoScale.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        delay(350)
        launch { subtitleAlpha.animateTo(1f, tween(500)) }
        launch { progress.animateTo(0.85f, tween(SPLASH_TOTAL_MS.toInt() - 500, easing = FastOutSlowInEasing)) }
        delay(SPLASH_TOTAL_MS - 450)
        launch { progress.animateTo(1f, tween(300, easing = FastOutSlowInEasing)) }
        delay(150)
        exitAlpha.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
        onReady()
    }

    val transition = rememberInfiniteTransition(label = "splashBg")
    val bgShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200), RepeatMode.Reverse),
        label = "bgShift"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .graphicsLayer { alpha = exitAlpha.value },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(colors.primary.copy(alpha = 0.16f + bgShift * 0.08f), Color.Transparent),
                    center = Offset((0.3f + bgShift * 0.4f) * 1000f, 380f),
                    radius = 900f
                )
            )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        alpha = logoAlpha.value
                    }
                    .clip(RoundedCornerShape(30.dp))
                    .background(Brush.verticalGradient(colors.primaryGradient)),
                contentAlignment = Alignment.Center
            ) {
                if (hasLogo) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource("bull.png"),
                        contentDescription = "BullMC",
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("B", fontSize = 56.sp, fontWeight = FontWeight.Black, color = colors.onPrimary)
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = subtitleAlpha.value }
            ) {
                Text(
                    "BULL MC",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    letterSpacing = 8.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "LAUNCHER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                    letterSpacing = 6.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                val shape = RoundedCornerShape(4.dp)
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(6.dp)
                        .clip(shape)
                        .background(colors.surfaceSunken)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.value)
                            .clip(shape)
                            .background(Brush.horizontalGradient(colors.primaryGradient))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Загрузка ${(progress.value * 100).toInt()}%",
                    fontSize = 11.sp,
                    color = colors.textMuted,
                    letterSpacing = 1.sp
                )
            }
        }

        Text(
            "v1.0.0",
            fontSize = 11.sp,
            color = colors.textMuted,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .graphicsLayer { alpha = subtitleAlpha.value }
        )
    }
}
