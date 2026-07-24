package com.runcheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.domain.model.ThemeMode
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppThemeViewModel
    @Inject
    constructor(
        manageUserPreferences: ManageUserPreferencesUseCase,
    ) : ViewModel() {
        val themeMode: StateFlow<ThemeMode?> =
            manageUserPreferences
                .observePreferences()
                .map { preferences -> preferences.themeMode }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = null,
                )
    }
