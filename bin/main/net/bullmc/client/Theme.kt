package net.bullmc.client

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.ui.graphics.Color

enum class ThemeName {
    DARK, PURPLE, CYAN, GREEN, RED
}

object ThemeManager {
    fun getColors(theme: ThemeName): Colors = when (theme) {
        ThemeName.DARK -> darkColors(
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            primary = Color(0xFFFF7A00),
            onPrimary = Color.White
        )
        ThemeName.PURPLE -> darkColors(
            background = Color(0xFF1A0F2E),
            surface = Color(0xFF2D1B4E),
            primary = Color(0xFF9D4EDD),
            onPrimary = Color.White
        )
        ThemeName.CYAN -> darkColors(
            background = Color(0xFF0F1419),
            surface = Color(0xFF1B2838),
            primary = Color(0xFF00D9FF),
            onPrimary = Color(0xFF0F1419)
        )
        ThemeName.GREEN -> darkColors(
            background = Color(0xFF0F2818),
            surface = Color(0xFF1A4D2E),
            primary = Color(0xFF40E0D0),
            onPrimary = Color.White
        )
        ThemeName.RED -> darkColors(
            background = Color(0xFF2C0B0E),
            surface = Color(0xFF4A1519),
            primary = Color(0xFFE63946),
            onPrimary = Color.White
        )
    }

    fun getThemeName(theme: ThemeName): String = when (theme) {
        ThemeName.DARK -> "Тёмная (стандартная)"
        ThemeName.PURPLE -> "Фиолетовая"
        ThemeName.CYAN -> "Голубая"
        ThemeName.GREEN -> "Зелёная"
        ThemeName.RED -> "Красная"
    }

    fun getPrimaryColor(theme: ThemeName): Color = when (theme) {
        ThemeName.DARK -> Color(0xFFFF7A00)
        ThemeName.PURPLE -> Color(0xFF9D4EDD)
        ThemeName.CYAN -> Color(0xFF00D9FF)
        ThemeName.GREEN -> Color(0xFF40E0D0)
        ThemeName.RED -> Color(0xFFE63946)
    }
}
