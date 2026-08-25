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
        val userId = user?.id

        val authHeader = getAuthHeader()

        if (user == null || authHeader == null) {
            Log.w("SyncManager", "❌ No authenticated user or Auth header is null. Skipping cloud sync.")
            return@withContext
        }

        val nonNullUserId = user.id

        Log.i("SyncManager", "✅ Authenticated user found: userId=$nonNullUserId. Starting syncAll...")

        try {
            syncOnboardingAnswers(nonNullUserId, authHeader)
            syncUserPreferences(nonNullUserId, authHeader)
            syncStreakDates(nonNullUserId, authHeader)
            syncGoals(nonNullUserId, authHeader)
            syncTransactions(nonNullUserId, authHeader)
            syncLoans(nonNullUserId, authHeader)
            syncLoanPayments(nonNullUserId, authHeader)
            syncAccounts(nonNullUserId, authHeader)
            syncAccountTransactions(nonNullUserId, authHeader)
            syncPendingTransactions(nonNullUserId, authHeader)
            syncAiConversations(nonNullUserId, authHeader)
            syncAiChatMessages(nonNullUserId, authHeader)

            Log.i("SyncManager", "Sync completed successfully.")
        } catch (e: Exception) {
            Log.w("SyncManager", "Sync skipped/failed: ${e.message}")
        }
    }

    private suspend inline fun <reified T : Any> pushRemote(
        tableName: String,
        authHeader: String?,
        items: List<T>
    ): Boolean {
        if (items.isEmpty()) return true
        if (authHeader.isNullOrBlank()) {
            Log.d("SyncManager", "Skipping push to $tableName: user is unauthenticated.")
            return false
        }
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
            } else if (response.status == HttpStatusCode.Unauthorized) {
                Log.w("SyncManager", "Push to $tableName unauthorized (401): session may be expired.")
                false
            } else {
                val body = response.bodyAsText()
                Log.w("SyncManager", "Failed to push to $tableName: ${response.status}, body: $body")
                false
            }
        } catch (e: Exception) {
            Log.w("SyncManager", "Error pushing to $tableName: ${e.message}")
            false
        }
    }

    private suspend inline fun <reified T : Any> pullRemote(
        tableName: String,
        authHeader: String?
    ): List<T>? {
        if (authHeader.isNullOrBlank()) {
            Log.d("SyncManager", "Skipping pull from $tableName: user is unauthenticated.")
            return null
        }
        Log.d("SyncManager", "Fetching items from $tableName")
        return try {
            val response = ktorClient.get("$apiUrl/sync/pull/$tableName") {
                header(HttpHeaders.Authorization, authHeader)
            }
            if (response.status.isSuccess()) {
                val remoteItems: List<T> = response.body()
                Log.i("SyncManager", "Successfully fetched ${remoteItems.size} items from $tableName")
                remoteItems
            } else if (response.status == HttpStatusCode.Unauthorized) {
                Log.w("SyncManager", "Pull from $tableName unauthorized (401): session may be expired.")
                null
            } else {
                Log.w("SyncManager", "Failed to fetch from $tableName: ${response.status}")
                null
            }
        } catch (e: Exception) {
            Log.w("SyncManager", "Error fetching from $tableName: ${e.message}")
            null
        }
    }

    private suspend fun syncUserPreferences(userId: String, authHeader: String) {
        val tableName = "user_preferences"
        val rawUnsynced = dao.getUnsyncedUserPreferences()
        val unsynced = mutableListOf<UserPreferencesEntity>()
        var hadLocalUser = false
        
        for (item in rawUnsynced) {
            if (item.userId == "local_user") {
                hadLocalUser = true
            }
            unsynced.add(item.copy(userId = userId, isSynced = true))
        }

        if (pushRemote(tableName, authHeader, unsynced)) {
            if (unsynced.isNotEmpty()) dao.insertUserPreferencesList(unsynced)
            if (hadLocalUser) {
                dao.deleteUserPreferencesByUserId("local_user")
            }
        }
        val remote: List<UserPreferencesEntity>? = pullRemote(tableName, authHeader)
        if (remote != null && remote.isNotEmpty()) {
            val localItem = dao.getUserPreferencesByUserId(userId)
            val itemsToInsert = mutableListOf<UserPreferencesEntity>()
            for (remoteItem in remote) {
                if (localItem == null) {
                    itemsToInsert.add(remoteItem.copy(isSynced = true))
                } else if (localItem.isSynced) {
                    if (remoteItem.updatedAt > localItem.updatedAt) {
                        itemsToInsert.add(remoteItem.copy(isSynced = true))
                    }
                }
            }
            if (itemsToInsert.isNotEmpty()) dao.insertUserPreferencesList(itemsToInsert)
        }
        val remotePrefs = dao.getUserPreferencesByUserId(userId)
        if (remotePrefs != null) {
            UserPreferences(context).applyFromEntity(remotePrefs)
        }
    }

    private suspend fun syncStreakDates(userId: String, authHeader: String) {
        val tableName = "streak_dates"
        val rawUnsynced = dao.getUnsyncedStreakDates()
        val mappedUnsynced = mutableListOf<StreakDateEntity>()
        val idsToDelete = mutableListOf<String>()
        
        for (item in rawUnsynced) {
            if (item.id.startsWith("local_user_")) {
                idsToDelete.add(item.id)
                val newId = item.id.replace("local_user_", "${userId}_")
                mappedUnsynced.add(item.copy(id = newId, userId = userId, isSynced = true))
            } else {
                mappedUnsynced.add(item.copy(userId = userId, isSynced = true))
            }
        }

        if (pushRemote(tableName, authHeader, mappedUnsynced)) {
            if (mappedUnsynced.isNotEmpty()) dao.insertStreakDates(mappedUnsynced)
            for (oldId in idsToDelete) {
                dao.deleteStreakDateById(oldId)
            }
        }
        val remote: List<StreakDateEntity>? = pullRemote(tableName, authHeader)
        if (remote != null && remote.isNotEmpty()) {
            val localMap = dao.getAllStreakDatesSync().associateBy { it.id }
            val itemsToInsert = mutableListOf<StreakDateEntity>()
            for (remoteItem in remote) {
                val localItem = localMap[remoteItem.id]
                if (localItem == null || localItem.isSynced) {
                    itemsToInsert.add(remoteItem.copy(isSynced = true))
                }
            }
            if (itemsToInsert.isNotEmpty()) dao.insertStreakDates(itemsToInsert)
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
            val localMap = dao.getAllGoalsSync().associateBy { it.id }
            val itemsToInsert = mutableListOf<Goal>()
            for (remoteItem in remote) {
                val localItem = localMap[remoteItem.id]
                if (localItem == null) {
                    itemsToInsert.add(remoteItem.copy(isSynced = true))
                } else if (localItem.isSynced) {
                    if (remoteItem.updatedAt > localItem.updatedAt) {
                        itemsToInsert.add(remoteItem.copy(isSynced = true))
                    }
                }
            }
            if (itemsToInsert.isNotEmpty()) dao.insertGoals(itemsToInsert)
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
            val localMap = dao.getAllTransactions().associateBy { it.id }
            val itemsToInsert = mutableListOf<Transaction>()
            for (remoteItem in remote) {
                val localItem = localMap[remoteItem.id]
                if (localItem == null) {
                    itemsToInsert.add(remoteItem.copy(isSynced = true))
                } else if (localItem.isSynced) {
                    if (remoteItem.updatedAt > localItem.updatedAt) {
                        itemsToInsert.add(remoteItem.copy(isSynced = true))
                    }
                }
            }
            if (itemsToInsert.isNotEmpty()) dao.insertTransactions(itemsToInsert)
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
            val localMap = dao.getAllLoansSync().associateBy { it.id }
            val itemsToInsert = mutableListOf<Loan>()
            for (remoteItem in remote) {
                val localItem = localMap[remoteItem.id]
                if (localItem == null) {
                    itemsToInsert.add(remoteItem.copy(isSynced = true))
                } else if (localItem.isSynced) {
                    if (remoteItem.updatedAt > localItem.updatedAt) {
                        itemsToInsert.add(remoteItem.copy(isSynced = true))
                    }
                }
            }
            if (itemsToInsert.isNotEmpty()) dao.insertLoans(itemsToInsert)
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
            val localMap = dao.getAllLoanPaymentsSync().associateBy { it.id }
            val itemsToInsert = mutableListOf<LoanPayment>()
            for (remoteItem in remote) {
                val localItem = localMap[remoteItem.id]
                if (localItem == null) {
                    itemsToInsert.add(remoteItem.copy(isSynced = true))
                } else if (localItem.isSynced) {
                    if (remoteItem.updatedAt > localItem.updatedAt) {
                        itemsToInsert.add(remoteItem.copy(isSynced = true))
                    }
                }
            }
            if (itemsToInsert.isNotEmpty()) dao.insertLoanPayments(itemsToInsert)
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
            val localMap = dao.getAllAccountTransactionsSync().associateBy { it.id }
            val itemsToInsert = mutableListOf<AccountTransaction>()
            for (remoteItem in remote) {
                val localItem = localMap[remoteItem.id]
                if (localItem == null) {
                    itemsToInsert.add(remoteItem.copy(isSynced = true))
                } else if (localItem.isSynced) {
                    if (remoteItem.updatedAt > localItem.updatedAt) {
                        itemsToInsert.add(remoteItem.copy(isSynced = true))
                    }
                }
            }
            if (itemsToInsert.isNotEmpty()) dao.insertAccountTransactions(itemsToInsert)
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
            val localMap = dao.getAllAccountsSync().associateBy { it.id }
            val itemsToInsert = mutableListOf<Account>()
            for (remoteItem in remote) {
                val localItem = localMap[remoteItem.id]
                if (localItem == null) {
                    itemsToInsert.add(remoteItem.copy(isSynced = true))
                } else if (localItem.isSynced) {
                    if (remoteItem.updatedAt > localItem.updatedAt) {
                        itemsToInsert.add(remoteItem.copy(isSynced = true))
                    }
                }
            }
            if (itemsToInsert.isNotEmpty()) dao.insertAccounts(itemsToInsert)
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
            val localMap = dao.getAllPendingTransactionsSync().associateBy { it.id }
            val itemsToInsert = mutableListOf<PendingTransaction>()
            for (remoteItem in remote) {
                val localItem = localMap[remoteItem.id]
                if (localItem == null) {
                    itemsToInsert.add(remoteItem.copy(isSynced = true))
                } else if (localItem.isSynced) {
                    if (remoteItem.updatedAt > localItem.updatedAt) {
                        itemsToInsert.add(remoteItem.copy(isSynced = true))
                    }
                }
            }
            if (itemsToInsert.isNotEmpty()) dao.insertPendingTransactions(itemsToInsert)
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
            val localMap = dao.getAllAiConversationsSync().associateBy { it.id }
            val itemsToInsert = mutableListOf<AiConversation>()
            for (remoteItem in remote) {
                val localItem = localMap[remoteItem.id]
                if (localItem == null) {
                    itemsToInsert.add(remoteItem.copy(isSynced = true))
                } else if (localItem.isSynced) {
                    if (remoteItem.updatedAt > localItem.updatedAt) {
                        itemsToInsert.add(remoteItem.copy(isSynced = true))
                    }
                }
            }
            if (itemsToInsert.isNotEmpty()) dao.insertAiConversations(itemsToInsert)
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
            val localMap = dao.getAllAiChatMessagesSync().associateBy { it.id }
            val itemsToInsert = mutableListOf<AiChatMessage>()
            for (remoteItem in remote) {
                val localItem = localMap[remoteItem.id]
                if (localItem == null) {
                    itemsToInsert.add(remoteItem.copy(isSynced = true))
                } else if (localItem.isSynced) {
                    if (remoteItem.updatedAt > localItem.updatedAt) {
                        itemsToInsert.add(remoteItem.copy(isSynced = true))
                    }
                }
            }
            if (itemsToInsert.isNotEmpty()) dao.insertAiChatMessages(itemsToInsert)
        }
    }

    private suspend fun syncOnboardingAnswers(userId: String?, authHeader: String?) {
        val tableName = "onboarding_answers"
        val rawUnsynced = dao.getUnsyncedOnboardingAnswers()
        val unsynced = rawUnsynced.map { it.copy(userId = userId, isSynced = true) }

        if (pushRemote(tableName, authHeader, unsynced)) {
            if (unsynced.isNotEmpty()) dao.insertOnboardingAnswers(unsynced)
        }
        
        if (authHeader == null) return
        
        val remote: List<OnboardingAnswer>? = pullRemote(tableName, authHeader)
        if (remote != null && remote.isNotEmpty()) {
            val localMap = dao.getAllOnboardingAnswersSync().associateBy { it.id }
            val itemsToInsert = mutableListOf<OnboardingAnswer>()
            for (remoteItem in remote) {
                val localItem = localMap[remoteItem.id]
                if (localItem == null) {
                    itemsToInsert.add(remoteItem.copy(isSynced = true))
                } else if (localItem.isSynced) {
                    if (remoteItem.updatedAt > localItem.updatedAt) {
                        itemsToInsert.add(remoteItem.copy(isSynced = true))
                    }
                }
            }
            if (itemsToInsert.isNotEmpty()) dao.insertOnboardingAnswers(itemsToInsert)
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
