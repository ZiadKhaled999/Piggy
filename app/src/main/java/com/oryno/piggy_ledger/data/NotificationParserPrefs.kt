package com.oryno.piggy_ledger.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificationParserPrefs(private val context: Context) {

    companion object {
        val ENABLED = booleanPreferencesKey("notification_parser_enabled")
        val WATCHED = stringSetPreferencesKey("notification_parser_watched_packages")
    }

    val enabled: Flow<Boolean> = context.dataStore.data.map { it[ENABLED] ?: false }
    val watchedPackages: Flow<Set<String>> = context.dataStore.data.map { it[WATCHED] ?: emptySet() }

    suspend fun setEnabled(value: Boolean) {
        context.dataStore.edit { it[ENABLED] = value }
    }

    suspend fun setWatchedPackages(packages: Set<String>) {
        context.dataStore.edit { it[WATCHED] = packages }
    }

    suspend fun addPackage(pkg: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[WATCHED] ?: emptySet()
            prefs[WATCHED] = current + pkg
        }
    }

    suspend fun removePackage(pkg: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[WATCHED] ?: emptySet()
            prefs[WATCHED] = current - pkg
        }
    }
}
