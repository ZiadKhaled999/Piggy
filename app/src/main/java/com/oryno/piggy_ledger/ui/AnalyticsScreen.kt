package com.oryno.piggy_ledger.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.data.AccountTransaction
import androidx.compose.ui.res.stringResource
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class AnalyticsTab { SPENDING, REVENUE }

enum class SpendingPeriod(val label: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}
enum class RevenuePeriod(val label: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}



data class SpendingPoint(
    val label: String,
    val value: Double,
    val fullDateLabel: String = ""
)
data class RevenuePoint(
    val label: String,
    val value: Double,
    val fullDateLabel: String = ""
)

val LightBg = Color(0xFFF8F9FA)
val CardBg = Color.White
val TextPrimary = Color(0xFF111827)
val TextSecondary = Color(0xFF6B7280)

val SpendingColors = listOf(
    Color(0xFFF59E0B), // Amber/Orange Activity
    Color(0xFF10B981), // Emerald Meals
    Color(0xFFFBBF24), // Yellow Office supplies
    Color(0xFF3B82F6), // Blue Rewards
    Color(0xFF8B5CF6), // Purple Internet
    Color(0xFF9CA3AF)  // Gray Other
)

val BarBgColor = Color(0xFFE5E7EB)
val BarTopLineColor = Color(0xFF9CA3AF)
val BarActiveColor = PinkPrimary
val BarActiveTopColor = Color.White
val BadgeGreen = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: PiggyLedgerViewModel,
    onBack: () -> Unit
) {
    val allTransactions by viewModel.allAccountTransactions.collectAsState()
    var currentTab by remember { mutableStateOf(AnalyticsTab.SPENDING) }

    val isPrivacyMode by viewModel.isPrivacyModeEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.analytics_title), fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightBg
                )
            )
        },
        containerColor = LightBg
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Segmented Tab
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color(0xFFF3F0F8), RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                AnalyticsTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable { currentTab = tab }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tab == AnalyticsTab.SPENDING) stringResource(R.string.tab_spending) else stringResource(R.string.tab_revenue),
                            color = if (isSelected) PinkPrimary else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (currentTab == AnalyticsTab.SPENDING) {
                SpendingView(allTransactions, isPrivacyMode = isPrivacyMode)
            } else {
                RevenueView(allTransactions, isPrivacyMode = isPrivacyMode)
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendingView(transactions: List<AccountTransaction>, isPrivacyMode: Boolean = false) {
    var selectedPeriod by remember { mutableStateOf(SpendingPeriod.WEEKLY) }
    var showPeriodSheet by remember { mutableStateOf(false) }

    val expenses = transactions.filter { it.amount < 0 }

    val points = remember(expenses, selectedPeriod) {
        val nowCal = Calendar.getInstance()
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH)

        when (selectedPeriod) {
            SpendingPeriod.WEEKLY -> {
                val result = mutableListOf<SpendingPoint>()
                val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                val fullDateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                for (i in 6 downTo 0) {
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                    val start = Calendar.getInstance().apply {
                        timeInMillis = cal.timeInMillis
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val end = Calendar.getInstance().apply {
                        timeInMillis = cal.timeInMillis
                        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    
                    val dayTxs = expenses.filter { it.timestamp in start..end }
                    val value = dayTxs.sumOf { abs(it.amount) }
                    val label = dayFormat.format(cal.time)
                    val fullDateLabel = fullDateFormat.format(cal.time)
                    result.add(SpendingPoint(label, value, fullDateLabel))
                }
                result
            }
            SpendingPeriod.MONTHLY -> {
                val result = mutableListOf<SpendingPoint>()
                for (i in 3 downTo 0) {
                    val start = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -(i + 1) * 7)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val end = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -i * 7)
                        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    val weekTxs = expenses.filter { it.timestamp in start..end }
                    val value = weekTxs.sumOf { abs(it.amount) }
                    result.add(SpendingPoint("W${4 - i}", value, "Week ${4 - i}"))
                }
                result
            }
            SpendingPeriod.YEARLY -> {
                val result = mutableListOf<SpendingPoint>()
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                val fullMonthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                for (i in 11 downTo 0) {
                    var m = currentMonth - i
                    var y = currentYear
                    if (m < 0) { m += 12; y -= 1 }
                    val monthTxs = expenses.filter { 
                        val tCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        tCal.get(Calendar.YEAR) == y && tCal.get(Calendar.MONTH) == m
                    }
                    val value = monthTxs.sumOf { abs(it.amount) }
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, y)
                        set(Calendar.MONTH, m)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    val monthName = monthFormat.format(cal.time)
                    val fullMonthName = fullMonthFormat.format(cal.time)
                    result.add(SpendingPoint(monthName, value, fullMonthName))
                }
                result
            }
        }
    }

    var weekdaysSum = 0.0
    var weekendsSum = 0.0
    
    val periodStart = when (selectedPeriod) {
        SpendingPeriod.WEEKLY -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6); set(Calendar.HOUR_OF_DAY, 0) }.timeInMillis
        SpendingPeriod.MONTHLY -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -28); set(Calendar.HOUR_OF_DAY, 0) }.timeInMillis
        SpendingPeriod.YEARLY -> Calendar.getInstance().apply { add(Calendar.MONTH, -11); set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0) }.timeInMillis
    }
    
    val periodTxs = expenses.filter { it.timestamp >= periodStart }
    periodTxs.forEach { tx ->
        val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            weekendsSum += abs(tx.amount)
        } else {
            weekdaysSum += abs(tx.amount)
        }
    }
    
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val titleText = when (selectedPeriod) {
                    SpendingPeriod.WEEKLY -> "Spending by Day"
                    SpendingPeriod.MONTHLY -> "Spending by Week"
                    SpendingPeriod.YEARLY -> "Spending by Month"
                }
                Text(
                    text = titleText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Row(
                    modifier = Modifier
                        .background(PinkPrimary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .clickable { showPeriodSheet = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Select Period",
                        tint = PinkPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = selectedPeriod.label,
                        fontSize = 14.sp,
                        color = PinkPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            SpendingBarChart(points = points, isPrivacyMode = isPrivacyMode)
            
            Spacer(Modifier.height(24.dp))
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Weekdays", fontSize = 14.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isPrivacyMode) "••••••" else format.format(weekdaysSum),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFE5E7EB)))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Weekends", fontSize = 14.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isPrivacyMode) "••••••" else format.format(weekendsSum),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
    
    if (showPeriodSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPeriodSheet = false },
            containerColor = Color.White
        ) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text(
                    text = "Select Period",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(16.dp))
                SpendingPeriod.entries.forEach { period ->
                    val isSelected = selectedPeriod == period
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                selectedPeriod = period
                                showPeriodSheet = false
                            }
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = period.label,
                            fontSize = 16.sp,
                            color = if (isSelected) PinkPrimary else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = PinkPrimary)
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SpendingBarChart(points: List<SpendingPoint>, isPrivacyMode: Boolean) {
    val textMeasurer = rememberTextMeasurer()
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    
    val maxVal = points.maxOfOrNull { it.value } ?: 0.0
    val yMax = if (maxVal <= 0) 10.0 else maxVal * 1.1

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        val width = size.width
        val height = size.height
        
        val barWidth = (width / points.size) * 0.6f
        val spacing = (width / points.size) * 0.4f
        
        val topPadding = 30.dp.toPx()
        val bottomPadding = 30.dp.toPx()
        val chartHeight = height - topPadding - bottomPadding
        
        val maxPointIndex = points.withIndex().maxByOrNull { it.value.value }?.index ?: -1
        
        points.forEachIndexed { i, point ->
            val isCurrent = i == maxPointIndex && point.value > 0
            val barColor = if (isCurrent) PinkPrimary else PinkPrimary.copy(alpha = 0.25f)
            
            val barHeight = ((point.value / yMax) * chartHeight).toFloat()
            val startX = i * (barWidth + spacing) + spacing / 2
            val startY = topPadding + chartHeight - barHeight
            
            // Draw Bar
            drawRoundRect(
                color = barColor,
                topLeft = Offset(startX, startY),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
            
            // Draw Value on Top
            val valStr = if (isPrivacyMode) "•••" else {
                val fStr = format.format(point.value)
                fStr.replace(".00", "") // Simplified
            }
            val valStyle = TextStyle(fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            val valMeasurer = textMeasurer.measure(valStr, valStyle)
            
            drawText(
                textMeasurer = textMeasurer,
                text = valStr,
                style = valStyle,
                topLeft = Offset(startX + barWidth / 2 - valMeasurer.size.width / 2, startY - valMeasurer.size.height - 4.dp.toPx())
            )
            
            // Draw X-axis Label
            val lblStyle = TextStyle(fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            val lblMeasurer = textMeasurer.measure(point.label, lblStyle)
            
            drawText(
                textMeasurer = textMeasurer,
                text = point.label,
                style = lblStyle,
                topLeft = Offset(startX + barWidth / 2 - lblMeasurer.size.width / 2, height - bottomPadding + 8.dp.toPx())
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevenueView(transactions: List<AccountTransaction>, isPrivacyMode: Boolean = false) {
    var selectedPeriod by remember { mutableStateOf(RevenuePeriod.WEEKLY) }
    var showPeriodSheet by remember { mutableStateOf(false) }
    
    val incomes = transactions.filter { it.amount > 0 }
    
    val points = remember(incomes, selectedPeriod) {
        val nowCal = Calendar.getInstance()
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH)
        
        when (selectedPeriod) {
            RevenuePeriod.WEEKLY -> {
                val result = mutableListOf<RevenuePoint>()
                val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                val fullDateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                for (i in 6 downTo 0) {
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                    val start = Calendar.getInstance().apply {
                        timeInMillis = cal.timeInMillis
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val end = Calendar.getInstance().apply {
                        timeInMillis = cal.timeInMillis
                        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    
                    val dayTxs = incomes.filter { it.timestamp in start..end }
                    val value = dayTxs.sumOf { it.amount }
                    val label = dayFormat.format(cal.time)
                    val fullDateLabel = fullDateFormat.format(cal.time)
                    result.add(RevenuePoint(label, value, fullDateLabel))
                }
                result
            }
            RevenuePeriod.MONTHLY -> {
                val result = mutableListOf<RevenuePoint>()
                for (i in 3 downTo 0) {
                    val start = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -(i + 1) * 7)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val end = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -i * 7)
                        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    val weekTxs = incomes.filter { it.timestamp in start..end }
                    val value = weekTxs.sumOf { it.amount }
                    result.add(RevenuePoint("W${4 - i}", value, "Week ${4 - i}"))
                }
                result
            }
            RevenuePeriod.YEARLY -> {
                val result = mutableListOf<RevenuePoint>()
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                val fullMonthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                for (i in 11 downTo 0) {
                    var m = currentMonth - i
                    var y = currentYear
                    if (m < 0) { m += 12; y -= 1 }
                    val monthTxs = incomes.filter { 
                        val tCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        tCal.get(Calendar.YEAR) == y && tCal.get(Calendar.MONTH) == m
                    }
                    val value = monthTxs.sumOf { it.amount }
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, y)
                        set(Calendar.MONTH, m)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    val monthName = monthFormat.format(cal.time)
                    val fullMonthName = fullMonthFormat.format(cal.time)
                    result.add(RevenuePoint(monthName, value, fullMonthName))
                }
                result
            }
        }
    }
    
    val totalRevenue = points.sumOf { it.value }
    val divisor = points.size.toDouble().coerceAtLeast(1.0)
    val avgValue = totalRevenue / divisor
    val peakPoint = points.maxByOrNull { it.value }

    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val titleText = when (selectedPeriod) {
                    RevenuePeriod.WEEKLY -> "Revenue by Day"
                    RevenuePeriod.MONTHLY -> "Revenue by Week"
                    RevenuePeriod.YEARLY -> "Revenue by Month"
                }
                Text(
                    text = titleText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Row(
                    modifier = Modifier
                        .background(PinkPrimary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .clickable { showPeriodSheet = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Select Period",
                        tint = PinkPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = selectedPeriod.label,
                        fontSize = 14.sp,
                        color = PinkPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            RevenueLineChart(points = points, isPrivacyMode = isPrivacyMode)

            Spacer(Modifier.height(24.dp))
            
            // Stats footer similar to the image
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Average", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isPrivacyMode) "••••••" else format.format(avgValue).replace(".00", ""),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0xFFE5E7EB)))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Highest", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isPrivacyMode) "••••••" else if (peakPoint != null) format.format(peakPoint.value).replace(".00", "") else "$0",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0xFFE5E7EB)))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Total", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isPrivacyMode) "••••••" else format.format(totalRevenue).replace(".00", ""),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
    
    if (showPeriodSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPeriodSheet = false },
            containerColor = Color.White
        ) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text(
                    text = "Select Period",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(16.dp))
                RevenuePeriod.entries.forEach { period ->
                    val isSelected = selectedPeriod == period
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                selectedPeriod = period
                                showPeriodSheet = false
                            }
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = period.label,
                            fontSize = 16.sp,
                            color = if (isSelected) PinkPrimary else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = PinkPrimary)
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun RevenueLineChart(points: List<RevenuePoint>, isPrivacyMode: Boolean = false) {
    val textMeasurer = rememberTextMeasurer()
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    
    val maxVal = points.maxOfOrNull { it.value } ?: 0.0
    val yMax = if (maxVal <= 0) 100.0 else maxVal * 1.15

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val width = size.width
        val height = size.height
        val leftPadding = 8.dp.toPx()
        val rightPadding = 8.dp.toPx()
        val topPadding = 16.dp.toPx()
        val bottomPadding = 32.dp.toPx()
        
        val chartWidth = width - leftPadding - rightPadding
        val chartHeight = height - topPadding - bottomPadding
        
        if (points.isEmpty()) return@Canvas

        val stepX = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth

        fun getX(i: Int): Float = leftPadding + (if (points.size > 1) i * stepX else chartWidth / 2f)
        fun getY(value: Double): Float = topPadding + chartHeight - ((value / yMax) * chartHeight).toFloat()

        // Horizontal grid lines
        val gridRatios = listOf(0.0f, 0.5f, 1.0f)
        gridRatios.forEach { ratio ->
            val gy = topPadding + chartHeight * (1f - ratio)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(0f, gy),
                end = Offset(width, gy),
                strokeWidth = 1f
            )
        }
        
        // Draw path using cubicTo for smooth curves
        val path = Path()
        if (points.isNotEmpty()) {
            path.moveTo(getX(0), getY(points[0].value))
            for (i in 0 until points.size - 1) {
                val x1 = getX(i)
                val y1 = getY(points[i].value)
                val x2 = getX(i + 1)
                val y2 = getY(points[i + 1].value)

                val cp1x = (x1 + x2) / 2f
                val cp1y = y1
                val cp2x = (x1 + x2) / 2f
                val cp2y = y2

                path.cubicTo(cp1x, cp1y, cp2x, cp2y, x2, y2)
            }
        }
        
        drawPath(
            path = path,
            color = PinkPrimary,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Fill area under path
        val fillPath = Path().apply {
            addPath(path)
            if (points.isNotEmpty()) {
                lineTo(getX(points.lastIndex), topPadding + chartHeight)
                lineTo(getX(0), topPadding + chartHeight)
                close()
            }
        }
        
        drawPath(
            path = fillPath,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(PinkPrimary.copy(alpha = 0.4f), Color.Transparent),
                startY = topPadding,
                endY = topPadding + chartHeight
            )
        )
        
        // Draw X-axis labels
        points.forEachIndexed { i, pt ->
            val textLayout = textMeasurer.measure(
                text = pt.label,
                style = TextStyle(color = TextSecondary, fontSize = 10.sp)
            )
            val tw = textLayout.size.width
            val px = getX(i)
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(px - tw / 2f, topPadding + chartHeight + 8.dp.toPx())
            )
        }
    }
}
