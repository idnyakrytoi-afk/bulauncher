package net.bullmc.client.theme

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class ThemeName {
    DARK, PURPLE, CYAN, GREEN, RED, OCEAN, LAVENDER, OLED
}

/**
 * Полная палитра лаунчера в стиле Feather / Badlion.
 *
 * Material-цвета из [ThemeManager] покрывают только часть компонентов,
 * поэтому весь собственный UI берёт цвета отсюда через [LocalBullColors].
 */
@Immutable
data class BullColors(
    val background: Color,
    val backgroundTop: Color,
    val surface: Color,
    val surfaceHover: Color,
    val surfaceSunken: Color,
    val border: Color,
    val borderStrong: Color,
    val primary: Color,
    val primaryVariant: Color,
    val onPrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val success: Color,
    val error: Color,
    val warning: Color,
    val isOled: Boolean = false
) {
    /** Градиент акцента — кнопки, прогресс, активные элементы. */
    val primaryGradient: List<Color> get() = listOf(primary, primaryVariant)

    /** Мягкий фон окна с подсветкой акцентом сверху. */
    val windowGradient: Brush
        get() = Brush.verticalGradient(
            colors = listOf(backgroundTop, background)
        )
}

/** Доступ к текущей палитре из любого composable без протаскивания параметров. */
val LocalBullColors = staticCompositionLocalOf { bullColors(ThemeName.DARK) }

/** Палитра для конкретной темы. */
fun bullColors(theme: ThemeName): BullColors = when (theme) {
    ThemeName.DARK -> BullColors(
        background = Color(0xFF0A0A0F),
        backgroundTop = Color(0xFF14121F),
        surface = Color(0xFF13131C),
        surfaceHover = Color(0xFF1B1B27),
        surfaceSunken = Color(0xFF0D0D13),
        border = Color(0xFF23232F),
        borderStrong = Color(0xFF31313F),
        primary = Color(0xFF8B5CF6),
        primaryVariant = Color(0xFF6366F1),
        onPrimary = Color.White,
        textPrimary = Color(0xFFF1F2F6),
        textSecondary = Color(0xFF9BA1B0),
        textMuted = Color(0xFF5C6270),
        success = Color(0xFF34D399),
        error = Color(0xFFF87171),
        warning = Color(0xFFFBBF24)
    )
    ThemeName.PURPLE -> BullColors(
        background = Color(0xFF0C0A14),
        backgroundTop = Color(0xFF1A1330),
        surface = Color(0xFF16121F),
        surfaceHover = Color(0xFF201A2E),
        surfaceSunken = Color(0xFF100D17),
        border = Color(0xFF2A2340),
        borderStrong = Color(0xFF3A3155),
        primary = Color(0xFFA78BFA),
        primaryVariant = Color(0xFF7C5CF6),
        onPrimary = Color(0xFF0C0A14),
        textPrimary = Color(0xFFF1EEFB),
        textSecondary = Color(0xFFA79EC4),
        textMuted = Color(0xFF665C82),
        success = Color(0xFF34D399),
        error = Color(0xFFF87171),
        warning = Color(0xFFFBBF24)
    )
    ThemeName.CYAN -> BullColors(
        background = Color(0xFF07131A),
        backgroundTop = Color(0xFF0C2A38),
        surface = Color(0xFF0E1F29),
        surfaceHover = Color(0xFF152B37),
        surfaceSunken = Color(0xFF0A1820),
        border = Color(0xFF1D3745),
        borderStrong = Color(0xFF2A4C5E),
        primary = Color(0xFF22D3EE),
        primaryVariant = Color(0xFF0EA5E9),
        onPrimary = Color(0xFF07131A),
        textPrimary = Color(0xFFEAF6FA),
        textSecondary = Color(0xFF9BB8C4),
        textMuted = Color(0xFF5A7683),
        success = Color(0xFF34D399),
        error = Color(0xFFF87171),
        warning = Color(0xFFFBBF24)
    )
    ThemeName.GREEN -> BullColors(
        background = Color(0xFF07160F),
        backgroundTop = Color(0xFF0D2A1C),
        surface = Color(0xFF0E2418),
        surfaceHover = Color(0xFF143222),
        surfaceSunken = Color(0xFF0A1A12),
        border = Color(0xFF1E3D2C),
        borderStrong = Color(0xFF2C5540),
        primary = Color(0xFF34D399),
        primaryVariant = Color(0xFF10B981),
        onPrimary = Color(0xFF07160F),
        textPrimary = Color(0xFFEAF7F0),
        textSecondary = Color(0xFF9CC0AC),
        textMuted = Color(0xFF5C7E6B),
        success = Color(0xFF34D399),
        error = Color(0xFFF87171),
        warning = Color(0xFFFBBF24)
    )
    ThemeName.RED -> BullColors(
        background = Color(0xFF170A0D),
        backgroundTop = Color(0xFF2E1219),
        surface = Color(0xFF241014),
        surfaceHover = Color(0xFF31171D),
        surfaceSunken = Color(0xFF1A0B0F),
        border = Color(0xFF3D1F26),
        borderStrong = Color(0xFF57303A),
        primary = Color(0xFFFB7185),
        primaryVariant = Color(0xFFE11D48),
        onPrimary = Color(0xFF170A0D),
        textPrimary = Color(0xFFFBEDEF),
        textSecondary = Color(0xFFC6A2A8),
        textMuted = Color(0xFF8A626B),
        success = Color(0xFF34D399),
        error = Color(0xFFF87171),
        warning = Color(0xFFFBBF24)
    )
    ThemeName.OCEAN -> BullColors(
        background = Color(0xFF071322),
        backgroundTop = Color(0xFF0D2748),
        surface = Color(0xFF0E2138),
        surfaceHover = Color(0xFF142C48),
        surfaceSunken = Color(0xFF0A1A2B),
        border = Color(0xFF1D3752),
        borderStrong = Color(0xFF2A4B6E),
        primary = Color(0xFF38BDF8),
        primaryVariant = Color(0xFF2563EB),
        onPrimary = Color(0xFF071322),
        textPrimary = Color(0xFFEAF3FB),
        textSecondary = Color(0xFF9BB4CC),
        textMuted = Color(0xFF5A7591),
        success = Color(0xFF34D399),
        error = Color(0xFFF87171),
        warning = Color(0xFFFBBF24)
    )
    ThemeName.LAVENDER -> BullColors(
        background = Color(0xFF100B1A),
        backgroundTop = Color(0xFF241636),
        surface = Color(0xFF1A1428),
        surfaceHover = Color(0xFF241C36),
        surfaceSunken = Color(0xFF140F20),
        border = Color(0xFF2F2544),
        borderStrong = Color(0xFF41345C),
        primary = Color(0xFFC084FC),
        primaryVariant = Color(0xFF9333EA),
        onPrimary = Color(0xFF100B1A),
        textPrimary = Color(0xFFF4EFFB),
        textSecondary = Color(0xFFB2A4C9),
        textMuted = Color(0xFF756792),
        success = Color(0xFF34D399),
        error = Color(0xFFF87171),
        warning = Color(0xFFFBBF24)
    )
    ThemeName.OLED -> BullColors(
        background = Color.Black,
        backgroundTop = Color(0xFF0B0B10),
        surface = Color(0xFF0A0A0F),
        surfaceHover = Color(0xFF14141C),
        surfaceSunken = Color(0xFF050507),
        border = Color(0xFF1C1C24),
        borderStrong = Color(0xFF2A2A34),
        primary = Color(0xFFA78BFA),
        primaryVariant = Color(0xFF7C5CF6),
        onPrimary = Color.Black,
        textPrimary = Color(0xFFF2F2F5),
        textSecondary = Color(0xFF9A9AA5),
        textMuted = Color(0xFF5A5A66),
        success = Color(0xFF34D399),
        error = Color(0xFFF87171),
        warning = Color(0xFFFBBF24),
        isOled = true
    )
}

