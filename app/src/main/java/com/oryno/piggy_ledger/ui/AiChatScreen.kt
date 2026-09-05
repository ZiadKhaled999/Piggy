package com.oryno.piggy_ledger.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import com.oryno.piggy_ledger.ai.NativeTtsManager
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.mikepenz.markdown.m3.Markdown
import com.oryno.piggy_ledger.ai.ActiveChatState
import com.mikepenz.markdown.m3.markdownColor
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ai.AiChatViewModel
import com.oryno.piggy_ledger.ai.SovereignAiResponse
import com.oryno.piggy_ledger.ai.UiBlock
import com.oryno.piggy_ledger.data.AiChatMessage
import com.oryno.piggy_ledger.data.AiConversation
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

// Light Theme Gemini-inspired Aesthetic Colors with Piggy Ledger Pink Accent
private val AiLightBackground = Color(0xFFFFFFFF)
private val AiLightSurface = Color(0xFFF9FAFB)
private val AiLightPillBackground = Color(0xFFDADADA)
private val AiLightBorder = Color(0xFFE2E8F0)
private val AiUserBubbleColor = Color(0xFFFCE7F3) // Light theme pink background for user messages
private val AiUserBubbleTextColor = Color(0xFF1E293B) // Dark text for light pink bubble
private val AiTextPrimary = Color(0xFF1E293B)
private val AiTextSecondary = Color(0xFF64748B)
private val AiPiggyPink = Color(0xFFF43F5E) // Piggy Ledger primary pink
private val AiPiggyPinkDark = Color(0xFFDB2777)
private val AiGeminiBlue = Color(0xFF1A73E8)
private val AiGeminiPurple = Color(0xFF8E51FF)
private val AiPinkAccent = Color(0xFFEC4899)
private val AiPinkGlowStart = Color(0xFFFDF2F8)
private val AiPinkGlowMid = Color(0xFFFCE7F3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit = {}
) {
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeChatState by viewModel.activeChatState.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val aiMessagesCount by viewModel.aiMessagesCount.collectAsStateWithLifecycle()
    val isLimitReached by viewModel.isLimitReached.collectAsStateWithLifecycle()
    val showPaywallPrompt by viewModel.showPaywallPrompt.collectAsStateWithLifecycle()
    
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    var showHistorySheet by remember { mutableStateOf(false) }
    val historySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var showOverflowMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var speechLang by remember { mutableStateOf(if (java.util.Locale.getDefault().language == "ar") "ar-EG" else "en-US") }

    // Native Android TextToSpeech manager
    val ttsManager = remember { NativeTtsManager(context) }
    val speakingMessageId by ttsManager.speakingMessageId.collectAsStateWithLifecycle()
    val isTtsPaused by ttsManager.isPaused.collectAsStateWithLifecycle()
    val ttsElapsedSeconds by ttsManager.elapsedSeconds.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            ttsManager.shutdown()
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = results?.get(0)
            if (!recognizedText.isNullOrBlank()) {
                inputText = (inputText + " " + recognizedText).trim()
            }
        }
    }

    val initialHistoryIds = remember(activeConversationId, chatHistory.isNotEmpty()) {
        if (chatHistory.isNotEmpty()) chatHistory.map { it.id }.toSet() else null
    }
    
    val animatedMessageIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(showPaywallPrompt) {
        if (showPaywallPrompt) {
            onNavigateToPaywall()
            viewModel.dismissPaywallPrompt()
        }
    }

    // Position automatically to bottom on opening or new messages
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    val isConversationActive = chatHistory.isNotEmpty() || isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isPremium) {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.ai_pro_unlimited),
                                    color = Color(0xFF334155),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.1.sp
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = if (aiMessagesCount >= 3) Color(0xFFFEE2E2) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (aiMessagesCount >= 3) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable { onNavigateToPaywall() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (aiMessagesCount >= 3) stringResource(R.string.ai_free_upgrade) else stringResource(R.string.ai_free_messages, aiMessagesCount),
                                    color = if (aiMessagesCount >= 3) Color(0xFFDC2626) else Color(0xFF475569),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    // Two-line hamburger button inside light pill container matching light theme aesthetic
                    Box(modifier = Modifier.padding(start = 12.dp)) {
                        Surface(
                            onClick = { showHistorySheet = true },
                            shape = CircleShape,
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.5.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(18.dp)
                                            .height(2.5.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(AiTextPrimary)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(18.dp)
                                            .height(2.5.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(AiTextPrimary)
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    // Right-side combined light capsule/pill holding Edit & More options
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                // Compose / New Chat Icon
                                IconButton(
                                    onClick = { viewModel.createNewConversation() },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = stringResource(R.string.ai_new_chat),
                                        tint = AiTextPrimary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                // 3 Vertical Dots (More Options)
                                IconButton(
                                    onClick = { showOverflowMenu = true },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = stringResource(R.string.ai_more_options),
                                        tint = AiTextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AiLightBackground,
                    titleContentColor = AiTextPrimary,
                    navigationIconContentColor = AiTextPrimary
                )
            )
        },
        containerColor = AiLightBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // Intense Atmospheric Pink Glow rising from the bottom of the screen (visible only before active conversation)
            AnimatedVisibility(
                visible = !isConversationActive,
                enter = fadeIn(tween(400)),
                exit = fadeOut(tween(400)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFFDF2F8).copy(alpha = 0.20f),
                                    Color(0xFFFCE7F3).copy(alpha = 0.65f),
                                    Color(0xFFFBCFE8).copy(alpha = 0.95f),
                                    Color(0xFFF9A8D4).copy(alpha = 1.0f)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                if (chatHistory.isEmpty() && !isLoading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        EmptyChatState(
                            userName = userName,
                            isLimitReached = isLimitReached,
                            onSuggestionClick = { query ->
                                if (!isLimitReached) {
                                    viewModel.sendMessage(query)
                                }
                            }
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
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
                                isLimitReached = isLimitReached,
                                onAnimationComplete = { 
                                    animatedMessageIds.add(message.id)
                                    coroutineScope.launch {
                                        if (chatHistory.isNotEmpty()) {
                                            listState.animateScrollToItem(chatHistory.size - 1)
                                        }
                                    }
                                },
                                onCtaClick = { cta -> 
                                    if (!isLimitReached) {
                                        if (cta == "RETRY_LAST") {
                                            viewModel.retryLastMessage()
                                        } else {
                                            viewModel.sendMessage(cta)
                                        }
                                    }
                                },
                                onTtsClick = { msgId, speechText -> ttsManager.speak(msgId, speechText) },
                                isSpeaking = speakingMessageId == message.id,
                                onNavigateToPaywall = onNavigateToPaywall
                            )
                        }
                        if (isLoading) {
                            item(key = "thinking_indicator") {
                                ThinkingIndicator()
                            }
                        }
                    }
                }

                // Floating Prompt Pill with Animated Dynamic Width
                // First time / empty conversation: compact pill (88% width)
                // Active conversation: expands to full width (100%)
                val targetPillWidthFraction = if (isConversationActive) 1f else 0.88f
                val animatedWidthFraction by animateFloatAsState(
                    targetValue = targetPillWidthFraction,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "prompt_pill_width"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    if (speakingMessageId != null) {
                        LiveActionTtsPlayerBar(
                            elapsedSeconds = ttsElapsedSeconds,
                            isPaused = isTtsPaused,
                            onTogglePlayPause = { ttsManager.togglePauseResume() },
                            onRestart = { ttsManager.restart() },
                            onCancel = { ttsManager.stop() },
                            modifier = Modifier.fillMaxWidth(animatedWidthFraction)
                        )
                    } else if (isLimitReached) {
                        Surface(
                            onClick = onNavigateToPaywall,
                            shape = RoundedCornerShape(36.dp),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth(animatedWidthFraction)
                                .height(56.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFFF43F5E),
                                                Color(0xFFE11D48),
                                                Color(0xFFDB2777)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.ai_upgrade_to_pro),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.2).sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = Color(0xFFDADADA),
                            shape = RoundedCornerShape(36.dp),
                            border = BorderStroke(1.dp, Color(0xFFCACACA)),
                            shadowElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth(animatedWidthFraction)
                        ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Text Input Field
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (inputText.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.ai_ask_piggy),
                                        color = Color(0xFF555555),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                                BasicTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    textStyle = TextStyle(
                                        color = Color(0xFF1E293B),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Normal,
                                        lineHeight = 22.sp
                                    ),
                                    maxLines = 5,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            if (inputText.isNotBlank() && !isLoading) {
                                                viewModel.sendMessage(inputText)
                                                inputText = ""
                                            }
                                        }
                                    ),
                                    cursorBrush = SolidColor(Color(0xFF1E293B)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            val isInputEmpty = inputText.isBlank()

                            // If loading, show Stop button
                            if (isLoading) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF202124))
                                        .clickable { viewModel.stopGeneration() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = stringResource(R.string.ai_stop_generation),
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                // Right actions when not generating
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isInputEmpty) {
                                        // Language toggle AR / EN for voice
                                        Text(
                                            text = if (speechLang == "ar-EG") "AR" else "EN",
                                            color = Color(0xFF333333),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    speechLang = if (speechLang == "ar-EG") "en-US" else "ar-EG"
                                                }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        )

                                        // Mic Icon
                                        IconButton(
                                            onClick = {
                                                try {
                                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLang)
                                                        putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayOf("ar-EG", "en-US", "ar-SA"))
                                                    }
                                                    speechRecognizerLauncher.launch(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, context.getString(R.string.speech_not_supported), Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = stringResource(R.string.ai_voice_input),
                                                tint = Color(0xFF2B2B2B),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    } else {
                                        // Send button
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2563EB))
                                                .clickable {
                                                    if (inputText.isNotBlank() && !isLoading) {
                                                        viewModel.sendMessage(inputText)
                                                        inputText = ""
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Send,
                                                contentDescription = stringResource(R.string.ai_send),
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .rotate(-45f)
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
        }
    }

    // Centered Menu for Top Bar 3-dots
    if (showOverflowMenu) {
        CenteredMenuDialog(
            onDismissRequest = { showOverflowMenu = false },
            title = stringResource(R.string.ai_chat_options),
            items = listOf(
                CenteredMenuItem(
                    title = stringResource(R.string.ai_chat_history),
                    onClick = {
                        showOverflowMenu = false
                        showHistorySheet = true
                    }
                ),
                CenteredMenuItem(
                    title = stringResource(R.string.ai_clear_current_chat),
                    isDestructive = true,
                    onClick = {
                        showOverflowMenu = false
                        viewModel.clearChat()
                    }
                )
            )
        )
    }

    val paywallSheetState = rememberModalBottomSheetState()

    // ModalBottomSheet for Chat History Menu
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = historySheetState,
            containerColor = Color(0xFFFAFAFC),
            contentColor = AiTextPrimary,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = Color(0xFFCBD5E1),
                    width = 40.dp,
                    height = 4.dp
                )
            }
        ) {
            ChatHistorySheetContent(
                conversations = conversations,
                activeChatState = activeChatState,
                activeConversationId = activeConversationId,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onSelectConversation = { id ->
                    viewModel.selectConversation(id)
                    coroutineScope.launch {
                        historySheetState.hide()
                        showHistorySheet = false
                    }
                },
                onNewChatClick = {
                    viewModel.createNewConversation()
                    coroutineScope.launch {
                        historySheetState.hide()
                        showHistorySheet = false
                    }
                },
                onDeleteConversation = { id ->
                    viewModel.deleteConversation(id)
                },
                onDeleteAllConversations = {
                    viewModel.clearAllConversations()
                },
                onTogglePinConversation = { id, pinned ->
                    viewModel.togglePinConversation(id, pinned)
                },
                onRenameConversation = { id, title ->
                    viewModel.renameConversation(id, title)
                },
                onCloseSheet = {
                    coroutineScope.launch {
                        historySheetState.hide()
                        showHistorySheet = false
                    }
                }
            )
        }
    }
}

