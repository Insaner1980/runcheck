package com.runcheck.ui.charger

import com.runcheck.domain.model.ChargerSummary
import com.runcheck.domain.usecase.AddChargerUseCase
import com.runcheck.domain.usecase.DeleteChargerUseCase
import com.runcheck.domain.usecase.GetChargerComparisonUseCase
import com.runcheck.domain.usecase.IsProUserUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.ui.MainDispatcherRule
import com.runcheck.ui.common.UiText
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChargerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getChargerComparison: GetChargerComparisonUseCase = mockk()
    private val addChargerUseCase: AddChargerUseCase = mockk(relaxed = true)
    private val deleteChargerUseCase: DeleteChargerUseCase = mockk(relaxed = true)
    private val observeProAccess: ObserveProAccessUseCase = mockk()
    private val isProUser: IsProUserUseCase = mockk()
    private val manageUserPreferences: ManageUserPreferencesUseCase = mockk(relaxed = true)

    @Before
    fun setup() {
        every { observeProAccess() } returns flowOf(false)
        every { isProUser() } returns false
        every { manageUserPreferences.observeSelectedChargerId() } returns flowOf(null)
        every { getChargerComparison() } returns flowOf(emptyList())
    }

    @Test
    fun `refresh locks charger comparison for non pro users`() {
        val viewModel = createViewModel()

        viewModel.refresh()

        assertEquals(ChargerUiState.Locked, viewModel.uiState.value)
    }

    @Test
    fun `pro observer loads charger data and selected charger`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val chargers = listOf(chargerSummary(id = 7L, name = "Desk charger"))
            every { observeProAccess() } returns flowOf(true)
            every { getChargerComparison() } returns flowOf(chargers)
            every { manageUserPreferences.observeSelectedChargerId() } returns flowOf(7L)
            val viewModel = createViewModel()

            viewModel.startObserving()
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue("Expected Success but got $state", state is ChargerUiState.Success)
            val success = state as ChargerUiState.Success
            assertEquals(chargers, success.chargers)
            assertEquals(7L, success.selectedChargerId)
        }

    @Test
    fun `pro actions are ignored when user is not pro`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { isProUser() } returns false
            val viewModel = createViewModel()

            viewModel.addCharger("Travel charger")
            viewModel.deleteCharger(3L)
            viewModel.selectCharger(3L)
            viewModel.clearSelectedCharger()
            runCurrent()

            coVerify(exactly = 0) { addChargerUseCase(any()) }
            coVerify(exactly = 0) { deleteChargerUseCase(any()) }
            coVerify(exactly = 0) { manageUserPreferences.setSelectedChargerId(any()) }
        }

    @Test
    fun `load errors are exposed as error state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { observeProAccess() } returns flowOf(true)
            every { getChargerComparison() } returns flow { throw IllegalStateException("charger failed") }
            val viewModel = createViewModel()

            viewModel.startObserving()
            runCurrent()

            assertEquals(ChargerUiState.Error(UiText.Dynamic("charger failed")), viewModel.uiState.value)
        }

    private fun createViewModel(): ChargerViewModel =
        ChargerViewModel(
            getChargerComparison = getChargerComparison,
            addChargerUseCase = addChargerUseCase,
            deleteChargerUseCase = deleteChargerUseCase,
            observeProAccess = observeProAccess,
            isProUser = isProUser,
            manageUserPreferences = manageUserPreferences,
        )

    private fun chargerSummary(
        id: Long,
        name: String,
    ): ChargerSummary =
        ChargerSummary(
            chargerId = id,
            chargerName = name,
            sessionCount = 2,
            avgChargingSpeedMa = 1_200,
            avgPowerMw = 5_000,
            latestChargingSpeedMa = 1_300,
            latestPowerMw = 5_200,
            avgTimeToFullMinutes = 90,
            lastUsed = 123L,
            hasActiveSession = false,
        )
}
