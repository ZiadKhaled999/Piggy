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

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; classDiscriminator = "type" }
    private val client = OkHttpClient()
    
    // We get the key from BuildConfig (requires GROQ_API_KEY in .env)
    // You can override this if Groq is preferred or DeepSeek is preferred.
    private val apiKey: String = BuildConfig.GROQ_API_KEY

    fun getAllConversations(): Flow<List<AiConversation>> {
        return dao.getAllConversationsFlow()
    }

    suspend fun saveConversation(conversation: AiConversation) {
        dao.insertConversation(conversation)
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
        dao.insertChatMessage(AiChatMessage(conversationId = conversationId, role = role, content = content))
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
        try {
            val isGroq = apiKey.startsWith("gsk_")
            val modelName = if (isGroq) "llama-3.1-8b-instant" else "deepseek-reasoner"
            val endpointUrl = if (isGroq) "https://api.groq.com/openai/v1/chat/completions" else "https://api.deepseek.com/chat/completions"
            
            val requestBody = GroqRequest(
                model = modelName,
                messages = messages,
                temperature = 0.5
            )
            
            val requestStr = json.encodeToString(requestBody)
            val request = Request.Builder()
                .url(endpointUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestStr.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
            
            if (response.isSuccessful) {
                val groqResponse = json.decodeFromString<GroqResponse>(responseBody)
                val rawContent = groqResponse.choices.firstOrNull()?.message?.content ?: return@withContext Result.failure(Exception("No content in response"))
                
                // Clean up any DeepSeek <think> tags if present
                val cleanedContent = if (rawContent.contains("</think>")) {
                    val thinkParts = rawContent.split("</think>")
                    val thinkingStr = thinkParts[0].replace("<think>", "").trim()
                    val actualAnswer = thinkParts.getOrElse(1) { "" }.trim()
                    if (actualAnswer.isNotBlank()) {
                        actualAnswer
                    } else {
                        thinkingStr
                    }
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
                Result.success(parsed)
            } else {
                android.util.Log.e("AiChat", "API Error: ${response.code} - $responseBody")
                val friendlyMessage = when (response.code) {
                    401, 403 -> "Service temporarily unavailable. Please try again later."
                    429 -> "Service is busy right now. Please wait a moment and try again."
                    500, 502, 503 -> "Service is temporarily unavailable. Please try again later."
                    else -> "Unable to complete request. Please check your connection and try again."
                }
                Result.failure(Exception(friendlyMessage))
            }
        } catch (e: Exception) {
            android.util.Log.e("AiChat", "Exception: ${e.message}", e)
            Result.failure(e)
        }
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
