package com.oryno.piggy_ledger.service

import android.content.Context
import android.util.Log
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.GetTokenOptions
import com.clerk.api.session.fetchToken
import com.oryno.piggy_ledger.BuildConfig
import com.oryno.piggy_ledger.data.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncManager(private val context: Context) {
    private val db = PiggyLedgerDatabase.getInstance(context)
    private val dao = db.piggyLedgerDao()
    private val ktorClient = ApiClient.ktorClient
    private val apiUrl = BuildConfig.NEON_API_URL

    private suspend fun getAuthHeader(): String? {
        val session = Clerk.sessionFlow.value
        if (session == null) {
            Log.w("SyncManager", "🔑 Clerk session is null!")
            return null
        }

        return try {
            val result = session.fetchToken(GetTokenOptions())
            if (result is ClerkResult.Success && !result.value.jwt.isNullOrEmpty()) {
                Log.d("SyncManager", "🔑 Successfully retrieved Clerk JWT token.")
                "Bearer ${result.value.jwt}"
            } else {
                Log.w("SyncManager", "🔑 Token fetch returned empty or non-success result: $result")
                null
            }
        } catch (e: Exception) {
            Log.w("SyncManager", "Could not fetch Clerk token", e)
            null
        }
    }

    suspend fun syncAll() = withContext(Dispatchers.IO) {
        Log.i("SyncManager", "🔥 syncAll() called! Neon API URL: $apiUrl")
        val user = Clerk.userFlow.value
        if (user == null) {
            Log.w("SyncManager", "❌ No authenticated user (Clerk.userFlow.value is null). Skipping cloud sync.")
            return@withContext
        }
        val userId = user.id

        val authHeader = getAuthHeader()
        if (authHeader == null) {
            Log.w("SyncManager", "❌ Auth header is null. Skipping cloud sync.")
            return@withContext
        }

        Log.i("SyncManager", "✅ Authenticated user found: userId=$userId. Starting syncAll...")

        try {
            syncUserPreferences(userId, authHeader)
            syncStreakDates(userId, authHeader)
            syncGoals(userId, authHeader)
            syncTransactions(userId, authHeader)
            syncLoans(userId, authHeader)
            syncLoanPayments(userId, authHeader)
            syncAccounts(userId, authHeader)
            syncAccountTransactions(userId, authHeader)
            syncPendingTransactions(userId, authHeader)
            syncAiConversations(userId, authHeader)
            syncAiChatMessages(userId, authHeader)

            Log.i("SyncManager", "Sync completed successfully.")
        } catch (e: Exception) {
            Log.e("SyncManager", "Sync failed", e)
            throw e
        }
    }

    private suspend inline fun <reified T : Any> pushRemote(
        tableName: String,
        authHeader: String,
        items: List<T>
    ): Boolean {
        if (items.isEmpty()) return true
        Log.d("SyncManager", "⬆️ Pushing ${items.size} items to $tableName")
        return try {
            val response = ktorClient.post("$apiUrl/sync/push/$tableName") {
                header(HttpHeaders.Authorization, authHeader)
                contentType(ContentType.Application.Json)
                setBody(items)
            }
            if (response.status.isSuccess()) {
                Log.i("SyncManager", "✅ Successfully pushed ${items.size} items to $tableName")
                true
            } else {
                val body = response.bodyAsText()
                Log.e("SyncManager", "Failed to push to $tableName: ${response.status}, body: $body")
                false
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Error pushing to $tableName", e)
            false
        }
    }

    private suspend inline fun <reified T : Any> pullRemote(
        tableName: String,
        authHeader: String
    ): List<T>? {
        Log.d("SyncManager", "Fetching items from $tableName")
        return try {
            val response = ktorClient.get("$apiUrl/sync/pull/$tableName") {
                header(HttpHeaders.Authorization, authHeader)
            }
            if (response.status.isSuccess()) {
                val remoteItems: List<T> = response.body()
                Log.i("SyncManager", "Successfully fetched ${remoteItems.size} items from $tableName")
                remoteItems
            } else {
                Log.e("SyncManager", "Failed to fetch from $tableName: ${response.status}")
                null
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Error fetching from $tableName", e)
            null
        }
    }

    private suspend fun syncUserPreferences(userId: String, authHeader: String) {
        val tableName = "user_preferences"
        val unsynced = dao.getUnsyncedUserPreferences().map { it.copy(userId = userId, isSynced = true) }
        if (pushRemote(tableName, authHeader, unsynced)) {
            if (unsynced.isNotEmpty()) dao.insertUserPreferencesList(unsynced)
        }
        val remote: List<UserPreferencesEntity>? = pullRemote(tableName, authHeader)
        if (remote != null && remote.isNotEmpty()) {
            val updated = remote.map { it.copy(isSynced = true) }
            dao.insertUserPreferencesList(updated)
        }
        val remotePrefs = dao.getUserPreferencesByUserId(userId)
        if (remotePrefs != null) {
            UserPreferences(context).applyFromEntity(remotePrefs)
        }
    }

    private suspend fun syncStreakDates(userId: String, authHeader: String) {
        val tableName = "streak_dates"
        val unsynced = dao.getUnsyncedStreakDates().map { it.copy(userId = userId, isSynced = true) }
        if (pushRemote(tableName, authHeader, unsynced)) {
            if (unsynced.isNotEmpty()) dao.insertStreakDates(unsynced)
        }
        val remote: List<StreakDateEntity>? = pullRemote(tableName, authHeader)
        if (remote != null && remote.isNotEmpty()) {
            val updated = remote.map { it.copy(isSynced = true) }
            dao.insertStreakDates(updated)
        }
        val remoteStreakDates = dao.getAllStreakDatesSync().map { it.dateStr }.toSet()
        if (remoteStreakDates.isNotEmpty()) {
            StreakManager.syncFromCloud(context, remoteStreakDates)
        }
    }

    private suspend fun syncGoals(userId: String, authHeader: String) {
        val tableName = "goals"
        val unsynced = dao.getUnsyncedGoals().map { it.copy(userId = userId, isSynced = true) }
        if (pushRemote(tableName, authHeader, unsynced)) {
            if (unsynced.isNotEmpty()) dao.insertGoals(unsynced)
        }
        val remote: List<Goal>? = pullRemote(tableName, authHeader)
        if (remote != null && remote.isNotEmpty()) {
            val updated = remote.map { it.copy(isSynced = true) }
            dao.insertGoals(updated)
        }
    }

    private suspend fun syncTransactions(userId: String, authHeader: String) {
        val tableName = "transactions"
        val unsynced = dao.getUnsyncedTransactions().map { it.copy(userId = userId, isSynced = true) }
        if (pushRemote(tableName, authHeader, unsynced)) {
            if (unsynced.isNotEmpty()) dao.insertTransactions(unsynced)
        }
        val remote: List<Transaction>? = pullRemote(tableName, authHeader)
        if (remote != null && remote.isNotEmpty()) {
            val updated = remote.map { it.copy(isSynced = true) }
            dao.insertTransactions(updated)
        }
    }

    private suspend fun syncLoans(userId: String, authHeader: String) {
        val tableName = "loans"
        val unsynced = dao.getUnsyncedLoans().map { it.copy(userId = userId, isSynced = true) }
        if (pushRemote(tableName, authHeader, unsynced)) {
            if (unsynced.isNotEmpty()) dao.insertLoans(unsynced)
        }
        val remote: List<Loan>? = pullRemote(tableName, authHeader)
        if (remote != null && remote.isNotEmpty()) {
            val updated = remote.map { it.copy(isSynced = true) }
            dao.insertLoans(updated)
        }
    }

    private suspend fun syncLoanPayments(userId: String, authHeader: String) {
        val tableName = "loan_payments"
        val unsynced = dao.getUnsyncedLoanPayments().map { it.copy(userId = userId, isSynced = true) }
        if (pushRemote(tableName, authHeader, unsynced)) {
            if (unsynced.isNotEmpty()) dao.insertLoanPayments(unsynced)
        }
        val remote: List<LoanPayment>? = pullRemote(tableName, authHeader)
        if (remote != null && remote.isNotEmpty()) {
            val updated = remote.map { it.copy(isSynced = true) }
            dao.insertLoanPayments(updated)
        }
    }

    private suspend fun syncAccounts(userId: String, authHeader: String) {
        val tableName = "accounts"
        val unsynced = dao.getUnsyncedAccounts().map { it.copy(userId = userId, isSynced = true) }
        if (pushRemote(tableName, authHeader, unsynced)) {
            if (unsynced.isNotEmpty()) dao.insertAccounts(unsynced)
        }
        val remote: List<Account>? = pullRemote(tableName, authHeader)
        if (remote != null && remote.isNotEmpty()) {
            val updated = remote.map { it.copy(isSynced = true) }
            dao.insertAccounts(updated)
        }
    }

    private suspend fun syncAccountTransactions(userId: String, authHeader: String) {
        val tableName = "account_transactions"
        val unsynced = dao.getUnsyncedAccountTransactions().map { it.copy(userId = userId, isSynced = true) }
        if (pushRemote(tableName, authHeader, unsynced)) {
            if (unsynced.isNotEmpty()) dao.insertAccountTransactions(unsynced)
        }
        val remote: List<AccountTransaction>? = pullRemote(tableName, authHeader)
        if (remote != null && remote.isNotEmpty()) {
            val updated = remote.map { it.copy(isSynced = true) }
            dao.insertAccountTransactions(updated)
        }
    }

    private suspend fun syncPendingTransactions(userId: String, authHeader: String) {
        val tableName = "pending_transactions"
        val unsynced = dao.getUnsyncedPendingTransactions().map { it.copy(userId = userId, isSynced = true) }
        if (pushRemote(tableName, authHeader, unsynced)) {
            if (unsynced.isNotEmpty()) dao.insertPendingTransactions(unsynced)
        }
        val remote: List<PendingTransaction>? = pullRemote(tableName, authHeader)
        if (remote != null && remote.isNotEmpty()) {
            val updated = remote.map { it.copy(isSynced = true) }
            dao.insertPendingTransactions(updated)
        }
    }

    private suspend fun syncAiConversations(userId: String, authHeader: String) {
        val tableName = "ai_conversations"
        val unsynced = dao.getUnsyncedAiConversations().map { it.copy(userId = userId, isSynced = true) }
        if (pushRemote(tableName, authHeader, unsynced)) {
            if (unsynced.isNotEmpty()) dao.insertAiConversations(unsynced)
        }
        val remote: List<AiConversation>? = pullRemote(tableName, authHeader)
        if (remote != null && remote.isNotEmpty()) {
            val updated = remote.map { it.copy(isSynced = true) }
            dao.insertAiConversations(updated)
        }
    }

    private suspend fun syncAiChatMessages(userId: String, authHeader: String) {
        val tableName = "ai_chat_messages"
        val unsynced = dao.getUnsyncedAiChatMessages().map { it.copy(userId = userId, isSynced = true) }
        if (pushRemote(tableName, authHeader, unsynced)) {
            if (unsynced.isNotEmpty()) dao.insertAiChatMessages(unsynced)
        }
        val remote: List<AiChatMessage>? = pullRemote(tableName, authHeader)
        if (remote != null && remote.isNotEmpty()) {
            val updated = remote.map { it.copy(isSynced = true) }
            dao.insertAiChatMessages(updated)
        }
    }

    suspend fun deleteFromCloud(tableName: String, id: String) = withContext(Dispatchers.IO) {
        val user = Clerk.userFlow.value
        if (user == null) return@withContext
        
        try {
            val authHeader = getAuthHeader() ?: return@withContext
            
            val response = ktorClient.delete("$apiUrl/sync/delete") {
                header(HttpHeaders.Authorization, authHeader)
                url {
                    parameters.append("tableName", tableName)
                    parameters.append("id", id)
                }
            }
            
            if (!response.status.isSuccess()) {
                Log.e("SyncManager", "Failed to delete remote record from $tableName: ${response.status}")
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to delete remote record from $tableName", e)
        }
    }
}
