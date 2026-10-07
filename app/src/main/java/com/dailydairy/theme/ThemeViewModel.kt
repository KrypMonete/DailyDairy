package com.dailydairy.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ThemeStore(application)
    private val initial = store.read()

    val palette: StateFlow<Palette> = store.palette.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        initial.first,
    )

    val appearance: StateFlow<Appearance> = store.appearance.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        initial.second,
    )

    fun setPalette(palette: Palette) {
        viewModelScope.launch { store.setPalette(palette) }
    }

    fun setAppearance(appearance: Appearance) {
        viewModelScope.launch { store.setAppearance(appearance) }
    }
}
