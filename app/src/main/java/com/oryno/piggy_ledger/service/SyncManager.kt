package com.oryno.piggy_ledger.service

import android.content.Context
import android.util.Log
import com.clerk.api.Clerk
import com.oryno.piggy_ledger.BuildConfig
import com.oryno.piggy_ledger.data.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.clerk.api.session.fetchToken
import com.clerk.api.session.GetTokenOptions
import com.clerk.api.network.serialization.ClerkResult

@OptIn(io.github.jan.supabase.annotations.SupabaseInternal::class, io.github.jan.supabase.annotations.SupabaseExperimental::class)
class SyncManager(private val context: Context) {
    private val db = PiggyLedgerDatabase.getInstance(context)
    private val dao = db.piggyLedgerDao()
    private val supabase = SupabaseManager.client

    private suspend fun getAuthHeader(): Map<String, String> {
        val anonHeader = mapOf(
            "Authorization" to "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
            "apikey" to BuildConfig.SUPABASE_ANON_KEY
        )

        val session = Clerk.sessionFlow.value ?: return anonHeader

        return try {
            val result = session.fetchToken(GetTokenOptions(template = "supabase"))
            if (result is ClerkResult.Success && !result.value.jwt.isNullOrEmpty()) {
                mapOf(
                    "Authorization" to "Bearer ${result.value.jwt}",
                    "apikey" to BuildConfig.SUPABASE_ANON_KEY
                )
            } else {
                val defaultTokenResult = session.fetchToken(GetTokenOptions())
                if (defaultTokenResult is ClerkResult.Success && !defaultTokenResult.value.jwt.isNullOrEmpty()) {
                    mapOf(
                        "Authorization" to "Bearer ${defaultTokenResult.value.jwt}",
                        "apikey" to BuildConfig.SUPABASE_ANON_KEY
                    )
                } else {
                    anonHeader
                }
            }
        } catch (e: Exception) {
            Log.w("SyncManager", "Could not fetch Clerk token, using anon key fallback", e)
            anonHeader
        }
    }

    suspend fun syncAll() = withContext(Dispatchers.IO) {
        val user = Clerk.userFlow.value
        val userId = user?.id ?: "local_user"

        Log.i("SyncManager", "Starting syncAll for userId=$userId")

        try {
            // Sync User Preferences & Onboarding Data
            syncTable("user_preferences", userId, dao.getUnsyncedUserPreferences(), { dao.getAllUserPreferencesSync() }, { dao.insertUserPreferencesList(it) })
            
            val remotePrefs = dao.getUserPreferencesByUserId(userId)
            if (remotePrefs != null) {
                UserPreferences(context).applyFromEntity(remotePrefs)
            }

            // Sync Streak Dates
            syncTable("streak_dates", userId, dao.getUnsyncedStreakDates(), { dao.getAllStreakDatesSync() }, { dao.insertStreakDates(it) })
            
            val remoteStreakDates = dao.getAllStreakDatesSync().map { it.dateStr }.toSet()
            if (remoteStreakDates.isNotEmpty()) {
                StreakManager.syncFromCloud(context, remoteStreakDates)
            }

            // Sync Goals
            syncTable("goals", userId, dao.getUnsyncedGoals(), { dao.getAllGoalsSync() }, { dao.insertGoals(it) })
            // Sync Transactions
            syncTable("transactions", userId, dao.getUnsyncedTransactions(), { dao.getAllTransactions() }, { dao.insertTransactions(it) })
            // Sync Loans
            syncTable("loans", userId, dao.getUnsyncedLoans(), { dao.getAllLoansSync() }, { dao.insertLoans(it) })
            // Sync LoanPayments
            syncTable("loan_payments", userId, dao.getUnsyncedLoanPayments(), { dao.getAllLoanPaymentsSync() }, { dao.insertLoanPayments(it) })
            // Sync Accounts
            syncTable("accounts", userId, dao.getUnsyncedAccounts(), { dao.getAllAccountsSync() }, { dao.insertAccounts(it) })
            // Sync AccountTransactions
            syncTable("account_transactions", userId, dao.getUnsyncedAccountTransactions(), { dao.getAllAccountTransactionsSync() }, { dao.insertAccountTransactions(it) })
            // Sync PendingTransactions
            syncTable("pending_transactions", userId, dao.getUnsyncedPendingTransactions(), { dao.getAllPendingTransactionsSync() }, { dao.insertPendingTransactions(it) })
            // Sync AI Conversations
            syncTable("ai_conversations", userId, dao.getUnsyncedAiConversations(), { dao.getAllAiConversationsSync() }, { dao.insertAiConversations(it) })
            // Sync AI Chat Messages
            syncTable("ai_chat_messages", userId, dao.getUnsyncedAiChatMessages(), { dao.getAllAiChatMessagesSync() }, { dao.insertAiChatMessages(it) })

            Log.i("SyncManager", "Sync completed successfully.")
        } catch (e: Exception) {
            Log.e("SyncManager", "Sync failed", e)
        }
    }

