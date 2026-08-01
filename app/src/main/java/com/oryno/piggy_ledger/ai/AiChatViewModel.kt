package com.oryno.piggy_ledger.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class AiChatViewModel(private val repository: AiChatRepository) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; classDiscriminator = "type" }

    val chatHistory = repository.getChatHistory().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val systemPrompt = """
        You are Piggy AI, an intelligent, warm, friendly, and financially astute co-pilot for Piggy Ledger.

        ### TONE & PERSONALITY
        - Be warm, engaging, approachable, and conversational, while maintaining sharp financial intelligence.
        - NEVER be stiff, cold, robotic, or overly strict.
        - For simple greetings (e.g. "hello", "hi", "hey"), respond naturally and warmly! (e.g. "Hello! How can I help you manage your finances today?").
        - For questions, provide a clear, well-structured MARKDOWN response using headers, bullet points, and bold text for key metrics.

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

    fun sendMessage(userText: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val currentHistory = chatHistory.value
            
            // 1. Immediately save user message to database so UI updates instantly
            repository.saveMessage(role = "user", content = userText)
            
            val contextData = repository.fetchContextData()
            
            // 2. Prepare clean message history for API call
            val apiMessages = mutableListOf<ChatMessageRequest>()
            
            // System prompt + context
            val fullSystemPrompt = "$systemPrompt\n\n### USER KNOWLEDGE HUB SNAPSHOT\n$contextData"
            apiMessages.add(ChatMessageRequest(role = "system", content = fullSystemPrompt))
            
            // Add previous history with cleaned text content (extract text if message content was JSON)
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
            
            if (responseResult.isSuccess) {
                val response = responseResult.getOrNull()
                if (response != null) {
                    // If archetypeRationale has text, store it cleanly as SovereignAiResponse
                    val finalResponse = if (response.archetypeRationale.isNotBlank()) {
                        response
                    } else {
                        SovereignAiResponse(archetypeRationale = "I analyzed your ledger data, but could not format the output. Please try asking again.")
                    }
                    val jsonString = json.encodeToString(SovereignAiResponse.serializer(), finalResponse)
                    repository.saveMessage(role = "assistant", content = jsonString)
                }
            } else {
                val errorStr = responseResult.exceptionOrNull()?.message ?: "Unknown error"
                val errorMsg = SovereignAiResponse(
                    thinkingProcess = kotlinx.serialization.json.JsonPrimitive("Error analyzing request."),
                    currentArchetype = "",
                    archetypeRationale = "> ⚠️ **System Note**: Could not connect to AI service.\n\n$errorStr\n\nPlease check your internet connection or API key.",
                    uiBlocks = listOf(
                        UiBlock.ActionBannerBlock("Please try again in a moment.", "RETRY")
                    )
                )
                repository.saveMessage(role = "assistant", content = json.encodeToString(SovereignAiResponse.serializer(), errorMsg))
            }
            
            _isLoading.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
