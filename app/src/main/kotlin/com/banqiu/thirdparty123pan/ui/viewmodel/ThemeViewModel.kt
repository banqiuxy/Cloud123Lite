package com.banqiu.thirdparty123pan.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banqiu.thirdparty123pan.data.prefs.SettingsStore
import com.banqiu.thirdparty123pan.data.prefs.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    settingsStore: SettingsStore
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsStore.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val dynamicColor: StateFlow<Boolean> = settingsStore.dynamicColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
}