/**
 * Custom Gemini 4-Sparkle Cluster Icon matching the uploaded design
 */
@Composable
fun GeminiSparkleIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF1E293B),
    isPulsing: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle_anim")
    val alpha by if (isPulsing) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val scale by if (isPulsing) {
        infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Canvas(
        modifier = modifier
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
    ) {
        val w = size.width
        val h = size.height
        val cx = w * 0.45f
        val cy = h * 0.5f
        val mainRadius = minOf(w, h) * 0.38f

        // Central 4-pointed star
        val path = Path().apply {
            moveTo(cx, cy - mainRadius)
            quadraticTo(cx, cy, cx + mainRadius, cy)
            quadraticTo(cx, cy, cx, cy + mainRadius)
            quadraticTo(cx, cy, cx - mainRadius, cy)
            quadraticTo(cx, cy, cx, cy - mainRadius)
            close()
        }
        drawPath(path, color = color)

        // Upper satellite dot
        drawCircle(
            color = color,
            radius = mainRadius * 0.22f,
            center = Offset(cx + mainRadius * 0.9f, cy - mainRadius * 0.7f)
        )
        // Lower satellite dot
        drawCircle(
            color = color,
            radius = mainRadius * 0.16f,
            center = Offset(cx + mainRadius * 0.8f, cy + mainRadius * 0.8f)
        )
    }
}

