package com.oryno.piggy_ledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.data.AccountTransaction
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import java.text.SimpleDateFormat
import java.util.*

enum class TxTypeFilter { ALL, EXPENSE, INCOME }
enum class TxTimeFilter { ALL_TIME, THIS_MONTH, LAST_MONTH, LAST_3_MONTHS, THIS_YEAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTransactionsScreen(
    viewModel: PiggyLedgerViewModel,
    initialAccountId: String? = null,
    onBack: () -> Unit,
    onNavigateToTransfer: () -> Unit = {}
) {
    BackHandler { onBack() }

    val accounts by viewModel.userAccounts.collectAsState()
    val allTransactions by viewModel.allAccountTransactions.collectAsState()
    val appCurrency by viewModel.appCurrency.collectAsState()
    val isPrivacyMode by viewModel.isPrivacyModeEnabled.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(initialAccountId) }
    var selectedTypeFilter by remember { mutableStateOf(TxTypeFilter.ALL) }
    var selectedTimeFilter by remember { mutableStateOf(TxTimeFilter.ALL_TIME) }
    var selectedCategoryKey by remember { mutableStateOf<String?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedTxForDetails by remember { mutableStateOf<AccountTransaction?>(null) }

    // Time boundaries calculation
    val now = remember { Calendar.getInstance() }
    val thisMonthStart = remember(now) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val lastMonthStart = remember(now) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val lastMonthEnd = thisMonthStart
    val last3MonthsStart = remember(now) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, -3)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val thisYearStart = remember(now) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Excluded accounts
    val excludedAccountIds = remember(accounts) { accounts.filter { it.exclude_from_all }.map { it.id }.toSet() }

    // Filter transactions
    val filteredTransactions = remember(
        allTransactions,
        selectedAccountId,
        searchQuery,
        selectedTypeFilter,
        selectedTimeFilter,
        selectedCategoryKey,
        excludedAccountIds
    ) {
        allTransactions.filter { tx ->
            // Account filter
            val matchesAccount = if (selectedAccountId == null) {
                !excludedAccountIds.contains(tx.account_id)
            } else {
                tx.account_id == selectedAccountId
            }
            if (!matchesAccount) return@filter false

            // Type filter
            when (selectedTypeFilter) {
                TxTypeFilter.ALL -> {}
                TxTypeFilter.EXPENSE -> if (tx.amount >= 0) return@filter false
                TxTypeFilter.INCOME -> if (tx.amount <= 0) return@filter false
            }

            // Time filter
            when (selectedTimeFilter) {
                TxTimeFilter.ALL_TIME -> {}
                TxTimeFilter.THIS_MONTH -> if (tx.timestamp < thisMonthStart) return@filter false
                TxTimeFilter.LAST_MONTH -> if (tx.timestamp !in lastMonthStart until lastMonthEnd) return@filter false
                TxTimeFilter.LAST_3_MONTHS -> if (tx.timestamp < last3MonthsStart) return@filter false
                TxTimeFilter.THIS_YEAR -> if (tx.timestamp < thisYearStart) return@filter false
            }

            // Category filter
            if (selectedCategoryKey != null) {
                val cleanedTarget = cleanCategoryKey(selectedCategoryKey!!)
                val isParsed = tx.merchant.contains("|")
                val catKey = if (isParsed) tx.merchant.split("|", limit = 2)[0] else ""
                val cleanedCat = cleanCategoryKey(catKey)
                if (!cleanedCat.equals(cleanedTarget, ignoreCase = true)) {
                    return@filter false
                }
            }

            // Search query
            if (searchQuery.isNotBlank()) {
                val query = searchQuery.trim().lowercase(Locale.getDefault())
                val merchantMatch = tx.merchant.lowercase(Locale.getDefault()).contains(query)
                val accountMatch = accounts.find { it.id == tx.account_id }?.let {
                    it.name.lowercase(Locale.getDefault()).contains(query) || (it.provider?.lowercase(Locale.getDefault())?.contains(query) == true)
                } ?: false
                if (!merchantMatch && !accountMatch) return@filter false
            }

            true
        }.sortedByDescending { it.timestamp }
    }

