package com.oryno.piggy_ledger.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NotificationParserPrefs(private val context: Context) {

    companion object {
        val ENABLED = booleanPreferencesKey("notification_parser_enabled")
        val WATCHED = stringSetPreferencesKey("notification_parser_watched_packages")
        val PACKAGE_ACCOUNTS = stringPreferencesKey("notification_parser_package_accounts")

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        internal fun decodePackageAccounts(raw: String?): Map<String, List<String>> {
            if (raw.isNullOrBlank()) return emptyMap()
            return try {
                json.decodeFromString<Map<String, List<String>>>(raw)
            } catch (_: Exception) {
                emptyMap()
            }
        }

        internal fun encodePackageAccounts(map: Map<String, List<String>>): String {
            return json.encodeToString(map)
        }
    }

    val enabled: Flow<Boolean> = context.dataStore.data.map { it[ENABLED] ?: false }
    val watchedPackages: Flow<Set<String>> = context.dataStore.data.map { it[WATCHED] ?: emptySet() }
    val packageAccountMap: Flow<Map<String, List<String>>> = context.dataStore.data.map {
        decodePackageAccounts(it[PACKAGE_ACCOUNTS])
    }

    suspend fun setEnabled(value: Boolean) {
        context.dataStore.edit { it[ENABLED] = value }
    }

    suspend fun setWatchedPackages(packages: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[WATCHED] = packages
            val map = decodePackageAccounts(prefs[PACKAGE_ACCOUNTS])
                .filterKeys { it in packages }
            prefs[PACKAGE_ACCOUNTS] = encodePackageAccounts(map)
        }
    }

    suspend fun addPackage(pkg: String) {
        if (pkg.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[WATCHED] ?: emptySet()
            prefs[WATCHED] = current + pkg
        }
    }

    suspend fun removePackage(pkg: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[WATCHED] ?: emptySet()
            prefs[WATCHED] = current - pkg
            val map = decodePackageAccounts(prefs[PACKAGE_ACCOUNTS]).toMutableMap()
            if (map.remove(pkg) != null) {
                prefs[PACKAGE_ACCOUNTS] = encodePackageAccounts(map)
            }
        }
    }

    /**
     * Links [accountId] to [packageName] (or unlinks it if [packageName] is null/blank).
     * Automatically adds [packageName] to watched packages, removes [accountId] from any
     * previously linked package so each account points to at most one app, and optionally
     * enables the parser when notification listener access is already granted.
     */
    suspend fun setAccountNotificationPackage(
        accountId: String,
        packageName: String?,
        autoEnableIfAccessGranted: Boolean = false
    ) {
        if (accountId.isBlank()) return
        context.dataStore.edit { prefs ->
            val map = decodePackageAccounts(prefs[PACKAGE_ACCOUNTS]).toMutableMap()

            // Remove accountId from any existing package entry
            val keysToUpdate = map.keys.toList()
            for (key in keysToUpdate) {
                val filtered = map[key]?.filter { it != accountId }.orEmpty()
                if (filtered.isEmpty()) {
                    map.remove(key)
                } else {
                    map[key] = filtered
                }
            }

            val cleanPkg = packageName?.trim()?.takeIf { it.isNotEmpty() }
            if (cleanPkg != null) {
                val existing = map[cleanPkg].orEmpty()
                map[cleanPkg] = (existing + accountId).distinct()

                val currentWatched = prefs[WATCHED] ?: emptySet()
                prefs[WATCHED] = currentWatched + cleanPkg

                if (autoEnableIfAccessGranted) {
                    prefs[ENABLED] = true
                }
            }

            prefs[PACKAGE_ACCOUNTS] = encodePackageAccounts(map)
        }
    }

    /**
     * Replaces all linked account IDs for a given watched [packageName].
     */
    suspend fun setAccountsForPackage(packageName: String, accountIds: List<String>) {
        if (packageName.isBlank()) return
        context.dataStore.edit { prefs ->
            val map = decodePackageAccounts(prefs[PACKAGE_ACCOUNTS]).toMutableMap()
            val cleaned = accountIds.filter { it.isNotBlank() }.distinct()
            if (cleaned.isEmpty()) {
                map.remove(packageName)
            } else {
                map[packageName] = cleaned
                val currentWatched = prefs[WATCHED] ?: emptySet()
                prefs[WATCHED] = currentWatched + packageName
            }
            prefs[PACKAGE_ACCOUNTS] = encodePackageAccounts(map)
        }
    }

    suspend fun unlinkAccountFromAllPackages(accountId: String) {
        setAccountNotificationPackage(accountId = accountId, packageName = null)
    }

    suspend fun getAccountsForPackage(packageName: String): List<String> {
        return packageAccountMap.first()[packageName].orEmpty()
    }

    suspend fun getPackageForAccount(accountId: String): String? {
        val map = packageAccountMap.first()
        return map.entries.firstOrNull { accountId in it.value }?.key
    }
}
