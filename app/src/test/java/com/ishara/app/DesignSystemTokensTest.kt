package com.ishara.app

import com.ishara.app.core.designsystem.theme.IshaaraPalette
import com.ishara.app.core.designsystem.theme.darkIshaaraColors
import com.ishara.app.core.designsystem.theme.lightIshaaraColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates design system token definitions and dark/light mode parity.
 */
class DesignSystemTokensTest {

    @Test
    fun lightMode_hasWhiteBackground_andDarkForeground() {
        val lightColors = lightIshaaraColors()
        assertFalse(lightColors.isDark)
        assertEquals(IshaaraPalette.PureWhite, lightColors.background)
        assertEquals(IshaaraPalette.Neutral900, lightColors.foreground)
    }

    @Test
    fun darkMode_hasDarkBackground_andLightForeground() {
        val darkColors = darkIshaaraColors()
        assertTrue(darkColors.isDark)
        assertEquals(IshaaraPalette.DarkBackground, darkColors.background)
        assertEquals(IshaaraPalette.Neutral50, darkColors.foreground)
    }

    @Test
    fun accentColor_isPreservedAcrossThemes() {
        val lightColors = lightIshaaraColors()
        val darkColors = darkIshaaraColors()

        // Both light and dark use Amber family transit accent
        assertEquals(IshaaraPalette.Amber500, lightColors.accent)
        assertEquals(IshaaraPalette.Amber400, darkColors.accent)
    }

    @Test
    fun darkTheme_isNotSimpleInversion() {
        val lightColors = lightIshaaraColors()
        val darkColors = darkIshaaraColors()

        // Dark background should be a rich dark charcoal, not inverted white
        assertNotEquals(IshaaraPalette.PureBlack, darkColors.background)
        assertEquals(IshaaraPalette.DarkBackground, darkColors.background)
    }
}
