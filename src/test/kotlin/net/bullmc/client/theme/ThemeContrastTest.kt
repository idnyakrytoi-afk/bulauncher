package net.bullmc.client.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Регрессия на OLED-тему: primary = белый, и любой белый текст
 * на сплошном primary-фоне становился невидимым.
 */
class ThemeContrastTest {

    @Test
    fun `onBackground picks black on light colors`() {
        assertEquals(Color.Black, ThemeManager.onBackground(Color.White))
        assertEquals(Color.Black, ThemeManager.onBackground(Color(0xFFFF7A00))) // оранжевый DARK
        assertEquals(Color.Black, ThemeManager.onBackground(Color(0xFF34D399))) // изумрудный
    }

    @Test
    fun `onBackground picks white on dark colors`() {
        assertEquals(Color.White, ThemeManager.onBackground(Color.Black))
        assertEquals(Color.White, ThemeManager.onBackground(Color(0xFF0D1117)))
    }

    @Test
    fun `every theme primary has a readable foreground`() {
        ThemeName.entries.forEach { theme ->
            val primary = ThemeManager.getPrimaryColor(theme)
            val fg = ThemeManager.onBackground(primary)
            // контраст: яркости текста и фона должны быть по разные стороны порога
            val readable = ThemeManager.luminance(fg) > 0.5 != ThemeManager.luminance(primary) > 0.5
            kotlin.test.assertTrue(readable, "$theme: текст $fg нечитаем на $primary")
        }
    }
}
