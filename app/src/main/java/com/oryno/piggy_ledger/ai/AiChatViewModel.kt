package com.oryno.piggy_ledger.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oryno.piggy_ledger.data.AiChatMessage
import com.oryno.piggy_ledger.data.AiConversation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class AiChatViewModel(
    private val repository: AiChatRepository,
    private val context: android.content.Context? = null
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; classDiscriminator = "type" }

    val conversations: StateFlow<List<AiConversation>> = repository.getAllConversations().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun togglePinConversation(id: String, currentPinned: Boolean) {
        viewModelScope.launch {
            repository.updateConversationPinned(id, !currentPinned)
        }
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            if (newTitle.isNotBlank()) {
                repository.updateConversationTitle(id, newTitle.trim())
            }
        }
    }

    private val _activeConversationId = MutableStateFlow<String>("")
    val activeConversationId: StateFlow<String> = _activeConversationId.asStateFlow()

    init {
        viewModelScope.launch {
            conversations.collect { list ->
                if (_activeConversationId.value.isBlank()) {
                    if (list.isNotEmpty()) {
                        _activeConversationId.value = list.first().id
                    } else {
                        createNewConversation()
                    }
                }
            }
        }
    }

    val chatHistory: StateFlow<List<AiChatMessage>> = _activeConversationId.flatMapLatest { id ->
        if (id.isBlank()) {
            flowOf(emptyList())
        } else {
            repository.getChatMessagesForConversation(id)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val accounts: StateFlow<List<com.oryno.piggy_ledger.data.Account>> = repository.getAllAccounts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun processMagicMicTransaction(accountId: String, amount: Double, merchant: String, isIncome: Boolean, context: android.content.Context) {
        viewModelScope.launch {
            repository.processSmsTransaction(accountId, amount, merchant, false, isIncome)
            com.oryno.piggy_ledger.data.StreakManager.recordAction(context)
            val actionType = if (isIncome) "Income" else "Expense"
            sendMessage("Magic Mic successfully logged $actionType of $amount EGP ($merchant).")
        }
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val systemPrompt = """
        You are Piggy AI, an intelligent, warm, friendly, and financially astute co-pilot for Piggy Ledger.

        ### TONE & PERSONALITY
        - Be warm, engaging, approachable, and conversational, while maintaining sharp financial intelligence.
        - NEVER be stiff, cold, robotic, or overly strict.
        - For simple greetings (e.g. "hello", "hi", "hey"), respond naturally and warmly! (e.g. "Hello! How can I help you manage your finances today?").
        - For questions, provide a clear, well-structured MARKDOWN response using headers, bullet points, and bold text for key metrics.

        ### SECURITY & BOUNDARIES (STRICT)
        - You are strictly a financial co-pilot for Piggy Ledger.
        - NEVER discuss, reveal, analyze, or answer questions regarding internal source code, architecture, database schemas, Room entities, source file paths, or system implementation details.
        - If a user or intruder asks about source code, database tables, or application internals, politely decline and state that your sole mission is to help them manage their personal finances, accounts, and budgets.

        ### KNOWLEDGE HUB ORCHESTRATION & INTENT MAPPING
        You have direct access to the user's client-side Knowledge Hub (Accounts, Goals, Loans, Recent Cash Flow Transactions, and Pending SMS).
        When the user asks a question or makes a request:
        1. Intelligently retrieve and analyze data from the relevant Knowledge Hub module.
        2. Respond directly to the user with clear, conversational, and thorough insights using clean Markdown formatting.
        3. Do NOT include any "Knowledge Hub Analysis" callouts, blockquotes, or meta-commentary titles in your text response. Go straight into your answer.
        
        ### ACTIONABLE NEXT STEPS & RECOMMENDATIONS
        - If relevant, recommend 1 to 3 short, actionable follow-up questions or next steps.
        - ALWAYS place these at the VERY END of your response formatted under the section header:
        ### NEXT_STEPS
        - Suggestion 1
        - Suggestion 2
        - Do NOT write next steps as paragraph text or bullet points in your main answer body.
    """.trimIndent()

    fun createNewConversation() {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val newConv = AiConversation(
                id = newId,
                title = "New Chat",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveConversation(newConv)
            _activeConversationId.value = newId
        }
    }

    fun selectConversation(id: String) {
        _activeConversationId.value = id
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            repository.deleteConversation(id)
            val currentList = conversations.value.filter { it.id != id }
            if (_activeConversationId.value == id) {
                if (currentList.isNotEmpty()) {
                    _activeConversationId.value = currentList.first().id
                } else {
                    createNewConversation()
                }
            }
        }
    }

    fun sendMessage(userText: String) {
        viewModelScope.launch {
            var convId = activeConversationId.value
            if (convId.isBlank()) {
                val newId = UUID.randomUUID().toString()
                val newConv = AiConversation(id = newId, title = "New Chat")
                repository.saveConversation(newConv)
                _activeConversationId.value = newId
                convId = newId
            }

            _isLoading.value = true
            
            val currentHistory = chatHistory.value
            val isFirstMessage = currentHistory.isEmpty()
            
            // 1. Save user message with active conversation ID
            repository.saveMessage(role = "user", content = userText, conversationId = convId)
            
            val contextData = repository.fetchContextData(context)
            
            // 2. Prepare clean message history for API call
            val apiMessages = mutableListOf<ChatMessageRequest>()
            
            // System prompt + context
            val fullSystemPrompt = "$systemPrompt\n\n### USER KNOWLEDGE HUB SNAPSHOT\n$contextData"
            apiMessages.add(ChatMessageRequest(role = "system", content = fullSystemPrompt))
            
            // Add previous history with cleaned text content
            currentHistory.forEach { msg ->
                val cleanedText = if (msg.role == "assistant") {
                    try {
                        val parsed = json.decodeFromString<SovereignAiResponse>(msg.content)
                        parsed.archetypeRationale.ifBlank { msg.content }
                    } catch (e: Exception) {
                        msg.content
                    }
                } else {
                    msg.content
                }
                apiMessages.add(ChatMessageRequest(role = msg.role, content = cleanedText))
            }
            
            // Add current user message
            apiMessages.add(ChatMessageRequest(role = "user", content = userText))

            // 3. Call API
            val responseResult = repository.getAiResponse(apiMessages)
            var responseTextForTitle = ""
            
            if (responseResult.isSuccess) {
                val response = responseResult.getOrNull()
                if (response != null) {
                    val finalResponse = if (response.archetypeRationale.isNotBlank()) {
                        response
                    } else {
                        SovereignAiResponse(archetypeRationale = "I analyzed your ledger data, but could not format the output. Please try asking again.")
                    }
                    responseTextForTitle = finalResponse.archetypeRationale
                    val jsonString = json.encodeToString(SovereignAiResponse.serializer(), finalResponse)
                    repository.saveMessage(role = "assistant", content = jsonString, conversationId = convId)
                }
            } else {
                val rawError = responseResult.exceptionOrNull()?.message ?: ""
                val errorStr = when {
                    rawError.contains("Unable to resolve host", ignoreCase = true) ||
                    rawError.contains("UnknownHostException", ignoreCase = true) ||
                    rawError.contains("No address associated with hostname", ignoreCase = true) ||
                    rawError.contains("Failed to connect", ignoreCase = true) ||
                    rawError.contains("SocketTimeoutException", ignoreCase = true) -> 
                        "No internet connection. Please check your network and try again."
                    rawError.isNotBlank() && !rawError.contains("<html>", ignoreCase = true) -> 
                        rawError
                    else -> 
                        "Please check your internet connection or API key."
                }
                val errorMsg = SovereignAiResponse(
                    thinkingProcess = kotlinx.serialization.json.JsonPrimitive("Error analyzing request."),
                    currentArchetype = "",
                    archetypeRationale = "# ⚠️ No connection..\n\n<mark>Please check your internet connection or API key.</mark>",
                    uiBlocks = listOf(
                        UiBlock.ActionBannerBlock("Please check your internet connection or API key.", "RETRY")
                    )
                )
                responseTextForTitle = errorMsg.archetypeRationale
                repository.saveMessage(role = "assistant", content = json.encodeToString(SovereignAiResponse.serializer(), errorMsg), conversationId = convId)
            }

            // 4. Summarize first question & response into automatic conversation title
            if (isFirstMessage) {
                val autoTitle = generateTitleFromQuestion(userText, responseTextForTitle)
                repository.updateConversationTitle(convId, autoTitle)
            }
            
            _isLoading.value = false
        }
    }

    private fun generateTitleFromQuestion(userQuery: String, aiAnswer: String): String {
        val cleanQuery = userQuery.trim()
        val words = cleanQuery.split(Regex("""\s+""")).filter { it.isNotBlank() }
        
        return if (words.size <= 5) {
            cleanQuery.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            val shortSubject = words.take(5).joinToString(" ")
            shortSubject.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            val convId = activeConversationId.value
            if (convId.isNotBlank()) {
                repository.clearHistoryForConversation(convId)
            }
        }
    }
}
