package com.runcheck

import android.content.res.Configuration
import com.runcheck.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppLaunchThemePolicyTest {
    @Test
    fun `starting splash uses one neutral surface in system light and dark modes`() {
        val application = RuntimeEnvironment.getApplication()
        val lightConfiguration =
            Configuration(application.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
            }
        val darkConfiguration =
            Configuration(application.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
            }

        val lightSplash =
            application
                .createConfigurationContext(lightConfiguration)
                .getColor(R.color.splash_background)
        val darkSplash =
            application
                .createConfigurationContext(darkConfiguration)
                .getColor(R.color.splash_background)

        assertEquals(lightSplash, darkSplash)
    }

    @Test
    fun `splash stays held until theme and matching system bar appearance are ready`() {
        assertTrue(shouldKeepSplashOnScreen(themeMode = null, systemBarsReady = false))
        assertTrue(shouldKeepSplashOnScreen(themeMode = ThemeMode.LIGHT, systemBarsReady = false))
        assertFalse(shouldKeepSplashOnScreen(themeMode = ThemeMode.LIGHT, systemBarsReady = true))
    }

    @Test
    fun `manual theme overrides opposing system appearance for system bar icons`() {
        val manualLight = resolveSystemBarAppearance(ThemeMode.LIGHT, systemInDarkTheme = true)
        val manualDark = resolveSystemBarAppearance(ThemeMode.DARK, systemInDarkTheme = false)

        assertTrue(manualLight.useDarkIcons)
        assertFalse(manualLight.useDarkScrim)
        assertFalse(manualDark.useDarkIcons)
        assertTrue(manualDark.useDarkScrim)
    }

    @Test
    fun `system theme follows runtime system appearance and unready theme has no appearance`() {
        assertFalse(resolveSystemBarAppearance(ThemeMode.SYSTEM, systemInDarkTheme = true).useDarkIcons)
        assertTrue(resolveSystemBarAppearance(ThemeMode.SYSTEM, systemInDarkTheme = false).useDarkIcons)
        assertNull(resolveSystemBarAppearanceOrNull(themeMode = null, systemInDarkTheme = true))
    }
}
