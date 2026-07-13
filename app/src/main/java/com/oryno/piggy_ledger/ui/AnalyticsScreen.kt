package com.oryno.piggy_ledger.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.atan2
import kotlin.math.sqrt

// Chart style options
enum class ChartType {
    PIE, DONUT, BAR
}

// Grouping options
enum class GroupingMode {
    CATEGORY, ACCOUNT, DAY, MONTH
}

// Time filter options
enum class TimePeriod {
    THIS_MONTH, LAST_3_MONTHS, THIS_YEAR, ALL_TIME
}

// Transaction type filter options
enum class TxTypeFilter {
    EXPENSES, INCOME, ALL
}

data class AnalyticsDataPoint(
    val id: String,
    val name: String,
    val amount: Double,
    val percentage: Float,
    val icon: ImageVector,
    val color: Color,
    val txCount: Int,
    val rawTransactions: List<AccountTransaction>
)

val chartColors = listOf(
    Color(0xFF3B82F6), // Vibrant Blue
    Color(0xFF10B981), // Emerald Green
    Color(0xFFF59E0B), // Amber Yellow
    Color(0xFFEF4444), // Crimson Red
    Color(0xFF8B5CF6), // Purple
    Color(0xFFEC4899), // Hot Pink
    Color(0xFF06B6D4), // Cyan
    Color(0xFF14B8A6), // Teal
    Color(0xFF6366F1), // Indigo
    Color(0xFFF43F5E), // Rose
    Color(0xFF84CC16), // Lime Green
    Color(0xFFD946EF)  // Fuchsia
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: PiggyLedgerViewModel,
    onBack: () -> Unit
) {
    val allTransactions by viewModel.allAccountTransactions.collectAsState()
    val allAccounts by viewModel.allAccounts.collectAsState()
    val context = LocalContext.current

    // Filter states
    var selectedPeriod by remember { mutableStateOf(TimePeriod.ALL_TIME) }
    var selectedType by remember { mutableStateOf(TxTypeFilter.EXPENSES) }
    var groupingMode by remember { mutableStateOf(GroupingMode.CATEGORY) }
    var chartType by remember { mutableStateOf(ChartType.DONUT) }
    var selectedAccountIdFilter by remember { mutableStateOf<Long?>(null) } // null = All Accounts

    // Bottom sheets state
    var showAccountBottomSheet by remember { mutableStateOf(false) }

    // Interactive chart states
    var selectedSliceIndex by remember { mutableStateOf(-1) }
    
    // Reset selected slice whenever filters change
    LaunchedEffect(selectedPeriod, selectedType, groupingMode, selectedAccountIdFilter) {
        selectedSliceIndex = -1
    }

    // Process and filter data
    val filteredAndGroupedData = remember(
        allTransactions,
        allAccounts,
        selectedPeriod,
        selectedType,
        groupingMode,
        selectedAccountIdFilter
    ) {
        // Compute correct start boundaries for filter
        val startTime = when (selectedPeriod) {
            TimePeriod.THIS_MONTH -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            TimePeriod.LAST_3_MONTHS -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -3)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            TimePeriod.THIS_YEAR -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            TimePeriod.ALL_TIME -> 0L
        }

        // Filter raw transactions
        val filteredTxs = allTransactions.filter { tx ->
            val matchesTime = tx.timestamp >= startTime
            val matchesAccount = (selectedAccountIdFilter == null) || (tx.account_id == selectedAccountIdFilter)
            val matchesType = when (selectedType) {
                TxTypeFilter.EXPENSES -> tx.amount < 0
                TxTypeFilter.INCOME -> tx.amount > 0
                TxTypeFilter.ALL -> true
            }
            matchesTime && matchesAccount && matchesType
        }

        val totalAbsoluteAmount = filteredTxs.sumOf { kotlin.math.abs(it.amount) }

        // Group transactions
        val points = when (groupingMode) {
            GroupingMode.CATEGORY -> {
                // Group by Category Key
                val groupedMap = filteredTxs.groupBy { tx ->
                    val isParsed = tx.merchant.contains("|")
                    if (isParsed) tx.merchant.split("|", limit = 2)[0] else "cat_other"
                }

                groupedMap.entries.mapIndexed { index, entry ->
                    val catKey = entry.key
                    val txs = entry.value
                    val sumAbs = txs.sumOf { kotlin.math.abs(it.amount) }
                    val percentage = if (totalAbsoluteAmount > 0) (sumAbs / totalAbsoluteAmount).toFloat() else 0f

                    val catInfo = categoriesList.find { it.key == catKey }
                    val localizedName = if (catInfo != null) {
                        context.getString(catInfo.nameRes)
                    } else if (catKey.startsWith("custom_")) {
                        catKey.substringAfter("custom_")
                    } else {
                        context.getString(R.string.cat_other)
                    }
                    val icon = catInfo?.icon ?: Icons.Default.Category
                    val color = chartColors[index % chartColors.size]

                    AnalyticsDataPoint(
                        id = catKey,
                        name = localizedName,
                        amount = sumAbs,
                        percentage = percentage,
                        icon = icon,
                        color = color,
                        txCount = txs.size,
                        rawTransactions = txs
                    )
                }
            }
            GroupingMode.ACCOUNT -> {
                // Group by Account
                val groupedMap = filteredTxs.groupBy { it.account_id }

                groupedMap.entries.mapIndexed { index, entry ->
                    val accId = entry.key
                    val txs = entry.value
                    val sumAbs = txs.sumOf { kotlin.math.abs(it.amount) }
                    val percentage = if (totalAbsoluteAmount > 0) (sumAbs / totalAbsoluteAmount).toFloat() else 0f

                    val account = allAccounts.find { it.id == accId }
                    val accName = account?.name ?: "Unknown Account"
                    val icon = when (account?.type) {
                        com.oryno.piggy_ledger.data.AccountType.BANK -> Icons.Default.AccountBalance
                        com.oryno.piggy_ledger.data.AccountType.CARD -> Icons.Default.CreditCard
                        com.oryno.piggy_ledger.data.AccountType.CASH -> Icons.Default.Payments
                        com.oryno.piggy_ledger.data.AccountType.WALLET -> Icons.Default.AccountBalanceWallet
                        else -> Icons.Default.AccountBalance
                    }
                    val color = chartColors[index % chartColors.size]

                    AnalyticsDataPoint(
                        id = accId.toString(),
                        name = accName,
                        amount = sumAbs,
                        percentage = percentage,
                        icon = icon,
                        color = color,
                        txCount = txs.size,
                        rawTransactions = txs
                    )
                }
            }
            GroupingMode.DAY -> {
                // Group by Day (YYYY-MM-DD)
                val dayFormatterForGroup = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dayFormatterForDisplay = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

                val groupedMap = filteredTxs.groupBy { tx ->
                    dayFormatterForGroup.format(Date(tx.timestamp))
                }

                groupedMap.entries.mapIndexed { index, entry ->
                    val dayKey = entry.key
                    val txs = entry.value
                    val sumAbs = txs.sumOf { kotlin.math.abs(it.amount) }
                    val percentage = if (totalAbsoluteAmount > 0) (sumAbs / totalAbsoluteAmount).toFloat() else 0f

                    val dateObj = try { dayFormatterForGroup.parse(dayKey) } catch (e: Exception) { null }
                    val displayName = if (dateObj != null) dayFormatterForDisplay.format(dateObj) else dayKey

                    val icon = Icons.Default.CalendarToday
                    val color = chartColors[index % chartColors.size]

                    AnalyticsDataPoint(
                        id = dayKey,
                        name = displayName,
                        amount = sumAbs,
                        percentage = percentage,
                        icon = icon,
                        color = color,
                        txCount = txs.size,
                        rawTransactions = txs
                    )
                }
            }
            GroupingMode.MONTH -> {
                // Group by Month (YYYY-MM)
                val monthFormatterForGroup = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val monthFormatterForDisplay = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

                val groupedMap = filteredTxs.groupBy { tx ->
                    monthFormatterForGroup.format(Date(tx.timestamp))
                }

                groupedMap.entries.mapIndexed { index, entry ->
                    val monthKey = entry.key
                    val txs = entry.value
                    val sumAbs = txs.sumOf { kotlin.math.abs(it.amount) }
                    val percentage = if (totalAbsoluteAmount > 0) (sumAbs / totalAbsoluteAmount).toFloat() else 0f

                    val dateObj = try { monthFormatterForGroup.parse(monthKey) } catch (e: Exception) { null }
                    val displayName = if (dateObj != null) monthFormatterForDisplay.format(dateObj) else monthKey

                    val icon = Icons.Default.CalendarToday
                    val color = chartColors[index % chartColors.size]

                    AnalyticsDataPoint(
                        id = monthKey,
                        name = displayName,
                        amount = sumAbs,
                        percentage = percentage,
                        icon = icon,
                        color = color,
                        txCount = txs.size,
                        rawTransactions = txs
                    )
                }
            }
        }

        if (groupingMode == GroupingMode.DAY || groupingMode == GroupingMode.MONTH) {
            points.sortedByDescending { it.id }
        } else {
            points.sortedByDescending { it.amount }
        }
    }

    val totalSpentFiltered = remember(filteredAndGroupedData) {
        filteredAndGroupedData.sumOf { it.amount }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.spending_analytics),
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyDark,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_icon),
                            tint = NavyDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.shadow(0.5.dp)
            )
        },
        containerColor = Color(0xFFF8FAFC) // Slate background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 120.dp) // Generous space for Floating Bottom Bar
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // FILTERS CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Time Period chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimePeriod.values().forEach { period ->
                            val label = when (period) {
                                TimePeriod.THIS_MONTH -> stringResource(R.string.this_month)
                                TimePeriod.LAST_3_MONTHS -> stringResource(R.string.last_3_months)
                                TimePeriod.THIS_YEAR -> stringResource(R.string.this_year)
                                TimePeriod.ALL_TIME -> stringResource(R.string.all_time)
                            }
                            val isSelected = selectedPeriod == period
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPeriod = period },
                                label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PinkPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = TextLight
                                ),
                                border = null,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Account Filter Trigger Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.filtered_account_label),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        
                        val currentAccountLabel = if (selectedAccountIdFilter == null) {
                            stringResource(R.string.all_accounts)
                        } else {
                            allAccounts.find { it.id == selectedAccountIdFilter }?.name ?: stringResource(R.string.all_accounts)
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(PinkPrimary.copy(alpha = 0.08f))
                                .clickable { showAccountBottomSheet = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = PinkPrimary
                            )
                            Text(
                                text = currentAccountLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PinkPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = PinkPrimary
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                    // Secondary Filters: Type, and Chart Type
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Type Selection: Expenses vs Income
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(4.dp)
                        ) {
                            val types = listOf(
                                TxTypeFilter.EXPENSES to stringResource(R.string.expenses),
                                TxTypeFilter.INCOME to stringResource(R.string.income),
                                TxTypeFilter.ALL to stringResource(R.string.all_filter)
                            )
                            types.forEach { (type, label) ->
                                val active = selectedType == type
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) NavyDark else Color.Transparent)
                                        .clickable { selectedType = type }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else TextLight
                                    )
                                }
                            }
                        }

                        // Chart Mode: Pie vs Donut vs Bar
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(4.dp)
                        ) {
                            listOf(
                                ChartType.DONUT to Icons.Default.DonutLarge,
                                ChartType.PIE to Icons.Default.PieChart,
                                ChartType.BAR to Icons.Default.BarChart
                            ).forEach { (type, icon) ->
                                val active = chartType == type
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) PinkPrimary else Color.Transparent)
                                        .clickable { chartType = type }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = type.name,
                                        tint = if (active) Color.White else TextLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // VISUALIZATION CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Grouping mode switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(4.dp)
                    ) {
                        listOf(
                            GroupingMode.CATEGORY to stringResource(R.string.group_by_category),
                            GroupingMode.ACCOUNT to stringResource(R.string.group_by_account),
                            GroupingMode.DAY to stringResource(R.string.group_by_day),
                            GroupingMode.MONTH to stringResource(R.string.group_by_month)
                        ).forEach { (mode, label) ->
                            val active = groupingMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) Color.White else Color.Transparent)
                                    .clickable { groupingMode = mode }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) NavyDark else TextLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (filteredAndGroupedData.isEmpty()) {
                        // Empty State inside visual card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(PinkPrimary.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InsertChartOutlined,
                                    contentDescription = null,
                                    tint = PinkPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.no_transactions_found),
                                fontWeight = FontWeight.Bold,
                                color = NavyDark,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.try_adjusting_filters),
                                color = TextLight,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // RENDER CUSTOM CHART
                        when (chartType) {
                            ChartType.BAR -> {
                                CustomBarChart(
                                    points = filteredAndGroupedData,
                                    selectedIndex = selectedSliceIndex,
                                    onSliceSelected = { selectedSliceIndex = it }
                                )
                            }
                            else -> {
                                CustomPieChart(
                                    points = filteredAndGroupedData,
                                    chartType = chartType,
                                    selectedIndex = selectedSliceIndex,
                                    onSliceSelected = { selectedSliceIndex = it },
                                    totalAmount = totalSpentFiltered
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ACTIVE SEGMENT DETAIL BANNER
                        AnimatedContent(
                            targetState = selectedSliceIndex,
                            transitionSpec = {
                                slideInVertically { height -> height } + fadeIn() togetherWith
                                        slideOutVertically { height -> -height } + fadeOut()
                            }
                        ) { index ->
                            if (index in filteredAndGroupedData.indices) {
                                val selectedPoint = filteredAndGroupedData[index]
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = selectedPoint.color.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, selectedPoint.color.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(selectedPoint.color),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = selectedPoint.icon,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = selectedPoint.name,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = NavyDark,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = stringResource(R.string.transaction_count_format, selectedPoint.txCount),
                                                color = TextLight,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = stringResource(R.string.amount_with_currency, String.format(Locale.getDefault(), "%,.2f", selectedPoint.amount), stringResource(R.string.currency_egp)),
                                                fontWeight = FontWeight.Black,
                                                color = NavyDark,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = String.format("%.1f%%", selectedPoint.percentage * 100f),
                                                fontWeight = FontWeight.Bold,
                                                color = PinkPrimary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Default Overall summary
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TouchApp,
                                        contentDescription = null,
                                        tint = TextLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.tap_segment_for_details),
                                        color = TextLight,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (filteredAndGroupedData.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                // DRILL DOWN LEGEND & LIST CARD
                Text(
                    text = stringResource(R.string.distribution_breakdowns),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NavyDark,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        filteredAndGroupedData.forEachIndexed { index, point ->
                            val isSelectedInChart = selectedSliceIndex == index
                            DistributionItemRow(
                                point = point,
                                isHighlighted = isSelectedInChart,
                                onClick = {
                                    selectedSliceIndex = if (isSelectedInChart) -1 else index
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet Account Filter
    if (showAccountBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccountBottomSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
            ) {
                Text(
                    text = stringResource(R.string.filter_by_account),
                    fontWeight = FontWeight.ExtraBold,
                    color = NavyDark,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: All Accounts
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAccountIdFilter = null
                                    showAccountBottomSheet = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedAccountIdFilter == null) PinkPrimary.copy(alpha = 0.08f) else Color(0xFFF8FAFC)
                            ),
                            border = if (selectedAccountIdFilter == null) BorderStroke(1.dp, PinkPrimary) else null,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE2E8F0)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AllInclusive,
                                            contentDescription = null,
                                            tint = NavyDark,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.all_accounts),
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark,
                                        fontSize = 14.sp
                                    )
                                }
                                if (selectedAccountIdFilter == null) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = PinkPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Remaining Accounts
                    items(allAccounts) { account ->
                        val isSelected = selectedAccountIdFilter == account.id
                        val icon = when (account.type) {
                            com.oryno.piggy_ledger.data.AccountType.BANK -> Icons.Default.AccountBalance
                            com.oryno.piggy_ledger.data.AccountType.CARD -> Icons.Default.CreditCard
                            com.oryno.piggy_ledger.data.AccountType.CASH -> Icons.Default.Payments
                            com.oryno.piggy_ledger.data.AccountType.WALLET -> Icons.Default.AccountBalanceWallet
                            else -> Icons.Default.AccountBalance
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAccountIdFilter = account.id
                                    showAccountBottomSheet = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PinkPrimary.copy(alpha = 0.08f) else Color(0xFFF8FAFC)
                            ),
                            border = if (isSelected) BorderStroke(1.dp, PinkPrimary) else null,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(PinkPrimary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = PinkPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = account.name,
                                            fontWeight = FontWeight.Bold,
                                            color = NavyDark,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = stringResource(R.string.balance_label, stringResource(R.string.amount_with_currency, String.format(Locale.getDefault(), "%,.2f", account.current_balance), stringResource(R.string.currency_egp))),
                                            fontSize = 11.sp,
                                            color = TextLight
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = PinkPrimary
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
fun CustomBarChart(
    points: List<AnalyticsDataPoint>,
    selectedIndex: Int,
    onSliceSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(points) { points.maxOfOrNull { it.amount } ?: 1.0 }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            // Draw horizontal dotted grid lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val height = size.height
                val width = size.width
                val lineCount = 4
                for (i in 0 until lineCount) {
                    val y = (height / (lineCount - 1)) * i
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // Draw vertical bars side by side (take top 6 points for perfect spacing on compact screens)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                points.take(6).forEachIndexed { index, point ->
                    val isSelected = selectedIndex == index
                    val heightFraction = if (maxVal > 0) (point.amount / maxVal).toFloat() else 0f
                    
                    val animatedHeightFraction by animateFloatAsState(
                        targetValue = heightFraction,
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSliceSelected(index) }
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (isSelected) {
                            Text(
                                text = String.format("%.0f EGP", point.amount),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = point.color,
                                modifier = Modifier.padding(bottom = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxHeight(animatedHeightFraction.coerceIn(0.08f, 1f))
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(
                                    if (isSelected) point.color else point.color.copy(alpha = 0.75f)
                                )
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            2.dp,
                                            NavyDark,
                                            RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                        )
                                    } else Modifier
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Icon(
                            imageVector = point.icon,
                            contentDescription = point.name,
                            tint = if (isSelected) point.color else TextLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomPieChart(
    points: List<AnalyticsDataPoint>,
    chartType: ChartType,
    selectedIndex: Int,
    onSliceSelected: (Int) -> Unit,
    totalAmount: Double,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { 32.dp.toPx() }

    Box(
        modifier = modifier
            .size(240.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points, chartType) {
                    detectTapGestures { offset ->
                        val width = size.width
                        val height = size.height
                        val center = Offset(width / 2f, height / 2f)

                        val x = offset.x - center.x
                        val y = offset.y - center.y

                        val radius = sqrt(x * x + y * y)
                        val maxOuterRadius = width / 2f
                        val minInnerRadius = if (chartType == ChartType.DONUT) maxOuterRadius - strokeWidthPx else 0f

                        if (radius in minInnerRadius..maxOuterRadius) {
                            var angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            var currentStartAngle = -90f
                            var foundIndex = -1

                            for (i in points.indices) {
                                val sweep = points[i].percentage * 360f
                                val normalizedStart = (currentStartAngle + 360f) % 360f
                                val normalizedEnd = (normalizedStart + sweep) % 360f

                                val matches = if (normalizedStart + sweep > 360f) {
                                    angle >= normalizedStart || angle <= normalizedEnd
                                } else {
                                    angle in normalizedStart..normalizedEnd
                                }

                                if (matches) {
                                    foundIndex = i
                                    break
                                }
                                currentStartAngle += sweep
                            }

                            if (foundIndex != -1) {
                                onSliceSelected(foundIndex)
                            }
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            var currentStartAngle = -90f

            points.forEachIndexed { index, point ->
                val sweepAngle = point.percentage * 360f
                if (sweepAngle > 0f) {
                    val isSelected = selectedIndex == index
                    
                    val scaleFactor = if (isSelected) 1.05f else 1.0f
                    val sizeScale = Size(width * scaleFactor, height * scaleFactor)
                    val offsetDelta = Offset(
                        (width - sizeScale.width) / 2f,
                        (height - sizeScale.height) / 2f
                    )

                    if (chartType == ChartType.DONUT) {
                        drawArc(
                            color = point.color,
                            startAngle = currentStartAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(
                                width = if (isSelected) strokeWidthPx * 1.25f else strokeWidthPx,
                                cap = StrokeCap.Butt
                            ),
                            size = sizeScale,
                            topLeft = offsetDelta
                        )
                    } else {
                        drawArc(
                            color = point.color,
                            startAngle = currentStartAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            style = Fill,
                            size = sizeScale,
                            topLeft = offsetDelta
                        )
                    }
                    currentStartAngle += sweepAngle
                }
            }
        }

        if (chartType == ChartType.DONUT) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(40.dp)
            ) {
                val centerTitle = if (selectedIndex in points.indices) {
                    points[selectedIndex].name
                } else {
                    stringResource(R.string.total_value)
                }

                val centerValue = if (selectedIndex in points.indices) {
                    points[selectedIndex].amount
                } else {
                    totalAmount
                }

                Text(
                    text = centerTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextLight,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = String.format("%,.0f", centerValue),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = NavyDark,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.currency_egp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PinkPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun DistributionItemRow(
    point: AnalyticsDataPoint,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    var expandedList by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isHighlighted) point.color.copy(alpha = 0.04f) else Color.Transparent)
            .border(
                1.dp,
                if (isHighlighted) point.color.copy(alpha = 0.15f) else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .clickable {
                onClick()
                expandedList = !expandedList
            }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(point.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = point.icon,
                    contentDescription = null,
                    tint = point.color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = point.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                    Text(
                        text = stringResource(R.string.amount_with_currency, String.format(Locale.getDefault(), "%,.2f", point.amount), stringResource(R.string.currency_egp)),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyDark
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        val animatedWidthFraction by animateFloatAsState(
                            targetValue = point.percentage,
                            animationSpec = spring(stiffness = Spring.StiffnessLow)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedWidthFraction)
                                .clip(CircleShape)
                                .background(point.color)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = String.format("%.1f%%", point.percentage * 100f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = expandedList && isHighlighted,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 8.dp)
                    .border(
                        BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(Color(0xFFF8FAFC))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.transaction_logs),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextLight,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
                point.rawTransactions.forEach { tx ->
                    val dateFormatted = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
                    val isParsed = tx.merchant.contains("|")
                    val desc = if (isParsed) tx.merchant.split("|", limit = 2).getOrNull(1) ?: "" else tx.merchant
                    val displayDesc = if (desc.isNotBlank()) desc else point.name

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = displayDesc,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = dateFormatted,
                                fontSize = 10.sp,
                                color = TextLight
                            )
                        }
                        Text(
                            text = String.format("%s%,.2f", if (tx.amount < 0) "-" else "+", kotlin.math.abs(tx.amount)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (tx.amount < 0) NavyDark else Color(0xFF10B981)
                        )
                    }
                }
            }
        }
    }
}
