package com.oryno.piggy_ledger.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    companion object {
        val HAS_ONBOARDED = booleanPreferencesKey("has_onboarded")
        val HAS_LANGUAGE_SELECTED = booleanPreferencesKey("has_language_selected")
        val IS_AUTHENTICATED = booleanPreferencesKey("is_authenticated")
        val AUTH_USER_EMAIL = stringPreferencesKey("auth_user_email")
        val AUTH_USER_NAME = stringPreferencesKey("auth_user_name")
        val AUTH_USER_PHOTO_URL = stringPreferencesKey("auth_user_photo_url")
        val IS_BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("is_biometric_lock_enabled")
        val LOCK_TIMEOUT_SECONDS = longPreferencesKey("lock_timeout_seconds")
        val LAST_EXIT_TIME = longPreferencesKey("last_exit_time")
        val IS_SCREENSHOT_PROTECTION_ENABLED = booleanPreferencesKey("is_screenshot_protection_enabled")
        val PIN_LOCK = stringPreferencesKey("pin_lock")
    }

    val hasOnboarded: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HAS_ONBOARDED] ?: false
    }
    
    val hasLanguageSelected: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HAS_LANGUAGE_SELECTED] ?: false
    }

    val isAuthenticated: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_AUTHENTICATED] ?: false
    }

    val authUserEmail: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[AUTH_USER_EMAIL] ?: ""
    }

    val authUserName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[AUTH_USER_NAME] ?: ""
    }

    val authUserPhotoUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[AUTH_USER_PHOTO_URL] ?: ""
    }

    val isBiometricLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_BIOMETRIC_LOCK_ENABLED] ?: false
    }

    val lockTimeoutSeconds: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[LOCK_TIMEOUT_SECONDS] ?: 0L // 0 means instant
    }

    val lastExitTime: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[LAST_EXIT_TIME] ?: 0L
    }

    val isScreenshotProtectionEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_SCREENSHOT_PROTECTION_ENABLED] ?: false
    }

    val pinLock: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PIN_LOCK]
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

    suspend fun saveAuthentication(authenticated: Boolean, email: String = "", name: String = "", photoUrl: String = "") {
        context.dataStore.edit { prefs ->
            prefs[IS_AUTHENTICATED] = authenticated
            prefs[AUTH_USER_EMAIL] = email
            prefs[AUTH_USER_NAME] = name
            prefs[AUTH_USER_PHOTO_URL] = photoUrl
        }
    }

    suspend fun saveBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_BIOMETRIC_LOCK_ENABLED] = enabled
        }
    }

    suspend fun saveLockTimeout(seconds: Long) {
        context.dataStore.edit { prefs ->
            prefs[LOCK_TIMEOUT_SECONDS] = seconds
        }
    }

    suspend fun saveLastExitTime(time: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_EXIT_TIME] = time
        }
    }

    suspend fun saveScreenshotProtectionEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_SCREENSHOT_PROTECTION_ENABLED] = enabled
        }
    }

    suspend fun savePinLock(pin: String?) {
        context.dataStore.edit { prefs ->
            if (pin == null) {
                prefs.remove(PIN_LOCK)
            } else {
                prefs[PIN_LOCK] = pin
            }
        }
    }
}
