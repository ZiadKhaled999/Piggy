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
    private val context: android.content.Context? = null,
    private val userPreferences: com.oryno.piggy_ledger.data.UserPreferences? = null
) : ViewModel() {

    val userName: StateFlow<String> = (userPreferences?.authUserName ?: flowOf("")).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

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

    private val _selectedModel = MutableStateFlow("Flash Extended")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    fun setSelectedModel(model: String) {
        _selectedModel.value = model
    }

    private var activeGenerationJob: kotlinx.coroutines.Job? = null

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        _isLoading.value = false
    }

    private val systemPrompt = """
        You are Piggy AI, an intelligent, warm, friendly, and financially astute co-pilot for Piggy Ledger.

        ### TONE & PERSONALITY
        - Be warm, engaging, approachable, and conversational, while maintaining sharp financial intelligence.
        - NEVER be stiff, cold, robotic, or overly strict.
        - For simple greetings (e.g. "hello", "hi", "hey"), respond naturally and warmly! (e.g. "Hello! How can I help you manage your finances today?").
        - For questions, provide a clear, well-structured MARKDOWN response using headers, bullet points, and bold text for key metrics.

        ### ASSISTANT ROLE & CAPABILITIES (STRICT)
        - You are strictly an ASSISTANT, FINANCIAL ADVISOR, and CO-PILOT.
        - You CANNOT directly perform database write actions, log loans, add goals, create transactions, or alter accounts for the user.
        - If the user asks you to log a loan, add a goal, record a transaction, or create an account, politely explain that as an AI assistant you provide financial insights and advice, and guide them with clear instructions on which screen/button in Piggy Ledger they can use to perform that action themselves.

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
            if (context != null) {
                repository.deleteConversation(context, id)
            }
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
        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            try {
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
                    val cleanUserError = when {
                        rawError.contains("Unable to resolve host", ignoreCase = true) ||
                        rawError.contains("UnknownHostException", ignoreCase = true) ||
                        rawError.contains("No address associated with hostname", ignoreCase = true) ||
                        rawError.contains("Failed to connect", ignoreCase = true) ||
                        rawError.contains("SocketTimeoutException", ignoreCase = true) ||
                        rawError.contains("connection", ignoreCase = true) -> 
                            "No internet connection. Please check your network and try again."
                        rawError.isNotBlank() && !rawError.contains("<html>", ignoreCase = true) && !rawError.contains("API", ignoreCase = true) -> 
                            rawError
                        else -> 
                            "Please check your internet connection and try again."
                    }
                    val errorMsg = SovereignAiResponse(
                        thinkingProcess = kotlinx.serialization.json.JsonPrimitive("Error analyzing request."),
                        currentArchetype = "",
                        archetypeRationale = "# ⚠️ No connection..\n\n<mark>$cleanUserError</mark>",
                        uiBlocks = listOf(
                            UiBlock.ActionBannerBlock(cleanUserError, "RETRY")
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
            } finally {
                _isLoading.value = false
                activeGenerationJob = null
            }
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

    fun clearAllConversations() {
        viewModelScope.launch {
            val all = conversations.value
            all.forEach { conv ->
                if (context != null) {
                    repository.deleteConversation(context, conv.id)
                }
            }
            createNewConversation()
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
