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
    private val client = OkHttpClient()
    
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
            val accounts = dao.getAllAccountsSync()
            val goals = dao.getAllGoalsSync()
            val loans = dao.getAllLoansSync()
            val recentTransactions = dao.getAllAccountTransactionsSync().take(30)
            val pending = dao.getAllPendingTransactionsSync()
            
            // Streak Info
            val streakInfo = if (context != null) {
                val current = com.oryno.piggy_ledger.data.StreakManager.getStreak(context)
                val longest = com.oryno.piggy_ledger.data.StreakManager.getLongestStreak(context)
                val hasActionToday = com.oryno.piggy_ledger.data.StreakManager.hasActionToday(context)
                "Active Streak: $current days (Longest: $longest days, Logged Today: $hasActionToday)"
            } else {
                "Streak status unavailable."
            }
            
            val totalNetBalance = accounts.sumOf { it.current_balance }
            
            val accountSummary = if (accounts.isEmpty()) "No accounts logged yet." 
                else accounts.joinToString("\n") { "- ${it.name} (${it.type}): ${it.current_balance} ${it.currency} (Provider: ${it.provider ?: "N/A"})" }
                
            val goalSummary = if (goals.isEmpty()) "No active goals set." 
                else goals.joinToString("\n") { "- ${it.name}: Target ${it.targetAmount}" }
                
            val loanSummary = if (loans.isEmpty()) "No active loans." 
                else loans.joinToString("\n") { "- ${it.type.name} with ${it.contactName}: Amount ${it.amount} (Paid Off: ${it.isPaidOff})" }
                
            val txSummary = if (recentTransactions.isEmpty()) "No recent transactions."
                else recentTransactions.joinToString("\n") { tx ->
                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(tx.timestamp))
                    "- $dateStr | ${tx.merchant}: ${tx.amount} (${tx.source})"
                }
                
            val pendingSummary = if (pending.isEmpty()) "None."
                else pending.joinToString("\n") { "- ${it.merchant}: ${it.amount}" }
                
            """
            |=== KNOWLEDGE HUB INDEX ===
            |[MODULE 0: USER STREAK & HABIT METRICS]
            |$streakInfo
            |
            |[MODULE 1: ACCOUNTS & LIQUIDITY]
            |Total Net Balance across all accounts: $totalNetBalance
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

        // Primary model as requested by user
        val primaryModel = "qwen/qwen3.6-27b"

        try {
            val requestBody = GroqRequest(
                model = primaryModel,
                messages = sanitizedMessages,
                temperature = 0.6,
                maxCompletionTokens = 2048,
                topP = 0.95,
                stream = false, // Current UI handles non-streaming result parsing
                reasoningEffort = "default"
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

                // Clean up any DeepSeek <think> tags if present
                val cleanedContent = if (rawContent.contains("</think>")) {
                    val thinkParts = rawContent.split("</think>")
                    val thinkingStr = thinkParts[0].replace("<think>", "").trim()
                    val actualAnswer = thinkParts.getOrElse(1) { "" }.trim()
                    if (actualAnswer.isNotBlank()) actualAnswer else thinkingStr
                } else {
                    rawContent
                }

                val jsonStr = extractJson(cleanedContent)
                val parsed = if (jsonStr.isNotBlank() && jsonStr.contains("archetype_rationale")) {
                    try {
                        json.decodeFromString<SovereignAiResponse>(jsonStr)
                    } catch (e: Exception) {
                        SovereignAiResponse(
                            archetypeRationale = cleanedContent,
                            currentArchetype = "",
                            uiBlocks = emptyList()
                        )
                    }
                } else {
                    SovereignAiResponse(
                        archetypeRationale = cleanedContent,
                        currentArchetype = "",
                        uiBlocks = emptyList()
                    )
                }
                return@withContext Result.success(parsed)
            } else {
                android.util.Log.e("AiChat", "Model $primaryModel API Error ${response.code}: $responseBody")

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
                    404 -> errorDetail ?: "AI Model not found ($primaryModel)."
                    429 -> "AI Rate limit reached. Please wait a few moments."
                    in 500..599 -> "AI Server is temporarily overloaded."
                    else -> errorDetail ?: "AI service response error (${response.code})."
                }
                lastException = Exception(message)
                return@withContext Result.failure(lastException)
            }
        } catch (e: java.net.UnknownHostException) {
                android.util.Log.e("AiChat", "DNS/Network issue: ${e.message}")
                return@withContext Result.failure(Exception("Unable to connect to AI server. Please check your internet connection."))
            } catch (e: java.net.SocketTimeoutException) {
                android.util.Log.e("AiChat", "Timeout: ${e.message}")
                return@withContext Result.failure(Exception("AI request timed out. Please check your connection and retry."))
            } catch (e: Exception) {
                android.util.Log.e("AiChat", "Exception: ${e.message}", e)
                lastException = e
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
