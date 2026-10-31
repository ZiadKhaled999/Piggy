package com.oryno.piggy_ledger.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
import kotlin.math.abs
import coil.compose.AsyncImage
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.data.AccountTransaction
import com.oryno.piggy_ledger.data.AccountType
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: PiggyLedgerViewModel,
    onNavigateToAddAccount: () -> Unit,
    onNavigateToEditAccount: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToSettingsPro: () -> Unit = {},
    onNavigateToTransfer: () -> Unit = {},
    onNavigateToAllTransactions: (String?) -> Unit = {}
) {
    val accounts by viewModel.userAccounts.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()
    val allTransactions by viewModel.allAccountTransactions.collectAsState()
    val allSubscriptions by viewModel.allSubscriptions.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()

    var showAccountSwitcher by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }

    // Month filter state
    val calendar = remember { Calendar.getInstance() }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var showMonthBottomSheet by remember { mutableStateOf(false) }

    // Balance obfuscation bound to global privacy mode
    val isPrivacyMode by viewModel.isPrivacyModeEnabled.collectAsState()
    val isBalanceVisible = !isPrivacyMode

    val monthNames = listOf(
        stringResource(R.string.month_january),
        stringResource(R.string.month_february),
        stringResource(R.string.month_march),
        stringResource(R.string.month_april),
        stringResource(R.string.month_may),
        stringResource(R.string.month_june),
        stringResource(R.string.month_july),
        stringResource(R.string.month_august),
        stringResource(R.string.month_september),
        stringResource(R.string.month_october),
        stringResource(R.string.month_november),
        stringResource(R.string.month_december)
    )

    // Calculate month boundary timestamps
    val monthStart = remember(selectedMonth, selectedYear) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val monthEnd = remember(selectedMonth, selectedYear) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1)
        }.timeInMillis
    }

    // Determine currently selected account
    val selectedAccount = remember(accounts, selectedAccountId) {
        accounts.find { it.id == selectedAccountId }
    }

    val linkedSubsForAccount = remember(allSubscriptions, selectedAccountId) {
        if (selectedAccountId == null) emptyList()
        else allSubscriptions.filter { !it.is_deleted && it.accountId == selectedAccountId && it.isActive }
    }

    var selectedTxForDetails by remember { mutableStateOf<AccountTransaction?>(null) }
    var filterAllTime by remember { mutableStateOf(true) } // Default to true so all transactions are shown and scrollable!

    val excludedAccountIds = remember(accounts) { accounts.filter { it.exclude_from_all }.map { it.id }.toSet() }

    // Filtered transaction list
    val filteredTransactions = remember(allTransactions, selectedAccountId, monthStart, monthEnd, excludedAccountIds) {
        allTransactions.filter { tx ->
            val matchesAccount = if (selectedAccountId == null) !excludedAccountIds.contains(tx.account_id) else (tx.account_id == selectedAccountId)
            val matchesTime = tx.timestamp in monthStart until monthEnd
            matchesAccount && matchesTime
        }
    }

    // List of transactions shown in the list
    val displayedTransactions = remember(allTransactions, selectedAccountId, monthStart, monthEnd, filterAllTime, excludedAccountIds) {
        allTransactions.filter { tx ->
            val matchesAccount = if (selectedAccountId == null) !excludedAccountIds.contains(tx.account_id) else (tx.account_id == selectedAccountId)
            val matchesTime = filterAllTime || (tx.timestamp in monthStart until monthEnd)
            matchesAccount && matchesTime
        }
    }

    // Metrics calculation
    val totalSpent = remember(filteredTransactions) {
        filteredTransactions.filter { it.amount < 0 }.sumOf { -it.amount }
    }

    val totalIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.amount > 0 }.sumOf { it.amount }
    }

    val netSavings = totalIncome - totalSpent
    val appCurrency by viewModel.appCurrency.collectAsState()
    val currencySymbol = selectedAccount?.currency?.let { getCurrencySymbol(it) } ?: getCurrencySymbol(appCurrency)

    val progress = if (monthlyBudget > 0) (totalSpent / monthlyBudget).toFloat().coerceIn(0f, 1f) else 0f

    // New calculations for Redesigned Header
    val lastMonthStart = remember(selectedMonth, selectedYear) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }.timeInMillis
    }
    val lastMonthEnd = monthStart

    val lastMonthSpent = remember(allTransactions, selectedAccountId, lastMonthStart, lastMonthEnd, excludedAccountIds) {
        allTransactions.filter { tx ->
            val matchesAccount = if (selectedAccountId == null) !excludedAccountIds.contains(tx.account_id) else (tx.account_id == selectedAccountId)
            val matchesTime = tx.timestamp in lastMonthStart until lastMonthEnd
            matchesAccount && tx.amount < 0
        }.sumOf { -it.amount }
    }

    val spendingTrend = totalSpent - lastMonthSpent
    val formattedTrendAmount = "$currencySymbol ${String.format(Locale.getDefault(), "%,.0f", kotlin.math.abs(spendingTrend))}"
    val trendSubtitle = when {
        spendingTrend > 0 -> stringResource(R.string.more_than_last_time, formattedTrendAmount)
        spendingTrend < 0 -> stringResource(R.string.less_than_last_time, formattedTrendAmount)
        else -> stringResource(R.string.same_as_last_time)
    }

    // Days in current month and days passed
    val daysPassed = remember(selectedMonth, selectedYear) {
        val now = Calendar.getInstance()
        if (now.get(Calendar.MONTH) == selectedMonth && now.get(Calendar.YEAR) == selectedYear) {
            now.get(Calendar.DAY_OF_MONTH)
        } else {
            31 // If it's a past month, assume full month passed for logic
        }
    }

    // Logic for warning card: 80% consumed or high burn rate
    val showBudgetWarning = progress >= 0.8f || (progress >= 0.5f && daysPassed <= 10)

    Scaffold(
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        if (accounts.isEmpty()) {
            // Beautiful Empty State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_no_goals),
                    contentDescription = null,
                    modifier = Modifier.size(280.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    stringResource(R.string.no_accounts_added),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NavyDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.no_accounts_desc),
                    fontSize = 16.sp,
                    color = TextLight,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(40.dp))
                
                Button(
                    onClick = onNavigateToAddAccount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_account), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 70.dp) // Leave room for bottom bar
            ) {
                val mainColumnScope = this
                // REDESIGNED PREMIUM HEADER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    PinkPrimary.copy(alpha = 0.25f),
                                    Color.White
                                )
                            )
                        )
                        .statusBarsPadding()
                ) {
                    // Background Chart Visualization
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        BackgroundSpendingChart(
                            transactions = filteredTransactions,
                            monthStart = monthStart,
                            monthEnd = monthEnd
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 16.dp, bottom = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Bar: Bank Switcher (Left) & Month Selector (Center)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Account Switcher Button (Now Left)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                                    .clickable { showAccountSwitcher = true }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedAccount != null) {
                                    BrandLogo(
                                        provider = selectedAccount.provider ?: selectedAccount.name,
                                        accountType = selectedAccount.type,
                                        iconColorHex = selectedAccount.icon_color,
                                        logoUrl = selectedAccount.logo_url,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_nav_accounts),
                                        contentDescription = "All Accounts",
                                        tint = PinkPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Month Selector (Center-ish)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(PinkPrimary.copy(alpha = 0.1f))
                                    .clickable { showMonthBottomSheet = true }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${monthNames[selectedMonth]} $selectedYear",
                                    color = PinkPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(18.dp))
                            }

                            // Placeholder for balance/symmetry
                            Box(modifier = Modifier.size(40.dp))
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Main Metrics
                        Text(
                            text = androidx.compose.ui.text.buildAnnotatedString {
                                append(stringResource(R.string.total_spent_in))
                                withStyle(style = androidx.compose.ui.text.SpanStyle(color = PinkPrimary, fontWeight = FontWeight.Black)) {
                                    append(monthNames[selectedMonth])
                                }
                            },
                            color = NavyDark.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = if (isBalanceVisible) "$currencySymbol ${String.format(Locale.getDefault(), "%,.0f", totalSpent)}" else "$currencySymbol ••••••",
                            color = NavyDark,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (spendingTrend <= 0) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (spendingTrend <= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = trendSubtitle,
                                color = TextLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Budget Warning Card (Conditional)
                        if (showBudgetWarning && monthlyBudget > 0) {
                            val leftInBudget = (monthlyBudget - totalSpent).coerceAtLeast(0.0)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = NavyDark),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.size(50.dp),
                                            color = if (progress > 0.9f) Color(0xFFEF4444) else PinkPrimary,
                                            trackColor = Color.White.copy(alpha = 0.1f),
                                            strokeWidth = 5.dp
                                        )
                                        Text(
                                            text = "${(progress * 100).toInt()}%",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column {
                                        Text(
                                            text = "$currencySymbol ${String.format(Locale.getDefault(), "%,.0f", leftInBudget)} " + stringResource(R.string.left_in_budget),
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = when {
                                                progress > 1.0f -> stringResource(R.string.budget_warning_over)
                                                progress > 0.9f -> stringResource(R.string.budget_warning_almost)
                                                daysPassed < 15 && progress > 0.6f -> stringResource(R.string.budget_warning_high_burn)
                                                else -> stringResource(R.string.budget_warning_monitoring)
                                            },
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. MORPHING ACTION BUTTONS CAPSULE BAR (Full Width, before Spending by Category)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 18.dp)
                ) {
                    MorphingActionButtonsBar(
                        filterAllTime = filterAllTime,
                        onToggleFilter = { filterAllTime = !filterAllTime },
                        onNavigateToTransfer = onNavigateToTransfer,
                        onAddTransaction = { showAddTransactionDialog = true }
                    )
                }

                // Linked Subscriptions for selected account/wallet
                if (selectedAccountId != null && linkedSubsForAccount.isNotEmpty()) {
                    val totalSubMonthly = linkedSubsForAccount.sumOf { sub ->
                        when (sub.billingCycle.uppercase()) {
                            "YEARLY" -> sub.amount / 12.0
                            "WEEKLY" -> sub.amount * 4.33
                            else -> sub.amount
                        }
                    }
                    val namesList = linkedSubsForAccount.joinToString(", ") { it.name }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(PinkPrimary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EventRepeat,
                                            contentDescription = null,
                                            tint = PinkPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.sub_linked_subscriptions_title),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PinkPrimary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.active_count, linkedSubsForAccount.size),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PinkPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(
                                    R.string.sub_wallet_commits_format,
                                    "${String.format(Locale.getDefault(), "%,.0f", totalSubMonthly)} ${selectedAccount?.currency ?: ""}",
                                    linkedSubsForAccount.size
                                ),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NavyDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = namesList,
                                fontSize = 12.sp,
                                color = TextLight,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 3. SPENDING BY CATEGORY SECTION
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.spending_by_category),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val categorySpentMap = remember(filteredTransactions) {
                        val map = mutableMapOf<String, Double>()
                        filteredTransactions.filter { it.amount < 0 }.forEach { tx ->
                            val rawKey = if (tx.merchant.contains("|")) {
                                tx.merchant.split("|", limit = 2)[0]
                            } else {
                                tx.merchant
                            }
                            val cleaned = cleanCategoryKey(rawKey)
                            map[cleaned] = (map[cleaned] ?: 0.0) + (-tx.amount)
                        }
                        map
                    }

                    val categoryLimits by viewModel.categoryLimits.collectAsState()
                    var categoryForLimitDialog by remember { mutableStateOf<TransactionCategory?>(null) }

                    val categories = remember { categoriesList.filter { it.key != "transfer" && it.key != "cat_transfer" } }
                    val virtualCount = 30000
                    val initialPage = remember(categories.size) {
                        if (categories.isNotEmpty()) {
                            (virtualCount / 2) - ((virtualCount / 2) % categories.size)
                        } else 0
                    }
                    val pagerState = rememberPagerState(initialPage = initialPage) { virtualCount }

                    LaunchedEffect(pagerState) {
                        while (true) {
                            delay(3500L)
                            if (!pagerState.isScrollInProgress) {
                                pagerState.animateScrollToPage(
                                    page = pagerState.currentPage + 1,
                                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                                )
                            }
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        pageSpacing = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val actualIndex = if (categories.isNotEmpty()) (page % categories.size + categories.size) % categories.size else 0
                        val cat = categories[actualIndex]
                        val cleanedKey = cleanCategoryKey(cat.key)
                        val spent = categorySpentMap[cleanedKey] ?: 0.0
                        val limit = categoryLimits[cleanedKey]
                        val categoryName = resolveCategoryDisplayName(cat.key)

                        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        val scale = 0.96f + (1f - 0.96f) * (1f - abs(pageOffset).coerceIn(0f, 1f))
                        val alpha = 0.85f + (1f - 0.85f) * (1f - abs(pageOffset).coerceIn(0f, 1f))

                        Surface(
                            onClick = { categoryForLimitDialog = cat },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(PinkPrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        tint = PinkPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = categoryName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val limitSubtext = if (limit != null && limit > 0) {
                                        val left = (limit - spent).coerceAtLeast(0.0)
                                        "/ $currencySymbol ${String.format(Locale.getDefault(), "%,.0f", left)} " + stringResource(R.string.left_in_budget)
                                    } else {
                                        stringResource(R.string.no_limit)
                                    }
                                    Text(
                                        text = limitSubtext,
                                        fontSize = 11.sp,
                                        color = TextLight,
                                        maxLines = 1
                                    )
                                }

                                Text(
                                    text = "$currencySymbol ${String.format(Locale.getDefault(), "%,.0f", spent)}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NavyDark
                                )
                            }
                        }
                    }

                    // Spend Limit Bottom Sheet Dialog
                    if (categoryForLimitDialog != null) {
                        val catToEdit = categoryForLimitDialog!!
                        val cleanedKey = cleanCategoryKey(catToEdit.key)
                        val currentLimit = categoryLimits[cleanedKey] ?: 0.0
                        var limitInput by remember { mutableStateOf(if (currentLimit > 0) currentLimit.toString() else "") }

                        ModalBottomSheet(
                            onDismissRequest = { categoryForLimitDialog = null },
                            containerColor = Color.White
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                                    .padding(bottom = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "${stringResource(R.string.set_spend_limit)}: ${resolveCategoryDisplayName(catToEdit.key)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDark
                                )

                                OutlinedTextField(
                                    value = limitInput,
                                    onValueChange = { limitInput = it },
                                    label = { Text("${stringResource(R.string.limit_amount)} ($currencySymbol)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Button(
                                    onClick = {
                                        val newLimit = limitInput.toDoubleOrNull() ?: 0.0
                                        viewModel.setCategoryLimit(cleanedKey, newLimit)
                                        categoryForLimitDialog = null
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                                ) {
                                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4. TRANSACTIONS HEADER TITLE BELOW THE BUTTONS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.recent_transactions),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyDark
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { 
                                onNavigateToAllTransactions(selectedAccountId)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.see_all),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PinkPrimary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = PinkPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // TRANSACTION CONTAINER
                Box(
                    modifier = mainColumnScope.run {
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    }
                ) {
                    if (displayedTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_piggy_track),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (filterAllTime) stringResource(R.string.no_transactions_yet) else stringResource(R.string.no_transactions_this_month),
                                fontWeight = FontWeight.Bold,
                                color = NavyDark,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.no_transactions_desc_sub),
                                color = TextLight,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(displayedTransactions, key = { it.id }) { tx ->
                            val formattedDate = remember(tx.timestamp) {
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(tx.timestamp))
                            }

                            // Parse category and description
                            val isParsed = tx.merchant.contains("|")
                            val (catDisplayName, txIcon, txDesc) = if (isParsed) {
                                val parts = tx.merchant.split("|", limit = 2)
                                val key = parts[0]
                                val desc = parts.getOrNull(1) ?: ""
                                
                                val localizedCatName = resolveCategoryDisplayName(key)
                                val cleaned = cleanCategoryKey(key)
                                val cat = categoriesList.find { 
                                    it.key.equals(key, ignoreCase = true) || 
                                    it.key.equals("cat_$cleaned", ignoreCase = true) ||
                                    cleanCategoryKey(it.key).equals(cleaned, ignoreCase = true)
                                }
                                val icon = cat?.icon ?: Icons.Default.Category
                                
                                Triple(localizedCatName, icon, desc)
                            } else {
                                val legacyMeta = getCategoryAndIconForMerchant(tx.merchant)
                                Triple(legacyMeta.first, legacyMeta.second, tx.merchant)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTxForDetails = tx },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Transaction Category Icon
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PinkPrimary.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = txIcon,
                                            contentDescription = null,
                                            tint = PinkPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = catDisplayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = NavyDark,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = formattedDate,
                                            fontSize = 12.sp,
                                            color = TextLight,
                                            maxLines = 1
                                        )
                                    }

                                    // Amount without currency symbol, only sign + formatted amount
                                    val amountFormatted = String.format(Locale.getDefault(), "%,.2f", if (tx.amount < 0) -tx.amount else tx.amount)
                                    val amountText = if (tx.amount < 0) "-$amountFormatted" else "+$amountFormatted"
                                    val amountColor = if (tx.amount < 0) NavyDark else Color(0xFF10B981)

                                    Text(
                                        text = if (isBalanceVisible) amountText else "••••••",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = amountColor,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // BOTTOM SHEET - ACCOUNT SWITCHER
    if (showAccountSwitcher) {
        ModalBottomSheet(
            onDismissRequest = { showAccountSwitcher = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.switch_account),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = NavyDark,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                LazyColumn {
                    // "All Accounts" Option
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectAccount(null)
                                    showAccountSwitcher = false
                                }
                                .background(if (selectedAccountId == null) PinkPrimary.copy(alpha = 0.05f) else Color.Transparent)
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(painter = painterResource(id = R.drawable.ic_nav_accounts), contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.all_accounts),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = NavyDark
                                )
                                Text(
                                    text = stringResource(R.string.all_accounts_sub),
                                    fontSize = 12.sp,
                                    color = TextLight
                                )
                            }
                            if (selectedAccountId == null) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = PinkPrimary)
                            }
                        }
                    }

                    // Individual accounts
                    items(accounts) { account ->
                        val isSelected = selectedAccountId == account.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectAccount(account.id)
                                    showAccountSwitcher = false
                                }
                                .background(if (isSelected) PinkPrimary.copy(alpha = 0.05f) else Color.Transparent)
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BrandLogo(
                                provider = account.provider ?: account.name,
                                accountType = account.type,
                                iconColorHex = account.icon_color,
                                logoUrl = account.logo_url,
                                modifier = Modifier.size(40.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                val displayName = if (!account.label.isNullOrBlank()) {
                                    "${account.name} (${account.label})"
                                } else {
                                    account.name
                                }
                                Text(
                                    text = displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = NavyDark
                                )
                                val localizedType = when (account.type) {
                                    com.oryno.piggy_ledger.data.AccountType.BANK -> stringResource(R.string.bank)
                                    com.oryno.piggy_ledger.data.AccountType.CARD -> stringResource(R.string.card)
                                    com.oryno.piggy_ledger.data.AccountType.CASH -> stringResource(R.string.cash)
                                    com.oryno.piggy_ledger.data.AccountType.WALLET -> stringResource(R.string.e_wallet)
                                    com.oryno.piggy_ledger.data.AccountType.ASSET_GOLD -> "Gold"
                                    com.oryno.piggy_ledger.data.AccountType.ASSET_SILVER -> "Silver"
                                    com.oryno.piggy_ledger.data.AccountType.CERTIFICATE -> "Certificate"
                                    com.oryno.piggy_ledger.data.AccountType.INVESTMENT -> "Investment"
                                    com.oryno.piggy_ledger.data.AccountType.ASSET_PROPERTY -> "Property"
                                    else -> account.type.name
                                }
                                Text(
                                    text = "$localizedType • ${getCurrencySymbol(account.currency)} ${String.format(Locale.getDefault(), "%,.2f", account.current_balance)}",
                                    fontSize = 12.sp,
                                    color = TextLight
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    showAccountSwitcher = false
                                    onNavigateToEditAccount(account.id)
                                }
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Edit Account", tint = TextLight, modifier = Modifier.size(20.dp))
                            }

                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = PinkPrimary)
                            }
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFE2E8F0))
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAccountSwitcher = false
                                    onNavigateToAddAccount()
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PinkPrimary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = PinkPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.add_new_account),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = PinkPrimary
                            )
                        }
                    }
                }
        }
    }
}

    // MONTH SELECTOR BOTTOM SHEET
    if (showMonthBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMonthBottomSheet = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFE2E8F0)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_month),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = NavyDark,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(monthNames) { index, name ->
                        val isSelected = selectedMonth == index
                        val bg = if (isSelected) PinkPrimary.copy(alpha = 0.1f) else Color(0xFFF1F5F9)
                        val textCol = if (isSelected) PinkPrimary else NavyDark
                        val borderMod = if (isSelected) Modifier.border(1.dp, PinkPrimary, RoundedCornerShape(12.dp)) else Modifier

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .then(borderMod)
                                .clickable {
                                    selectedMonth = index
                                    showMonthBottomSheet = false
                                }
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.take(3), // e.g. "Jan", "Feb"
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = textCol,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // EDIT BUDGET BOTTOM SHEET
    if (showEditBudgetDialog) {
        ModalBottomSheet(
            onDismissRequest = { showEditBudgetDialog = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = NavyDark.copy(alpha = 0.3f)) }
        ) {
            var newBudgetStr by remember { mutableStateOf(monthlyBudget.toString()) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.update_budget),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                OutlinedTextField(
                    value = newBudgetStr,
                    onValueChange = { newBudgetStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text("${stringResource(R.string.budget_amount)} ($currencySymbol)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PinkPrimary,
                        focusedLabelColor = PinkPrimary,
                        cursorColor = PinkPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val parsed = newBudgetStr.replace(currencySymbol, "").replace("$", "").trim().toDoubleOrNull()
                        if (parsed != null && parsed >= 0.0) {
                            viewModel.setMonthlyBudget(parsed)
                        }
                        showEditBudgetDialog = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                ) {
                    Text(stringResource(R.string.save), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    // ADD MANUAL TRANSACTION DIALOG
    if (showAddTransactionDialog) {
        AddTransactionScreen(
            viewModel = viewModel,
            selectedAccountId = selectedAccountId,
            accounts = accounts,
            onDismiss = { showAddTransactionDialog = false },
            onNavigateToAddAccount = onNavigateToAddAccount,
            onNavigateToSettingsPro = onNavigateToSettingsPro,
            onNavigateToTransfer = {
                showAddTransactionDialog = false
                onNavigateToTransfer()
            }
        )
    }

    // TRANSACTION DETAILS BOTTOM SHEET
    selectedTxForDetails?.let { tx ->
        val formattedDate = remember(tx.timestamp) {
            SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
        }

        // Parse category and description
        val isParsed = tx.merchant.contains("|")
        val (txTitle, txIcon, txSubtext) = if (isParsed) {
            val parts = tx.merchant.split("|", limit = 2)
            val key = parts[0]
            val desc = parts.getOrNull(1) ?: ""
            
            val localizedCatName = resolveCategoryDisplayName(key)
            val cleaned = cleanCategoryKey(key)
            val cat = categoriesList.find { 
                it.key.equals(key, ignoreCase = true) || 
                it.key.equals("cat_$cleaned", ignoreCase = true) ||
                cleanCategoryKey(it.key).equals(cleaned, ignoreCase = true)
            }
            val icon = cat?.icon ?: Icons.Default.Category
            
            val title = if (desc.isNotBlank()) desc else localizedCatName
            val sub = if (desc.isNotBlank()) localizedCatName else ""
            
            Triple(title, icon, sub)
        } else {
            val legacyMeta = getCategoryAndIconForMerchant(tx.merchant)
            Triple(tx.merchant, legacyMeta.second, legacyMeta.first)
        }

        val txAccount = accounts.find { it.id == tx.account_id }
        val amountColor = if (tx.amount < 0) NavyDark else Color(0xFF10B981)
        val amountSign = if (tx.amount < 0) "-" else "+"
        val rawAmountStr = String.format("%,.2f", if (tx.amount < 0) -tx.amount else tx.amount)
        val currencySymbolDetails = txAccount?.currency?.let { getCurrencySymbol(it) } ?: getCurrencySymbol(appCurrency)

        ModalBottomSheet(
            onDismissRequest = { selectedTxForDetails = null },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFE2E8F0)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Category Icon Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PinkPrimary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = txIcon,
                        contentDescription = null,
                        tint = PinkPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Amount
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$amountSign$currencySymbolDetails $rawAmountStr",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = amountColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (tx.amount < 0) "Expense" else "Income",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (tx.amount < 0) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                // Details Rows
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DetailRow(
                        label = "Title",
                        value = txTitle
                    )

                    if (txSubtext.isNotBlank()) {
                        DetailRow(
                            label = "Category",
                            value = txSubtext
                        )
                    }

                    if (txAccount != null) {
                        DetailRow(
                            label = "Account",
                            value = txAccount.name,
                            icon = {
                                BrandLogo(
                                    provider = txAccount.provider,
                                    accountType = txAccount.type,
                                    iconColorHex = txAccount.icon_color,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }

                    DetailRow(
                        label = "Date & Time",
                        value = formattedDate
                    )

                    DetailRow(
                        label = "Logged Via",
                        value = if (tx.source == "AUTOMATIC_SMS") "Parsed SMS Notification" else "Manual Entry"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { selectedTxForDetails = null },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.close), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    icon: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextLight
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                icon()
            }
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark,
                textAlign = TextAlign.End
            )
        }
    }
}

// Category and icon matcher based on merchant keywords
fun getCategoryAndIconForMerchant(merchant: String): Pair<String, ImageVector> {
    val clean = merchant.lowercase()
    return when {
        clean.contains("uber") || clean.contains("careem") || clean.contains("taxi") || clean.contains("transport") || clean.contains("airport") -> 
            "Transport" to Icons.Default.DirectionsCar
        clean.contains("dinner") || clean.contains("food") || clean.contains("restaurant") || clean.contains("burger") || clean.contains("pizza") || clean.contains("mexican") -> 
            "Food" to Icons.Default.Restaurant
        clean.contains("grocery") || clean.contains("groceries") || clean.contains("supermarket") || clean.contains("market") || clean.contains("hyperone") || clean.contains("carrefour") -> 
            "Groceries" to Icons.Default.ShoppingCart
        clean.contains("coffee") || clean.contains("latte") || clean.contains("starbucks") || clean.contains("cafe") || clean.contains("espresso") || clean.contains("cappuccino") -> 
            "Coffee" to Icons.Default.LocalCafe
        clean.contains("salary") || clean.contains("transfer") || clean.contains("deposit") || clean.contains("income") -> 
            "Income" to Icons.Default.TrendingUp
        else -> 
            "Other" to Icons.Default.Payments
    }
}

@Composable
private fun MorphingActionButtonsBar(
    filterAllTime: Boolean,
    onToggleFilter: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3500L)
            activeIndex = (activeIndex + 1) % 3
        }
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF1E293B), // Sleek off-black / dark slate pill background matching uploaded image
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Button 0: Add Transaction
            MorphingButton(
                icon = Icons.Default.Add,
                label = stringResource(R.string.add_transaction),
                isExpanded = activeIndex == 0,
                modifier = Modifier.weight(if (activeIndex == 0) 1.6f else 1.0f),
                onClick = {
                    activeIndex = 0
                    onAddTransaction()
                }
            )

            // Button 1: Transfer
            MorphingButton(
                icon = Icons.Default.SwapHoriz,
                label = stringResource(R.string.transfer),
                isExpanded = activeIndex == 1,
                modifier = Modifier.weight(if (activeIndex == 1) 1.6f else 1.0f),
                onClick = {
                    activeIndex = 1
                    onNavigateToTransfer()
                }
            )

            // Button 2: Filter
            MorphingButton(
                icon = if (filterAllTime) Icons.Default.List else Icons.Default.FilterList,
                label = if (filterAllTime) stringResource(R.string.all_time) else stringResource(R.string.this_month),
                isExpanded = activeIndex == 2,
                modifier = Modifier.weight(if (activeIndex == 2) 1.6f else 1.0f),
                onClick = {
                    activeIndex = 2
                    onToggleFilter()
                }
            )
        }
    }
}

