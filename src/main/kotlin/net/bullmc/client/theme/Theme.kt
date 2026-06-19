package net.bullmc.client.theme

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.ui.graphics.Color

enum class ThemeName {
    DARK, PURPLE, CYAN, GREEN, RED, OCEAN, LAVENDER
}

object ThemeManager {
    fun getColors(theme: ThemeName): Colors = when (theme) {
        ThemeName.DARK -> darkColors(
            background = Color(0xFF0D1117),
            surface = Color(0xFF161B22),
            primary = Color(0xFFFF7A00),
            onPrimary = Color.White
        )
        ThemeName.PURPLE -> darkColors(
            background = Color(0xFF13111C),
            surface = Color(0xFF1C1930),
            primary = Color(0xFFA78BFA),
            onPrimary = Color(0xFF13111C)
        )
        ThemeName.CYAN -> darkColors(
            background = Color(0xFF0B1620),
            surface = Color(0xFF122232),
            primary = Color(0xFF22D3EE),
            onPrimary = Color(0xFF0B1620)
        )
        ThemeName.GREEN -> darkColors(
            background = Color(0xFF0B1A14),
            surface = Color(0xFF122E21),
            primary = Color(0xFF34D399),
            onPrimary = Color(0xFF0B1A14)
        )
        ThemeName.RED -> darkColors(
            background = Color(0xFF1A0B0E),
            surface = Color(0xFF2A1216),
            primary = Color(0xFFFB7185),
            onPrimary = Color(0xFF1A0B0E)
        )
        ThemeName.OCEAN -> darkColors(
            background = Color(0xFF0A1628),
            surface = Color(0xFF122640),
            primary = Color(0xFF38BDF8),
            onPrimary = Color(0xFF0A1628)
        )
        ThemeName.LAVENDER -> darkColors(
            background = Color(0xFF14101E),
            surface = Color(0xFF201A30),
            primary = Color(0xFFC084FC),
            onPrimary = Color(0xFF14101E)
        )
    }

    fun getThemeName(theme: ThemeName): String = when (theme) {
        ThemeName.DARK -> "GitHub Dark"
        ThemeName.PURPLE -> "Amethyst"
        ThemeName.CYAN -> "Ice"
        ThemeName.GREEN -> "Emerald"
        ThemeName.RED -> "Rose"
        ThemeName.OCEAN -> "Ocean"
        ThemeName.LAVENDER -> "Lavender"
    }

    fun getPrimaryColor(theme: ThemeName): Color = when (theme) {
        ThemeName.DARK -> Color(0xFFFF7A00)
        ThemeName.PURPLE -> Color(0xFFA78BFA)
        ThemeName.CYAN -> Color(0xFF22D3EE)
        ThemeName.GREEN -> Color(0xFF34D399)
        ThemeName.RED -> Color(0xFFFB7185)
        ThemeName.OCEAN -> Color(0xFF38BDF8)
        ThemeName.LAVENDER -> Color(0xFFC084FC)
    }

    fun getSurfaceVariant(theme: ThemeName): Color = when (theme) {
        ThemeName.DARK -> Color(0xFF21262D)
        ThemeName.PURPLE -> Color(0xFF272140)
        ThemeName.CYAN -> Color(0xFF1A3040)
        ThemeName.GREEN -> Color(0xFF1A4030)
        ThemeName.RED -> Color(0xFF3A1A1E)
        ThemeName.OCEAN -> Color(0xFF1A3450)
        ThemeName.LAVENDER -> Color(0xFF2E2448)
    }
}