    private suspend inline fun <reified T : Any> syncTable(
        tableName: String,
        userId: String,
        unsyncedLocal: List<T>,
        crossinline getAllLocal: suspend () -> List<T>,
        crossinline saveAllLocal: suspend (List<T>) -> Unit
    ) {
        val headers = getAuthHeader()
        
        try {
            // 1. Push local unsynced changes to Supabase
            if (unsyncedLocal.isNotEmpty()) {
                val toPush = unsyncedLocal.map { item ->
                    when (item) {
                        is UserPreferencesEntity -> item.copy(userId = userId, isSynced = true) as T
                        is StreakDateEntity -> item.copy(userId = userId, isSynced = true) as T
                        is Goal -> item.copy(userId = userId, isSynced = true) as T
                        is Transaction -> item.copy(userId = userId, isSynced = true) as T
                        is Loan -> item.copy(userId = userId, isSynced = true) as T
                        is LoanPayment -> item.copy(userId = userId, isSynced = true) as T
                        is Account -> item.copy(userId = userId, isSynced = true) as T
                        is AccountTransaction -> item.copy(userId = userId, isSynced = true) as T
                        is PendingTransaction -> item.copy(userId = userId, isSynced = true) as T
                        is AiConversation -> item.copy(userId = userId, isSynced = true) as T
                        is AiChatMessage -> item.copy(userId = userId, isSynced = true) as T
                        else -> item
                    }
                }
                Log.d("SyncManager", "Pushing ${toPush.size} items to $tableName")
                // Supabase upsert
                supabase.postgrest[tableName].upsert(toPush) {
                    headers.forEach { (k, v) -> this.headers.append(k, v) }
                }
                
                // Mark as synced locally
                saveAllLocal(toPush)
                Log.i("SyncManager", "Successfully pushed ${toPush.size} items to $tableName")
            }

            // 2. Fetch all for this user from Supabase
            Log.d("SyncManager", "Fetching items from $tableName for userId=$userId")
            val remoteItems = supabase.postgrest[tableName]
                .select() {
                    filter {
                        eq("userId", userId)
                    }
                    headers.forEach { (k, v) -> this.headers.append(k, v) }
                }
                .decodeList<T>()

            // 3. Update local database
            if (remoteItems.isNotEmpty()) {
                val finalLocal = remoteItems.map { item ->
                    when (item) {
                        is UserPreferencesEntity -> item.copy(isSynced = true) as T
                        is StreakDateEntity -> item.copy(isSynced = true) as T
                        is Goal -> item.copy(isSynced = true) as T
                        is Transaction -> item.copy(isSynced = true) as T
                        is Loan -> item.copy(isSynced = true) as T
                        is LoanPayment -> item.copy(isSynced = true) as T
                        is Account -> item.copy(isSynced = true) as T
                        is AccountTransaction -> item.copy(isSynced = true) as T
                        is PendingTransaction -> item.copy(isSynced = true) as T
                        is AiConversation -> item.copy(isSynced = true) as T
                        is AiChatMessage -> item.copy(isSynced = true) as T
                        else -> item
                    }
                }
                saveAllLocal(finalLocal)
                Log.i("SyncManager", "Successfully synced ${finalLocal.size} items from $tableName")
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Sync error on table $tableName", e)
        }
    }
}
