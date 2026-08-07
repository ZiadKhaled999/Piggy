package com.oryno.piggy_ledger.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
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
    onMenuClick: () -> Unit,
    onNavigateToCreateGoal: () -> Unit,
    onNavigateToMyGoals: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettingsPro: () -> Unit = {},
    onNavigateToStreak: () -> Unit = {}
) {
    val context = LocalContext.current
    val streakCount = remember { com.oryno.piggy_ledger.data.StreakManager.getStreak(context) }
    val goals by viewModel.goals.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()
    val loans by viewModel.loans.collectAsState()
    val accountTransactions by viewModel.allAccountTransactions.collectAsState()
    
    val pendingTransactions by viewModel.allPendingTransactions.collectAsState()
    val isPrivacyMode by viewModel.isPrivacyModeEnabled.collectAsState()
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
    val profileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val user by Clerk.userFlow.collectAsStateWithLifecycle()

    val isPremiumState by viewModel.isPremium.collectAsStateWithLifecycle()
    var customerInfo by remember { mutableStateOf<com.revenuecat.purchases.CustomerInfo?>(null) }
    
    LaunchedEffect(isPremiumState, showProfileBottomSheet) {
        if (showProfileBottomSheet || customerInfo == null) {
            try {
                if (com.revenuecat.purchases.Purchases.isConfigured) {
                    com.revenuecat.purchases.Purchases.sharedInstance.getCustomerInfo(
                        object : com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback {
                            override fun onReceived(info: com.revenuecat.purchases.CustomerInfo) {
                                customerInfo = info
                                val active = info.entitlements.all.values.any { it.isActive } || info.entitlements["Piggy Ledger Pro"]?.isActive == true
                                if (active != isPremiumState) {
                                    viewModel.setPremiumStatus(active)
                                }
                            }
                            override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                                // Optionally ignore
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    val entitlement = remember(customerInfo) {
        customerInfo?.entitlements?.active?.values?.firstOrNull() ?: customerInfo?.entitlements?.get("Piggy Ledger Pro")
    }
    val isProUser = isPremiumState

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
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StreakBadgePill(
                            streakCount = streakCount,
                            onClick = onNavigateToStreak
                        )

                        PremiumAvatar(
                            imageUrl = userPhotoUrl,
                            isPro = isProUser,
                            size = 44.dp,
                            onClick = { showProfileBottomSheet = true }
                        )
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
                    onClick = onNavigateToAccounts,
                    isPrivacyMode = isPrivacyMode,
                    onTogglePrivacy = { viewModel.togglePrivacyMode(context) },
                    onUpdateAccountColor = { acc, newColorHex ->
                        if (acc != null) {
                            viewModel.updateAccount(acc.copy(icon_color = newColorHex, updatedAt = System.currentTimeMillis()))
                        } else {
                            val defaultAccount = com.oryno.piggy_ledger.data.Account(
                                name = "Main Wallet",
                                type = com.oryno.piggy_ledger.data.AccountType.CASH,
                                icon_color = newColorHex,
                                currency = "EGP",
                                starting_balance = 0.0,
                                current_balance = totalBalance
                            )
                            viewModel.addAccount(defaultAccount)
                        }
                    }
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
                                    text = if (isPrivacyMode) "$ ••••••" else "$${String.format("%,.0f", totalSpent)}",
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
                                    text = if (isPrivacyMode) "$ ••••••" else "$${String.format("%,.0f", totalLoan)}",
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
                GoalsHorizontalList(goals = goals, transactions = transactions, onClick = onNavigateToMyGoals, isPrivacyMode = isPrivacyMode)
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
                sheetState = profileSheetState,
                containerColor = Color.White,
                dragHandle = { BottomSheetDefaults.DragHandle(color = NavyDark.copy(alpha = 0.2f)) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 40.dp, top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 2. Subscription Details Card
                    if (isProUser) {
                        val originalDate = entitlement?.originalPurchaseDate
                        val latestDate = entitlement?.latestPurchaseDate
                        val expirationDate = entitlement?.expirationDate

                        val prodId = entitlement?.productIdentifier?.lowercase() ?: "pro"
                        val planType = when {
                            prodId.contains("lifetime") || prodId.contains("life") || prodId.contains("lt") || expirationDate == null -> "Piggy Ledger Pro"
                            prodId.contains("yearly") || prodId.contains("annual") || prodId.contains("yr") -> "Pro (Yearly)"
                            prodId.contains("monthly") || prodId.contains("mth") || prodId.contains("mo") -> "Pro (Monthly)"
                            else -> "Piggy Ledger Pro"
                        }

                        val isLifetime = planType.contains("Lifetime")

                        val isVeryShortCycle = remember(originalDate, latestDate, expirationDate) {
                            val start = latestDate ?: originalDate ?: java.util.Date()
                            val end = expirationDate
                            end != null && (end.time - start.time < 24L * 60L * 60L * 1000L)
                        }

                        val dateFormat = remember(isVeryShortCycle) {
                            if (isVeryShortCycle) {
                                java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
                            } else {
                                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                            }
                        }

                        val latestDateStr = remember(latestDate, originalDate, dateFormat) { 
                            val d = latestDate ?: originalDate
                            d?.let { dateFormat.format(it) } ?: "N/A" 
                        }
                        val expirationDateStr = remember(expirationDate, dateFormat) { expirationDate?.let { dateFormat.format(it) } ?: "N/A" }

                        val remainingTimeStr = remember(expirationDate) {
                            expirationDate?.let { expDate ->
                                val diffMs = expDate.time - System.currentTimeMillis()
                                when {
                                    diffMs <= 0 -> "Expired"
                                    diffMs >= 24L * 60L * 60L * 1000L -> "${diffMs / (24L * 60L * 60L * 1000L)} days left"
                                    diffMs >= 60L * 60L * 1000L -> "${diffMs / (60L * 60L * 1000L)} hours left"
                                    else -> "${diffMs / (60L * 1000L)} minutes left"
                                }
                            } ?: "N/A"
                        }

                        val progress = remember(latestDate, originalDate, expirationDate) {
                            val start = latestDate ?: originalDate
                            if (start != null && expirationDate != null) {
                                val totalMs = expirationDate.time - start.time
                                val remainingMs = expirationDate.time - System.currentTimeMillis()
                                if (totalMs > 0L) {
                                    (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                            } else {
                                0f
                            }
                        }

                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F6)), // Light coral pink tint
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD1D5)),
                            shape = RoundedCornerShape(20.dp)
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = PinkPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Piggy Ledger Pro",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NavyDark
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(PinkPrimary)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFFFD1D5), thickness = 1.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Subscription Plan", fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(planType, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                    }
                                    if (!isLifetime) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Remaining Time", fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.SemiBold)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(remainingTimeStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PinkPrimary)
                                        }
                                    } else {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Status", fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.SemiBold)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("Lifetime Access", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PinkPrimary)
                                        }
                                    }
                                }

                                if (!isLifetime) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(CircleShape),
                                            color = PinkPrimary,
                                            trackColor = PinkPrimary.copy(alpha = 0.12f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${(progress * 100).toInt()}% of billing period remaining",
                                            fontSize = 11.sp,
                                            color = TextLight,
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Subscription Date", fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(latestDateStr, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = NavyDark)
                                    }
                                    if (!isLifetime) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Expiration Date", fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.SemiBold)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(expirationDateStr, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = NavyDark)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Standard Free Plan
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), // Slate light background
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(20.dp)
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.StarBorder,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Standard Membership",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NavyDark
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFE2E8F0))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "FREE",
                                            color = Color(0xFF64748B),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Plan Tier",
                                        fontSize = 11.sp,
                                        color = TextLight,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Free Edition (Standard Ledger)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark
                                    )
                                }
                                
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Features Enabled",
                                        fontSize = 11.sp,
                                        color = TextLight,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "✓ Basic ledger entry & local savings tracking\n✓ Offline data isolation & device security",
                                        fontSize = 13.sp,
                                        color = NavyDark,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Buttons
                    if (!isProUser) {
                        Button(
                            onClick = {
                                showProfileBottomSheet = false
                                onNavigateToSettingsPro()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upgrade to Premium Pro", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                showProfileBottomSheet = false
                                onNavigateToSettingsPro() // Brings up billing / customer center options
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manage Plan & Billing", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        TextButton(
                            onClick = { showProfileBottomSheet = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Close", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextLight)
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
    onResolve: (String) -> Unit,
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
fun VirtualCardsWidget(
    totalBalance: Double, 
    accounts: List<com.oryno.piggy_ledger.data.Account>, 
    onClick: () -> Unit,
    isPrivacyMode: Boolean = false,
    onTogglePrivacy: () -> Unit = {},
    onUpdateAccountColor: (com.oryno.piggy_ledger.data.Account?, String) -> Unit = { _, _ -> }
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isCardIdVisible by remember { mutableStateOf(true) }
    var showColorPickerSheet by remember { mutableStateOf(false) }

    val safeIndex = if (accounts.isNotEmpty()) currentIndex % accounts.size else 0
    val activeAccount = accounts.getOrNull(safeIndex)

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
            colors = CardDefaults.cardColors(containerColor = parseHexColor(activeAccount?.icon_color, PinkPrimary).copy(alpha = 0.5f))
        ) {}
        
        // Front Card
        val frontGradient = getAccountGradient(activeAccount?.icon_color)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(frontGradient)) {
                // Decor circles
                Box(modifier = Modifier.offset(x = 200.dp, y = (-40).dp).size(200.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)))
                Box(modifier = Modifier.offset(x = (-50).dp, y = 100.dp).size(150.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)))

                AnimatedContent(
                    targetState = Pair(safeIndex, activeAccount?.icon_color),
                    transitionSpec = {
                        (slideInVertically { height -> -height } + fadeIn())
                            .togetherWith(slideOutVertically { height -> height } + fadeOut()) using SizeTransform(clip = false)
                    },
                    label = "CardSwitchTransition",
                    modifier = Modifier.fillMaxSize()
                ) { (targetIndex, _) ->
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
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Privacy Toggle (Eyeball) Button
                                IconButton(
                                    onClick = onTogglePrivacy,
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                        .size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Privacy Mode",
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                // Brush Button (ALWAYS visible for 1 account or more)
                                IconButton(
                                    onClick = { showColorPickerSheet = true },
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                        .size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Brush,
                                        contentDescription = "Customize Card Color",
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                // Switch button (if accounts.size > 1)
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
                        }
                        
                        Column {
                            Text(
                                text = if (isPrivacyMode) "$••••••" else "$${String.format("%,.2f", currentBalanceToShow)}",
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

    if (showColorPickerSheet) {
        CardColorPickerBottomSheet(
            account = activeAccount,
            onDismiss = { showColorPickerSheet = false },
            onSelectColor = { selectedColorHex ->
                onUpdateAccountColor(activeAccount, selectedColorHex)
                showColorPickerSheet = false
            }
        )
    }
}

@Composable
fun GoalsHorizontalList(
    goals: List<com.oryno.piggy_ledger.data.Goal>, 
    transactions: List<com.oryno.piggy_ledger.data.Transaction>, 
    onClick: () -> Unit,
    isPrivacyMode: Boolean = false
) {
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
                            val goalText = if (isPrivacyMode) {
                                "$••••••"
                            } else if (isOpenSavings) {
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

@Composable
fun PremiumAvatar(
    imageUrl: String?,
    isPro: Boolean,
    size: Dp = 44.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size + 14.dp)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.White)
                .let {
                    if (isPro) {
                        it.border(2.5.dp, Color(0xFFFBBF24), CircleShape)
                    } else {
                        it.border(1.dp, Color(0xFFE2E8F0), CircleShape)
                    }
                }
                .padding(if (isPro) 3.dp else 0.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = NavyDark,
                        modifier = Modifier.size(size * 0.55f)
                    )
                }
            }
        }

        if (isPro) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = (-2).dp)
                    .rotate(22f)
                    .size((size * 0.45f).coerceAtLeast(18.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    val path = Path().apply {
                        moveTo(w * 0.1f, h * 0.85f)
                        lineTo(w * 0.1f, h * 0.35f)
                        lineTo(w * 0.35f, h * 0.6f)
                        lineTo(w * 0.5f, h * 0.15f)
                        lineTo(w * 0.65f, h * 0.6f)
                        lineTo(w * 0.9f, h * 0.35f)
                        lineTo(w * 0.9f, h * 0.85f)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFFFBBF24)
                    )
                    val radius = w * 0.1f
                    drawCircle(color = Color(0xFFFBBF24), radius = radius, center = Offset(w * 0.1f, h * 0.35f))
                    drawCircle(color = Color(0xFFFBBF24), radius = radius, center = Offset(w * 0.5f, h * 0.15f))
                    drawCircle(color = Color(0xFFFBBF24), radius = radius, center = Offset(w * 0.9f, h * 0.35f))
                }
            }
        }
    }
}

@Composable
fun StreakBadgePill(
    streakCount: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.3f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(40.dp)
        ) {
            // Flame section (opacity 0 background)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(Color.Transparent)
                    .padding(start = 10.dp, end = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.streak),
                    contentDescription = "Streak",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Number section (opacity 0.5 background)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(Color(0xFFFFF7ED).copy(alpha = 0.5f))
                    .padding(start = 6.dp, end = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$streakCount",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFC2410C)
                )
            }
        }
    }
}
