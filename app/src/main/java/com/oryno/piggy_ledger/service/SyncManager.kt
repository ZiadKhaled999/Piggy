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

    suspend fun syncAll(): Boolean = withContext(Dispatchers.IO) {
        Log.i("SyncManager", "🔥 syncAll() called! Neon API URL: $apiUrl")
        val user = Clerk.userFlow.value
        val userId = user?.id

        val authHeader = getAuthHeader()

        if (user == null || authHeader == null) {
            Log.w("SyncManager", "❌ No authenticated user or Auth header is null. Skipping cloud sync.")
            return@withContext false
        }

        val nonNullUserId = user.id

        Log.i("SyncManager", "✅ Authenticated user found: userId=$nonNullUserId. Starting syncAll...")

        try {
            var allOk = true
            allOk = syncOnboardingAnswers(nonNullUserId, authHeader) && allOk
            allOk = syncUserPreferences(nonNullUserId, authHeader) && allOk
            allOk = syncStreakDates(nonNullUserId, authHeader) && allOk
            allOk = syncGoals(nonNullUserId, authHeader) && allOk
            allOk = syncTransactions(nonNullUserId, authHeader) && allOk
            allOk = syncLoans(nonNullUserId, authHeader) && allOk
            allOk = syncLoanPayments(nonNullUserId, authHeader) && allOk
            allOk = syncAccounts(nonNullUserId, authHeader) && allOk
            allOk = syncAccountTransactions(nonNullUserId, authHeader) && allOk
            allOk = syncPendingTransactions(nonNullUserId, authHeader) && allOk
            allOk = syncAiConversations(nonNullUserId, authHeader) && allOk
            allOk = syncAiChatMessages(nonNullUserId, authHeader) && allOk

            Log.i("SyncManager", "Sync completed successfully. Success: $allOk")
            allOk
        } catch (e: Exception) {
            Log.w("SyncManager", "Sync skipped/failed: ${e.message}")
            false
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

    private suspend fun syncUserPreferences(userId: String, authHeader: String): Boolean {
        val tableName = "user_preferences"
        
        // 1. Pull remote first to see if the user already has saved preferences in the cloud
        val remote: List<UserPreferencesEntity>? = pullRemote(tableName, authHeader)
        val pullOk = remote != null
        val remoteItem = remote?.firstOrNull { it.userId == userId }
        
        val localItem = dao.getUserPreferencesByUserId(userId)
        
        // 2. Determine if remote should override local
        val shouldAdoptRemote = if (remoteItem == null) {
            false
        } else if (localItem == null) {
            true
        } else {
            // A local preference is a fresh default if it has "USD" as currency AND was created recently (last 5 mins)
            val isLocalFreshDefault = (localItem.appCurrency == "USD") && 
                    (System.currentTimeMillis() - localItem.updatedAt < 300000)
            isLocalFreshDefault || (remoteItem.updatedAt > localItem.updatedAt)
        }
        
        var pushOk = true
        
        if (shouldAdoptRemote && remoteItem != null) {
            Log.i("SyncManager", "☁️ Overwriting local preferences with cloud preferences (currency: ${remoteItem.appCurrency})")
            dao.insertUserPreferences(remoteItem.copy(isSynced = true))
            UserPreferences(context).applyFromEntity(remoteItem)
            
            // Clean up any remaining local_user entry
            dao.deleteUserPreferencesByUserId("local_user")
        } else {
            // Local is newer, or there's no remote preference. We push local.
            val rawUnsynced = dao.getUnsyncedUserPreferences()
            val unsynced = mutableListOf<UserPreferencesEntity>()
            var hadLocalUser = false
            
            for (item in rawUnsynced) {
                if (item.userId == "local_user") {
                    hadLocalUser = true
                }
                unsynced.add(item.copy(userId = userId, isSynced = true))
            }
            
            if (unsynced.isNotEmpty()) {
                pushOk = pushRemote(tableName, authHeader, unsynced)
                if (pushOk) {
                    dao.insertUserPreferencesList(unsynced)
                    if (hadLocalUser) {
                        dao.deleteUserPreferencesByUserId("local_user")
                    }
                }
            }
            
            // Just in case remote has different updates that we need to apply (timestamp wins)
            if (remoteItem != null && localItem != null && remoteItem.updatedAt > localItem.updatedAt) {
                dao.insertUserPreferences(remoteItem.copy(isSynced = true))
                UserPreferences(context).applyFromEntity(remoteItem)
            }
        }
        
        return pushOk && pullOk
    }
    private suspend fun syncStreakDates(userId: String, authHeader: String): Boolean {
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

        val pushOk = pushRemote(tableName, authHeader, mappedUnsynced)
        if (pushOk) {
            if (mappedUnsynced.isNotEmpty()) dao.insertStreakDates(mappedUnsynced)
            for (oldId in idsToDelete) {
                dao.deleteStreakDateById(oldId)
            }
        }
        val remote: List<StreakDateEntity>? = pullRemote(tableName, authHeader)
        val pullOk = remote != null
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
        return pushOk && pullOk
    }
    private suspend fun syncGoals(userId: String, authHeader: String): Boolean {
        val tableName = "goals"
        val unsynced = dao.getUnsyncedGoals().map { it.copy(userId = userId, isSynced = true) }
        val pushOk = pushRemote(tableName, authHeader, unsynced)
        if (pushOk) {
            if (unsynced.isNotEmpty()) dao.insertGoals(unsynced)
        }
        val remote: List<Goal>? = pullRemote(tableName, authHeader)
        val pullOk = remote != null
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
        return pushOk && pullOk
    }
    private suspend fun syncTransactions(userId: String, authHeader: String): Boolean {
        val tableName = "transactions"
        val rawUnsynced = dao.getUnsyncedTransactions()
        
        // Filter out orphaned transactions where the referenced goal does not exist locally.
        val existingGoalIds = dao.getAllGoalsSync().map { it.id }.toSet()
        val unsynced = mutableListOf<Transaction>()
        val orphaned = mutableListOf<Transaction>()
        
        for (item in rawUnsynced) {
            if (existingGoalIds.contains(item.goalId)) {
                unsynced.add(item.copy(userId = userId, isSynced = true))
            } else {
                Log.w("SyncManager", "⚠️ Orphaned transaction found (goalId: ${item.goalId} does not exist locally). Marking as synced to clear queue.")
                orphaned.add(item.copy(userId = userId, isSynced = true))
            }
        }
        
        var pushOk = true
        if (unsynced.isNotEmpty()) {
            pushOk = pushRemote(tableName, authHeader, unsynced)
            if (pushOk) {
                dao.insertTransactions(unsynced)
            }
        }
        
        // Quietly clear orphaned records so they stop clogging the queue
        if (orphaned.isNotEmpty()) {
            dao.insertTransactions(orphaned)
        }
        
        val remote: List<Transaction>? = pullRemote(tableName, authHeader)
        val pullOk = remote != null
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
        return pushOk && pullOk
    }
    private suspend fun syncLoans(userId: String, authHeader: String): Boolean {
        val tableName = "loans"
        val unsynced = dao.getUnsyncedLoans().map { it.copy(userId = userId, isSynced = true) }
        val pushOk = pushRemote(tableName, authHeader, unsynced)
        if (pushOk) {
            if (unsynced.isNotEmpty()) dao.insertLoans(unsynced)
        }
        val remote: List<Loan>? = pullRemote(tableName, authHeader)
        val pullOk = remote != null
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
        return pushOk && pullOk
    }
    private suspend fun syncLoanPayments(userId: String, authHeader: String): Boolean {
        val tableName = "loan_payments"
        val rawUnsynced = dao.getUnsyncedLoanPayments()
        
        // Filter out orphaned loan payments where the referenced loan does not exist locally.
        val existingLoanIds = dao.getAllLoansSync().map { it.id }.toSet()
        val unsynced = mutableListOf<LoanPayment>()
        val orphaned = mutableListOf<LoanPayment>()
        
        for (item in rawUnsynced) {
            if (existingLoanIds.contains(item.loanId)) {
                unsynced.add(item.copy(userId = userId, isSynced = true))
            } else {
                Log.w("SyncManager", "⚠️ Orphaned loan payment found (loanId: ${item.loanId} does not exist locally). Marking as synced to clear queue.")
                orphaned.add(item.copy(userId = userId, isSynced = true))
            }
        }
        
        var pushOk = true
        if (unsynced.isNotEmpty()) {
            pushOk = pushRemote(tableName, authHeader, unsynced)
            if (pushOk) {
                dao.insertLoanPayments(unsynced)
            }
        }
        
        // Quietly clear orphaned records so they stop clogging the queue
        if (orphaned.isNotEmpty()) {
            dao.insertLoanPayments(orphaned)
        }
        
        val remote: List<LoanPayment>? = pullRemote(tableName, authHeader)
        val pullOk = remote != null
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
        return pushOk && pullOk
    }
    private suspend fun syncAccountTransactions(userId: String, authHeader: String): Boolean {
        val tableName = "account_transactions"
        val rawUnsynced = dao.getUnsyncedAccountTransactions()
        
        // Filter out orphaned account transactions where the referenced account does not exist locally.
        val existingAccountIds = dao.getAllAccountsSync().map { it.id }.toSet()
        val unsynced = mutableListOf<AccountTransaction>()
        val orphaned = mutableListOf<AccountTransaction>()
        
        for (item in rawUnsynced) {
            if (existingAccountIds.contains(item.account_id)) {
                unsynced.add(item.copy(userId = userId, isSynced = true))
            } else {
                Log.w("SyncManager", "⚠️ Orphaned account transaction found (accountId: ${item.account_id} does not exist locally). Marking as synced to clear queue.")
                orphaned.add(item.copy(userId = userId, isSynced = true))
            }
        }
        
        var pushOk = true
        if (unsynced.isNotEmpty()) {
            pushOk = pushRemote(tableName, authHeader, unsynced)
            if (pushOk) {
                dao.insertAccountTransactions(unsynced)
            }
        }
        
        // Quietly clear orphaned records so they stop clogging the queue
        if (orphaned.isNotEmpty()) {
            dao.insertAccountTransactions(orphaned)
        }
        
        val remote: List<AccountTransaction>? = pullRemote(tableName, authHeader)
        val pullOk = remote != null
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
        return pushOk && pullOk
    }
    private suspend fun syncAccounts(userId: String, authHeader: String): Boolean {
        val tableName = "accounts"
        val unsynced = dao.getUnsyncedAccounts().map { it.copy(userId = userId, isSynced = true) }
        val pushOk = pushRemote(tableName, authHeader, unsynced)
        if (pushOk) {
            if (unsynced.isNotEmpty()) dao.insertAccounts(unsynced)
        }
        val remote: List<Account>? = pullRemote(tableName, authHeader)
        val pullOk = remote != null
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
        return pushOk && pullOk
    }
    private suspend fun syncPendingTransactions(userId: String, authHeader: String): Boolean {
        val tableName = "pending_transactions"
        val unsynced = dao.getUnsyncedPendingTransactions().map { it.copy(userId = userId, isSynced = true) }
        val pushOk = pushRemote(tableName, authHeader, unsynced)
        if (pushOk) {
            if (unsynced.isNotEmpty()) dao.insertPendingTransactions(unsynced)
        }
        val remote: List<PendingTransaction>? = pullRemote(tableName, authHeader)
        val pullOk = remote != null
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
        return pushOk && pullOk
    }
    private suspend fun syncAiConversations(userId: String, authHeader: String): Boolean {
        val tableName = "ai_conversations"
        val unsynced = dao.getUnsyncedAiConversations().map { it.copy(userId = userId, isSynced = true) }
        val pushOk = pushRemote(tableName, authHeader, unsynced)
        if (pushOk) {
            if (unsynced.isNotEmpty()) dao.insertAiConversations(unsynced)
        }
        val remote: List<AiConversation>? = pullRemote(tableName, authHeader)
        val pullOk = remote != null
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
        return pushOk && pullOk
    }
    private suspend fun syncAiChatMessages(userId: String, authHeader: String): Boolean {
        val tableName = "ai_chat_messages"
        val unsynced = dao.getUnsyncedAiChatMessages().map { it.copy(userId = userId, isSynced = true) }
        val pushOk = pushRemote(tableName, authHeader, unsynced)
        if (pushOk) {
            if (unsynced.isNotEmpty()) dao.insertAiChatMessages(unsynced)
        }
        val remote: List<AiChatMessage>? = pullRemote(tableName, authHeader)
        val pullOk = remote != null
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
        return pushOk && pullOk
    }
    private suspend fun syncOnboardingAnswers(userId: String?, authHeader: String?): Boolean {
        val tableName = "onboarding_answers"
        val rawUnsynced = dao.getUnsyncedOnboardingAnswers()
        val unsynced = rawUnsynced.map { it.copy(userId = userId, isSynced = true) }

        val pushOk = pushRemote(tableName, authHeader, unsynced)
        if (pushOk) {
            if (unsynced.isNotEmpty()) dao.insertOnboardingAnswers(unsynced)
        }
        
        if (authHeader == null) return false

        val remote: List<OnboardingAnswer>? = pullRemote(tableName, authHeader)
        val pullOk = remote != null
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
    return pushOk && pullOk
    }

    suspend fun deleteFromCloud(tableName: String, id: String) = withContext(Dispatchers.IO) {
        val user = Clerk.userFlow.value
        if (user == null) return@withContext false
        
        try {
            val authHeader = getAuthHeader() ?: return@withContext false
            
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