    val totalSpent = remember(filteredTransactions) {
        filteredTransactions.filter { it.amount < 0 }.sumOf { -it.amount }
    }
    val totalIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.amount > 0 }.sumOf { it.amount }
    }

    val hasActiveFilters = selectedAccountId != null ||
            selectedTypeFilter != TxTypeFilter.ALL ||
            selectedTimeFilter != TxTimeFilter.ALL_TIME ||
            selectedCategoryKey != null

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.all_transactions),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = NavyDark
                        )
                        val subtitleText = selectedAccountId?.let { accId ->
                            accounts.find { it.id == accId }?.name
                        } ?: stringResource(R.string.all_accounts)
                        Text(
                            text = subtitleText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextLight
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NavyDark
                        )
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        IconButton(
                            onClick = { showFilterSheet = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (hasActiveFilters) PinkPrimary.copy(alpha = 0.12f) else Color.Transparent
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = stringResource(R.string.filter_options),
                                tint = if (hasActiveFilters) PinkPrimary else NavyDark
                            )
                        }
                        if (hasActiveFilters) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PinkPrimary)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            Surface(
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_transactions_hint),
                                color = TextLight,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextLight, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextLight, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PinkPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal Quick Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // All/Expense/Income filter chips
                        FilterChip(
                            selected = selectedTypeFilter == TxTypeFilter.ALL,
                            onClick = { selectedTypeFilter = TxTypeFilter.ALL },
                            label = { Text(stringResource(R.string.type_all), fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = selectedTypeFilter == TxTypeFilter.EXPENSE,
                            onClick = { selectedTypeFilter = TxTypeFilter.EXPENSE },
                            label = { Text(stringResource(R.string.type_expense), fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = selectedTypeFilter == TxTypeFilter.INCOME,
                            onClick = { selectedTypeFilter = TxTypeFilter.INCOME },
                            label = { Text(stringResource(R.string.type_income), fontSize = 12.sp) }
                        )

                        // Divider
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .width(1.dp)
                                .background(Color(0xFFE2E8F0))
                        )

                        // Time filter quick chip
                        FilterChip(
                            selected = selectedTimeFilter != TxTimeFilter.ALL_TIME,
                            onClick = { showFilterSheet = true },
                            label = {
                                Text(
                                    text = when (selectedTimeFilter) {
                                        TxTimeFilter.ALL_TIME -> stringResource(R.string.time_all_time)
                                        TxTimeFilter.THIS_MONTH -> stringResource(R.string.time_this_month)
                                        TxTimeFilter.LAST_MONTH -> stringResource(R.string.time_last_month)
                                        TxTimeFilter.LAST_3_MONTHS -> stringResource(R.string.time_last_3_months)
                                        TxTimeFilter.THIS_YEAR -> stringResource(R.string.time_this_year)
                                    },
                                    fontSize = 12.sp
                                )
                            },
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )

                        // Reset button if filters active
                        if (hasActiveFilters) {
                            TextButton(
                                onClick = {
                                    selectedAccountId = null
                                    selectedTypeFilter = TxTypeFilter.ALL
                                    selectedTimeFilter = TxTimeFilter.ALL_TIME
                                    selectedCategoryKey = null
                                    searchQuery = ""
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = PinkPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.reset_filters), color = PinkPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Overview Metric Summary Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.transaction_count_badge, filteredTransactions.size),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isPrivacyMode) "••••••" else "${getCurrencySymbol(appCurrency)} ${String.format(Locale.getDefault(), "%,.0f", totalSpent)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = NavyDark
                        )
                        Text(
                            text = stringResource(R.string.total_expense_label),
                            fontSize = 11.sp,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (totalIncome > 0) {
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .width(1.dp)
                                .background(Color(0xFFE2E8F0))
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.total_income_label),
                                fontSize = 11.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isPrivacyMode) "••••••" else "+${getCurrencySymbol(appCurrency)} ${String.format(Locale.getDefault(), "%,.0f", totalIncome)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }

            // Transactions List or Empty State
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_piggy_track),
                            contentDescription = null,
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_transactions_match_filter),
                            fontWeight = FontWeight.Bold,
                            color = NavyDark,
                            fontSize = 17.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.try_adjusting_filters),
                            color = TextLight,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        if (hasActiveFilters || searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    selectedAccountId = null
                                    selectedTypeFilter = TxTypeFilter.ALL
                                    selectedTimeFilter = TxTimeFilter.ALL_TIME
                                    selectedCategoryKey = null
                                    searchQuery = ""
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.reset_filters), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        val formattedDate = remember(tx.timestamp) {
                            SimpleDateFormat("yyyy-MM-dd • hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
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

                        val txAccount = accounts.find { it.id == tx.account_id }
                        val accountCurrency = txAccount?.currency?.let { getCurrencySymbol(it) } ?: getCurrencySymbol(appCurrency)
                        val isExpense = tx.amount < 0
                        val amountColor = if (isExpense) NavyDark else Color(0xFF10B981)
                        val amountSign = if (isExpense) "-" else "+"
                        val absAmount = kotlin.math.abs(tx.amount)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTxForDetails = tx },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Category Icon Badge
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isExpense) PinkPrimary.copy(alpha = 0.08f) else Color(0xFF10B981).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = txIcon,
                                        contentDescription = null,
                                        tint = if (isExpense) PinkPrimary else Color(0xFF10B981),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Title, Category & Account Badge
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (txDesc.isNotBlank()) txDesc else catDisplayName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = formattedDate,
                                            fontSize = 11.sp,
                                            color = TextLight
                                        )
                                        if (txAccount != null) {
                                            Text("•", fontSize = 10.sp, color = TextLight)
                                            Text(
                                                text = txAccount.name,
                                                fontSize = 11.sp,
                                                color = PinkPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Amount
                                Text(
                                    text = if (isPrivacyMode) "••••••" else "$amountSign$accountCurrency ${String.format(Locale.getDefault(), "%,.2f", absAmount)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = amountColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // FILTER BOTTOM SHEET
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFE2E8F0)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.filter_options),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = NavyDark
                    )
                    TextButton(onClick = {
                        selectedAccountId = null
                        selectedTypeFilter = TxTypeFilter.ALL
                        selectedTimeFilter = TxTimeFilter.ALL_TIME
                        selectedCategoryKey = null
                    }) {
                        Text(stringResource(R.string.reset_filters), color = PinkPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Account Selection
                Text(
                    text = stringResource(R.string.filter_by_account_label),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedAccountId == null,
                        onClick = { selectedAccountId = null },
                        label = { Text(stringResource(R.string.all_accounts_option)) }
                    )
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text(acc.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Transaction Type Selection
                Text(
                    text = stringResource(R.string.filter_by_type),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTypeFilter == TxTypeFilter.ALL,
                        onClick = { selectedTypeFilter = TxTypeFilter.ALL },
                        label = { Text(stringResource(R.string.type_all)) }
                    )
                    FilterChip(
                        selected = selectedTypeFilter == TxTypeFilter.EXPENSE,
                        onClick = { selectedTypeFilter = TxTypeFilter.EXPENSE },
                        label = { Text(stringResource(R.string.type_expense)) }
                    )
                    FilterChip(
                        selected = selectedTypeFilter == TxTypeFilter.INCOME,
                        onClick = { selectedTypeFilter = TxTypeFilter.INCOME },
                        label = { Text(stringResource(R.string.type_income)) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Date Range Selection
                Text(
                    text = stringResource(R.string.filter_by_date),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTimeFilter == TxTimeFilter.ALL_TIME,
                        onClick = { selectedTimeFilter == TxTimeFilter.ALL_TIME },
                        label = { Text(stringResource(R.string.time_all_time)) }
                    )
                    FilterChip(
                        selected = selectedTimeFilter == TxTimeFilter.THIS_MONTH,
                        onClick = { selectedTimeFilter = TxTimeFilter.THIS_MONTH },
                        label = { Text(stringResource(R.string.time_this_month)) }
                    )
                    FilterChip(
                        selected = selectedTimeFilter == TxTimeFilter.LAST_MONTH,
                        onClick = { selectedTimeFilter = TxTimeFilter.LAST_MONTH },
                        label = { Text(stringResource(R.string.time_last_month)) }
                    )
                    FilterChip(
                        selected = selectedTimeFilter == TxTimeFilter.LAST_3_MONTHS,
                        onClick = { selectedTimeFilter = TxTimeFilter.LAST_3_MONTHS },
                        label = { Text(stringResource(R.string.time_last_3_months)) }
                    )
                    FilterChip(
                        selected = selectedTimeFilter == TxTimeFilter.THIS_YEAR,
                        onClick = { selectedTimeFilter = TxTimeFilter.THIS_YEAR },
                        label = { Text(stringResource(R.string.time_this_year)) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Category Selection
                Text(
                    text = stringResource(R.string.filter_by_category),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategoryKey == null,
                        onClick = { selectedCategoryKey = null },
                        label = { Text(stringResource(R.string.type_all)) }
                    )
                    categoriesList.filter { it.key != "cat_transfer" && it.key != "transfer" }.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryKey == cat.key,
                            onClick = { selectedCategoryKey = cat.key },
                            label = { Text(resolveCategoryDisplayName(cat.key)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                ) {
                    Text(
                        text = stringResource(R.string.apply_filters),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    // TRANSACTION DETAILS BOTTOM SHEET
    selectedTxForDetails?.let { tx ->
        val formattedDate = remember(tx.timestamp) {
            SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
        }

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
        val isExpense = tx.amount < 0
        val amountColor = if (isExpense) NavyDark else Color(0xFF10B981)
        val amountSign = if (isExpense) "-" else "+"
        val rawAmountStr = String.format(Locale.getDefault(), "%,.2f", kotlin.math.abs(tx.amount))
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

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isPrivacyMode) "••••••" else "$amountSign$currencySymbolDetails $rawAmountStr",
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
