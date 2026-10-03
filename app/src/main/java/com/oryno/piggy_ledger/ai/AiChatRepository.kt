package com.oryno.piggy_ledger.ai

import com.oryno.piggy_ledger.BuildConfig
import com.oryno.piggy_ledger.data.AiChatMessage
import com.oryno.piggy_ledger.data.PiggyLedgerDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

import com.oryno.piggy_ledger.data.AiConversation

class AiChatRepository(private val dao: PiggyLedgerDao) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; classDiscriminator = "type"; encodeDefaults = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    
    // We get the key from BuildConfig (requires GROQ_API_KEY in .env)
    // You can override this if Groq is preferred or DeepSeek is preferred.
    private val apiKey: String = BuildConfig.GROQ_API_KEY

    fun getAllConversations(): Flow<List<AiConversation>> {
        return dao.getAllConversationsFlow()
    }

    suspend fun saveConversation(conversation: AiConversation) {
        val user = com.clerk.api.Clerk.userFlow.value
        val userId = user?.id ?: "local_user"
        val updatedConversation = if (conversation.userId.isBlank()) conversation.copy(userId = userId) else conversation
        dao.insertConversation(updatedConversation)
    }

    suspend fun updateConversationTitle(id: String, title: String) {
        dao.updateConversationTitle(id, title)
    }

    suspend fun updateConversationPinned(id: String, isPinned: Boolean) {
        dao.updateConversationPinned(id, isPinned)
    }

    suspend fun deleteConversation(context: android.content.Context, id: String) {
        dao.deleteConversationById(id)
        dao.deleteChatMessagesForConversation(id)
        try {
            com.oryno.piggy_ledger.service.SyncManager(context).deleteFromCloud("ai_conversations", id)
        } catch (e: Exception) {}
    }

    fun getChatMessagesForConversation(conversationId: String): Flow<List<AiChatMessage>> {
        return dao.getChatMessagesForConversationFlow(conversationId)
    }

    fun getChatHistory(): Flow<List<AiChatMessage>> {
        return dao.getAllChatMessagesFlow()
    }

    fun getUserAiMessagesCountFlow(): Flow<Int> {
        return dao.getUserAiMessagesCountFlow()
    }

    suspend fun getUserAiMessagesCount(): Int {
        return dao.getUserAiMessagesCount()
    }

    suspend fun saveMessage(role: String, content: String, conversationId: String = "default") {
        val user = com.clerk.api.Clerk.userFlow.value
        val userId = user?.id ?: "local_user"
        dao.insertChatMessage(AiChatMessage(conversationId = conversationId, role = role, content = content, userId = userId))
    }

    suspend fun clearHistoryForConversation(conversationId: String) {
        dao.deleteChatMessagesForConversation(conversationId)
    }

    suspend fun clearHistory() {
        dao.clearChatMessages()
    }

    suspend fun deleteMessage(id: String) {
        dao.deleteChatMessageById(id)
    }

    suspend fun fetchContextData(context: android.content.Context? = null): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val allAccountsList = dao.getAllAccountsSync().filter { !it.is_deleted }
            val accountsMap = allAccountsList.associateBy { it.id }
            val accounts = allAccountsList.filter { !it.exclude_from_all }
            val excludedAccountIds = allAccountsList.filter { it.exclude_from_all }.map { it.id }.toSet()
            
            val goals = dao.getActiveGoalsSync()
            val goalTransactions = dao.getActiveTransactionsSync()
            val loans = dao.getAllLoansSync().filter { !it.is_deleted }
            val loanPayments = dao.getAllLoanPaymentsSync().filter { !it.is_deleted }
            
            val allAccountTxs = dao.getAllAccountTransactionsSync()
                .filter { !it.is_deleted && !excludedAccountIds.contains(it.account_id) }
                .sortedByDescending { it.timestamp }
            
            val pending = dao.getAllPendingTransactionsSync().filter { !it.is_deleted }
            
            val primaryCurrency = accounts.firstOrNull()?.currency ?: "EGP"
            val totalIncome = allAccountTxs.filter { it.amount > 0 }.sumOf { it.amount }
            val totalExpenses = allAccountTxs.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }
            val totalNetBalance = accounts.sumOf { it.current_balance }

            // Streak Info
            val streakInfo = if (context != null) {
                val current = com.oryno.piggy_ledger.data.StreakManager.getStreak(context)
                val longest = com.oryno.piggy_ledger.data.StreakManager.getLongestStreak(context)
                val hasActionToday = com.oryno.piggy_ledger.data.StreakManager.hasActionToday(context)
                "Active Streak: $current days (Longest: $longest days, Logged Today: $hasActionToday)"
            } else {
                "Streak status unavailable."
            }
            
            val accountSummary = if (accounts.isEmpty()) "No accounts logged yet." 
                else accounts.joinToString("\n") { "- ${it.name} (${it.type}): ${it.current_balance} ${it.currency} (Provider: ${it.provider ?: "N/A"})" }
                
            val goalSummary = if (goals.isEmpty()) "No active goals set." 
                else goals.joinToString("\n") { g ->
                    val saved = goalTransactions.filter { it.goalId == g.id }.sumOf { it.amount }
                    "- ${g.name}: Current $saved / Target ${g.targetAmount} $primaryCurrency"
                }
                
            val loanSummary = if (loans.isEmpty()) "No active loans." 
                else loans.joinToString("\n") { loan ->
                    val pmts = loanPayments.filter { it.loanId == loan.id }
                    val totalPaid = pmts.sumOf { it.amount }
                    val remaining = maxOf(0.0, loan.amount - totalPaid)
                    "- ${loan.type.name} with ${loan.contactName}: Principal ${loan.amount} $primaryCurrency | Paid: $totalPaid | Remaining: $remaining | Paid Off: ${loan.isPaidOff}"
                }

            val topExpenseMerchants = allAccountTxs.filter { it.amount < 0 }
                .groupBy { it.merchant.ifBlank { "Uncategorized/Other" } }
                .mapValues { entry -> entry.value.sumOf { kotlin.math.abs(it.amount) } }
                .entries.sortedByDescending { it.value }
                .take(15)
                .joinToString("\n") { "- ${it.key}: ${it.value} $primaryCurrency" }

            val topIncomeSources = allAccountTxs.filter { it.amount > 0 }
                .groupBy { it.merchant.ifBlank { "General Income" } }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .entries.sortedByDescending { it.value }
                .take(15)
                .joinToString("\n") { "- ${it.key}: ${it.value} $primaryCurrency" }
                
            val txSummary = if (allAccountTxs.isEmpty()) "No transactions logged in account history."
                else allAccountTxs.joinToString("\n") { tx ->
                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(tx.timestamp))
                    val accountName = accountsMap[tx.account_id]?.name ?: "Account"
                    val typeLabel = if (tx.amount >= 0) "INCOME" else "EXPENSE"
                    "- $dateStr | $accountName | $typeLabel | ${tx.merchant.ifBlank { "General Transaction" }}: ${tx.amount} $primaryCurrency (${tx.source})"
                }
                
            val pendingSummary = if (pending.isEmpty()) "None."
                else pending.joinToString("\n") { "- ${it.merchant}: ${it.amount} $primaryCurrency (${it.sender})" }
                
            """
            |USER FINANCIAL CONTEXT & COMPLETE HISTORICAL TRANSACTION LEDGER
            |
            |Primary Currency: $primaryCurrency
            |Total Historical Income: $totalIncome $primaryCurrency
            |Total Historical Expenses: $totalExpenses $primaryCurrency
            |Total Net Balance Across Accounts: $totalNetBalance $primaryCurrency
            |Total Transactions Recorded: ${allAccountTxs.size}
            |
            |ACCOUNTS SUMMARY:
            |$accountSummary
            |
            |SAVINGS GOALS:
            |$goalSummary
            |
            |LOANS & DEBTS:
            |$loanSummary
            |
            |TOP EXPENSE CATEGORIES / MERCHANTS:
            |${topExpenseMerchants.ifBlank { "None." }}
            |
            |TOP INCOME SOURCES:
            |${topIncomeSources.ifBlank { "None." }}
            |
            |COMPLETE TRANSACTION LEDGER (${allAccountTxs.size} transactions total, sorted newest first):
            |$txSummary
            |
            |PENDING SMS TRANSACTIONS:
            |$pendingSummary
            |
            |USER STREAK STATUS:
            |$streakInfo
            """.trimMargin()
        } catch (e: Exception) {
            "Knowledge Hub unavailable."
        }
    }

    suspend fun getAiResponse(messages: List<ChatMessageRequest>): Result<SovereignAiResponse> = withContext(Dispatchers.IO) {
        val sanitizedMessages = messages.filter { it.content.isNotBlank() }
        if (sanitizedMessages.isEmpty()) {
            return@withContext Result.failure(Exception("Please enter a question."))
        }

        val endpointUrl = "https://piggy-ai-gateway.albhyrytwamrwhy.workers.dev/"
        var lastException: Exception? = null

        for (attempt in 1..2) {
            try {
                val requestBody = GroqRequest(
                    model = "@cf/meta/llama-3.1-8b-instruct-fp8",
                    messages = sanitizedMessages,
                    temperature = 0.6,
                    maxCompletionTokens = 2048,
                    topP = 0.95,
                    stream = false,
                    reasoningEffort = null
                )

                val requestStr = json.encodeToString(requestBody)

                val request = Request.Builder()
                    .url(endpointUrl)
                    .addHeader("Authorization", "Bearer DUMMY_KEY_CF")
                    .post(requestStr.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    val groqResponse = json.decodeFromString<GroqResponse>(responseBody)
                    val rawContent = groqResponse.choices.firstOrNull()?.message?.content
                        ?: return@withContext Result.failure(Exception("AI did not produce text content. Please try again."))
                    
                    val cleanedContent = AiSanitizer.sanitizeThinking(rawContent).ifBlank {
                        "I've analyzed your financial ledger. How can I assist you with your finances today?"
                    }

                    val jsonStr = extractJson(cleanedContent)
                    val parsed = if (jsonStr.isNotBlank() && jsonStr.contains("archetype_rationale")) {
                        try {
                            val decoded = json.decodeFromString<SovereignAiResponse>(jsonStr)
                            val cleanRationale = AiSanitizer.sanitizeThinking(decoded.archetypeRationale).ifBlank {
                                "I've analyzed your financial ledger. How can I assist you with your finances today?"
                            }
                            decoded.copy(
                                archetypeRationale = cleanRationale,
                                thinkingProcess = null
                            )
                        } catch (e: Exception) {
                            SovereignAiResponse(
                                archetypeRationale = cleanedContent,
                                currentArchetype = "",
                                uiBlocks = emptyList(),
                                thinkingProcess = null
                            )
                        }
                    } else {
                        SovereignAiResponse(
                            archetypeRationale = cleanedContent,
                            currentArchetype = "",
                            uiBlocks = emptyList(),
                            thinkingProcess = null
                        )
                    }
                    
                    return@withContext Result.success(parsed)
                } else {
                    android.util.Log.w("AiChat", "Worker attempt $attempt API Error ${response.code}: $responseBody")
                    val message = when (response.code) {
                        429 -> "Piggy is receiving high demand right now. Please try again shortly."
                        in 500..599 -> "Service is temporarily busy. Please try again shortly."
                        else -> "Service is temporarily unavailable. Please try again."
                    }
                    lastException = Exception(message)
                }
            } catch (e: java.net.UnknownHostException) {
                android.util.Log.e("AiChat", "DNS/Network issue on attempt $attempt: ${e.message}")
                lastException = Exception("Internet connection appears to be offline.")
            } catch (e: java.net.SocketTimeoutException) {
                android.util.Log.w("AiChat", "Timeout on attempt $attempt: ${e.message}")
                lastException = Exception("Connection timed out. Please check your network and retry.")
            } catch (e: Exception) {
                android.util.Log.e("AiChat", "Exception on attempt $attempt: ${e.message}", e)
                lastException = Exception("Service encountered a brief hiccup. Please try again.")
            }
            
            if (attempt < 2) {
                kotlinx.coroutines.delay(600)
            }
        }
        
        Result.failure(lastException ?: Exception("Service is temporarily busy. Please try again."))
    }

    fun getAllAccounts(): kotlinx.coroutines.flow.Flow<List<com.oryno.piggy_ledger.data.Account>> {
        return dao.getAllAccounts()
    }

    suspend fun processSmsTransaction(accountId: String, amount: Double, merchant: String, applyFee: Boolean, isIncome: Boolean) {
        dao.processSmsTransaction(accountId, amount, merchant, applyFee, isIncome)
    }

    private fun extractJson(content: String): String {
        val startIndex = content.indexOf("{")
        val endIndex = content.lastIndexOf("}")
        if (startIndex != -1 && endIndex != -1 && endIndex >= startIndex) {
            return content.substring(startIndex, endIndex + 1)
        }
        return content
    }
}