data class ConversationGroup(
    val title: String,
    val items: List<AiConversation>
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatHistorySheetContent(
    conversations: List<AiConversation>,
    activeChatState: ActiveChatState = ActiveChatState.Draft,
    activeConversationId: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectConversation: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onDeleteAllConversations: () -> Unit,
    onTogglePinConversation: (String, Boolean) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onCloseSheet: () -> Unit
) {
    val context = LocalContext.current
    var conversationToRename by remember { mutableStateOf<AiConversation?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var selectedConvForMenu by remember { mutableStateOf<AiConversation?>(null) }
    var showSheetSettingsMenu by remember { mutableStateOf(false) }
    var showConfirmClearAllDialog by remember { mutableStateOf(false) }

    val conversationGroups = remember(conversations, searchQuery) {
        val filtered = if (searchQuery.isBlank()) conversations
        else conversations.filter { it.title.contains(searchQuery, ignoreCase = true) }

        val pinned = filtered.filter { it.isPinned }
        val unpinned = filtered.filter { !it.isPinned }

        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val sevenDaysMs = 7 * oneDayMs
        val thirtyDaysMs = 30 * oneDayMs

        val todayItems = mutableListOf<AiConversation>()
        val sevenDaysItems = mutableListOf<AiConversation>()
        val thirtyDaysItems = mutableListOf<AiConversation>()
        val olderItems = mutableListOf<AiConversation>()

        unpinned.forEach { conv ->
            val diff = now - conv.updatedAt
            when {
                diff < oneDayMs -> todayItems.add(conv)
                diff < sevenDaysMs -> sevenDaysItems.add(conv)
                diff < thirtyDaysMs -> thirtyDaysItems.add(conv)
                else -> olderItems.add(conv)
            }
        }

        val groups = mutableListOf<ConversationGroup>()
        if (pinned.isNotEmpty()) groups.add(ConversationGroup(context.getString(R.string.ai_history_pinned), pinned))
        if (todayItems.isNotEmpty()) groups.add(ConversationGroup(context.getString(R.string.ai_history_today), todayItems))
        if (sevenDaysItems.isNotEmpty()) groups.add(ConversationGroup(context.getString(R.string.ai_history_7_days), sevenDaysItems))
        if (thirtyDaysItems.isNotEmpty()) groups.add(ConversationGroup(context.getString(R.string.ai_history_30_days), thirtyDaysItems))
        if (olderItems.isNotEmpty()) groups.add(ConversationGroup(context.getString(R.string.ai_history_older), olderItems))

        groups
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .background(Color(0xFFFAFAFC))
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Sheet Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.ai_chat_history),
                    color = AiTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 3 Vertical Dots for BottomSheet Settings
                Surface(
                    onClick = { showSheetSettingsMenu = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF1F6),
                    border = BorderStroke(1.dp, Color(0xFFFCE7F3))
                ) {
                    Box(
                        modifier = Modifier.padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.ai_history_settings),
                            tint = Color(0xFFDB2777),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search bar
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.ai_search_conversations),
                    tint = AiTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.ai_search_conversations), color = Color(0xFF94A3B8), fontSize = 14.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = AiTextPrimary,
                        unfocusedTextColor = AiTextPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color(0xFFE2E8F0))
        Spacer(modifier = Modifier.height(8.dp))

        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.ai_no_previous_chats), color = Color(0xFF94A3B8), fontSize = 14.sp)
                }
            }
        } else if (conversationGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.ai_no_matching_conversations), color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                conversationGroups.forEach { group ->
                    item(key = "header_${group.title}") {
                        Text(
                            text = group.title,
                            color = AiTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                        )
                    }
                    items(group.items, key = { it.id }) { conv ->
                        val isSelected = activeChatState is ActiveChatState.Existing && activeChatState.chatId == conv.id

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFFFFF1F6) else Color.White,
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFFFBCFE8) else Color(0xFFF1F5F9)),
                                shadowElevation = if (isSelected) 0.dp else 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .combinedClickable(
                                        onClick = { onSelectConversation(conv.id) },
                                        onLongClick = { selectedConvForMenu = conv }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFFDB2777) else AiTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    if (conv.isPinned) {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "Pinned",
                                            tint = Color(0xFFE11D48),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = conv.title,
                                        color = if (isSelected) Color(0xFFDB2777) else AiTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { selectedConvForMenu = conv },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = AiTextSecondary,
                                            modifier = Modifier.size(16.dp)
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

    // Centered Menu for Conversation Item 3-dots
    if (selectedConvForMenu != null) {
        val targetConv = selectedConvForMenu!!
        CenteredMenuDialog(
            onDismissRequest = { selectedConvForMenu = null },
            title = targetConv.title,
            items = listOf(
                CenteredMenuItem(
                    title = stringResource(R.string.ai_rename),
                    onClick = {
                        val conv = targetConv
                        selectedConvForMenu = null
                        renameInputText = conv.title
                        conversationToRename = conv
                    }
                ),
                CenteredMenuItem(
                    title = if (targetConv.isPinned) stringResource(R.string.ai_unpin) else stringResource(R.string.ai_pin),
                    onClick = {
                        val conv = targetConv
                        selectedConvForMenu = null
                        onTogglePinConversation(conv.id, conv.isPinned)
                    }
                ),
                CenteredMenuItem(
                    title = stringResource(R.string.ai_delete_conversation),
                    isDestructive = true,
                    onClick = {
                        val conv = targetConv
                        selectedConvForMenu = null
                        onDeleteConversation(conv.id)
                    }
                )
            )
        )
    }

    // Centered Menu for BottomSheet Settings 3-dots
    if (showSheetSettingsMenu) {
        CenteredMenuDialog(
            onDismissRequest = { showSheetSettingsMenu = false },
            title = stringResource(R.string.ai_history_settings),
            statCount = conversations.size,
            statLabel = if (conversations.size == 1) stringResource(R.string.ai_conversation_saved) else stringResource(R.string.ai_conversations_saved),
            items = listOf(
                CenteredMenuItem(
                    title = stringResource(R.string.ai_new_conversation),
                    onClick = {
                        showSheetSettingsMenu = false
                        onNewChatClick()
                    }
                ),
                CenteredMenuItem(
                    title = stringResource(R.string.ai_clear_all_history),
                    isDestructive = true,
                    onClick = {
                        showSheetSettingsMenu = false
                        showConfirmClearAllDialog = true
                    }
                ),
                CenteredMenuItem(
                    title = stringResource(R.string.ai_close_history),
                    onClick = {
                        showSheetSettingsMenu = false
                        onCloseSheet()
                    }
                )
            )
        )
    }

    // Clear All History Confirmation Dialog
    if (showConfirmClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearAllDialog = false },
            title = { Text(stringResource(R.string.ai_clear_all_confirm_title), fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)) },
            text = { Text(stringResource(R.string.ai_clear_all_confirm_desc), color = Color(0xFF64748B), fontSize = 14.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmClearAllDialog = false
                        onDeleteAllConversations()
                    }
                ) {
                    Text(stringResource(R.string.ai_delete_all), fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearAllDialog = false }) {
                    Text(stringResource(R.string.cancel_btn), color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (conversationToRename != null) {
        val renameSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { conversationToRename = null },
            sheetState = renameSheetState,
            containerColor = Color.White,
            contentColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFE2E8F0)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp, top = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.ai_rename_conversation),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.ai_conversation_title)) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFDB2777),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedLabelColor = Color(0xFFDB2777)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { conversationToRename = null },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(stringResource(R.string.cancel_btn), color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            val target = conversationToRename
                            if (target != null && renameInputText.isNotBlank()) {
                                onRenameConversation(target.id, renameInputText.trim())
                            }
                            conversationToRename = null
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB2777)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(stringResource(R.string.save), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

data class CenteredMenuItem(
    val title: String,
    val isSelected: Boolean = false,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun CenteredMenuDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    statCount: Int? = null,
    statLabel: String? = null,
    items: List<CenteredMenuItem>
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.88f)) // Transparent frosted light background
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
        ) {
            // Absolute Top Title in H3 style
            if (title != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = 54.dp, start = 24.dp, end = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = Color(0xFF1E293B),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.4).sp
                    )
                }
            }

            // Soft Piggy Pink Ambient Glow in the center
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(360.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFCE7F3).copy(alpha = 0.80f),
                                Color(0xFFFFF1F6).copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Centered Vertically Stacked Content (No white cards, pure typography)
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(top = 64.dp, bottom = 84.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // "X conversations saved" Big Pink Stat Display
                if (statCount != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 32.dp)
                    ) {
                        Text(
                            text = "$statCount",
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFDB2777),
                            lineHeight = 56.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statLabel ?: if (statCount == 1) stringResource(R.string.ai_conversation_saved) else stringResource(R.string.ai_conversations_saved),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.4.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Vertical list of text elements displayed below each other with small nice hr dividers
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEachIndexed { index, item ->
                        val isHighlighted = item.isSelected
                        val textColor = when {
                            item.isDestructive -> Color(0xFFE11D48)
                            isHighlighted -> Color(0xFFDB2777)
                            else -> Color(0xFF1E293B)
                        }
                        val fontSize = if (isHighlighted) 22.sp else 19.sp
                        val fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.SemiBold

                        Box(
                            modifier = Modifier
                                .wrapContentWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(onClick = item.onClick)
                                .padding(vertical = 10.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.title,
                                color = textColor,
                                fontSize = fontSize,
                                fontWeight = fontWeight,
                                textAlign = TextAlign.Center,
                                letterSpacing = if (isHighlighted) (-0.3).sp else (-0.1).sp
                            )
                        }

                        // Small nice hr between elements (slightly longer than text, not full width)
                        if (index < items.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(1.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color(0xFFDB2777).copy(alpha = 0.25f),
                                                Color(0xFFDB2777).copy(alpha = 0.25f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            // Bottom Floating White Circular Close "X" Button (matching screenshot)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 44.dp)
            ) {
                Surface(
                    onClick = onDismissRequest,
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 10.dp,
                    border = BorderStroke(1.5.dp, Color(0xFFFCE7F3)),
                    modifier = Modifier.size(58.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close_btn),
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

sealed interface LoadingPattern {
    data class TwoStep(@StringRes val step1: Int, @StringRes val step2: Int) : LoadingPattern
    data class SinglePulse(@StringRes val phrase: Int) : LoadingPattern
}

private val LOADING_PATTERNS = listOf(
    // 2-Step Rapid Cycles (Swap copy at 1.5 seconds)
    LoadingPattern.TwoStep(R.string.ai_thinking_analyzing, R.string.ai_thinking_ready),
    LoadingPattern.TwoStep(R.string.ai_thinking_gathering, R.string.ai_thinking_finishing),
    LoadingPattern.TwoStep(R.string.ai_thinking_on_it, R.string.ai_thinking_coming),
    LoadingPattern.TwoStep(R.string.ai_thinking_connecting, R.string.ai_thinking_polishing),
    LoadingPattern.TwoStep(R.string.ai_thinking_reading, R.string.ai_thinking_second),

    // Single-Pulse Statuses (Static 3-second display)
    // Professional & Crisp
    LoadingPattern.SinglePulse(R.string.ai_thinking_processing),
    LoadingPattern.SinglePulse(R.string.ai_thinking_generating),
    LoadingPattern.SinglePulse(R.string.ai_thinking_synthesizing),
    // Casual & Friendly
    LoadingPattern.SinglePulse(R.string.ai_thinking_cooking),
    LoadingPattern.SinglePulse(R.string.ai_thinking_sorting),
    LoadingPattern.SinglePulse(R.string.ai_thinking_working),
    // Action-Focused
    LoadingPattern.SinglePulse(R.string.ai_thinking_fetching),
    LoadingPattern.SinglePulse(R.string.ai_thinking_building),
    LoadingPattern.SinglePulse(R.string.ai_thinking_together)
)

/**
 * Thinking / Status Indicator with punchy 2-to-4 word loading copy:
 * Supports 2-Step Rapid Cycles (swap at 1.5s) and Single-Pulse Statuses
 */
@Composable
fun ThinkingIndicator() {
    val pattern = remember { LOADING_PATTERNS.random() }
    val step1 = when (pattern) {
        is LoadingPattern.TwoStep -> stringResource(pattern.step1)
        is LoadingPattern.SinglePulse -> stringResource(pattern.phrase)
    }
    val step2 = if (pattern is LoadingPattern.TwoStep) stringResource(pattern.step2) else ""

    var currentText by remember { mutableStateOf(step1) }

    LaunchedEffect(pattern) {
        if (pattern is LoadingPattern.TwoStep) {
            delay(1500L) // Single swap limit: change text at 1.5s mark
            currentText = step2
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GeminiSparkleIcon(
            modifier = Modifier.size(24.dp),
            color = Color(0xFFDB2777),
            isPulsing = true
        )
        Spacer(modifier = Modifier.width(12.dp))
        AnimatedContent(
            targetState = currentText,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + slideInVertically(animationSpec = tween(220)) { it / 3 })
                    .togetherWith(fadeOut(animationSpec = tween(180)) + slideOutVertically(animationSpec = tween(180)) { -it / 3 })
            },
            label = "ThinkingIndicatorTextAnimation"
        ) { text ->
            Text(
                text = text,
                color = AiTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.2).sp
            )
        }
    }
}

data class CategoryPillData(
    val icon: String,
    @StringRes val label: Int,
    @StringRes val query: Int
)

@Composable
fun EmptyChatState(
    userName: String = "",
    isLimitReached: Boolean = false,
    onSuggestionClick: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    // We need to fetch strings inside the composable, not remember block for resources
    val dayOfYear = remember { java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR) }
    val templates = listOf(
        Pair(R.string.ai_phrase_mic_yours, ""),
        Pair(R.string.ai_phrase_up_to_you, ""),
        Pair(R.string.ai_phrase_grow_wealth, "?"),
        Pair(R.string.ai_phrase_master_numbers, "?"),
        Pair(R.string.ai_phrase_financial_mind, "?"),
        Pair(R.string.ai_phrase_optimize_spending, ""),
        Pair(R.string.ai_phrase_money_copilot, "")
    )
    val selectedTemplate = templates[dayOfYear % templates.size]
    val phrasePrefix = stringResource(selectedTemplate.first)
    val phraseSuffix = selectedTemplate.second
    val displayName = if (userName.isNotBlank()) userName.trim() else stringResource(R.string.ai_default_name)

    val annotatedPhrase = buildAnnotatedString {
        append(phrasePrefix)
        withStyle(
            style = SpanStyle(
                color = Color(0xFFDB2777),
                fontWeight = FontWeight.ExtraBold
            )
        ) {
            append(displayName)
        }
        if (phraseSuffix.isNotEmpty()) {
            append(phraseSuffix)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 4 },
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Real app logo
            Image(
                painter = painterResource(id = R.drawable.img_app_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Rotating catchy daily phrase with highlighted pink username
            Text(
                text = annotatedPhrase,
                color = AiTextPrimary,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            if (!isLimitReached) {
                Spacer(modifier = Modifier.height(32.dp))

                // Row 1 Infinite Looping Slider in Light Theme
                val row1Pills = remember {
                    listOf(
                        CategoryPillData("🎯", R.string.ai_sugg_savings, R.string.ai_query_savings),
                        CategoryPillData("💡", R.string.ai_sugg_audit, R.string.ai_query_audit),
                        CategoryPillData("👑", R.string.ai_sugg_runway, R.string.ai_query_runway),
                        CategoryPillData("📈", R.string.ai_sugg_cash_flow, R.string.ai_query_cash_flow)
                    )
                }
                InfinitePillRow(pills = row1Pills, initialOffset = 1000, scrollSpeed = 0.9f, onSuggestionClick = onSuggestionClick)

                Spacer(modifier = Modifier.height(12.dp))

                // Row 2 Infinite Looping Slider in Light Theme
                val row2Pills = remember {
                    listOf(
                        CategoryPillData("⚡", R.string.ai_sugg_optimize, R.string.ai_query_optimize),
                        CategoryPillData("📊", R.string.ai_sugg_forecast, R.string.ai_query_forecast),
                        CategoryPillData("💳", R.string.ai_sugg_loans, R.string.ai_query_loans),
                        CategoryPillData("🏷️", R.string.ai_sugg_top_category, R.string.ai_query_top_category)
                    )
                }
                InfinitePillRow(pills = row2Pills, initialOffset = 3000, scrollSpeed = 0.7f, onSuggestionClick = onSuggestionClick)

                Spacer(modifier = Modifier.height(12.dp))

                // Row 3 Infinite Looping Slider in Light Theme
                val row3Pills = remember {
                    listOf(
                        CategoryPillData("📱", R.string.ai_sugg_pending_sms, R.string.ai_query_pending_sms),
                        CategoryPillData("💰", R.string.ai_sugg_balances, R.string.ai_query_balances),
                        CategoryPillData("✨", R.string.ai_sugg_streaks, R.string.ai_query_streaks),
                        CategoryPillData("🏦", R.string.ai_sugg_liquidity, R.string.ai_query_liquidity)
                    )
                }
                InfinitePillRow(pills = row3Pills, initialOffset = 5000, scrollSpeed = 1.0f, onSuggestionClick = onSuggestionClick)
            }
        }
    }
}

@Composable
fun InfinitePillRow(
    pills: List<CategoryPillData>,
    initialOffset: Int,
    scrollSpeed: Float,
    onSuggestionClick: (String) -> Unit
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialOffset)

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(16)
            listState.scrollBy(scrollSpeed)
        }
    }

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(
            count = Int.MAX_VALUE,
            itemContent = { index ->
                val pill = pills[index % pills.size]
                val labelText = stringResource(pill.label)
                val queryText = stringResource(pill.query)
                CategoryPill(icon = pill.icon, label = labelText) {
                    onSuggestionClick(queryText)
                }
            }
        )
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
        color = Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = AiTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

sealed class NextStep {
    data class Raw(val text: String) : NextStep()
    data class Resource(@StringRes val resId: Int) : NextStep()
}

data class ProcessedResponse(
    val mainText: String,
    val nextSteps: List<NextStep>
)

fun parseResponseTextAndNextSteps(rawText: String): ProcessedResponse {
    var text = com.oryno.piggy_ledger.ai.AiSanitizer.sanitizeThinking(rawText)
    
    // Service notice cards should not present unrelated finance CTA suggestions
    if (text.startsWith("# ⚠️") || text.startsWith("# ⏳")) {
        return ProcessedResponse(mainText = text, nextSteps = emptyList())
    }

    if (text.contains("Knowledge Hub Analysis", ignoreCase = true)) {
        val lines = text.split("\n").filterNot { line ->
            val trimmed = line.trim()
            trimmed.contains("Knowledge Hub Analysis", ignoreCase = true) ||
                    (trimmed.startsWith(">") && (trimmed.contains("Analysis", ignoreCase = true) || trimmed.contains("Mapped request", ignoreCase = true)))
        }
        text = lines.joinToString("\n").trim()
    }

    val nextStepHeaderRegex = Regex("""(?i)(###?\s*(next[_\s]*steps?|suggested[_\s]*next[_\s]*steps?|actionable[_\s]*next[_\s]*steps?|recommended[_\s]*next[_\s]*steps?|recommendations|follow-up\s*questions|related\s*questions|suggested\s*questions|what\s*to\s*ask\s*next|الخطوات\s*التالية|خطوات\s*مقترحة|أسئلة\s*مقترحة|أسئلة\s*متابعة)|(next[_\s]*steps?|suggested[_\s]*next[_\s]*steps?|actionable[_\s]*next[_\s]*steps?|الخطوات\s*التالية|خطوات\s*مقترحة):)""")
    
    val match = nextStepHeaderRegex.find(text)
    if (match != null) {
        val mainTextPart = text.substring(0, match.range.first).trim()
        val stepsPart = text.substring(match.range.last + 1).trim()
        
        val nextStepsList = mutableListOf<NextStep>()
        val lines = stepsPart.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.startsWith("•") || trimmed.matches(Regex("""^\d+\..*"""))) {
                val cleanStep = trimmed.replaceFirst(Regex("""^([-*•]|\d+\.)\s*"""), "").replace("**", "").trim()
                if (cleanStep.isNotBlank()) {
                    nextStepsList.add(NextStep.Raw(cleanStep))
                }
            } else if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                val cleanStep = trimmed.replace("**", "").trim()
                if (cleanStep.isNotBlank() && nextStepsList.size < 2) {
                    nextStepsList.add(NextStep.Raw(cleanStep))
                }
            }
        }
        
        if (nextStepsList.isNotEmpty()) {
            return ProcessedResponse(
                mainText = if (mainTextPart.isNotBlank()) mainTextPart else text,
                nextSteps = nextStepsList.take(2)
            )
        }
    }

    // Contextual CTA follow-up questions (Strictly 2 suggestions)
    val lowerText = text.lowercase()
    val contextualNextSteps = when {
        lowerText.contains("goal") || lowerText.contains("save") || lowerText.contains("target") -> listOf(
            NextStep.Resource(R.string.ai_query_savings),
            NextStep.Resource(R.string.ai_cta_accelerate_savings)
        )
        lowerText.contains("loan") || lowerText.contains("debt") || lowerText.contains("borrow") -> listOf(
            NextStep.Resource(R.string.ai_query_loans),
            NextStep.Resource(R.string.ai_cta_total_debt)
        )
        lowerText.contains("streak") || lowerText.contains("habit") -> listOf(
            NextStep.Resource(R.string.ai_query_streaks),
            NextStep.Resource(R.string.ai_cta_logged_today)
        )
        lowerText.contains("account") || lowerText.contains("balance") || lowerText.contains("cash") -> listOf(
            NextStep.Resource(R.string.ai_cta_balances_summary),
            NextStep.Resource(R.string.ai_query_liquidity)
        )
        else -> listOf(
            NextStep.Resource(R.string.ai_query_audit),
            NextStep.Resource(R.string.ai_cta_optimize_budget)
        )
    }

    return ProcessedResponse(mainText = text, nextSteps = contextualNextSteps.take(2))
}

