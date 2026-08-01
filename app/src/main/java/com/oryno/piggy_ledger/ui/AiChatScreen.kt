package com.oryno.piggy_ledger.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oryno.piggy_ledger.ai.AiChatViewModel
import com.oryno.piggy_ledger.ai.SovereignAiResponse
import com.oryno.piggy_ledger.ai.UiBlock
import com.oryno.piggy_ledger.data.AiChatMessage
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

// Minimalist Light Theme Colors
private val AiBackground = Color(0xFFFAFAFA)
private val AiSurface = Color(0xFFFFFFFF)
private val AiBorder = Color(0xFFEBEBEB)
private val AiAccent = Color(0xFF000000)
private val AiText = Color(0xFF111111)
private val AiDimText = Color(0xFF717171)
private val AiUserBubble = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel,
    onNavigateBack: () -> Unit
) {
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }

    var initialHistoryIds by remember { mutableStateOf<Set<Int>?>(null) }
    if (initialHistoryIds == null && chatHistory.isNotEmpty()) {
        initialHistoryIds = chatHistory.map { it.id }.toSet()
    }
    val animatedMessageIds = remember { mutableStateListOf<Int>() }

    // Position automatically to bottom on opening or new messages
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.scrollToItem(chatHistory.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Piggy AI", color = AiText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, letterSpacing = (-0.5).sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF6366F1).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "Beta",
                                color = Color(0xFF4F46E5),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AiText)
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = AiText)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(AiSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Clear Chat", color = AiText) },
                            onClick = {
                                viewModel.clearChat()
                                showMenu = false
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AiBackground,
                    scrolledContainerColor = AiBackground
                )
            )
        },
        containerColor = AiBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (chatHistory.isEmpty()) {
                EmptyChatState(
                    onSuggestionClick = {
                        viewModel.sendMessage(it)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    itemsIndexed(
                        items = chatHistory,
                        key = { _, msg -> msg.id }
                    ) { _, message ->
                        val isInitial = initialHistoryIds?.contains(message.id) == true
                        val isAnimated = animatedMessageIds.contains(message.id)
                        ChatMessageItem(
                            message = message,
                            shouldStream = !isInitial && !isAnimated,
                            onAnimationComplete = { animatedMessageIds.add(message.id) },
                            onCtaClick = { cta -> viewModel.sendMessage(cta) }
                        )
                    }
                    if (isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = AiAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    CircularProgressIndicator(
                                        color = AiAccent,
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 1.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analyzing Knowledge Hub...", color = AiDimText, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Input Area
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(AiBackground.copy(alpha = 0.95f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Message...", color = AiDimText, fontSize = 15.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(AiSurface),
                    shape = RoundedCornerShape(32.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = AiBorder,
                        unfocusedBorderColor = AiBorder,
                        focusedTextColor = AiText,
                        unfocusedTextColor = AiText,
                        cursorColor = AiAccent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    }),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (inputText.isNotBlank()) AiAccent else AiBorder)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (inputText.isNotBlank()) Color.White else AiDimText, modifier = Modifier.size(16.dp))
                        }
                    },
                    maxLines = 3,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp)
                )
            }
        }
    }
}

