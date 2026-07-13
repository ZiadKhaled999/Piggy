package com.oryno.piggy_ledger.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.AccountType
import com.oryno.piggy_ledger.ui.theme.*
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: PiggyLedgerViewModel,
    onNavigateToCreateGoal: () -> Unit,
    onNavigateToMyGoals: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToAnalytics: () -> Unit
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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F6F9))) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                // Header Profile
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
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
                    
                    Box(
                        modifier = Modifier
                            .size(52.dp)
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
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            if (pendingTransactions.isNotEmpty()) {
                val firstPending = pendingTransactions.first()
                item {
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
                                Text("Spent", color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
                                Text("Payoffs", color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
                    Text("Savings Goals", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    Text("See all", fontSize = 14.sp, color = PinkPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onNavigateToMyGoals() }.padding(4.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                GoalsHorizontalList(goals = goals, transactions = transactions, onClick = onNavigateToMyGoals)
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
                    Text(
                        text = stringResource(R.string.auth_profile_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(PinkPrimary.copy(alpha = 0.15f)),
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
                            val initial = userFullName.take(1)
                            Text(
                                text = initial.uppercase(),
                                color = PinkPrimary,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = userFullName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                    Text(
                        text = userEmail,
                        fontSize = 14.sp,
                        color = TextLight,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(R.string.auth_welcome_subtitle), fontSize = 13.sp, color = NavyDark.copy(alpha = 0.8f))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            showProfileBottomSheet = false
                            viewModel.signOut()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color.White)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.auth_sign_out), fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📥",
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.pending_sms_detected),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
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
                Text("No goals set yet. Tap to start saving!", color = TextLight, fontSize = 14.sp)
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            goals.take(2).forEach { goal ->
                val saved = transactions.filter { it.goalId == goal.id }.sumOf { it.amount }
                val progress = if (goal.targetAmount > 0) (saved / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                
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
                            Text(text = "$${String.format("%.0f", saved)} / $${String.format("%.0f", goal.targetAmount)}", color = TextLight, fontSize = 12.sp)
                        }
                        
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text(text = "${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = PinkPrimary, fontSize = 12.sp)
                            }
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
