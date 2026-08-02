package com.oryno.piggy_ledger.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.oryno.piggy_ledger.ai.AiChatViewModel
import com.oryno.piggy_ledger.ai.SovereignAiResponse
import com.oryno.piggy_ledger.ai.UiBlock
import com.oryno.piggy_ledger.data.AiChatMessage
import com.oryno.piggy_ledger.data.AiConversation
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val prefs = remember { context.getSharedPreferences("piggy_ledger_prefs", android.content.Context.MODE_PRIVATE) }
    var hasSeenMagicMicTooltip by remember { mutableStateOf(prefs.getBoolean("has_seen_magic_mic_tooltip", false)) }
    var showMagicMicTooltipDialog by remember { mutableStateOf(false) }
    var transcribedText by remember { mutableStateOf<String?>(null) }
    var showAccountPickerForMic by remember { mutableStateOf(false) }
    var pendingMicTransactionData by remember { mutableStateOf<Triple<Double, String, Boolean>?>(null) }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull() ?: ""
            if (spoken.isNotBlank()) {
                transcribedText = spoken
            }
        }
    }

    val handleMagicMicClick = {
        if (!hasSeenMagicMicTooltip) {
            showMagicMicTooltipDialog = true
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your transaction or request...")
            }
            try {
                speechRecognizerLauncher.launch(intent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Speech recognition not available", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFFAFAFA),
                drawerContentColor = Color(0xFF0F172A),
                modifier = Modifier.width(320.dp)
            ) {
                ChatHistoryDrawerContent(
                    conversations = conversations,
                    activeConversationId = activeConversationId,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onSelectConversation = { id ->
                        viewModel.selectConversation(id)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onNewChatClick = {
                        viewModel.createNewConversation()
                        coroutineScope.launch { drawerState.close() }
                    },
                    onDeleteConversation = { id ->
                        viewModel.deleteConversation(id)
                    },
                    onTogglePinConversation = { id, pinned ->
                        viewModel.togglePinConversation(id, pinned)
                    },
                    onRenameConversation = { id, title ->
                        viewModel.renameConversation(id, title)
                    }
                )
            }
        }
    ) {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Chat History", tint = AiText)
                            }
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AiText)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.createNewConversation() }) {
                            Icon(Icons.Default.Add, contentDescription = "New Chat", tint = Color(0xFF4F46E5))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AiBackground,
                        titleContentColor = AiText,
                        navigationIconContentColor = AiText
                    )
                )
            },
            containerColor = AiBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
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
                                onSuggestionClick = { query ->
                                    viewModel.sendMessage(query)
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
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
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
                                    ThinkingIndicator()
                                }
                            }
                        }
                    }

                    // Material Design 3 Expressive Prompt Bar
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(36.dp),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .navigationBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Middle Text Prompt Input
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (inputText.isEmpty()) {
                                    Text(
                                        text = "Ask Piggy AI...",
                                        color = Color(0xFF475569),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                                BasicTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    textStyle = TextStyle(
                                        color = Color(0xFF0F172A),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal,
                                        lineHeight = 22.sp
                                    ),
                                    maxLines = 4,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            if (inputText.isNotBlank() && !isLoading) {
                                                viewModel.sendMessage(inputText)
                                                inputText = ""
                                            }
                                        }
                                    ),
                                    cursorBrush = SolidColor(Color(0xFF00B0FF)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                )
                            }

                            // Right Microphone Icon Button
                            IconButton(
                                onClick = handleMagicMicClick,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Input",
                                    tint = Color(0xFF1E293B),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Rightmost Cyan Action Pill (Waveform / Send)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFF00B0FF), CircleShape)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (inputText.isNotBlank() && !isLoading) {
                                            viewModel.sendMessage(inputText)
                                            inputText = ""
                                        } else {
                                            handleMagicMicClick()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (inputText.isBlank()) {
                                    WaveformAudioIcon(barColor = Color(0xFF0F172A))
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // First-time Magic Mic Tooltip Dialog
                if (showMagicMicTooltipDialog) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f))
                            .clickable(enabled = false) {},
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AiSurface),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .padding(bottom = 80.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Magic Mic", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AiText)
                                    }
                                    IconButton(onClick = {
                                        prefs.edit().putBoolean("has_seen_magic_mic_tooltip", true).apply()
                                        hasSeenMagicMicTooltip = true
                                        showMagicMicTooltipDialog = false
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your transaction...")
                                        }
                                        try { speechRecognizerLauncher.launch(intent) } catch (e: Exception) {}
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AiDimText)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Speak naturally to instantly log expenses, income, or transfers!\n\nJust say: 'I paid 350 EGP for groceries using CIB' or 'Got 15000 EGP salary'. Magic Mic will parse and log it automatically.",
                                    color = AiDimText,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        prefs.edit().putBoolean("has_seen_magic_mic_tooltip", true).apply()
                                        hasSeenMagicMicTooltip = true
                                        showMagicMicTooltipDialog = false
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your transaction...")
                                        }
                                        try { speechRecognizerLauncher.launch(intent) } catch (e: Exception) {}
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Got it, Start Magic Mic", color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // Transcription Bottom Sheet (Edit and Confirm)
                if (transcribedText != null) {
                    ModalBottomSheet(
                        onDismissRequest = { transcribedText = null },
                        containerColor = AiSurface
                    ) {
                        var editableText by remember(transcribedText) { mutableStateOf(transcribedText ?: "") }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .navigationBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Magic Mic Transcription", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AiText)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Review and edit your spoken text before confirming:", fontSize = 13.sp, color = AiDimText)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editableText,
                                onValueChange = { editableText = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = AiBorder,
                                    focusedTextColor = AiText,
                                    unfocusedTextColor = AiText
                                ),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 3
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {},
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, Color(0xFF6366F1)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Edit", color = Color(0xFF6366F1), fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        val text = editableText.trim()
                                        if (text.isNotBlank()) {
                                            val isIncome = text.contains("salary", true) || text.contains("got", true) || text.contains("income", true) || text.contains("received", true) || text.contains("earned", true) || text.contains("deposit", true)
                                            val amountRegex = Regex("""\d+(\.\d+)?""")
                                            val match = amountRegex.find(text)
                                            val amount = match?.value?.toDoubleOrNull() ?: 0.0
                                            val merchant = text.take(50)

                                            if (accounts.size == 1) {
                                                val accId = accounts.first().id
                                                viewModel.processMagicMicTransaction(accId, amount, merchant, isIncome, context)
                                                transcribedText = null
                                            } else if (accounts.isNotEmpty()) {
                                                pendingMicTransactionData = Triple(amount, merchant, isIncome)
                                                showAccountPickerForMic = true
                                                transcribedText = null
                                            } else {
                                                android.widget.Toast.makeText(context, "Please create an account first!", android.widget.Toast.LENGTH_SHORT).show()
                                                transcribedText = null
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Confirm", color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                // Account Picker Bottom Sheet (if multiple accounts)
                if (showAccountPickerForMic && pendingMicTransactionData != null) {
                    val (amount, merchant, isIncome) = pendingMicTransactionData!!
                    ModalBottomSheet(
                        onDismissRequest = { showAccountPickerForMic = false; pendingMicTransactionData = null },
                        containerColor = AiSurface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .navigationBarsPadding()
                        ) {
                            Text("Select Preferred Account", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AiText)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("You have multiple accounts. Choose which account to log this transaction into:", fontSize = 13.sp, color = AiDimText)
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.heightIn(max = 300.dp)
                            ) {
                                items(accounts) { acc ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = AiBackground),
                                        border = BorderStroke(1.dp, AiBorder),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.processMagicMicTransaction(acc.id, amount, merchant, isIncome, context)
                                                showAccountPickerForMic = false
                                                pendingMicTransactionData = null
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(acc.name, fontWeight = FontWeight.Bold, color = AiText, fontSize = 16.sp)
                                                val accNo = acc.bank_account_no ?: acc.card_numbers ?: ""
                                                if (accNo.isNotBlank()) {
                                                    Text(accNo, color = AiDimText, fontSize = 12.sp)
                                                }
                                            }
                                            Text("${acc.current_balance} EGP", fontWeight = FontWeight.SemiBold, color = Color(0xFF6366F1))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

data class ConversationGroup(
    val title: String,
    val items: List<AiConversation>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatHistoryDrawerContent(
    conversations: List<AiConversation>,
    activeConversationId: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectConversation: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onTogglePinConversation: (String, Boolean) -> Unit,
    onRenameConversation: (String, String) -> Unit
) {
    var conversationToRename by remember { mutableStateOf<AiConversation?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var menuConversationId by remember { mutableStateOf<String?>(null) }

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
        if (pinned.isNotEmpty()) groups.add(ConversationGroup("Pinned", pinned))
        if (todayItems.isNotEmpty()) groups.add(ConversationGroup("Today", todayItems))
        if (sevenDaysItems.isNotEmpty()) groups.add(ConversationGroup("7 Days", sevenDaysItems))
        if (thirtyDaysItems.isNotEmpty()) groups.add(ConversationGroup("30 Days", thirtyDaysItems))
        if (olderItems.isNotEmpty()) groups.add(ConversationGroup("Older", olderItems))

        groups
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp)
    ) {
        // Search bar
        Surface(
            shape = RoundedCornerShape(24.dp),
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
                    contentDescription = "Search",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search chat content...", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // "New Chat" button inside history menu
        Surface(
            onClick = onNewChatClick,
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF6366F1),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New Conversation",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFE2E8F0))
        Spacer(modifier = Modifier.height(8.dp))

        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No previous chats", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        } else if (conversationGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No matching conversations", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                conversationGroups.forEach { group ->
                    item(key = "header_${group.title}") {
                        Text(
                            text = group.title,
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                        )
                    }
                    items(group.items, key = { it.id }) { conv ->
                        val isSelected = conv.id == activeConversationId
                        val showDropdown = menuConversationId == conv.id

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFFE2E8F0) else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.dp, Color(0xFFCBD5E1)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        onClick = { onSelectConversation(conv.id) },
                                        onLongClick = { menuConversationId = conv.id }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (conv.isPinned) {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "Pinned",
                                            tint = Color(0xFF6366F1),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = conv.title,
                                        color = if (isSelected) Color(0xFF0F172A) else Color(0xFF334155),
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { menuConversationId = conv.id },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = showDropdown,
                                onDismissRequest = { menuConversationId = null },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename", color = Color(0xFF0F172A)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        menuConversationId = null
                                        renameInputText = conv.title
                                        conversationToRename = conv
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (conv.isPinned) "Unpin" else "Pin", color = Color(0xFF0F172A)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.PushPin, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        menuConversationId = null
                                        onTogglePinConversation(conv.id, conv.isPinned)
                                    }
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                DropdownMenuItem(
                                    text = { Text("Delete", color = Color(0xFFEF4444)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        menuConversationId = null
                                        onDeleteConversation(conv.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (conversationToRename != null) {
        AlertDialog(
            onDismissRequest = { conversationToRename = null },
            title = { Text("Rename Conversation", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    singleLine = true,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = conversationToRename
                        if (target != null && renameInputText.isNotBlank()) {
                            onRenameConversation(target.id, renameInputText.trim())
                        }
                        conversationToRename = null
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToRename = null }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
fun ThinkingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExpressiveLoadingIndicator(size = 28.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Piggy AI is analyzing Knowledge Hub...",
            color = AiDimText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

data class CategoryPillData(
    val icon: String,
    val label: String,
    val query: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyChatState(
    onSuggestionClick: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var showInfoBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    if (showInfoBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInfoBottomSheet = false },
            containerColor = Color.White,
            contentColor = Color(0xFF0F172A),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFCBD5E1)) }
        ) {
            HowPiggyAiWorksSheetContent(onDismiss = { showInfoBottomSheet = false })
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { it / 4 },
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
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

            Spacer(modifier = Modifier.height(28.dp))

            // Row 1 Infinite Looping Slider
            val row1Pills = remember {
                listOf(
                    CategoryPillData("🎤", "go-to savings goals", "What are my current savings goals progress?"),
                    CategoryPillData("💡", "audit recent spending", "Audit my recent spending transactions"),
                    CategoryPillData("👑", "most successful runway", "What is my current runway and budget balance?"),
                    CategoryPillData("📈", "cash flow trends", "What are my recent cash flow trends?")
                )
            }
            InfinitePillRow(pills = row1Pills, initialOffset = 1000, scrollSpeed = 1.0f, onSuggestionClick = onSuggestionClick)

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2 Infinite Looping Slider
            val row2Pills = remember {
                listOf(
                    CategoryPillData("💻", "expense shortcuts", "How can I optimize my monthly expenses?"),
                    CategoryPillData("📊", "cash flow forecast", "Show my cash flow forecast"),
                    CategoryPillData("💳", "loan repayment status", "What is my loan repayment status?"),
                    CategoryPillData("🏷️", "top spending category", "Which category do I spend the most on?")
                )
            }
            InfinitePillRow(pills = row2Pills, initialOffset = 3000, scrollSpeed = 0.8f, onSuggestionClick = onSuggestionClick)

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3 Infinite Looping Slider
            val row3Pills = remember {
                listOf(
                    CategoryPillData("📱", "review pending SMS logs", "Are there any pending SMS transactions to review?"),
                    CategoryPillData("💰", "account balances", "Show all my account balances"),
                    CategoryPillData("⚡", "instant pay fees summary", "Summary of instant pay fees across my accounts"),
                    CategoryPillData("🎯", "goal milestones", "Which savings goals are closest to completion?")
                )
            }
            InfinitePillRow(pills = row3Pills, initialOffset = 5000, scrollSpeed = 1.2f, onSuggestionClick = onSuggestionClick)

            Spacer(modifier = Modifier.height(28.dp))

            // Footer link
            TextButton(onClick = { showInfoBottomSheet = true }) {
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
fun HowPiggyAiWorksSheetContent(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "How Piggy AI Works",
                color = Color(0xFF0F172A),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Privacy-first, client-side financial intelligence engine",
                color = Color(0xFF64748B),
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
        Spacer(modifier = Modifier.height(20.dp))

        val sections = listOf(
            "1. On-Device Context" to "Piggy AI securely accesses your local accounts, transactions, and savings goals directly on your device.",
            "2. Smart Habit Engine" to "Tracks your daily financial logging streak and provides personalized recommendations.",
            "3. Privacy & Offline Ready" to "Your financial data stays private on your device with secure local storage and full offline capability."
        )

        sections.forEach { (title, description) ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    color = Color(0xFF0F172A),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    color = Color(0xFF334155),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "Got it, thanks!",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
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

    // Gentle auto-scroll continuous slider effect
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
                CategoryPill(icon = pill.icon, label = pill.label) {
                    onSuggestionClick(pill.query)
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

    val nextStepHeaderRegex = Regex("""(?i)(###?\s*(next[_\s]*steps?|suggested[_\s]*next[_\s]*steps?|actionable[_\s]*next[_\s]*steps?|recommended[_\s]*next[_\s]*steps?|recommendations|follow-up\s*questions|related\s*questions|suggested\s*questions|what\s*to\s*ask\s*next)|(next[_\s]*steps?|suggested[_\s]*next[_\s]*steps?|actionable[_\s]*next[_\s]*steps?):)""")
    
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
        
        if (nextStepsList.isNotEmpty()) {
            return ProcessedResponse(
                mainText = if (mainTextPart.isNotBlank()) mainTextPart else text,
                nextSteps = nextStepsList.take(3)
            )
        }
    }

    // Contextual CTA follow-up questions when explicit next steps header is not returned
    val lowerText = text.lowercase()
    val contextualNextSteps = when {
        lowerText.contains("goal") || lowerText.contains("save") || lowerText.contains("target") -> listOf(
            "What are my active savings goals progress?",
            "How much do I need to complete my top goal?",
            "How can I accelerate my savings rate?"
        )
        lowerText.contains("loan") || lowerText.contains("debt") || lowerText.contains("borrow") -> listOf(
            "What is my loan repayment status?",
            "How much total debt do I owe?",
            "Audit my recent loan payments"
        )
        lowerText.contains("streak") || lowerText.contains("habit") -> listOf(
            "What is my current logging streak status?",
            "Did I log my financial activity today?",
            "How can I maintain a 30-day logging streak?"
        )
        lowerText.contains("account") || lowerText.contains("balance") || lowerText.contains("cash") -> listOf(
            "Show all my account balances summary",
            "Which account holds my largest liquidity?",
            "Audit my recent spending transactions"
        )
        else -> listOf(
            "Audit my recent spending transactions",
            "How can I optimize my monthly budget?",
            "What are my top spending categories?"
        )
    }

    return ProcessedResponse(mainText = text, nextSteps = contextualNextSteps)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(message.content))
                    Toast.makeText(context, "Question copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(28.dp).padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy question",
                    tint = AiDimText,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            SelectionContainer {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = message.content,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFF10B981))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
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

                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Wrap text response in SelectionContainer for manual highlight & copy
                        SelectionContainer {
                            Column {
                                if (displayedText.isNotBlank()) {
                                    FormattedMarkdownText(displayedText)
                                    Spacer(modifier = Modifier.height(12.dp))
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

                        // Action row with Copy button inside card footer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
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
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy response",
                                    tint = if (isCopied) Color(0xFF10B981) else AiDimText,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                // Render Next Steps as CTA pill buttons outside response card
                if (nextStepsList.isNotEmpty() && charCount >= mainAnswerText.length) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        nextStepsList.forEach { stepText ->
                            Surface(
                                onClick = { onCtaClick(stepText) },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFEEF2FF),
                                border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stepText,
                                        color = Color(0xFF312E81),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
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
                    val isWarning = block.text.contains("No connection", ignoreCase = true) || block.text.contains("⚠️", ignoreCase = true)
                    if (isWarning || block.level == 1) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.5.dp, Color(0xFFFCA5A5)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = block.text,
                                    color = Color(0xFFDC2626),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp
                                )
                            }
                        }
                    } else {
                        val fontSize = when (block.level) {
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
        colors = CardDefaults.cardColors(containerColor = AiSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AiBorder),
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
        border = BorderStroke(1.dp, AiBorder),
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
        border = BorderStroke(1.dp, AiBorder),
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
        border = BorderStroke(1.dp, AiBorder),
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
        border = BorderStroke(1.dp, AiBorder),
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
                    border = BorderStroke(1.dp, AiBorder),
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
        border = BorderStroke(1.dp, AiBorder),
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

enum class GeometricShapeType {
    SparkleStar,
    OctagramStar,
    Hexagon
}

@Composable
fun RotatingGeometricShape(
    modifier: Modifier = Modifier,
    shapeType: GeometricShapeType = GeometricShapeType.SparkleStar,
    colors: List<Color> = listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B)),
    rotationDurationMs: Int = 8000
) {
    val infiniteTransition = rememberInfiniteTransition(label = "geometric_rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val brush = Brush.sweepGradient(colors)

    Canvas(modifier = modifier.rotate(angle)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = kotlin.math.min(w, h) / 2f

        when (shapeType) {
            GeometricShapeType.SparkleStar -> {
                val path = Path().apply {
                    moveTo(cx, cy - radius)
                    quadraticTo(cx, cy, cx + radius, cy)
                    quadraticTo(cx, cy, cx, cy + radius)
                    quadraticTo(cx, cy, cx - radius, cy)
                    quadraticTo(cx, cy, cx, cy - radius)
                    close()
                }
                drawPath(path = path, brush = brush)
            }
            GeometricShapeType.OctagramStar -> {
                val path = Path()
                val numPoints = 8
                val innerRadius = radius * 0.45f
                for (i in 0 until numPoints * 2) {
                    val r = if (i % 2 == 0) radius else innerRadius
                    val a = (i * Math.PI / numPoints).toFloat()
                    val x = cx + r * kotlin.math.cos(a)
                    val y = cy + r * kotlin.math.sin(a)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path = path, brush = brush, style = Stroke(width = 2.5.dp.toPx()))
            }
            GeometricShapeType.Hexagon -> {
                val path = Path()
                for (i in 0 until 6) {
                    val a = (i * Math.PI / 3).toFloat()
                    val x = cx + radius * kotlin.math.cos(a)
                    val y = cy + radius * kotlin.math.sin(a)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path = path, brush = brush, style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}

@Composable
fun WaveformAudioIcon(
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF0F172A)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val heights = listOf(10.dp, 18.dp, 12.dp, 16.dp)
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(h)
                    .background(barColor, RoundedCornerShape(2.dp))
            )
        }
    }
}
