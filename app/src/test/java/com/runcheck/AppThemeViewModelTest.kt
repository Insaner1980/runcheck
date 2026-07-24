package com.runcheck

import com.runcheck.domain.model.ThemeMode
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.ui.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppThemeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `theme stays unready until the first preference value arrives`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val preferences = MutableSharedFlow<UserPreferences>()
            val manageUserPreferences = mockk<ManageUserPreferencesUseCase>()
            every { manageUserPreferences.observePreferences() } returns preferences

            val viewModel = AppThemeViewModel(manageUserPreferences)
            runCurrent()

            assertNull(viewModel.themeMode.value)

            preferences.emit(UserPreferences(themeMode = ThemeMode.LIGHT))
            runCurrent()

            assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
        }
}