/**
 * Chat Message Item with Pill-shaped User Bubble and Clean Gemini Layout
 */
@Composable
fun ChatMessageItem(
    message: AiChatMessage,
    shouldStream: Boolean,
    isLimitReached: Boolean = false,
    onAnimationComplete: () -> Unit,
    onCtaClick: (String) -> Unit,
    onTtsClick: (String, String) -> Unit = { _, _ -> },
    isSpeaking: Boolean = false,
    onNavigateToPaywall: () -> Unit = {}
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
        // User Pill Bubble (Image 1 style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(message.content))
                    Toast.makeText(context, context.getString(R.string.question_copied_to_clipboard), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.ai_copy_question),
                    tint = AiTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            SelectionContainer {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(AiUserBubbleColor)
                        .border(BorderStroke(1.dp, Color(0xFFFBCFE8)), RoundedCornerShape(24.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = message.content,
                        color = AiUserBubbleTextColor,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    } else {
        // AI Response Layout (Image 1 style: Sparkle icon + Clean Answer Typography)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            GeminiSparkleIcon(
                modifier = Modifier
                    .size(22.dp)
                    .padding(top = 4.dp),
                color = Color(0xFF1E293B),
                isPulsing = false
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

                val rawRationale = com.oryno.piggy_ledger.ai.AiSanitizer.sanitizeThinking(decodedResponse?.archetypeRationale ?: message.content)
                val processedResponse = remember(rawRationale) {
                    parseResponseTextAndNextSteps(rawRationale)
                }
                val mainAnswerText = processedResponse.mainText
                val nextStepsList = processedResponse.nextSteps

                // Rapid, smooth streaming display - dynamically chunked so it completes briskly in ~250-350ms
                var charCount by remember(message.id, mainAnswerText, shouldStream) {
                    mutableStateOf(if (shouldStream) 0 else mainAnswerText.length)
                }

                LaunchedEffect(message.id, shouldStream, mainAnswerText) {
                    if (shouldStream && charCount < mainAnswerText.length) {
                        val totalLength = mainAnswerText.length
                        // Dynamically scale step so text streams rapidly and pleasantly in ~15-20 frames (~250-320ms)
                        val chunkStep = maxOf(12, totalLength / 18)
                        while (charCount < totalLength) {
                            delay(16)
                            charCount = minOf(charCount + chunkStep, totalLength)
                        }
                        onAnimationComplete()
                    } else {
                        charCount = mainAnswerText.length
                    }
                }

                val displayedText = remember(mainAnswerText, charCount) {
                    mainAnswerText.take(charCount)
                }

                // AI Answer Content in clean, high-contrast light typography (tap to instantly finish streaming)
                SelectionContainer(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = charCount < mainAnswerText.length
                    ) {
                        charCount = mainAnswerText.length
                        onAnimationComplete()
                    }
                ) {
                    Column {
                        if (displayedText.isNotBlank()) {
                            FormattedMarkdownText(displayedText)
                            Spacer(modifier = Modifier.height(10.dp))
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
                        is UiBlock.ActionBannerBlock -> ActionBanner(
                            block = block, 
                            onUpgradeClick = onNavigateToPaywall,
                            onRetryClick = { onCtaClick("RETRY_LAST") }
                        )
                        is UiBlock.HighlightTextBlock -> HighlightedText(block)
                        is UiBlock.GroupBlock -> GroupBlockRenderer(block)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                    }
                }

                // Bottom actions row with Copy and Native TTS speech actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy button
                    IconButton(
                        onClick = {
                            val textToCopy = mainAnswerText.ifBlank { message.content }
                            clipboardManager.setText(AnnotatedString(textToCopy))
                            isCopied = true
                            Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.ai_copy_response),
                            tint = if (isCopied) Color(0xFF10B981) else AiTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Native TTS Speak / Stop button
                    IconButton(
                        onClick = {
                            val textToSpeak = mainAnswerText.ifBlank { message.content }
                            onTtsClick(message.id, textToSpeak)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isSpeaking) stringResource(R.string.ai_stop_speech) else stringResource(R.string.ai_read_aloud),
                            tint = if (isSpeaking) AiGeminiBlue else AiTextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Render Next Steps as CTA pill buttons (Strictly 2 suggestions, responsive to screen size)
                if (!isLimitReached && nextStepsList.isNotEmpty() && charCount >= mainAnswerText.length) {
                    Spacer(modifier = Modifier.height(10.dp))
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isSmallScreen = maxWidth < 360.dp
                        val suggestionFontSize = if (isSmallScreen) 11.5.sp else 13.sp
                        val suggestionLineHeight = if (isSmallScreen) 16.sp else 18.sp
                        val horizontalPadding = if (isSmallScreen) 10.dp else 14.dp
                        val verticalPadding = if (isSmallScreen) 7.dp else 9.dp
                        val iconSize = if (isSmallScreen) 12.dp else 14.dp

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            nextStepsList.take(2).forEach { step ->
                                val stepText = when (step) {
                                    is NextStep.Raw -> step.text
                                    is NextStep.Resource -> stringResource(step.resId)
                                }
                                Surface(
                                    onClick = { onCtaClick(stepText) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stepText,
                                            color = AiTextPrimary,
                                            fontSize = suggestionFontSize,
                                            lineHeight = suggestionLineHeight,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = AiGeminiBlue,
                                            modifier = Modifier.size(iconSize)
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
}

@Composable
fun FormattedMarkdownText(text: String) {
    val blocks = remember(text) { parseMarkdownBlocks(text) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.CodeBlock -> {
                    RenderCodeBlock(
                        language = block.language,
                        code = block.code,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(block.code))
                            Toast.makeText(context, context.getString(R.string.code_copied_to_clipboard), Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                is MarkdownBlock.CustomProgress -> {
                    RenderProgressCard(block)
                }
                is MarkdownBlock.CustomSteps -> {
                    RenderStepsCard(block)
                }
                is MarkdownBlock.CustomStat -> {
                    RenderStatCard(block)
                }
                is MarkdownBlock.Header -> {
                    val isWarning = block.text.contains("No connection", ignoreCase = true) || 
                                    block.text.contains("Connection lost", ignoreCase = true) || 
                                    block.text.contains("⚠️", ignoreCase = true)
                    if (isWarning || block.level == 1) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.5.dp, Color(0xFFFCA5A5)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = block.text,
                                    color = Color(0xFFDC2626),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp
                                )
                            }
                        }
                    } else {
                        Markdown(
                            content = "#".repeat(block.level) + " " + block.text,
                            colors = markdownColor(
                                text = AiTextPrimary,
                                codeText = Color(0xFF1E293B),
                                inlineCodeText = AiGeminiBlue,
                                inlineCodeBackground = Color(0xFFF1F5F9),
                                dividerColor = Color(0xFFE2E8F0)
                            )
                        )
                    }
                }
                is MarkdownBlock.Callout -> {
                    StandardQuoteBlock(block.text)
                }
                is MarkdownBlock.HorizontalRule -> {
                    HorizontalDivider(
                        color = Color(0xFFE2E8F0),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                is MarkdownBlock.Table -> {
                    RenderMarkdownTable(block.headers, block.rows)
                }
                is MarkdownBlock.StandardMarkdown -> {
                    Markdown(
                        content = block.rawMarkdown,
                        colors = markdownColor(
                            text = AiTextPrimary,
                            codeText = Color(0xFF1E293B),
                            inlineCodeText = AiGeminiBlue,
                            inlineCodeBackground = Color(0xFFF1F5F9),
                            dividerColor = Color(0xFFE2E8F0)
                        )
                    )
                }
                is MarkdownBlock.Paragraph -> {
                    Markdown(
                        content = block.text,
                        colors = markdownColor(
                            text = AiTextPrimary,
                            codeText = Color(0xFF1E293B),
                            inlineCodeText = AiGeminiBlue,
                            inlineCodeBackground = Color(0xFFF1F5F9),
                            dividerColor = Color(0xFFE2E8F0)
                        )
                    )
                }
                is MarkdownBlock.BulletItem -> {
                    Markdown(
                        content = "- " + block.text,
                        colors = markdownColor(
                            text = AiTextPrimary,
                            codeText = Color(0xFF1E293B),
                            inlineCodeText = AiGeminiBlue,
                            inlineCodeBackground = Color(0xFFF1F5F9),
                            dividerColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun RenderCodeBlock(
    language: String,
    code: String,
    onCopy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF1E3A8A),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = language.uppercase(),
                        color = Color(0xFF93C5FD),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    onClick = onCopy,
                    color = Color(0xFF334155),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.ai_copy),
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.ai_copy),
                            color = Color(0xFFF8FAFC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

            // Code Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = code,
                    color = Color(0xFFF1F5F9),
                    fontSize = 12.5.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun RenderProgressCard(block: MarkdownBlock.CustomProgress) {
    val pct = if (block.max > 0f) ((block.value / block.max) * 100).toInt().coerceIn(0, 100) else 0
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = block.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = AiTextPrimary
                )
                Text(
                    text = "$pct%",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = AiGeminiBlue
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (block.value / block.max.coerceAtLeast(1f)).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = AiGeminiBlue,
                trackColor = Color(0xFFE0E7FF)
            )
        }
    }
}

@Composable
fun RenderStepsCard(block: MarkdownBlock.CustomSteps) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF0FDF4),
        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (block.title.isNotBlank()) {
                Text(
                    text = block.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF166534),
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            block.steps.forEachIndexed { idx, stepText ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color(0xFF22C55E), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stepText,
                        color = Color(0xFF14532D),
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun RenderStatCard(block: MarkdownBlock.CustomStat) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFEEF2FF),
        border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = block.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4338CA)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = block.mainValue,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF312E81)
            )
            if (!block.subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = block.subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF6366F1)
                )
            }
        }
    }
}

@Composable
fun StandardQuoteBlock(quoteText: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF3F4F6),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
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
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
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
                            color = AiTextPrimary,
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
                                color = AiTextPrimary,
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
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class CustomProgress(val title: String, val value: Float, val max: Float) : MarkdownBlock()
    data class CustomSteps(val title: String, val steps: List<String>) : MarkdownBlock()
    data class CustomStat(val title: String, val mainValue: String, val subtitle: String? = null) : MarkdownBlock()
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class BulletItem(val text: String) : MarkdownBlock()
    data class Callout(val text: String) : MarkdownBlock()
    data class HorizontalRule(val dummy: Unit = Unit) : MarkdownBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class StandardMarkdown(val rawMarkdown: String) : MarkdownBlock()
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
        
        // Code block check: ```lang ... ```
        if (line.startsWith("```")) {
            val language = line.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size && lines[i].trim().startsWith("```")) {
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(language.ifBlank { "code" }, codeLines.joinToString("\n")))
            continue
        }

        // :::progress
        if (line.startsWith(":::progress")) {
            i++
            var title = "Progress"
            var value = 0f
            var max = 100f
            while (i < lines.size && !lines[i].trim().startsWith(":::")) {
                val l = lines[i].trim()
                if (l.startsWith("title:")) title = l.removePrefix("title:").trim()
                if (l.startsWith("value:")) value = l.removePrefix("value:").trim().toFloatOrNull() ?: 0f
                if (l.startsWith("max:")) max = l.removePrefix("max:").trim().toFloatOrNull() ?: 100f
                i++
            }
            if (i < lines.size && lines[i].trim().startsWith(":::")) i++
            blocks.add(MarkdownBlock.CustomProgress(title, value, max))
            continue
        }

        // :::steps
        if (line.startsWith(":::steps")) {
            i++
            var title = "Steps"
            val stepItems = mutableListOf<String>()
            while (i < lines.size && !lines[i].trim().startsWith(":::")) {
                val l = lines[i].trim()
                if (l.startsWith("title:")) {
                    title = l.removePrefix("title:").trim()
                } else if (l.isNotBlank()) {
                    val stepText = l.replaceFirst(Regex("""^\d+\.\s*"""), "").removePrefix("-").trim()
                    if (stepText.isNotBlank()) stepItems.add(stepText)
                }
                i++
            }
            if (i < lines.size && lines[i].trim().startsWith(":::")) i++
            blocks.add(MarkdownBlock.CustomSteps(title, stepItems))
            continue
        }
        
        // Table check
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
        
        // Horizontal Rule check
        if (line.all { it == '-' || it == '*' || it == '_' } && line.length >= 3) {
            blocks.add(MarkdownBlock.HorizontalRule())
            i++
            continue
        }
        
        // Callout / Blockquote
        if (line.startsWith(">")) {
            val calloutLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                calloutLines.add(lines[i].trim().removePrefix(">").trim())
                i++
            }
            blocks.add(MarkdownBlock.Callout(calloutLines.joinToString("\n")))
            continue
        }
        
        // Headers
        if (line.startsWith("#")) {
            val level = line.takeWhile { it == '#' }.length
            val title = line.dropWhile { it == '#' }.trim()
            blocks.add(MarkdownBlock.Header(level, title))
            i++
            continue
        }
        
        // Bullet items
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
        
        // Regular Paragraph
        blocks.add(MarkdownBlock.StandardMarkdown(line))
        i++
    }
    
    return blocks
}