@Composable
private fun MorphingButton(
    icon: ImageVector,
    label: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isExpanded) Color.White else Color.White.copy(alpha = 0.15f)
    val contentColor = if (isExpanded) NavyDark else Color.White

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        modifier = modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (isExpanded) 12.dp else 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        color = contentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundSpendingChart(
    transactions: List<AccountTransaction>,
    monthStart: Long,
    monthEnd: Long
) {
    val dailySpending = remember(transactions, monthStart, monthEnd) {
        val map = mutableMapOf<Int, Double>()
        val cal = Calendar.getInstance()
        
        // Initialize with zeros for each day of month
        cal.timeInMillis = monthStart
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..maxDays) map[i] = 0.0
        
        // Aggregate
        transactions.filter { it.amount < 0 }.forEach { tx ->
            cal.timeInMillis = tx.timestamp
            val day = cal.get(Calendar.DAY_OF_MONTH)
            map[day] = (map[day] ?: 0.0) + (-tx.amount)
        }
        
        // Create cumulative points for a growth chart look
        var runningTotal = 0.0
        val points = mutableListOf<Double>()
        for (i in 1..maxDays) {
            runningTotal += (map[i] ?: 0.0)
            points.add(runningTotal)
        }
        points
    }

    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .graphicsLayer { alpha = 0.15f } // Subtly blended into background
    ) {
        if (dailySpending.isEmpty()) return@Canvas
        
        val maxVal = dailySpending.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val width = size.width
        val height = size.height
        val stepX = width / (dailySpending.size - 1).coerceAtLeast(1)
        
        val path = androidx.compose.ui.graphics.Path()
        dailySpending.forEachIndexed { index, value ->
            val x = if (isRtl) width - (index * stepX) else index * stepX
            val y = height - (value.toFloat() / maxVal.toFloat() * height * 0.8f)
            if (index == 0) path.moveTo(x, y) else {
                val prevX = if (isRtl) width - ((index - 1) * stepX) else (index - 1) * stepX
                val prevY = height - (dailySpending[index - 1].toFloat() / maxVal.toFloat() * height * 0.8f)
                
                // Use cubicTo for a smoother "wave" look instead of straight lineTo
                path.cubicTo(
                    (prevX + x) / 2f, prevY,
                    (prevX + x) / 2f, y,
                    x, y
                )
            }
        }

        drawPath(
            path = path,
            color = PinkPrimary,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
        
        // Fill area under path
        val fillPath = androidx.compose.ui.graphics.Path().apply {
            addPath(path)
            if (isRtl) {
                lineTo(0f, height)
                lineTo(width, height)
            } else {
                lineTo(width, height)
                lineTo(0f, height)
            }
            close()
        }
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(PinkPrimary.copy(alpha = 0.3f), Color.Transparent)
            )
        )
    }
}

