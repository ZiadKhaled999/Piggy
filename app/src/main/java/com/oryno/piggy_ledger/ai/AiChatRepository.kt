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

class AiChatRepository(private val dao: PiggyLedgerDao) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; classDiscriminator = "type" }
    private val client = OkHttpClient()
    
    // We get the key from BuildConfig (requires GROQ_API_KEY in .env)
    // You can override this if Groq is preferred or DeepSeek is preferred.
    private val apiKey: String = BuildConfig.GROQ_API_KEY

    fun getChatHistory(): Flow<List<AiChatMessage>> {
        return dao.getAllChatMessagesFlow()
    }

    suspend fun saveMessage(role: String, content: String) {
        dao.insertChatMessage(AiChatMessage(role = role, content = content))
    }

    suspend fun clearHistory() {
        dao.clearChatMessages()
    }

    suspend fun fetchContextData(): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val accounts = dao.getAllAccountsSync()
            val goals = dao.getAllGoalsSync()
            val loans = dao.getAllLoansSync()
            val recentTransactions = dao.getAllAccountTransactionsSync().take(15)
            val pending = dao.getAllPendingTransactionsSync()
            
            val accountSummary = if (accounts.isEmpty()) "No accounts logged yet." 
                else accounts.joinToString("\n") { "- ${it.name} (${it.type}): ${it.current_balance} ${it.currency}" }
                
            val goalSummary = if (goals.isEmpty()) "No active goals set." 
                else goals.joinToString("\n") { "- ${it.name}: Target ${it.targetAmount}" }
                
            val loanSummary = if (loans.isEmpty()) "No active loans." 
                else loans.joinToString("\n") { "- ${it.type.name} with ${it.contactName}: ${it.amount} (Paid: ${it.isPaidOff})" }
                
            val txSummary = if (recentTransactions.isEmpty()) "No recent transactions."
                else recentTransactions.joinToString("\n") { "- ${it.merchant}: ${it.amount} (Account ID: ${it.account_id})" }
                
            val pendingSummary = if (pending.isEmpty()) "None."
                else pending.joinToString("\n") { "- ${it.merchant}: ${it.amount}" }
                
            """
            |=== KNOWLEDGE HUB INDEX ===
            |[MODULE 1: ACCOUNTS & LIQUIDITY]
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
                Result.failure(Exception("API Error ${response.code}: $responseBody"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AiChat", "Exception: ${e.message}", e)
            Result.failure(e)
        }
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
