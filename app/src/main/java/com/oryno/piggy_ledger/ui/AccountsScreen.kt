package com.oryno.piggy_ledger.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onNavigateToSettingsPro: () -> Unit = {}
) {
    val accounts by viewModel.allAccounts.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()
    val allTransactions by viewModel.allAccountTransactions.collectAsState()
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

    var selectedTxForDetails by remember { mutableStateOf<AccountTransaction?>(null) }
    var filterAllTime by remember { mutableStateOf(true) } // Default to true so all transactions are shown and scrollable!

    // Filtered transaction list
    val filteredTransactions = remember(allTransactions, selectedAccountId, monthStart, monthEnd) {
        allTransactions.filter { tx ->
            val matchesAccount = (selectedAccountId == null) || (tx.account_id == selectedAccountId)
            val matchesTime = tx.timestamp in monthStart until monthEnd
            matchesAccount && matchesTime
        }
    }

    // List of transactions shown in the list
    val displayedTransactions = remember(allTransactions, selectedAccountId, monthStart, monthEnd, filterAllTime) {
        allTransactions.filter { tx ->
            val matchesAccount = (selectedAccountId == null) || (tx.account_id == selectedAccountId)
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
    val currencySymbol = selectedAccount?.currency ?: "EGP"

    val progress = if (monthlyBudget > 0) (totalSpent / monthlyBudget).toFloat().coerceIn(0f, 1f) else 0f

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
                // PREMIUM HEADER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(NavyDark)
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp, bottom = 32.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Top Bar: Month Selector & Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Month Selector
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .clickable { showMonthBottomSheet = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${monthNames[selectedMonth]} $selectedYear",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(16.dp))
                            }

                            // Right Action Icons
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Account Switcher
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .clickable { showAccountSwitcher = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AccountBalance, null, tint = PinkPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Main Budget Metric: Spent vs Budget
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.total_spent), color = TextLight, fontSize = 12.sp, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isBalanceVisible) "$currencySymbol ${String.format(Locale.getDefault(), "%,.0f", totalSpent)}" else "$currencySymbol ••••••",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        // Budget Bar + Details
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = if (progress > 0.9f) Color(0xFFEF4444) else PinkPrimary,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { showEditBudgetDialog = true },
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${stringResource(R.string.budget)}: $currencySymbol ${String.format(Locale.getDefault(), "%,.0f", monthlyBudget)}", color = TextLight, fontSize = 11.sp)
                                Text(stringResource(R.string.percent_used, (progress * 100).toInt()), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // TRANSACTIONS HEADER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.transactions),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyDark
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // All/Month Toggle Chip
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9))
                                .clickable { filterAllTime = !filterAllTime }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (filterAllTime) Icons.Default.List else Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = PinkPrimary
                            )
                            Text(
                                text = if (filterAllTime) stringResource(R.string.all_time) else stringResource(R.string.this_month),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PinkPrimary
                            )
                        }

                        Button(
                            onClick = { showAddTransactionDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PinkPrimary.copy(alpha = 0.08f),
                                contentColor = PinkPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.add_btn), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // TRANSACTION LAZY COLUMN
                if (displayedTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
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
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PinkPrimary)
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
                            // Account logo/icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                val providerRes = getProviderDrawableRes(account.provider)
                                val logoSource: Any? = when {
                                    account.local_logo_path != null -> File(account.local_logo_path)
                                    account.logo_url != null -> account.logo_url
                                    else -> null
                                }

                                if (logoSource != null) {
                                    AsyncImage(
                                        model = logoSource,
                                        contentDescription = account.provider ?: "Logo",
                                        modifier = Modifier.fillMaxSize().padding(4.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else if (providerRes != null) {
                                    Image(
                                        painter = painterResource(id = providerRes),
                                        contentDescription = account.provider ?: "Logo",
                                        modifier = Modifier.fillMaxSize().padding(4.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(android.graphics.Color.parseColor(account.icon_color))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (account.type) {
                                                AccountType.BANK -> Icons.Default.AccountBalance
                                                AccountType.CARD -> Icons.Default.CreditCard
                                                AccountType.WALLET -> Icons.Default.AccountBalanceWallet
                                                AccountType.CASH -> Icons.Default.Payments
                                            },
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

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
                                }
                                Text(
                                    text = "$localizedType • ${account.currency} ${String.format(Locale.getDefault(), "%,.2f", account.current_balance)}",
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
                        val parsed = newBudgetStr.toDoubleOrNull()
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
            onNavigateToSettingsPro = onNavigateToSettingsPro
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
        val currencySymbolDetails = txAccount?.currency ?: "EGP"

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
