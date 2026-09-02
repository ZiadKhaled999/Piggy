package com.oryno.piggy_ledger.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    companion object {
        val HAS_ONBOARDED = booleanPreferencesKey("has_onboarded")
        val HAS_LANGUAGE_SELECTED = booleanPreferencesKey("has_language_selected")
        val HAS_HEARD_ABOUT_US = booleanPreferencesKey("has_heard_about_us")
        val IS_AUTHENTICATED = booleanPreferencesKey("is_authenticated")
        val AUTH_USER_EMAIL = stringPreferencesKey("auth_user_email")
        val AUTH_USER_NAME = stringPreferencesKey("auth_user_name")
        val AUTH_USER_PHOTO_URL = stringPreferencesKey("auth_user_photo_url")
        val IS_BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("is_biometric_lock_enabled")
        val LOCK_TIMEOUT_SECONDS = longPreferencesKey("lock_timeout_seconds")
        val LAST_EXIT_TIME = longPreferencesKey("last_exit_time")
        val IS_SCREENSHOT_PROTECTION_ENABLED = booleanPreferencesKey("is_screenshot_protection_enabled")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val PREMIUM_EXPIRY_TIMESTAMP = longPreferencesKey("premium_expiry_timestamp")
        val IS_LIFETIME_PREMIUM = booleanPreferencesKey("is_lifetime_premium")
        val AI_MESSAGES_COUNT = intPreferencesKey("ai_messages_count")
        val PERSONALIZED_INTENT = intPreferencesKey("personalized_intent")
        val PERSONALIZED_INTENSITY = intPreferencesKey("personalized_intensity")
        val SAVING_MODE = stringPreferencesKey("saving_mode")
        val CUSTOM_IDENTIFIERS_JSON = stringPreferencesKey("custom_identifiers_json")
        val IS_PRIVACY_MODE_ENABLED = booleanPreferencesKey("is_privacy_mode_enabled")
        val PREFERRED_ACCOUNT_ID = stringPreferencesKey("preferred_account_id")
        val APP_CURRENCY = stringPreferencesKey("app_currency")
    }

    val appCurrency: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[APP_CURRENCY] ?: "USD"
    }

    val preferredAccountId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PREFERRED_ACCOUNT_ID]
    }

    val customIdentifiersJson: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[CUSTOM_IDENTIFIERS_JSON] ?: "{}"
    }

    val personalizedIntent: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PERSONALIZED_INTENT] ?: -1
    }

    val personalizedIntensity: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PERSONALIZED_INTENSITY] ?: -1
    }

    val savingMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SAVING_MODE] ?: "piggy"
    }

    val hasOnboarded: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HAS_ONBOARDED] ?: false
    }
    
    val hasLanguageSelected: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HAS_LANGUAGE_SELECTED] ?: false
    }

    val hasHeardAboutUs: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HAS_HEARD_ABOUT_US] ?: false
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

    val isPrivacyModeEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_PRIVACY_MODE_ENABLED] ?: false
    }

    val isPremium: Flow<Boolean> = context.dataStore.data.map { prefs ->
        val isPremiumFlag = prefs[IS_PREMIUM] ?: false
        val expiry = prefs[PREMIUM_EXPIRY_TIMESTAMP] ?: 0L
        val isLifetime = prefs[IS_LIFETIME_PREMIUM] ?: false
        
        if (isLifetime) return@map true
        if (isPremiumFlag && (expiry == 0L || expiry > System.currentTimeMillis())) return@map true
        false
    }

    val aiMessagesCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[AI_MESSAGES_COUNT] ?: 0
    }

    suspend fun incrementAiMessagesCount() {
        context.dataStore.edit { prefs ->
            val current = prefs[AI_MESSAGES_COUNT] ?: 0
            prefs[AI_MESSAGES_COUNT] = current + 1
        }
    }

    suspend fun resetAiMessagesCount() {
        context.dataStore.edit { prefs ->
            prefs[AI_MESSAGES_COUNT] = 0
        }
    }

    suspend fun saveOnboarding(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HAS_ONBOARDED] = completed
        }
        syncPreferencesToDb()
    }
    
    suspend fun saveLanguageSelected(selected: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HAS_LANGUAGE_SELECTED] = selected
        }
        syncPreferencesToDb()
    }

    suspend fun saveHeardAboutUs(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HAS_HEARD_ABOUT_US] = completed
        }
        syncPreferencesToDb()
    }

    suspend fun saveAuthentication(authenticated: Boolean, email: String = "", name: String = "", photoUrl: String = "") {
        context.dataStore.edit { prefs ->
            prefs[IS_AUTHENTICATED] = authenticated
            prefs[AUTH_USER_EMAIL] = email
            prefs[AUTH_USER_NAME] = name
            prefs[AUTH_USER_PHOTO_URL] = photoUrl
        }
        syncPreferencesToDb()
    }

    suspend fun saveBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_BIOMETRIC_LOCK_ENABLED] = enabled
        }
        syncPreferencesToDb()
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
        syncPreferencesToDb()
    }

    suspend fun savePrivacyModeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_PRIVACY_MODE_ENABLED] = enabled
        }
    }

    suspend fun savePremiumStatus(isPremium: Boolean, expiryTimestamp: Long = 0L, isLifetime: Boolean = false) {
        context.dataStore.edit { prefs ->
            prefs[IS_PREMIUM] = isPremium
            prefs[PREMIUM_EXPIRY_TIMESTAMP] = expiryTimestamp
            prefs[IS_LIFETIME_PREMIUM] = isLifetime
        }
        syncPreferencesToDb()
    }

    suspend fun savePersonalization(intent: Int, intensity: Int, savingMode: String) {
        context.dataStore.edit { prefs ->
            prefs[PERSONALIZED_INTENT] = intent
            prefs[PERSONALIZED_INTENSITY] = intensity
            prefs[SAVING_MODE] = savingMode
        }
        syncPreferencesToDb()
    }

    suspend fun saveCustomIdentifiersJson(json: String) {
        context.dataStore.edit { prefs ->
            prefs[CUSTOM_IDENTIFIERS_JSON] = json
        }
        syncPreferencesToDb()
    }

    suspend fun savePreferredAccountId(accountId: String?) {
        context.dataStore.edit { prefs ->
            if (accountId != null) {
                prefs[PREFERRED_ACCOUNT_ID] = accountId
            } else {
                prefs.remove(PREFERRED_ACCOUNT_ID)
            }
        }
        syncPreferencesToDb()
    }

    suspend fun saveAppCurrency(currencyCode: String) {
        context.dataStore.edit { prefs ->
            prefs[APP_CURRENCY] = currencyCode
        }
        syncPreferencesToDb()
    }

    suspend fun syncPreferencesToDb() {
        try {
            val user = com.clerk.api.Clerk.userFlow.value
            val userId = user?.id ?: "local_user"
            val prefs = context.dataStore.data.first()
            val entity = UserPreferencesEntity(
                userId = userId,
                hasOnboarded = prefs[HAS_ONBOARDED] ?: false,
                hasLanguageSelected = prefs[HAS_LANGUAGE_SELECTED] ?: false,
                hasHeardAboutUs = prefs[HAS_HEARD_ABOUT_US] ?: false,
                personalizedIntent = prefs[PERSONALIZED_INTENT] ?: -1,
                personalizedIntensity = prefs[PERSONALIZED_INTENSITY] ?: -1,
                savingMode = prefs[SAVING_MODE] ?: "piggy",
                customIdentifiersJson = prefs[CUSTOM_IDENTIFIERS_JSON] ?: "{}",
                isBiometricLockEnabled = prefs[IS_BIOMETRIC_LOCK_ENABLED] ?: false,
                isScreenshotProtectionEnabled = prefs[IS_SCREENSHOT_PROTECTION_ENABLED] ?: false,
                isPremium = prefs[IS_PREMIUM] ?: false,
                premiumExpiryTimestamp = prefs[PREMIUM_EXPIRY_TIMESTAMP] ?: 0L,
                isLifetimePremium = prefs[IS_LIFETIME_PREMIUM] ?: false,
                preferredAccountId = prefs[PREFERRED_ACCOUNT_ID],
                appCurrency = prefs[APP_CURRENCY] ?: "USD",
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            val dao = PiggyLedgerDatabase.getInstance(context.applicationContext).piggyLedgerDao()
            dao.insertUserPreferences(entity)
            triggerSync(context)
        } catch (e: Exception) {
            android.util.Log.e("UserPreferences", "Failed to sync preferences to Room", e)
        }
    }

    suspend fun applyFromEntity(entity: UserPreferencesEntity) {
        context.dataStore.edit { prefs ->
            if (entity.hasOnboarded) prefs[HAS_ONBOARDED] = true
            if (entity.hasLanguageSelected) prefs[HAS_LANGUAGE_SELECTED] = true
            if (entity.hasHeardAboutUs) prefs[HAS_HEARD_ABOUT_US] = true
            prefs[PERSONALIZED_INTENT] = entity.personalizedIntent
            prefs[PERSONALIZED_INTENSITY] = entity.personalizedIntensity
            prefs[SAVING_MODE] = entity.savingMode
            prefs[CUSTOM_IDENTIFIERS_JSON] = entity.customIdentifiersJson
            prefs[IS_BIOMETRIC_LOCK_ENABLED] = entity.isBiometricLockEnabled
            prefs[IS_SCREENSHOT_PROTECTION_ENABLED] = entity.isScreenshotProtectionEnabled
            prefs[IS_PREMIUM] = entity.isPremium
            prefs[PREMIUM_EXPIRY_TIMESTAMP] = entity.premiumExpiryTimestamp
            prefs[IS_LIFETIME_PREMIUM] = entity.isLifetimePremium
            if (entity.preferredAccountId != null) {
                prefs[PREFERRED_ACCOUNT_ID] = entity.preferredAccountId
            } else {
                prefs.remove(PREFERRED_ACCOUNT_ID)
            }
            prefs[APP_CURRENCY] = entity.appCurrency
        }
    }

    suspend fun clearForLogout() {
        context.dataStore.edit { prefs ->
            val hasOnboarded = prefs[HAS_ONBOARDED] ?: true
            val hasLanguageSelected = prefs[HAS_LANGUAGE_SELECTED] ?: true
            val hasHeardAboutUs = prefs[HAS_HEARD_ABOUT_US] ?: true

            prefs.clear()

            prefs[HAS_ONBOARDED] = hasOnboarded
            prefs[HAS_LANGUAGE_SELECTED] = hasLanguageSelected
            prefs[HAS_HEARD_ABOUT_US] = hasHeardAboutUs
        }
    }

    suspend fun getInitialDestination(): com.oryno.piggy_ledger.ui.Screen {
        val prefs = context.dataStore.data.first()
        val hasLang = prefs[HAS_LANGUAGE_SELECTED] ?: false
        if (!hasLang) return com.oryno.piggy_ledger.ui.Screen.LanguageSelection
        val hasHeard = prefs[HAS_HEARD_ABOUT_US] ?: false
        if (!hasHeard) return com.oryno.piggy_ledger.ui.Screen.HearAboutUs
        val hasOnboarded = prefs[HAS_ONBOARDED] ?: false
        if (!hasOnboarded) return com.oryno.piggy_ledger.ui.Screen.Onboarding
        val isAuth = prefs[IS_AUTHENTICATED] ?: false
        if (!isAuth) return com.oryno.piggy_ledger.ui.Screen.Auth
        return com.oryno.piggy_ledger.ui.Screen.MainContainer
    }

    suspend fun clearAll() {
        clearForLogout()
    }

    private fun triggerSync(context: Context) {
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.oryno.piggy_ledger.service.SyncWorker>().build()
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "SyncWork",
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest
        )
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                com.oryno.piggy_ledger.service.SyncManager(context).syncAll()
            } catch (e: Exception) {
                android.util.Log.e("UserPreferences", "Direct sync trigger failed", e)
            }
        }
    }
}
