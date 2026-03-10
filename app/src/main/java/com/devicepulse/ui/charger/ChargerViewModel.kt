package com.devicepulse.ui.charger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicepulse.data.db.dao.ChargerDao
import com.devicepulse.data.db.entity.ChargerProfileEntity
import com.devicepulse.domain.usecase.GetChargerComparisonUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChargerViewModel @Inject constructor(
    private val getChargerComparison: GetChargerComparisonUseCase,
    private val chargerDao: ChargerDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChargerUiState>(ChargerUiState.Loading)
    val uiState: StateFlow<ChargerUiState> = _uiState.asStateFlow()

    init {
        loadChargerData()
    }

    fun refresh() {
        loadChargerData()
    }

    fun addCharger(name: String) {
        viewModelScope.launch {
            chargerDao.insertCharger(
                ChargerProfileEntity(
                    name = name.trim(),
                    created = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteCharger(id: Long) {
        viewModelScope.launch {
            chargerDao.deleteChargerById(id)
        }
    }

    private fun loadChargerData() {
        viewModelScope.launch {
            combine(
                getChargerComparison(),
                chargerDao.getAllSessions()
            ) { chargers, sessions ->
                ChargerUiState.Success(
                    chargers = chargers,
                    sessions = sessions
                )
            }.catch { e ->
                _uiState.value = ChargerUiState.Error(e.message ?: "Unknown error")
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