@Composable
fun EmptyChatState(onSuggestionClick: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Piggy AI Search", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Piggy AI uses client-side Knowledge Hub orchestration to securely analyze your local ledger accounts, spending history, savings goals, and SMS logs without sharing private keys.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { it / 4 },
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Sparkling Red/Coral Star Icon
            Surface(
                shape = CircleShape,
                color = Color(0xFFFF5252).copy(alpha = 0.12f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Piggy AI",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Find what Piggy AI knows",
                color = AiText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Mock "Ask a question" search bar trigger
            Surface(
                onClick = { onSuggestionClick("What is my current financial status?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFF3F4F6),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ask a question...",
                        color = AiDimText,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = AiDimText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Staggered Chip Rows (Row 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryPill(icon = "🎤", label = "go-to savings goals") {
                    onSuggestionClick("What are my current savings goals progress?")
                }
                CategoryPill(icon = "💡", label = "audit recent spending") {
                    onSuggestionClick("Audit my recent spending transactions")
                }
                CategoryPill(icon = "👑", label = "most successful runway") {
                    onSuggestionClick("What is my current runway and budget balance?")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Staggered Chip Rows (Row 2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryPill(icon = "💻", label = "expense shortcuts") {
                    onSuggestionClick("How can I optimize my monthly expenses?")
                }
                CategoryPill(icon = "📊", label = "cash flow forecast") {
                    onSuggestionClick("Show my cash flow forecast")
                }
                CategoryPill(icon = "💳", label = "loan repayment status") {
                    onSuggestionClick("What is my loan repayment status?")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Staggered Chip Rows (Row 3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryPill(icon = "📱", label = "review pending SMS logs") {
                    onSuggestionClick("Are there any pending SMS transactions to review?")
                }
                CategoryPill(icon = "💰", label = "account balances") {
                    onSuggestionClick("Show all my account balances")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer link
            TextButton(onClick = { showInfoDialog = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Learn how Piggy AI search works",
                        color = AiDimText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = AiDimText,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryPill(
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

data class ProcessedResponse(
    val mainText: String,
    val nextSteps: List<String>
)

fun parseResponseTextAndNextSteps(rawText: String): ProcessedResponse {
    var text = rawText
    
    if (text.contains("Knowledge Hub Analysis", ignoreCase = true)) {
        val lines = text.split("\n").filterNot { line ->
            val trimmed = line.trim()
            trimmed.contains("Knowledge Hub Analysis", ignoreCase = true) ||
                    (trimmed.startsWith(">") && (trimmed.contains("Analysis", ignoreCase = true) || trimmed.contains("Mapped request", ignoreCase = true)))
        }
        text = lines.joinToString("\n").trim()
    }

    val nextStepHeaderRegex = Regex("""(?i)(###?\s*(next\s*steps?|suggested\s*next\s*steps?|actionable\s*next\s*steps?|recommended\s*next\s*steps?)|(next\s*steps?|suggested\s*next\s*steps?|actionable\s*next\s*steps?):)""")
    
    val match = nextStepHeaderRegex.find(text)
    if (match != null) {
        val mainTextPart = text.substring(0, match.range.first).trim()
        val stepsPart = text.substring(match.range.last + 1).trim()
        
        val nextStepsList = mutableListOf<String>()
        val lines = stepsPart.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.startsWith("•") || trimmed.matches(Regex("""^\d+\..*"""))) {
                val cleanStep = trimmed.replaceFirst(Regex("""^([-*•]|\d+\.)\s*"""), "").replace("**", "").trim()
                if (cleanStep.isNotBlank()) {
                    nextStepsList.add(cleanStep)
                }
            } else if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                val cleanStep = trimmed.replace("**", "").trim()
                if (cleanStep.isNotBlank() && nextStepsList.size < 4) {
                    nextStepsList.add(cleanStep)
                }
            }
        }
        
        return ProcessedResponse(
            mainText = if (mainTextPart.isNotBlank()) mainTextPart else text,
            nextSteps = nextStepsList
        )
    }

    return ProcessedResponse(mainText = text, nextSteps = emptyList())
}

@Composable
fun ChatMessageItem(
    message: AiChatMessage,
    shouldStream: Boolean,
    onAnimationComplete: () -> Unit,
    onCtaClick: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(1500)
            isCopied = false
        }
    }

    if (message.role == "user") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            SelectionContainer {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp))
                        .background(AiUserBubble)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(message.content, color = AiText, fontSize = 15.sp, lineHeight = 22.sp)
                }
            }
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = AiAccent,
                modifier = Modifier.size(20.dp).padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                val json = remember { Json { ignoreUnknownKeys = true; isLenient = true; classDiscriminator = "type" } }
                val decodedResponse: SovereignAiResponse? = remember(message.content) {
                    try {
                        json.decodeFromString<SovereignAiResponse>(message.content)
                    } catch (e: Exception) {
                        null
                    }
                }

                val rawRationale = decodedResponse?.archetypeRationale ?: message.content
                val processedResponse = remember(rawRationale) {
                    parseResponseTextAndNextSteps(rawRationale)
                }
                val mainAnswerText = processedResponse.mainText
                val nextStepsList = processedResponse.nextSteps

                // Streaming typewriter transition only once on new creation
                var charCount by remember(message.id, mainAnswerText, shouldStream) {
                    mutableStateOf(if (shouldStream) 0 else mainAnswerText.length)
                }

                LaunchedEffect(message.id, shouldStream, mainAnswerText) {
                    if (shouldStream && charCount < mainAnswerText.length) {
                        while (charCount < mainAnswerText.length) {
                            delay(10)
                            charCount++
                        }
                        onAnimationComplete()
                    } else {
                        charCount = mainAnswerText.length
                    }
                }

                val displayedText = remember(mainAnswerText, charCount) {
                    mainAnswerText.take(charCount)
                }

                // Wrap text response in SelectionContainer for manual highlight & copy
                SelectionContainer {
                    Column {
                        if (displayedText.isNotBlank()) {
                            FormattedMarkdownText(displayedText)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Render UI Blocks
                        decodedResponse?.uiBlocks?.forEach { block ->
                            when (block) {
                                is UiBlock.KpiCardBlock -> KpiCard(block)
                                is UiBlock.StreakStatusBlock -> StreakStatus(block)
                                is UiBlock.MetricGridBlock -> MetricGrid(block)
                                is UiBlock.InteractiveChartBlock -> InteractiveChart(block)
                                is UiBlock.ReflectivePollBlock -> ReflectivePoll(block)
                                is UiBlock.LedgerItemBlock -> LedgerItem(block)
                                is UiBlock.ActionBannerBlock -> ActionBanner(block)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                // Action row with Copy button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val textToCopy = mainAnswerText.ifBlank { message.content }
                            clipboardManager.setText(AnnotatedString(textToCopy))
                            isCopied = true
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy response",
                            tint = if (isCopied) Color(0xFF10B981) else AiDimText,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Render Next Steps as CTA pill buttons outside response area
                if (nextStepsList.isNotEmpty() && charCount >= mainAnswerText.length) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        nextStepsList.forEach { stepText ->
                            Surface(
                                onClick = { onCtaClick(stepText) },
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF3F4F6),
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stepText,
                                        color = AiText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = AiDimText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormattedMarkdownText(text: String) {
    val blocks = remember(text) { parseMarkdownBlocks(text) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val fontSize = when (block.level) {
                        1 -> 21.sp
                        2 -> 19.sp
                        else -> 17.sp
                    }
                    Text(
                        text = parseAnnotatedString(block.text),
                        color = AiText,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("• ", color = AiAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = parseAnnotatedString(block.text),
                            color = AiText,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
                is MarkdownBlock.Callout -> {
                    StandardQuoteBlock(block.text)
                }
                is MarkdownBlock.HorizontalRule -> {
                    HorizontalDivider(
                        color = Color(0xFFE5E7EB),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
                is MarkdownBlock.Table -> {
                    RenderMarkdownTable(block.headers, block.rows)
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseAnnotatedString(block.text),
                        color = AiText,
                        fontSize = 15.sp,
                        lineHeight = 23.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StandardQuoteBlock(quoteText: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF3F4F6),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(24.dp)
                    .background(Color(0xFF6B7280), RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = parseAnnotatedString(quoteText),
                color = Color(0xFF374151),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun RenderMarkdownTable(headers: List<String>, rows: List<List<String>>) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3F4F6))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                headers.forEach { headerText ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = parseAnnotatedString(headerText),
                            color = AiText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
            
            // Data Rows
            rows.forEachIndexed { index, rowCells ->
                val bg = if (index % 2 == 0) Color.White else Color(0xFFFAFAFA)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bg)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    headers.indices.forEach { colIndex ->
                        val cellText = rowCells.getOrNull(colIndex) ?: ""
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = parseAnnotatedString(cellText),
                                color = AiText,
                                fontSize = 13.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
                if (index < rows.size - 1) {
                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                }
            }
        }
    }
}

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class BulletItem(val text: String) : MarkdownBlock()
    data class Callout(val text: String) : MarkdownBlock()
    data class HorizontalRule(val dummy: Unit = Unit) : MarkdownBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val lines = text.split("\n")
    val blocks = mutableListOf<MarkdownBlock>()
    var i = 0
    
    while (i < lines.size) {
        val rawLine = lines[i]
        val line = rawLine.trim()
        
        if (line.isEmpty()) {
            i++
            continue
        }
        
        // 1. Table check (starts with | or contains | with markdown table formatting)
        if (line.startsWith("|") && line.endsWith("|") && line.length >= 2) {
            val tableLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|") && lines[i].trim().length >= 2) {
                tableLines.add(lines[i].trim())
                i++
            }
            if (tableLines.isNotEmpty()) {
                val headerRowStr = tableLines[0]
                val headerContent = if (headerRowStr.length >= 2) headerRowStr.substring(1, headerRowStr.length - 1) else ""
                val headers = headerContent
                    .split("|")
                    .map { it.trim() }
                
                val bodyRows = mutableListOf<List<String>>()
                for (rowIndex in 1 until tableLines.size) {
                    val rowStr = tableLines[rowIndex]
                    // Skip separator row like |---|---|
                    val contentWithoutPipes = rowStr.replace("|", "").replace("-", "").replace(":", "").trim()
                    if (contentWithoutPipes.isEmpty()) {
                        continue
                    }
                    val rowContent = if (rowStr.length >= 2) rowStr.substring(1, rowStr.length - 1) else ""
                    val cleanCells = rowContent
                        .split("|")
                        .map { it.trim() }
                    bodyRows.add(cleanCells)
                }
                blocks.add(MarkdownBlock.Table(headers, bodyRows))
            }
            continue
        }
        
        // 2. Horizontal Rule check
        if (line.all { it == '-' || it == '*' || it == '_' } && line.length >= 3) {
            blocks.add(MarkdownBlock.HorizontalRule())
            i++
            continue
        }
        
        // 3. Callout / Blockquote
        if (line.startsWith(">")) {
            val calloutLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                calloutLines.add(lines[i].trim().removePrefix(">").trim())
                i++
            }
            blocks.add(MarkdownBlock.Callout(calloutLines.joinToString("\n")))
            continue
        }
        
        // 4. Headers
        if (line.startsWith("#")) {
            val level = line.takeWhile { it == '#' }.length
            val title = line.dropWhile { it == '#' }.trim()
            blocks.add(MarkdownBlock.Header(level, title))
            i++
            continue
        }
        
        // 5. Bullet items
        if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("• ")) {
            val bulletText = line.substring(2).trim()
            blocks.add(MarkdownBlock.BulletItem(bulletText))
            i++
            continue
        }
        
        if (line.matches(Regex("""^\d+\.\s+.*"""))) {
            val text = line.replaceFirst(Regex("""^\d+\.\s+"""), "").trim()
            blocks.add(MarkdownBlock.BulletItem(text))
            i++
            continue
        }
        
        // 6. Regular Paragraph
        blocks.add(MarkdownBlock.Paragraph(line))
        i++
    }
    
    return blocks
}

private fun parseAnnotatedString(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val parts = text.split("**")
        for (i in parts.indices) {
            if (i % 2 == 1) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(parts[i])
                }
            } else {
                append(parts[i])
            }
        }
    }
}

@Composable
fun KpiCard(block: UiBlock.KpiCardBlock) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AiSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AiBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(block.title, color = AiDimText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(block.primaryValue, color = AiText, fontSize = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(block.subValue, color = AiDimText, fontSize = 14.sp)
        }
    }
}

@Composable
fun StreakStatus(block: UiBlock.StreakStatusBlock) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AiSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AiBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Discipline Streak", color = AiDimText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${block.currentStreak}", color = AiText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
                    Text(" days", color = AiDimText, fontSize = 15.sp, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(block.statusMessage, color = AiText, fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(block.streakFreezesAvailable) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(AiBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AiDimText))
                    }
                }
            }
        }
    }
}

@Composable
fun MetricGrid(block: UiBlock.MetricGridBlock) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AiSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AiBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(block.title, color = AiText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(16.dp))
            val chunked = block.metrics.chunked(2)
            chunked.forEachIndexed { index, rowMetrics ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowMetrics.forEach { metric ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(AiBackground, RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text(metric.label, color = AiDimText, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(metric.value, color = AiText, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                if (index < chunked.size - 1) Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun InteractiveChart(block: UiBlock.InteractiveChartBlock) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AiSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AiBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(block.title, color = AiText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(24.dp))
            
            // Minimalist Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val maxVal = block.dataPoints.maxOfOrNull { it.value } ?: 1.0
                block.dataPoints.forEach { point ->
                    val heightRatio = (point.value / maxVal).toFloat()
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .fillMaxHeight(heightRatio)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(AiText)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(point.label.take(3), color = AiDimText, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ReflectivePoll(block: UiBlock.ReflectivePollBlock) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AiSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AiBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(block.promptMessage, color = AiText, fontSize = 15.sp, lineHeight = 22.sp)
            Spacer(modifier = Modifier.height(20.dp))
            block.options.forEach { option ->
                Button(
                    onClick = { /* TODO */ },
                    colors = ButtonDefaults.buttonColors(containerColor = AiBackground, contentColor = AiText),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AiBorder),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                ) {
                    Text(option, fontWeight = FontWeight.Normal, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun LedgerItem(block: UiBlock.LedgerItemBlock) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AiSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AiBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(block.merchant, color = AiText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(block.flagReason, color = AiDimText, fontSize = 13.sp, lineHeight = 18.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(block.amount, color = AiText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(block.impactOnRunway, color = AiDimText, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ActionBanner(block: UiBlock.ActionBannerBlock) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AiText),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { /* TODO Action */ }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                block.message,
                color = AiSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, 
                contentDescription = "Action", 
                tint = AiSurface, 
                modifier = Modifier.scale(scaleX = -1f, scaleY = 1f).size(20.dp)
            )
        }
    }
}
