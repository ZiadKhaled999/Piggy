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

    suspend fun fetchContextData(context: android.content.Context? = null): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val allAccountsList = dao.getAllAccountsSync().filter { !it.is_deleted }
            val accounts = allAccountsList.filter { !it.exclude_from_all }
            val excludedAccountIds = allAccountsList.filter { it.exclude_from_all }.map { it.id }.toSet()
            
            val goals = dao.getActiveGoalsSync()
            val goalTransactions = dao.getActiveTransactionsSync()
            val loans = dao.getAllLoansSync().filter { !it.is_deleted }
            val allAccountTxs = dao.getAllAccountTransactionsSync().filter { !it.is_deleted && !excludedAccountIds.contains(it.account_id) }
            val recentTransactions = allAccountTxs.take(30)
            val pending = dao.getAllPendingTransactionsSync()
            
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
                    "- ${g.name}:\n  Current: $saved $primaryCurrency\n  Target: ${g.targetAmount} $primaryCurrency"
                }
                
            val loanSummary = if (loans.isEmpty()) "No active loans." 
                else loans.joinToString("\n") { "- ${it.type.name} with ${it.contactName}: Amount ${it.amount} $primaryCurrency (Paid Off: ${it.isPaidOff})" }
                
            val txSummary = if (recentTransactions.isEmpty()) "No recent transactions."
                else recentTransactions.joinToString("\n") { tx ->
                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(tx.timestamp))
                    "- $dateStr | ${tx.merchant}: ${tx.amount} $primaryCurrency (${tx.source})"
                }
                
            val pendingSummary = if (pending.isEmpty()) "None."
                else pending.joinToString("\n") { "- ${it.merchant}: ${it.amount} $primaryCurrency" }
                
            """
            |USER FINANCIAL CONTEXT
            |
            |Currency: $primaryCurrency
            |
            |Current period:
            |Income: $totalIncome
            |Expenses: $totalExpenses
            |Balance: $totalNetBalance
            |
            |Accounts:
            |$accountSummary
            |
            |Goals:
            |$goalSummary
            |
            |Loans & Debts:
            |$loanSummary
            |
            |Recent Transactions:
            |$txSummary
            |
            |Pending SMS:
            |$pendingSummary
            |
            |=== KNOWLEDGE HUB INDEX ===
            |[MODULE 0: USER STREAK & HABIT METRICS]
            |$streakInfo
            |
            |[MODULE 1: ACCOUNTS & LIQUIDITY]
            |Total Net Balance across all accounts: $totalNetBalance $primaryCurrency
            |$accountSummary
            |
            |[MODULE 2: SAVINGS & FINANCIAL GOALS]
            |$goalSummary
            |
            |[MODULE 3: LOANS & DEBT]
            |$loanSummary
            |
            |[MODULE 4: RECENT CASH FLOW TRANSACTIONS]
            |$txSummary
            |
            |[MODULE 5: PENDING SMS TRANSACTIONS]
            |$pendingSummary
            |===========================
            """.trimMargin()
        } catch (e: Exception) {
            "Knowledge Hub unavailable."
        }
    }

    suspend fun getAiResponse(messages: List<ChatMessageRequest>): Result<SovereignAiResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "YOUR_GROQ_API_KEY") {
            return@withContext Result.failure(Exception("AI service key is not configured. Please ensure GROQ_API_KEY is set in your configuration."))
        }

        val sanitizedMessages = messages.filter { it.content.isNotBlank() }
        if (sanitizedMessages.isEmpty()) {
            return@withContext Result.failure(Exception("Please enter a message to send."))
        }

        val isGroq = apiKey.startsWith("gsk_") || !apiKey.startsWith("sk-")
        val endpointUrl = if (isGroq) "https://api.groq.com/openai/v1/chat/completions" else "https://api.deepseek.com/chat/completions"

        var lastException: Exception? = null

        // Primary model + fallback models if Groq is used
        val modelsToTry = if (isGroq) {
            listOf("qwen/qwen3.6-27b", "llama-3.3-70b-versatile")
        } else {
            listOf("deepseek-chat")
        }

        for (candidateModel in modelsToTry) {
            for (attempt in 1..2) {
                try {
                    val requestBody = GroqRequest(
                        model = candidateModel,
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
                        .addHeader("Authorization", "Bearer $apiKey")
                        .post(requestStr.toRequestBody("application/json".toMediaType()))
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string().orEmpty()

                    if (response.isSuccessful && responseBody.isNotBlank()) {
                        val groqResponse = json.decodeFromString<GroqResponse>(responseBody)
                        val rawContent = groqResponse.choices.firstOrNull()?.message?.content
                            ?: return@withContext Result.failure(Exception("AI did not produce text content. Please try again."))

                        // Completely eliminate thinking tokens and tags from the ground up
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
                        android.util.Log.w("AiChat", "Model $candidateModel attempt $attempt API Error ${response.code}: $responseBody")

                        // Extract detailed error message from response json if available
                        val errorDetail = try {
                            val parsedObj = json.parseToJsonElement(responseBody)
                            val errObj = parsedObj.toString()
                            if (errObj.contains("\"message\"")) {
                                val msgPart = errObj.substringAfter("\"message\":\"").substringBefore("\"")
                                if (msgPart.isNotBlank() && !msgPart.contains("{")) msgPart else null
                            } else null
                        } catch (e: Exception) {
                            null
                        }

                        val message = when (response.code) {
                            401 -> "AI Authentication failed. Please check your API key."
                            403 -> "AI Access forbidden. Please verify your account access."
                            404 -> errorDetail ?: "AI Model not found ($candidateModel)."
                            429 -> "AI Rate limit reached. Retrying shortly..."
                            in 500..599 -> "AI Server is temporarily busy. Retrying..."
                            else -> errorDetail ?: "AI service response error (${response.code})."
                        }
                        lastException = Exception(message)
                        if (response.code == 401 || response.code == 403) {
                            return@withContext Result.failure(lastException)
                        }
                    }
                } catch (e: java.net.UnknownHostException) {
                    android.util.Log.e("AiChat", "DNS/Network issue on attempt $attempt: ${e.message}")
                    lastException = Exception("Unable to connect to AI server. Please check your internet connection.")
                } catch (e: java.net.SocketTimeoutException) {
                    android.util.Log.w("AiChat", "Timeout on $candidateModel attempt $attempt: ${e.message}")
                    lastException = Exception("AI request timed out. Please check your connection and retry.")
                } catch (e: Exception) {
                    android.util.Log.e("AiChat", "Exception on $candidateModel attempt $attempt: ${e.message}", e)
                    lastException = e
                }

                // Short backoff before next retry
                if (attempt < 2) {
                    kotlinx.coroutines.delay(600)
                }
            }
        }

        Result.failure(lastException ?: Exception("Unable to get AI response. Please try again."))
    }

    fun getAllAccounts(): Flow<List<com.oryno.piggy_ledger.data.Account>> {
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