object ThemeManager {
    fun getColors(theme: ThemeName): Colors {
        val c = bullColors(theme)
        return darkColors(
            background = c.background,
            surface = c.surface,
            primary = c.primary,
            primaryVariant = c.primaryVariant,
            secondary = c.primaryVariant,
            onPrimary = c.onPrimary,
            onSecondary = c.onPrimary,
            onBackground = c.textPrimary,
            onSurface = c.textPrimary
        )
    }

    fun getThemeName(theme: ThemeName): String = when (theme) {
        ThemeName.DARK -> "Feather"
        ThemeName.PURPLE -> "Amethyst"
        ThemeName.CYAN -> "Ice"
        ThemeName.GREEN -> "Emerald"
        ThemeName.RED -> "Rose"
        ThemeName.OCEAN -> "Ocean"
        ThemeName.LAVENDER -> "Lavender"
        ThemeName.OLED -> "OLED"
    }

    fun getPrimaryColor(theme: ThemeName): Color = bullColors(theme).primary

    fun getSurfaceVariant(theme: ThemeName): Color = bullColors(theme).surfaceHover

    fun isOledTheme(theme: ThemeName): Boolean = theme == ThemeName.OLED

    /**
     * Читаемый цвет текста поверх произвольного фона.
     * В OLED-теме primary = белый, поэтому захардкоженный Color.White
     * на primary-фоне становился невидимым — этот хелпер подбирает контраст.
     */
    fun onBackground(bg: Color): Color = if (luminance(bg) > 0.5) Color.Black else Color.White

    /** Относительная яркость по ITU-R BT.709 (Rec.709). */
    fun luminance(c: Color): Double = 0.2126 * c.red + 0.7152 * c.green + 0.0722 * c.blue
}
