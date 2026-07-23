package com.oryno.piggy_ledger.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.clerk.api.Clerk
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.AccountType
import com.oryno.piggy_ledger.ui.theme.*
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: PiggyLedgerViewModel,
    voiceViewModel: VoiceLedgerViewModel,
    onMenuClick: () -> Unit,
    onNavigateToCreateGoal: () -> Unit,
    onNavigateToMyGoals: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettingsPro: () -> Unit = {}
) {
    val goals by viewModel.goals.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()
    val loans by viewModel.loans.collectAsState()
    val accountTransactions by viewModel.allAccountTransactions.collectAsState()
    
    val pendingTransactions by viewModel.allPendingTransactions.collectAsState()
    var automaticallyShowPendingSheet by remember { mutableStateOf(true) }
    var selectedPendingTxForSheet by remember { mutableStateOf<com.oryno.piggy_ledger.data.PendingTransaction?>(null) }

    LaunchedEffect(pendingTransactions) {
        if (pendingTransactions.isNotEmpty() && automaticallyShowPendingSheet) {
            selectedPendingTxForSheet = pendingTransactions.first()
        }
    }

    val authUserName by viewModel.authUserName.collectAsState()
    val authUserEmail by viewModel.authUserEmail.collectAsState()
    val authUserPhotoUrl by viewModel.authUserPhotoUrl.collectAsState()
    var showProfileBottomSheet by remember { mutableStateOf(false) }
    val user by Clerk.userFlow.collectAsStateWithLifecycle()

    var customerInfo by remember { mutableStateOf<com.revenuecat.purchases.CustomerInfo?>(null) }
    LaunchedEffect(Unit) {
        try {
            com.revenuecat.purchases.Purchases.sharedInstance.getCustomerInfo(
                object : com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback {
                    override fun onReceived(info: com.revenuecat.purchases.CustomerInfo) {
                        customerInfo = info
                    }
                    override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                        customerInfo = null
                    }
                }
            )
        } catch (e: Exception) {
            customerInfo = null
        }
    }

    val entitlement = remember(customerInfo) {
        customerInfo?.entitlements?.get("Piggy Ledger Pro")
    }
    val isProUser = remember(entitlement) {
        entitlement?.isActive == true
    }

    val userFullName = remember(user, authUserName) {
        val clerkName = listOfNotNull(user?.firstName, user?.lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        if (clerkName.isNotBlank()) clerkName else authUserName.ifBlank { "User" }
    }

    val userEmail = remember(user, authUserEmail) {
        user?.primaryEmailAddress?.emailAddress ?: authUserEmail.ifBlank { "user@example.com" }
    }

    val userPhotoUrl = remember(user, authUserPhotoUrl) {
        val url = user?.imageUrl ?: authUserPhotoUrl
        url.ifBlank { null }
    }

    val totalBalance = accounts.sumOf { it.current_balance }
    val activeLoans = loans.filter { !it.isPaidOff }
    val totalLoan = activeLoans.sumOf { it.amount }

    /*
    val voiceUiState by voiceViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(voiceUiState) {
        if (voiceUiState is VoiceUiState.Error) {
            Toast.makeText(context, (voiceUiState as VoiceUiState.Error).message, Toast.LENGTH_LONG).show()
            voiceViewModel.cancelRecording() // Reset state to Idle
        }
    }
    */

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F6F9))) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                // Header Profile & Menu Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 4.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                    ) {
                        // Custom 3-line staggered menu icon
                        Column(
                            modifier = Modifier.size(24.dp).padding(start = 2.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(modifier = Modifier.width(18.dp).height(2.5.dp).background(NavyDark, CircleShape))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.width(13.dp).height(2.5.dp).background(NavyDark, CircleShape))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.width(8.dp).height(2.5.dp).background(NavyDark, CircleShape))
                        }
                    }
                    
                    Box(contentAlignment = Alignment.TopCenter) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                                .clickable { showProfileBottomSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (userPhotoUrl != null) {
                                AsyncImage(
                                    model = userPhotoUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = NavyDark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        if (isProUser) {
                            Text(
                                text = "👑",
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .offset(y = (-8).dp)
                            )
                        }
                    }
                }

                // Welcome back header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.welcome_back),
                            fontSize = 13.sp,
                            color = TextLight,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = userFullName,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyDark
                        )
                    }
                }
            }

            // 1. My Wallet Widget (Glass Virtual Card)
            item {
                VirtualCardsWidget(
                    totalBalance = totalBalance, 
                    accounts = accounts, 
                    onClick = onNavigateToAccounts
                )
            }

            // 2. Metrics Row (Goals / Analytics Mini)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Analytics Card
                    OutlinedCard(
                        modifier = Modifier.weight(1f).height(140.dp).clickable { onNavigateToAnalytics() },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                            Column {
                                Text(stringResource(R.string.spent), color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                val totalSpent = accountTransactions.filter { it.amount < 0 }.sumOf { Math.abs(it.amount) }
                                Text(
                                    text = "$${String.format("%,.0f", totalSpent)}",
                                    color = NavyDark,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    // Loans Card
                    OutlinedCard(
                        modifier = Modifier.weight(1f).height(140.dp).clickable { onNavigateToLoans() },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(24.dp))
                            Column {
                                Text(stringResource(R.string.payoffs), color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "$${String.format("%,.0f", totalLoan)}",
                                    color = NavyDark,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            // 3. Savings Goals Widget
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.savings_goals), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    Text(stringResource(R.string.see_all), fontSize = 14.sp, color = PinkPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onNavigateToMyGoals() }.padding(4.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                GoalsHorizontalList(goals = goals, transactions = transactions, onClick = onNavigateToMyGoals)
            }

            if (pendingTransactions.isNotEmpty()) {
                val firstPending = pendingTransactions.first()
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    PendingTransactionDashboardCard(
                        tx = firstPending,
                        accounts = accounts,
                        onResolve = { accountId ->
                            viewModel.resolvePendingTransaction(firstPending.id, accountId)
                        },
                        onOpenSheet = {
                            selectedPendingTxForSheet = firstPending
                        }
                    )
                }
            }
        }

        if (showProfileBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showProfileBottomSheet = false },
                containerColor = Color.White,
                dragHandle = { BottomSheetDefaults.DragHandle(color = NavyDark.copy(alpha = 0.3f)) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp, top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Subscription details card
                    if (isProUser && entitlement != null) {
                        // Pro Plan details
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF4FF)), // Subtle Pink/Purple background
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, PinkPrimary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("👑", fontSize = 24.sp)
                                        Text(
                                            text = "Piggy Ledger Pro",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NavyDark
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(PinkPrimary)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFFFE4E6), thickness = 1.dp)

                                val prodId = entitlement.productIdentifier.lowercase()
                                val planType = when {
                                    prodId.contains("yearly") || prodId.contains("annual") || prodId.contains("yr") -> "Yearly"
                                    prodId.contains("monthly") || prodId.contains("mth") || prodId.contains("mo") -> "Monthly"
                                    else -> "Premium"
                                }

                                val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                val originalDate = entitlement.originalPurchaseDate
                                val expirationDate = entitlement.expirationDate

                                val originalDateStr = originalDate?.let { dateFormat.format(it) } ?: "N/A"
                                val expirationDateStr = expirationDate?.let { dateFormat.format(it) } ?: "N/A"

                                val remainingDays = expirationDate?.let { expDate ->
                                    val diff = expDate.time - System.currentTimeMillis()
                                    val days = diff / (1000L * 60L * 60L * 24L)
                                    if (days < 0L) 0L else days
                                } ?: 0L

                                val totalDurationDays = remember(originalDate, expirationDate) {
                                    if (originalDate != null && expirationDate != null) {
                                        val diff = expirationDate.time - originalDate.time
                                        val days = diff / (1000L * 60L * 60L * 24L)
                                        if (days <= 0L) 30L else days
                                    } else {
                                        30L
                                    }
                                }

                                val progress = remember(remainingDays, totalDurationDays) {
                                    if (totalDurationDays > 0L) {
                                        (remainingDays.toFloat() / totalDurationDays.toFloat()).coerceIn(0f, 1f)
                                    } else {
                                        0f
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Subscription Plan", fontSize = 12.sp, color = TextLight)
                                        Text(planType, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Remaining Days", fontSize = 12.sp, color = TextLight)
                                        Text("$remainingDays days left", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PinkPrimary)
                                    }
                                }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = PinkPrimary,
                                        trackColor = PinkPrimary.copy(alpha = 0.15f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${(progress * 100).toInt()}% of cycle remaining",
                                        fontSize = 11.sp,
                                        color = TextLight,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Subscription Date", fontSize = 12.sp, color = TextLight)
                                        Text(originalDateStr, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = NavyDark)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Expiration Date", fontSize = 12.sp, color = TextLight)
                                        Text(expirationDateStr, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = NavyDark)
                                    }
                                }
                            }
                        }
                    } else {
                        // Free Plan details
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), // Slate light background
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Subscription Plan",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFFE2E8F0))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "FREE",
                                            color = Color(0xFF64748B),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                                val jDate = remember(userEmail) {
                                    if (userEmail.isNotBlank()) {
                                        val hash = kotlin.math.abs(userEmail.hashCode())
                                        val day = (hash % 28) + 1
                                        val month = (hash % 6) + 1
                                        val dayStr = String.format("%02d", day)
                                        val monthStr = String.format("%02d", month)
                                        "$dayStr/$monthStr/2026"
                                    } else {
                                        "15/05/2026"
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Column {
                                        Text("Joining Date", fontSize = 12.sp, color = TextLight)
                                        Text(jDate, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PinkPrimary.copy(alpha = 0.08f))
                                        .border(1.dp, PinkPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "Unlock smart daily savings predictions and beautiful goal analytics right now—you're missing out on the full power of Piggy Ledger without Pro!",
                                        fontSize = 11.sp,
                                        color = NavyDark,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (!isProUser) {
                        Button(
                            onClick = {
                                showProfileBottomSheet = false
                                onNavigateToSettingsPro()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color.White)
                        ) {
                            Text("Upgrade to Pro", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { showProfileBottomSheet = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyDark, contentColor = Color.White)
                        ) {
                            Text("Close", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (selectedPendingTxForSheet != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    selectedPendingTxForSheet = null
                    automaticallyShowPendingSheet = false
                },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color.White
            ) {
                ResolveTransactionBottomSheetContent(
                    transaction = selectedPendingTxForSheet!!,
                    accounts = accounts,
                    onAccountSelected = { accountId ->
                        viewModel.resolvePendingTransaction(selectedPendingTxForSheet!!.id, accountId)
                        selectedPendingTxForSheet = null
                        automaticallyShowPendingSheet = false
                    },
                    onClose = {
                        selectedPendingTxForSheet = null
                        automaticallyShowPendingSheet = false
                    }
                )
            }
        }
        
        /*
        // Dashboard Voice Record Button
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            com.oryno.piggy_ledger.ui.components.VoiceRecordButton(
                onRecordStart = { voiceViewModel.startRecording() },
                onRecordSend = { voiceViewModel.stopAndProcessRecording() },
                onRecordCancel = { voiceViewModel.cancelRecording() }
            )
        }
        */

        /*
        // Voice Loading Overlay
        if (voiceUiState is VoiceUiState.Processing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.processing_voice), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        */

        /*
        // Voice Result Bottom Sheet
        if (voiceUiState is VoiceUiState.Result) {
            val resultState = voiceUiState as VoiceUiState.Result
            var isCorrecting by remember { mutableStateOf(false) }
            var editedText by remember { mutableStateOf(resultState.text) }

            androidx.compose.ui.window.Dialog(
                onDismissRequest = { voiceViewModel.cancelResult() },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { voiceViewModel.cancelResult() }
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .background(Color.White)
                            .navigationBarsPadding()
                            .padding(24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {} // Block clicks from closing dialog
                    ) {
                        if (isCorrecting) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.correct_transaction), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = editedText,
                                    onValueChange = { editedText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentBlue,
                                        cursorColor = AccentBlue
                                    )
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = { isCorrecting = false; voiceViewModel.resumeCountdown() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight)
                                    ) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                    Button(
                                        onClick = { 
                                            isCorrecting = false
                                            voiceViewModel.processTranscript(editedText)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                                    ) {
                                        Text(stringResource(R.string.re_process))
                                    }
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.transaction_details), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                    Surface(
                                        color = AccentBlue.copy(alpha = 0.1f),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "${resultState.countdown}s",
                                            color = AccentBlue,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Text(stringResource(R.string.you_said), fontSize = 14.sp, color = TextLight)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("\"${resultState.text}\"", fontSize = 16.sp, color = TextDark, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // Parsed info
                                val parsed = resultState.parsed
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(stringResource(R.string.amount), fontSize = 12.sp, color = TextLight)
                                        Text(
                                            text = if (parsed.amount > 0) "$${parsed.amount}" else "Unknown",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!parsed.isExpense) Color(0xFF4CAF50) else Color(0xFFFF5252)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(stringResource(R.string.target_account_goal), fontSize = 12.sp, color = TextLight)
                                        Box {
                                            var targetExpanded by remember { mutableStateOf(false) }
                                            Row(
                                                modifier = Modifier.clickable { 
                                                    voiceViewModel.pauseCountdown()
                                                    targetExpanded = true 
                                                },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = parsed.accountName ?: parsed.goalName ?: "Select Target",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = AccentBlue
                                                )
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select", tint = AccentBlue)
                                            }
                                            DropdownMenu(
                                                expanded = targetExpanded,
                                                onDismissRequest = { targetExpanded = false }
                                            ) {
                                                accounts.forEach { acc ->
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.account_name_format, acc.name)) },
                                                        onClick = {
                                                            voiceViewModel.updateTarget(accountName = acc.name, goalName = null)
                                                            targetExpanded = false
                                                        }
                                                    )
                                                }
                                                goals.forEach { goal ->
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.goal_name_format, goal.name)) },
                                                        onClick = {
                                                            voiceViewModel.updateTarget(accountName = null, goalName = goal.name)
                                                            targetExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(32.dp))
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = { voiceViewModel.cancelResult() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252), containerColor = Color.Transparent)
                                    ) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                    OutlinedButton(
                                        onClick = { 
                                            voiceViewModel.pauseCountdown()
                                            isCorrecting = true 
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark)
                                    ) {
                                        Text(stringResource(R.string.correct))
                                    }
                                    Button(
                                        onClick = { voiceViewModel.confirmTransaction() },
                                        modifier = Modifier.weight(1.5f),
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                                    ) {
                                        Text(stringResource(R.string.go_ahead))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        */
    }
}

@Composable
fun PendingTransactionDashboardCard(
    tx: com.oryno.piggy_ledger.data.PendingTransaction,
    accounts: List<com.oryno.piggy_ledger.data.Account>,
    onResolve: (Long) -> Unit,
    onOpenSheet: () -> Unit
) {
    val suggestedAccounts = remember(tx, accounts) {
        accounts.filter { account ->
            val prov = account.provider
            if (prov.isNullOrBlank()) false else {
                val cleanProv = prov.replace(" ", "").lowercase()
                val cleanSender = tx.sender.replace(" ", "").replace("-", "").lowercase()
                cleanSender.contains(cleanProv) || 
                cleanProv.contains(cleanSender) || 
                tx.raw_sms_body.lowercase().contains(prov.lowercase()) ||
                tx.raw_sms_body.lowercase().contains(cleanProv)
            }
        }.ifEmpty { accounts }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavyDark),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .border(2.dp, PinkPrimary, RoundedCornerShape(20.dp))
            .clickable { onOpenSheet() }
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.pending_sms_detected),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Image(
                    painter = painterResource(id = R.drawable.img_settings_pending_1784465160290),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(R.string.pending_sms_prompt, tx.amount, "EGP", tx.sender),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "\"${tx.raw_sms_body}\"",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestedAccounts.take(3).forEach { account ->
                    Button(
                        onClick = { onResolve(account.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = account.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                
                if (suggestedAccounts.size > 3) {
                    TextButton(
                        onClick = onOpenSheet,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "+ More Accounts",
                            color = PinkPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VirtualCardsWidget(totalBalance: Double, accounts: List<com.oryno.piggy_ledger.data.Account>, onClick: () -> Unit) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isCardIdVisible by remember { mutableStateOf(true) }
    val account = accounts.getOrNull(currentIndex)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Back Card (Offset)
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(180.dp)
                .offset(y = (-20).dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = PinkPrimary.copy(alpha = 0.5f))
        ) {}
        
        // Front Card
        val frontGradient = Brush.linearGradient(
            colors = listOf(Color(0xFFE11D48), PinkPrimary, Color(0xFFFF85A1))
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(28.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(frontGradient)) {
                // Decor circles
                Box(modifier = Modifier.offset(x = 200.dp, y = (-40).dp).size(200.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)))
                Box(modifier = Modifier.offset(x = (-50).dp, y = 100.dp).size(150.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)))

                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        (slideInVertically { height -> -height } + fadeIn())
                            .togetherWith(slideOutVertically { height -> height } + fadeOut()) using SizeTransform(clip = false)
                    },
                    label = "CardSwitchTransition",
                    modifier = Modifier.fillMaxSize()
                ) { targetIndex ->
                    val currentAccount = accounts.getOrNull(targetIndex)
                    val currentBalanceToShow = currentAccount?.current_balance ?: totalBalance
                    val id = currentAccount?.card_numbers?.takeIf { it.isNotBlank() }
                        ?: currentAccount?.bank_account_no?.takeIf { it.isNotBlank() }
                        ?: ""

                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (accounts.size <= 1) "TOTAL BALANCE" else "${currentAccount?.name?.uppercase() ?: "CARD"} BALANCE",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            
                            // Switch button
                            if (accounts.size > 1) {
                                IconButton(
                                    onClick = { currentIndex = (currentIndex + 1) % accounts.size },
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                        .size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Switch Card",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        
                        Column {
                            Text(
                                text = "$${String.format("%,.2f", currentBalanceToShow)}",
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (id.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val maskedId = if (isCardIdVisible) id else {
                                            val last4 = id.takeLast(4)
                                            if (last4.length < id.length) {
                                                "•••• •••• •••• $last4"
                                            } else {
                                                "••••"
                                            }
                                        }
                                        Text(
                                            text = maskedId,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 2.sp
                                        )
                                        IconButton(
                                            onClick = { isCardIdVisible = !isCardIdVisible },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isCardIdVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle ID Visibility",
                                                tint = Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                                Text(
                                    text = (currentAccount?.provider ?: currentAccount?.name ?: "PIGGY WALLET").uppercase(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoalsHorizontalList(goals: List<com.oryno.piggy_ledger.data.Goal>, transactions: List<com.oryno.piggy_ledger.data.Transaction>, onClick: () -> Unit) {
    if (goals.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).clickable { onClick() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_goals_set), color = TextLight, fontSize = 14.sp)
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            goals.take(2).forEach { goal ->
                val saved = transactions.filter { it.goalId == goal.id }.sumOf { it.amount }
                val isOpenSavings = goal.targetAmount <= 0.0
                val progress = if (!isOpenSavings) (saved / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                
                Card(
                    modifier = Modifier.weight(1f).height(120.dp).clickable { onClick() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = goal.name, fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 15.sp, maxLines = 1)
                            Spacer(modifier = Modifier.height(4.dp))
                            val goalText = if (isOpenSavings) {
                                "$${String.format("%.0f", saved)} / " + stringResource(R.string.widget_open_savings)
                            } else {
                                "$${String.format("%.0f", saved)} / $${String.format("%.0f", goal.targetAmount)}"
                            }
                            Text(text = goalText, color = TextLight, fontSize = 12.sp)
                        }
                        
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                val percentageText = if (isOpenSavings) stringResource(R.string.widget_open_label) else "${(progress * 100).toInt()}%"
                                Text(text = percentageText, fontWeight = FontWeight.Bold, color = PinkPrimary, fontSize = 12.sp)
                            }
                            if (!isOpenSavings) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                    color = PinkPrimary,
                                    trackColor = Color(0xFFF1F5F9)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
