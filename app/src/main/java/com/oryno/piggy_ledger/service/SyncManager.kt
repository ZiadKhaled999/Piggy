package com.oryno.piggy_ledger.service

import android.content.Context
import android.util.Log
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.GetTokenOptions
import com.clerk.api.session.fetchToken
import com.oryno.piggy_ledger.BuildConfig
import com.oryno.piggy_ledger.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class SyncManager(private val context: Context) {

    companion object {
        private val syncMutex = Mutex()
        private const val BATCH_SIZE = 500
    }

    private val dao = PiggyLedgerDatabase.getInstance(context).piggyLedgerDao()
    private val checkpointStore = SyncCheckpointStore(context)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    suspend fun syncAll(): Boolean = syncAll(null)

    suspend fun syncAll(prefetchedAuthHeader: String?): Boolean =
        syncMutex.withLock {
            withContext(Dispatchers.IO) {
                val user = Clerk.userFlow.value ?: return@withContext false
                val auth = prefetchedAuthHeader ?: getAuthHeader() ?: return@withContext false
                val userId = user.id

                try {
                    val startCheckpoint = checkpointStore.get(userId)
                    val isFirstSync = startCheckpoint == 0L

                    while (true) {
                        val batch = collectUnsynced(BATCH_SIZE)
                        if (batch.isEmpty()) break

                        val resp = ApiClient.push(auth, PushRequest(batch))
                            ?: return@withContext false

                        markAccepted(resp.accepted + resp.skipped)
                        applyConflicts(resp.conflicts)

                        if (batch.size < BATCH_SIZE) break
                    }

                    var checkpoint = startCheckpoint
                    do {
                        val pull = ApiClient.pull(auth, checkpoint, BATCH_SIZE)
                            ?: return@withContext false
                        applyRemote(pull.changes, isFirstSync)
                        checkpoint = pull.checkpoint
                        checkpointStore.put(userId, checkpoint)
                    } while (pull.hasMore)

                    SyncWorker.clearFullResyncFlag(context)
                    true
                } catch (e: Exception) {
                    Log.w("SyncManager", "sync failed: ${e.message}", e)
                    false
                }
            }
        }

    private suspend fun collectUnsynced(limit: Int): List<SyncChange> {
        val out = ArrayList<SyncChange>(limit)

        fun <T> add(table: String, items: List<T>, ser: KSerializer<T>) {
            if (out.size >= limit) return
            val take = (limit - out.size).coerceAtMost(items.size)
            items.take(take).forEach { item ->
                out += SyncChange(
                    table = table,
                    data = json.encodeToJsonElement(ser, item) as JsonObject
                )
            }
        }

        add("onboarding_answers", dao.getUnsyncedOnboardingAnswers(), OnboardingAnswer.serializer())
        add("user_preferences", dao.getUnsyncedUserPreferences(), UserPreferencesEntity.serializer())
        add("streak_dates", dao.getUnsyncedStreakDates(), StreakDateEntity.serializer())
        add("goals", dao.getUnsyncedGoals(), Goal.serializer())
        add("accounts", dao.getUnsyncedAccounts(), Account.serializer())
        add("loans", dao.getUnsyncedLoans(), Loan.serializer())
        add("assets", dao.getUnsyncedAssets(), AssetEntity.serializer())
        add("subscriptions", dao.getUnsyncedSubscriptions(), SubscriptionEntity.serializer())
        add("journal_entries", dao.getUnsyncedJournalEntries(), JournalEntryEntity.serializer())
        add("postings", dao.getUnsyncedPostings(), PostingEntity.serializer())
        add("transactions", dao.getUnsyncedTransactions(), Transaction.serializer())
        add("loan_payments", dao.getUnsyncedLoanPayments(), LoanPayment.serializer())
        add("account_transactions", dao.getUnsyncedAccountTransactions(), AccountTransaction.serializer())
        add("pending_transactions", dao.getUnsyncedPendingTransactions(), PendingTransaction.serializer())
        add("ai_conversations", dao.getUnsyncedAiConversations(), AiConversation.serializer())
        add("ai_chat_messages", dao.getUnsyncedAiChatMessages(), AiChatMessage.serializer())

        return out
    }

    private suspend fun markAccepted(accepted: List<ChangeId>) {
        accepted.groupBy { it.table }.forEach { (table, ids) ->
            val idSet = ids.map { it.id }.toSet()
            when (table) {
                "onboarding_answers" -> dao.markOnboardingAnswersSynced(idSet)
                "user_preferences" -> {
                    dao.markUserPreferencesSynced(idSet)
                    dao.deleteUserPreferencesByUserId("local_user")
                }
                "streak_dates" -> dao.markStreakDatesSynced(idSet)
                "goals" -> dao.markGoalsSynced(idSet)
                "accounts" -> dao.markAccountsSynced(idSet)
                "loans" -> dao.markLoansSynced(idSet)
                "assets" -> dao.markAssetsSynced(idSet)
                "subscriptions" -> dao.markSubscriptionsSynced(idSet)
                "journal_entries" -> dao.markJournalEntriesSynced(idSet)
                "postings" -> dao.markPostingsSynced(idSet)
                "transactions" -> dao.markTransactionsSynced(idSet)
                "loan_payments" -> dao.markLoanPaymentsSynced(idSet)
                "account_transactions" -> dao.markAccountTransactionsSynced(idSet)
                "pending_transactions" -> dao.markPendingTransactionsSynced(idSet)
                "ai_conversations" -> dao.markAiConversationsSynced(idSet)
                "ai_chat_messages" -> dao.markAiChatMessagesSynced(idSet)
            }
        }
    }

    private suspend fun applyConflicts(conflicts: List<SyncChange>) {
        conflicts.groupBy { it.table }.forEach { (table, list) ->
            when (table) {
                "onboarding_answers" -> list.forEach {
                    dao.insertOnboardingAnswer(
                        json.decodeFromJsonElement(OnboardingAnswer.serializer(), it.data)
                            .copy(isSynced = true)
                    )
                }
                "user_preferences" -> list.forEach {
                    val e = json.decodeFromJsonElement(UserPreferencesEntity.serializer(), it.data)
                    dao.insertUserPreferences(e.copy(isSynced = true))
                }
                "streak_dates" -> dao.insertStreakDates(
                    list.map {
                        json.decodeFromJsonElement(StreakDateEntity.serializer(), it.data)
                            .copy(isSynced = true)
                    }
                )
                "goals" -> dao.insertGoals(
                    list.map { json.decodeFromJsonElement(Goal.serializer(), it.data).copy(isSynced = true) }
                )
                "accounts" -> dao.insertAccounts(
                    list.map { json.decodeFromJsonElement(Account.serializer(), it.data).copy(isSynced = true) }
                )
                "loans" -> dao.insertLoans(
                    list.map { json.decodeFromJsonElement(Loan.serializer(), it.data).copy(isSynced = true) }
                )
                "assets" -> dao.insertAssets(
                    list.map { json.decodeFromJsonElement(AssetEntity.serializer(), it.data).copy(isSynced = true) }
                )
                "subscriptions" -> dao.insertSubscriptions(
                    list.map { json.decodeFromJsonElement(SubscriptionEntity.serializer(), it.data).copy(isSynced = true) }
                )
                "journal_entries" -> dao.insertJournalEntries(
                    list.map { json.decodeFromJsonElement(JournalEntryEntity.serializer(), it.data).copy(isSynced = true) }
                )
                "postings" -> dao.insertPostings(
                    list.map { json.decodeFromJsonElement(PostingEntity.serializer(), it.data).copy(isSynced = true) }
                )
                "transactions" -> dao.insertTransactions(
                    list.map { json.decodeFromJsonElement(Transaction.serializer(), it.data).copy(isSynced = true) }
                )
                "loan_payments" -> dao.insertLoanPayments(
                    list.map { json.decodeFromJsonElement(LoanPayment.serializer(), it.data).copy(isSynced = true) }
                )
                "account_transactions" -> dao.insertAccountTransactions(
                    list.map { json.decodeFromJsonElement(AccountTransaction.serializer(), it.data).copy(isSynced = true) }
                )
                "pending_transactions" -> dao.insertPendingTransactions(
                    list.map { json.decodeFromJsonElement(PendingTransaction.serializer(), it.data).copy(isSynced = true) }
                )
                "ai_conversations" -> dao.insertAiConversations(
                    list.map { json.decodeFromJsonElement(AiConversation.serializer(), it.data).copy(isSynced = true) }
                )
                "ai_chat_messages" -> dao.insertAiChatMessages(
                    list.map { json.decodeFromJsonElement(AiChatMessage.serializer(), it.data).copy(isSynced = true) }
                )
            }
        }
    }

    private suspend fun applyRemote(changes: List<SyncChange>, isFirstSync: Boolean) {
        changes.groupBy { it.table }.forEach { (table, list) ->
            when (table) {
                "onboarding_answers" -> {
                    val toInsert = mergeList(
                        remote = list.map { json.decodeFromJsonElement(OnboardingAnswer.serializer(), it.data) },
                        local = dao.getAllOnboardingAnswersSync(),
                        id = { it.id }, updatedAt = { it.updatedAt }, isSynced = { it.isSynced },
                        withSynced = { it.copy(isSynced = true) }
                    )
                    if (toInsert.isNotEmpty()) dao.insertOnboardingAnswers(toInsert)
                }
                "user_preferences" -> list.forEach {
                    val e = json.decodeFromJsonElement(UserPreferencesEntity.serializer(), it.data)
                    val local = dao.getUserPreferencesByUserId(e.userId)
                    if (isFirstSync || local == null || e.updatedAt >= local.updatedAt) {
                        dao.insertUserPreferences(e.copy(isSynced = true))
                    }
                }
                "streak_dates" -> {
                    val toInsert = mergeList(
                        remote = list.map { json.decodeFromJsonElement(StreakDateEntity.serializer(), it.data) },
                        local = dao.getAllStreakDatesSync(),
                        id = { it.id }, updatedAt = { it.updatedAt }, isSynced = { it.isSynced },
                        withSynced = { it.copy(isSynced = true) }
                    )
                    if (toInsert.isNotEmpty()) dao.insertStreakDates(toInsert)
                }
                "goals" -> {
                    val toInsert = mergeList(
                        remote = list.map { json.decodeFromJsonElement(Goal.serializer(), it.data) },
                        local = dao.getAllGoalsSync(),
                        id = { it.id }, updatedAt = { it.updatedAt }, isSynced = { it.isSynced },
                        withSynced = { it.copy(isSynced = true) }
                    )
                    if (toInsert.isNotEmpty()) dao.insertGoals(toInsert)
                }
                "accounts" -> {
                    val toInsert = mergeList(
                        remote = list.map { json.decodeFromJsonElement(Account.serializer(), it.data) },
                        local = dao.getAllAccountsSync(),
                        id = { it.id }, updatedAt = { it.updatedAt }, isSynced = { it.isSynced },
                        withSynced = { it.copy(isSynced = true) }
                    )
                    if (toInsert.isNotEmpty()) dao.insertAccounts(toInsert)
                }
                "loans" -> {
                    val toInsert = mergeList(
                        remote = list.map { json.decodeFromJsonElement(Loan.serializer(), it.data) },
                        local = dao.getAllLoansSync(),
                        id = { it.id }, updatedAt = { it.updatedAt }, isSynced = { it.isSynced },
                        withSynced = { it.copy(isSynced = true) }
                    )
                    if (toInsert.isNotEmpty()) dao.insertLoans(toInsert)
                }
                "assets" -> {
                    val toInsert = mergeList(
                        remote = list.map { json.decodeFromJsonElement(AssetEntity.serializer(), it.data) },
                        local = dao.getAllAssetsSyncFull(),
                        id = { it.id }, updatedAt = { it.updatedAt }, isSynced = { it.isSynced },
                        withSynced = { it.copy(isSynced = true) }
                    )
                    if (toInsert.isNotEmpty()) dao.insertAssets(toInsert)
                }
                "subscriptions" -> {
                    val toInsert = mergeList(
                        remote = list.map { json.decodeFromJsonElement(SubscriptionEntity.serializer(), it.data) },
                        local = dao.getAllSubscriptionsSyncFull(),
                        id = { it.id }, updatedAt = { it.updatedAt }, isSynced = { it.isSynced },
                        withSynced = { it.copy(isSynced = true) }
                    )
                    if (toInsert.isNotEmpty()) dao.insertSubscriptions(toInsert)
                }
                "journal_entries" -> {
                    val toInsert = mergeList(
                        remote = list.map { json.decodeFromJsonElement(JournalEntryEntity.serializer(), it.data) },
                        local = dao.getAllJournalEntriesSync(),
                        id = { it.id }, updatedAt = { it.updatedAt }, isSynced = { it.isSynced },
                        withSynced = { it.copy(isSynced = true) }
                    )
                    if (toInsert.isNotEmpty()) dao.insertJournalEntries(toInsert)
                }
                "postings" -> {
                    val toInsert = mergeList(
                        remote = list.map { json.decodeFromJsonElement(PostingEntity.serializer(), it.data) },
                        local = dao.getAllPostingsSync(),
                        id = { it.id }, updatedAt = { it.updatedAt }, isSynced = { it.isSynced },
                        withSynced = { it.copy(isSynced = true) }
                    )
                    if (toInsert.isNotEmpty()) dao.insertPostings(toInsert)
                }
                "transactions" -> {
                    val toInsert = mergeList(
                        remote = list.map { json.decodeFromJsonElement(Transaction.serializer(), it.data) },
                        local = dao.getAllTransactionsSync(),
                        id = { it.id }, updatedAt = { it.updatedAt }, isSynced = { it.isSynced },
                        withSynced = { it.copy(isSynced = true) }
                    )
                    if (toInsert.isNotEmpty()) dao.insertTransactions(toInsert)
                }
                "loan_payments" -> {
                    val toInsert = mergeList(
                        remote = list.map { json.decodeFromJsonElement(LoanPayment.serializer(), it.data) },
                        local = dao.getAllLoanPaymentsSync(),
                        id = { it.id }, updatedAt = { it.updatedAt }, isSynced = { it.isSynced },
                        withSynced = { it.copy(isSynced = true) }
                    )
                    if (toInsert.isNotEmpty()) dao.insertLoanPayments(toInsert)
                }
                "account_transactions" -> {
                    val toInsert = mergeList(
                        remote = list.map { json.decodeFromJsonElement(AccountTransaction.serializer(), it.data) },
                        local = dao.getAllAccountTransactionsSync(),
                        id = { it.id }, updatedAt = { it.updatedAt }, isSynced = { it.isSynced },
                        withSynced = { it.copy(isSynced = true) }
                    )
                    if (toInsert.isNotEmpty()) dao.insertAccountTransactions(toInsert)
                }
                "pending_transactions" -> {
                    val toInsert = mergeList(
                        remote = list.map { json.decodeFromJsonElement(PendingTransaction.serializer(), it.data) },
                        local = dao.getAllPendingTransactionsSync(),
                        id = { it.id }, updatedAt = { it.updatedAt }, isSynced = { it.isSynced },
                        withSynced = { it.copy(isSynced = true) }
                    )
                    if (toInsert.isNotEmpty()) dao.insertPendingTransactions(toInsert)
                }
                "ai_conversations" -> {
                    val toInsert = mergeList(
                        remote = list.map { json.decodeFromJsonElement(AiConversation.serializer(), it.data) },
                        local = dao.getAllAiConversationsSync(),
                        id = { it.id }, updatedAt = { it.updatedAt }, isSynced = { it.isSynced },
                        withSynced = { it.copy(isSynced = true) }
                    )
                    if (toInsert.isNotEmpty()) dao.insertAiConversations(toInsert)
                }
                "ai_chat_messages" -> {
                    val toInsert = mergeList(
                        remote = list.map { json.decodeFromJsonElement(AiChatMessage.serializer(), it.data) },
                        local = dao.getAllAiChatMessagesSync(),
                        id = { it.id }, updatedAt = { it.updatedAt }, isSynced = { it.isSynced },
                        withSynced = { it.copy(isSynced = true) }
                    )
                    if (toInsert.isNotEmpty()) dao.insertAiChatMessages(toInsert)
                }
            }
        }
    }

    private fun <T> mergeList(
        remote: List<T>,
        local: List<T>,
        id: (T) -> String,
        updatedAt: (T) -> Long,
        isSynced: (T) -> Boolean,
        withSynced: (T) -> T
    ): List<T> {
        val localMap = local.associateBy(id)
        return remote.filter { r ->
            val l = localMap[id(r)]
            l == null || (isSynced(l) && updatedAt(r) > updatedAt(l))
        }.map(withSynced)
    }

    private suspend fun getAuthHeader(): String? {
        val session = Clerk.sessionFlow.value ?: return null
        val result = session.fetchToken(GetTokenOptions())
        if (result is ClerkResult.Success && !result.value.jwt.isNullOrEmpty()) {
            return "Bearer ${result.value.jwt}"
        }
        return null
    }

    @Deprecated("Deletes are tombstones in the batch push")
    suspend fun deleteFromCloud(table: String, id: String): Boolean = true
}
