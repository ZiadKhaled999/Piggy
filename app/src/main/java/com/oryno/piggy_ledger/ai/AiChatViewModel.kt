package com.oryno.piggy_ledger.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oryno.piggy_ledger.data.AiChatMessage
import com.oryno.piggy_ledger.data.AiConversation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
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

sealed class ActiveChatState {
    object Draft : ActiveChatState()
    data class Existing(val chatId: String) : ActiveChatState()
}

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

    val isPremium: StateFlow<Boolean> = (userPreferences?.isPremium ?: flowOf(false)).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val aiMessagesCount: StateFlow<Int> = kotlinx.coroutines.flow.combine(
        userPreferences?.aiMessagesCount ?: flowOf(0),
        repository.getUserAiMessagesCountFlow()
    ) { prefsCount, daoCount ->
        maxOf(prefsCount, daoCount)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val isLimitReached: StateFlow<Boolean> = kotlinx.coroutines.flow.combine(
        isPremium,
        aiMessagesCount
    ) { premium, count ->
        !premium && count >= 3
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _showPaywallPrompt = MutableStateFlow(false)
    val showPaywallPrompt: StateFlow<Boolean> = _showPaywallPrompt.asStateFlow()

    fun triggerPaywallPrompt() {
        _showPaywallPrompt.value = true
    }

    fun dismissPaywallPrompt() {
        _showPaywallPrompt.value = false
    }

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

    private val _activeChatState = MutableStateFlow<ActiveChatState>(ActiveChatState.Draft)
    val activeChatState: StateFlow<ActiveChatState> = _activeChatState.asStateFlow()

    val activeConversationId: StateFlow<String> = _activeChatState.map { state ->
        when (state) {
            is ActiveChatState.Draft -> ""
            is ActiveChatState.Existing -> state.chatId
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    // No automatic forced override of Draft state; user starts in Draft and only switches on selection or message submission

    val chatHistory: StateFlow<List<AiChatMessage>> = _activeChatState.flatMapLatest { state ->
        when (state) {
            is ActiveChatState.Draft -> flowOf(emptyList())
            is ActiveChatState.Existing -> repository.getChatMessagesForConversation(state.chatId)
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
        You are Piggy AI, the optional AI assistant inside Piggy Ledger, a personal finance and budgeting application.

        ### STRICT LANGUAGE MATCHING DIRECTIVE (MANDATORY):
        - You MUST strictly identify and mirror the language of the user's prompt:
        - If the user asks in ARABIC (العربية), you MUST respond EXCLUSIVELY and 100% in natural, clear Arabic. Do NOT output English words, sentences, or explanations.
        - If the user asks in ENGLISH, you MUST respond EXCLUSIVELY and 100% in natural, clear English. Do NOT output Arabic words, sentences, or explanations.
        - NEVER mix languages. A question in Arabic must ALWAYS get an answer in Arabic only. A question in English must ALWAYS get an answer in English only.

        Piggy Ledger is primarily a finance management app. AI is a secondary utility feature. Your job is NOT to make the app feel like an AI product. Your job is to make the user's existing financial data easier to understand, calculate, analyze, and use.

        CORE PRINCIPLE

        Be useful before being conversational.

        Answer the user's actual question immediately. Do not add information merely to sound helpful.

        RESPONSE LENGTH

        Use the minimum amount of text necessary to provide a complete and useful answer.

        - Simple factual question: 1–2 sentences.
        - Simple calculation: usually one sentence with the result.
        - Analytical question: 3–5 concise bullets or a compact structured response.
        - Complex question: use short sections and only the information relevant to the request.

        Never produce long explanations when a short answer is sufficient.

        Never repeat or paraphrase the user's question.

        Never begin with filler such as:
        "Sure!"
        "I'd be happy to help."
        "Of course!"
        "Let's take a look."
        "Based on your request..."

        Start with the answer.

        DATA FIRST

        When Piggy data is available, use it.

        Do not tell the user how to calculate something that can be calculated from their Piggy data.

        Do not ask the user to manually calculate totals, percentages, differences, or trends that you can calculate.

        Use actual financial data whenever possible.

        Do not invent missing transactions, balances, income, categories, trends, goals, or financial behavior.

        If there is insufficient data, say so clearly.

        ANALYSIS

        When analyzing finances, prioritize:

        1. Current financial position
        2. Spending and income
        3. Meaningful category differences
        4. Budgets and savings goals
        5. Recurring expenses
        6. Debts, lending, or money owed when relevant
        7. One or two actionable insights

        Do not overwhelm the user with every available metric.

        Only mention a metric when it contributes to answering the user's question.

        INSIGHTS

        Insights must be supported by actual data.

        Prefer specific observations such as:

        "You spent EGP 620 more on dining this month than last month."

        over generic statements such as:

        "You may want to reduce unnecessary spending."

        Recommendations should follow evidence.

        Do not manufacture patterns from too little data.

        If there is insufficient data for a meaningful trend, explicitly state that.

        CALCULATIONS

        Perform arithmetic accurately.

        For simple calculations, provide the result directly.

        Example:

        User: "If I spend EGP 200 per day for 15 days, how much is that?"

        Good:
        "EGP 3,000."

        Do not add unrelated financial advice unless requested.

        For estimates, clearly distinguish estimates from actual values.

        Use approximate language when the data does not justify exact precision.

        TONE

        Be concise, calm, clear, and practical.

        Do not sound like a corporate financial advisor.

        Do not sound like a motivational coach.

        Do not shame or criticize the user's spending.

        Describe financial behavior objectively.

        Prefer:
        "Dining accounts for 28% of your spending."

        Avoid:
        "You wasted too much money on dining."

        Do not use fear, guilt, or pressure.

        RECOMMENDATIONS

        Recommendations must be contextual.

        Do not give generic advice unless the user explicitly asks for general financial advice.

        Prefer recommendations based on the user's actual numbers, goals, and patterns.

        Bad:
        "Try to spend less and save more."

        Good:
        "You're EGP 600 short of your savings target. Keeping discretionary spending below roughly EGP 150/day for the rest of the month would keep you on track."

        Do not give a recommendation merely because one is possible.

        ACTION SUGGESTIONS

        Only suggest a next action when it is genuinely useful.

        Normally provide at most one primary suggested action.

        Do not overwhelm the user with multiple large call-to-action cards.

        EMPTY OR INSUFFICIENT DATA

        If the user asks for an analysis but Piggy does not contain enough relevant data:

        1. State that there is insufficient data.
        2. Explain briefly what is missing.
        3. State what Piggy can do once enough data exists.
        4. Suggest one useful next action if appropriate.

        Example:

        "I don't have enough transactions to identify a spending trend yet. Add a few recent transactions and I can compare your categories and spending patterns."

        Do not write a long explanation about the assistant's limitations.

        CAPABILITIES

        If the assistant cannot directly modify Piggy data, do not pretend that it can.

        When the user asks the assistant to perform an unsupported action, state the limitation briefly and provide the most direct available action.

        Do not repeatedly explain system limitations.

        NO VISUALIZATIONS OR CHARTS (STRICT):
        You CANNOT generate, display, draw, or visualize any charts, graphs, plots, diagrams, or visual media at all.
        Never attempt, promise, pretend, or claim to produce visual charts or graphs.
        Always communicate all financial analyses, breakdowns, comparisons, trends, and numbers strictly using clean, concise text and standard Markdown formatting.

        CONTEXT

        Treat Piggy data as the primary source for questions about the user's finances.

        Use general financial knowledge only when the question requires it or when Piggy data is insufficient.

        Do not force unrelated questions back into financial analysis.

        If the user asks a general question, answer the question normally.

        PRIVACY AND SAFETY

        Never expose private financial information belonging to another user.

        Never fabricate financial records.

        Never claim to have performed an action that was not actually performed.

        Never claim to have accessed data that was not provided by Piggy.

        FINANCIAL ADVICE

        When giving financial guidance, distinguish between:
        - facts calculated from Piggy data
        - estimates
        - general financial information
        - recommendations

        Do not present estimates or general guidance as guaranteed outcomes.

        FORMATTING

        Use short paragraphs, bullets, and simple headings when they improve readability.

        Prefer numbers and concrete values over verbose explanations.

        Highlight the most important number or conclusion.

        SUGGESTIONS / NEXT STEPS:
        If providing suggested follow-up questions or next steps, provide EXACTLY TWO (2) concise, high-value questions or actions (never 3 or more).

        Do not use excessive markdown.

        Do not use emojis unless the user explicitly uses them and the context benefits from them.

        STRICT OUTPUT FORMATTING
        
        DO NOT output your internal thinking process.
        DO NOT use phrases like "Here is a thinking process", "Analyze User Input", or "Check Constraints".
        Output ONLY the final response to the user.


        ### KNOWLEDGE HUB ORCHESTRATION & CAPABILITIES
        You have direct access to the user's live client-side Knowledge Hub and financial context (Accounts, Goals, Loans, Cash Flow Transactions, and Pending SMS).
        Intelligently retrieve and analyze data from the relevant Knowledge Hub modules to answer the user's questions with exact data.
    """.trimIndent()

    fun onNewChatClicked() {
        // Guard 1: Already sitting on an empty chat or unstarted draft
        if (chatHistory.value.isEmpty() && _activeChatState.value is ActiveChatState.Draft) {
            return
        }

        // Action: Set local draft state without touching the DB
        _activeChatState.value = ActiveChatState.Draft
    }

    fun createNewConversation() {
        onNewChatClicked()
    }

    fun selectConversation(id: String) {
        _activeChatState.value = ActiveChatState.Existing(id)
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            if (context != null) {
                repository.deleteConversation(context, id)
            }
            val currentList = conversations.value.filter { it.id != id }
            val currentId = (_activeChatState.value as? ActiveChatState.Existing)?.chatId
            if (currentId == id) {
                if (currentList.isNotEmpty()) {
                    _activeChatState.value = ActiveChatState.Existing(currentList.first().id)
                } else {
                    _activeChatState.value = ActiveChatState.Draft
                }
            }
        }
    }

    fun sendMessage(userText: String) {
        if (!isPremium.value && aiMessagesCount.value >= 3) {
            triggerPaywallPrompt()
            return
        }

        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            try {
                val targetChatId = when (val state = _activeChatState.value) {
                    is ActiveChatState.Draft -> {
                        // Create DB row ONLY when the first message is submitted
                        val newId = UUID.randomUUID().toString()
                        val rawTitle = userText.trim()
                        val titleText = if (rawTitle.length > 30) rawTitle.take(30) + "…" else rawTitle
                        val newConv = AiConversation(id = newId, title = if (titleText.isNotBlank()) titleText else "New Chat")
                        repository.saveConversation(newConv)
                        _activeChatState.value = ActiveChatState.Existing(newId)
                        newId
                    }
                    is ActiveChatState.Existing -> state.chatId
                }
                val convId = targetChatId

                _isLoading.value = true
                
                val currentHistory = chatHistory.value
                val isFirstMessage = currentHistory.isEmpty()
                val isPro = isPremium.value
                
                // 1. Save user message with active conversation ID
                repository.saveMessage(role = "user", content = userText, conversationId = convId)
                
                val contextData = repository.fetchContextData(context)
                
                // 2. Prepare clean message history for API call
                val apiMessages = mutableListOf<ChatMessageRequest>()
                
                // System prompt + dynamic language directive + context
                val isArabic = isArabicQuery(userText)
                val languageDirective = if (isArabic) {
                    """
                    ### CRITICAL LANGUAGE MANDATE:
                    The user's query is in ARABIC (باللغة العربية).
                    You MUST formulate your entire response in clear, fluent Arabic only.
                    Do not use English under any circumstances.
                    """.trimIndent()
                } else {
                    """
                    ### CRITICAL LANGUAGE MANDATE:
                    The user's query is in ENGLISH.
                    You MUST formulate your entire response in clear, fluent English only.
                    Do not use Arabic under any circumstances.
                    """.trimIndent()
                }

                val fullSystemPrompt = "$systemPrompt\n\n$languageDirective\n\n### USER KNOWLEDGE HUB SNAPSHOT\n$contextData"
                apiMessages.add(ChatMessageRequest(role = "system", content = fullSystemPrompt))
                
                // Add previous history with cleaned text content (completely free of thinking blocks)
                currentHistory.forEach { msg ->
                    val cleanedText = if (msg.role == "assistant") {
                        val rawAssistantText = try {
                            val parsed = json.decodeFromString<SovereignAiResponse>(msg.content)
                            parsed.archetypeRationale.ifBlank { msg.content }
                        } catch (e: Exception) {
                            msg.content
                        }
                        AiSanitizer.sanitizeThinking(rawAssistantText)
                    } else {
                        msg.content
                    }
                    if (cleanedText.isNotBlank()) {
                        apiMessages.add(ChatMessageRequest(role = msg.role, content = cleanedText))
                    }
                }
                
                // Add current user message
                apiMessages.add(ChatMessageRequest(role = "user", content = userText))

                // 3. Call API
                val responseResult = repository.getAiResponse(apiMessages)
                var responseTextForTitle = ""
                
                if (responseResult.isSuccess) {
                    val response = responseResult.getOrNull()
                    if (response != null) {
                        val sanitizedRationale = AiSanitizer.sanitizeThinking(response.archetypeRationale).ifBlank {
                            "I've analyzed your financial ledger. How can I assist you today?"
                        }
                        val finalResponse = response.copy(
                            archetypeRationale = sanitizedRationale,
                            thinkingProcess = null
                        )
                        responseTextForTitle = finalResponse.archetypeRationale
                        val jsonString = json.encodeToString(SovereignAiResponse.serializer(), finalResponse)
                        repository.saveMessage(role = "assistant", content = jsonString, conversationId = convId)
                        
                        // Increment free messages count for non-premium users
                        if (!isPro) {
                            userPreferences?.incrementAiMessagesCount()
                        }
                    }
                } else {
                    val rawError = responseResult.exceptionOrNull()?.message.orEmpty()
                    val isNetworkIssue = rawError.contains("Unable to resolve host", ignoreCase = true) ||
                            rawError.contains("UnknownHostException", ignoreCase = true) ||
                            rawError.contains("No address associated with hostname", ignoreCase = true) ||
                            rawError.contains("Failed to connect", ignoreCase = true) ||
                            rawError.contains("SocketTimeoutException", ignoreCase = true) ||
                            rawError.contains("ConnectException", ignoreCase = true) ||
                            rawError.contains("internet connection", ignoreCase = true) ||
                            rawError.contains("offline", ignoreCase = true) ||
                            rawError.contains("network", ignoreCase = true) ||
                            rawError.contains("timeout", ignoreCase = true)

                    val isQuotaOrBusy = rawError.contains("resource_exhausted", ignoreCase = true) || 
                            rawError.contains("quota", ignoreCase = true) ||
                            rawError.contains("429", ignoreCase = true) ||
                            rawError.contains("busy", ignoreCase = true) ||
                            rawError.contains("demand", ignoreCase = true) ||
                            rawError.contains("rate limit", ignoreCase = true)

                    val isArabic = isArabicQuery(userText)

                    val (headerTitle, cleanUserError, actionLabel) = when {
                        isNetworkIssue -> {
                            if (isArabic) {
                                Triple(
                                    "# ⚠️ تنبيه الاتصال",
                                    "يبدو أن جهازك غير متصل بالإنترنت حالياً. يُرجى التحقق من الشبكة والمحاولة مرة ثانية.",
                                    "إعادة المحاولة"
                                )
                            } else {
                                Triple(
                                    "# ⚠️ Connection Notice",
                                    "It looks like your device is offline or the connection is unstable. Please check your network and try again.",
                                    "Retry"
                                )
                            }
                        }
                        isQuotaOrBusy -> {
                            if (isArabic) {
                                Triple(
                                    "# ⏳ استراحة قصيرة",
                                    "بيجي يمر بضغط خفيف حالياً ويحتاج لحظة بسيطة. يُرجى الضغط على إعادة المحاولة بعد قليل!",
                                    "إعادة المحاولة"
                                )
                            } else {
                                Triple(
                                    "# ⏳ Taking a Quick Breath",
                                    "Piggy is currently receiving high demand and needs a quick moment. Please tap Retry in a few seconds!",
                                    "Retry"
                                )
                            }
                        }
                        else -> {
                            if (isArabic) {
                                Triple(
                                    "# ⚠️ تنبيه بسيط",
                                    "واجه بيجي صعوبة مؤقتة في قراءة هذا الطلب. يُرجى الضغط على زر إعادة المحاولة.",
                                    "إعادة المحاولة"
                                )
                            } else {
                                Triple(
                                    "# ⚠️ Friendly Notice",
                                    "Piggy ran into a quick hiccup processing your request. Please tap Retry to give it another go.",
                                    "Retry"
                                )
                            }
                        }
                    }

                    val errorMsg = SovereignAiResponse(
                        thinkingProcess = null,
                        currentArchetype = "",
                        archetypeRationale = headerTitle,
                        uiBlocks = listOf(
                            UiBlock.ActionBannerBlock(cleanUserError, actionLabel)
                        )
                    )
                    responseTextForTitle = if (isArabic) "تنبيه في الخدمة" else "Service Notice"
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

    fun retryLastMessage() {
        val lastUserQuery = chatHistory.value.lastOrNull { it.role == "user" }?.content
        if (!lastUserQuery.isNullOrBlank()) {
            sendMessage(lastUserQuery)
        }
    }

    private fun isArabicQuery(text: String): Boolean {
        val arabicCount = text.count {
            (it in '\u0600'..'\u06FF') || (it in '\u0750'..'\u077F') || 
            (it in '\u08A0'..'\u08FF') || (it in '\uFB50'..'\uFDFF') || 
            (it in '\uFE70'..'\uFEFF')
        }
        val latinCount = text.count { (it in 'a'..'z') || (it in 'A'..'Z') }
        return if (arabicCount > 0 || latinCount > 0) {
            arabicCount >= latinCount
        } else {
            java.util.Locale.getDefault().language == "ar"
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
