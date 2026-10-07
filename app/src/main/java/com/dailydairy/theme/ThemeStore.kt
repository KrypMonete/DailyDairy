package com.dailydairy.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "theme")

class ThemeStore(private val context: Context) {
    private val paletteKey = stringPreferencesKey("palette")
    private val appearanceKey = stringPreferencesKey("appearance")

    val palette: Flow<Palette> = context.dataStore.data.map { prefs ->
        prefs[paletteKey]?.let { runCatching { Palette.valueOf(it) }.getOrNull() } ?: Palette.Sistem
    }

    val appearance: Flow<Appearance> = context.dataStore.data.map { prefs ->
        prefs[appearanceKey]?.let { runCatching { Appearance.valueOf(it) }.getOrNull() } ?: Appearance.System
    }

    fun read(): Pair<Palette, Appearance> = runBlocking {
        palette.first() to appearance.first()
    }

    suspend fun setPalette(palette: Palette) {
        context.dataStore.edit { it[paletteKey] = palette.name }
    }

    suspend fun setAppearance(appearance: Appearance) {
        context.dataStore.edit { it[appearanceKey] = appearance.name }
    }
}