private fun parseAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        val cleanText = text
        if (cleanText.contains("<mark>") && cleanText.contains("</mark>")) {
            val markParts = cleanText.split("<mark>", "</mark>")
            for (i in markParts.indices) {
                if (i % 2 == 1) {
                    withStyle(
                        style = SpanStyle(
                            background = Color(0xFFFEF08A),
                            color = Color(0xFF854D0E),
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(markParts[i])
                    }
                } else {
                    appendBoldParts(markParts[i])
                }
            }
        } else {
            appendBoldParts(cleanText)
        }
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendBoldParts(text: String) {
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

@Composable
fun KpiCard(block: UiBlock.KpiCardBlock) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(block.title, color = AiTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(block.primaryValue, color = AiTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(block.subValue, color = AiTextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
fun StreakStatus(block: UiBlock.StreakStatusBlock) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Discipline Streak", color = AiTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${block.currentStreak}", color = AiTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(" days", color = AiTextSecondary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 3.dp, start = 4.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(block.statusMessage, color = AiTextPrimary, fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(block.streakFreezesAvailable) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AiGeminiBlue))
                    }
                }
            }
        }
    }
}

@Composable
fun MetricGrid(block: UiBlock.MetricGridBlock) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(block.title, color = AiTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(14.dp))
            val chunked = block.metrics.chunked(2)
            chunked.forEachIndexed { index, rowMetrics ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowMetrics.forEach { metric ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Text(metric.label, color = AiTextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(metric.value, color = AiTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (index < chunked.size - 1) Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun InteractiveChart(block: UiBlock.InteractiveChartBlock) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(block.title, color = AiTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val maxVal = block.dataPoints.maxOfOrNull { it.value } ?: 1.0
                block.dataPoints.forEach { point ->
                    val heightRatio = (point.value / maxVal).toFloat()
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Box(
                            modifier = Modifier
                                .width(26.dp)
                                .fillMaxHeight(heightRatio.coerceAtLeast(0.05f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(AiGeminiBlue)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(point.label.take(3), color = AiTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ReflectivePoll(block: UiBlock.ReflectivePollBlock) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(block.promptMessage, color = AiTextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
            Spacer(modifier = Modifier.height(16.dp))
            block.options.forEach { option ->
                Button(
                    onClick = { /* Option click */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = AiTextPrimary),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(block.merchant, color = AiTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(block.flagReason, color = AiTextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(block.amount, color = AiTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(block.impactOnRunway, color = AiTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ActionBanner(
    block: UiBlock.ActionBannerBlock, 
    onUpgradeClick: () -> Unit = {},
    onRetryClick: () -> Unit = {}
) {
    val isUpgrade = block.message.contains("upgrade", ignoreCase = true) || 
            block.message.contains("pro", ignoreCase = true) ||
            block.actionPayload.contains("upgrade", ignoreCase = true) ||
            block.actionPayload.contains("ترقية", ignoreCase = true)

    val containerColor = if (isUpgrade) Color(0xFFFDF2F8) else Color(0xFFFEF2F2)
    val borderColor = if (isUpgrade) Color(0xFFFBCFE8) else Color(0xFFFECACA)
    val textColor = if (isUpgrade) Color(0xFF831843) else Color(0xFF991B1B)
    val buttonColor = if (isUpgrade) Color(0xFFDB2777) else Color(0xFFDC2626)

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = block.message,
                color = textColor,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                lineHeight = 19.sp
            )
            if (block.actionPayload.isNotBlank()) {
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        if (isUpgrade) onUpgradeClick() else onRetryClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = block.actionPayload,
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun HighlightedText(block: UiBlock.HighlightTextBlock) {
    Surface(
        color = Color(0xFFFCE7F3),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = block.text,
            color = Color(0xFFDB2777),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun GroupBlockRenderer(block: UiBlock.GroupBlock) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = block.title,
            color = AiTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (block.type == "CIRCLE") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                block.items.forEach { item ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(AiGeminiBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.title.take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.value, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else { // Default to CARD
            block.items.forEach { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.title, fontWeight = FontWeight.Medium)
                        Text(item.value, fontWeight = FontWeight.Bold, color = AiPiggyPink)
                    }
                }
            }
        }
    }
}
