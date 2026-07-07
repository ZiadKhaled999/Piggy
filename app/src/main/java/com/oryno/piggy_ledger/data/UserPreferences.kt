package com.oryno.piggy_ledger.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    companion object {
        val HAS_ONBOARDED = booleanPreferencesKey("has_onboarded")
        val HAS_LANGUAGE_SELECTED = booleanPreferencesKey("has_language_selected")
    }

    val hasOnboarded: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HAS_ONBOARDED] ?: false
    }
    
    val hasLanguageSelected: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HAS_LANGUAGE_SELECTED] ?: false
    }

    suspend fun saveOnboarding(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HAS_ONBOARDED] = completed
        }
    }
    
    suspend fun saveLanguageSelected(selected: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HAS_LANGUAGE_SELECTED] = selected
        }
    }
}
