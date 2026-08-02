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

enum class SpendingPeriod { LAST_7_DAYS, LAST_30_DAYS, THIS_YEAR, ALL_TIME }
enum class RevenuePeriod { WEEKLY, MONTHLY, YEARLY }

data class SpendingSlice(
    val categoryName: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

data class RevenueBar(
    val label: String,
    val value: Double,
    val change: Double
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
                SpendingView(allTransactions)
            } else {
                RevenueView(allTransactions)
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SpendingView(transactions: List<AccountTransaction>) {
    var selectedPeriod by remember { mutableStateOf(SpendingPeriod.LAST_30_DAYS) }
    var showDropdown by remember { mutableStateOf(false) }
    
    val expenses = remember(transactions, selectedPeriod) {
        val cal = Calendar.getInstance()
        val startTime = when (selectedPeriod) {
            SpendingPeriod.LAST_7_DAYS -> { cal.add(Calendar.DAY_OF_YEAR, -7); cal.timeInMillis }
            SpendingPeriod.LAST_30_DAYS -> { cal.add(Calendar.DAY_OF_YEAR, -30); cal.timeInMillis }
            SpendingPeriod.THIS_YEAR -> { cal.set(Calendar.DAY_OF_YEAR, 1); cal.timeInMillis }
            SpendingPeriod.ALL_TIME -> 0L
        }
        transactions.filter { it.amount < 0 && it.timestamp >= startTime }
    }

    val totalSpending = expenses.sumOf { abs(it.amount) }
    val slices = remember(expenses) {
        val grouped = expenses.groupBy { tx ->
            if (tx.merchant.contains("|")) {
                val rawCat = tx.merchant.split("|", limit = 2)[0]
                val cleaned = cleanCategoryKey(rawCat)
                val catItem = categoriesList.find { 
                    it.key.equals(rawCat, ignoreCase = true) || 
                    it.key.equals("cat_$cleaned", ignoreCase = true) ||
                    cleanCategoryKey(it.key).equals(cleaned, ignoreCase = true)
                }
                catItem?.key ?: cleaned
            } else {
                val raw = tx.merchant.trim()
                if (raw.startsWith("cat_", ignoreCase = true) || raw.startsWith("cat ", ignoreCase = true) || raw.startsWith("custom_", ignoreCase = true)) {
                    val cleaned = cleanCategoryKey(raw)
                    val catItem = categoriesList.find { 
                        it.key.equals(raw, ignoreCase = true) || 
                        it.key.equals("cat_$cleaned", ignoreCase = true) ||
                        cleanCategoryKey(it.key).equals(cleaned, ignoreCase = true)
                    }
                    catItem?.key ?: cleaned
                } else {
                    val legacyCategory = getCategoryAndIconForMerchant(tx.merchant).first
                    if (legacyCategory.isNotBlank() && legacyCategory != "Other") legacyCategory else "cat_other"
                }
            }
        }
        val rawSlices = grouped.map { (catKey, txs) ->
            val amount = txs.sumOf { abs(it.amount) }
            val percentage = if (totalSpending > 0) (amount / totalSpending).toFloat() else 0f
            SpendingSlice(catKey, amount, percentage, Color.Gray)
        }.sortedByDescending { it.amount }

        val top = rawSlices.take(5).toMutableList()
        val rest = rawSlices.drop(5)
        if (rest.isNotEmpty()) {
            val restAmount = rest.sumOf { it.amount }
            val restPercentage = if (totalSpending > 0) (restAmount / totalSpending).toFloat() else 0f
            top.add(SpendingSlice("category_other_marker", restAmount, restPercentage, Color.Gray))
        }

        top.mapIndexed { index, slice -> 
            val finalName = if (slice.categoryName == "category_other_marker") "Other" else slice.categoryName
            slice.copy(categoryName = finalName, color = SpendingColors[index % SpendingColors.size])
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.tab_spending), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(LightBg, RoundedCornerShape(16.dp))
                            .clickable { showDropdown = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        val periodText = when (selectedPeriod) {
                            SpendingPeriod.LAST_7_DAYS -> stringResource(R.string.filter_last_7_days)
                            SpendingPeriod.LAST_30_DAYS -> stringResource(R.string.filter_last_30_days)
                            SpendingPeriod.THIS_YEAR -> stringResource(R.string.filter_this_year)
                            SpendingPeriod.ALL_TIME -> stringResource(R.string.filter_all_time)
                        }
                        Text(periodText, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    }
                    
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        SpendingPeriod.entries.forEach { period ->
                            val text = when (period) {
                                SpendingPeriod.LAST_7_DAYS -> stringResource(R.string.filter_last_7_days)
                                SpendingPeriod.LAST_30_DAYS -> stringResource(R.string.filter_last_30_days)
                                SpendingPeriod.THIS_YEAR -> stringResource(R.string.filter_this_year)
                                SpendingPeriod.ALL_TIME -> stringResource(R.string.filter_all_time)
                            }
                            DropdownMenuItem(
                                text = { Text(text) },
                                onClick = {
                                    selectedPeriod = period
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            if (slices.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_spending_data), color = TextSecondary)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.fillMaxWidth(0.6f).aspectRatio(1f)) {
                        DonutChart(slices = slices, total = totalSpending)
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        slices.forEach { slice ->
                            val catName = resolveCategoryDisplayName(slice.categoryName)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(slice.color))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = catName,
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
fun DonutChart(slices: List<SpendingSlice>, total: Double) {
    var selectedIndex by remember { mutableStateOf(-1) }
    val textMeasurer = rememberTextMeasurer()
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    val totalText = format.format(total).replace(".00", "")
    val totalLabel = stringResource(R.string.analytics_total)

    val gapAngle = if (slices.size > 1) 3f else 0f
    val totalGaps = if (slices.size > 1) slices.size * gapAngle else 0f
    val availableSweep = 360f - totalGaps

    val sliceAngles = remember(slices) {
        if (slices.isEmpty()) emptyList()
        else {
            val count = slices.size
            val minSweep = if (availableSweep >= count * 8f) 8f else (availableSweep / count)
            
            var allocatedMin = 0f
            var largeSumPct = 0f
            val isSmall = BooleanArray(count)
            
            slices.forEachIndexed { i, slice ->
                val raw = slice.percentage * availableSweep
                if (raw < minSweep) {
                    isSmall[i] = true
                    allocatedMin += minSweep
                } else {
                    largeSumPct += slice.percentage
                }
            }
            
            val remainingSweep = (availableSweep - allocatedMin).coerceAtLeast(0f)
            val sweeps = FloatArray(count)
            slices.forEachIndexed { i, slice ->
                if (isSmall[i]) {
                    sweeps[i] = minSweep
                } else {
                    sweeps[i] = if (largeSumPct > 0) (slice.percentage / largeSumPct) * remainingSweep else (remainingSweep / count)
                }
            }
            
            var currentStart = -90f
            slices.indices.map { i ->
                val start = currentStart
                val sweep = sweeps[i]
                currentStart += sweep + gapAngle
                Pair(start, sweep)
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(slices, sliceAngles) {
                detectTapGestures { offset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val angle = (Math.toDegrees(atan2((offset.y - center.y).toDouble(), (offset.x - center.x).toDouble())) + 360) % 360
                    
                    var hit = -1
                    sliceAngles.forEachIndexed { i, (start, sweep) ->
                        val normStart = (start % 360 + 360) % 360
                        val normEnd = ((start + sweep) % 360 + 360) % 360
                        
                        val isHit = if (normStart <= normEnd) {
                            angle >= normStart && angle <= normEnd
                        } else {
                            angle >= normStart || angle <= normEnd
                        }
                        if (isHit) {
                            hit = i
                        }
                    }
                    selectedIndex = if (selectedIndex == hit) -1 else hit
                }
            }
    ) {
        val strokeWidth = 24.dp.toPx()
        val radius = (size.minDimension - strokeWidth - 40.dp.toPx()) / 2 
        val center = Offset(size.width / 2f, size.height / 2f)
        
        val titleStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        val subtitleStyle = TextStyle(fontSize = 12.sp, color = TextSecondary)
        
        val titleResult = textMeasurer.measure(totalText, titleStyle)
        val subtitleResult = textMeasurer.measure(totalLabel, subtitleStyle)
        
        drawText(
            textMeasurer = textMeasurer,
            text = totalText,
            style = titleStyle,
            topLeft = Offset(center.x - titleResult.size.width / 2f, center.y - titleResult.size.height / 2f - 8.dp.toPx())
        )
        drawText(
            textMeasurer = textMeasurer,
            text = totalLabel,
            style = subtitleStyle,
            topLeft = Offset(center.x - subtitleResult.size.width / 2f, center.y + 4.dp.toPx())
        )

        slices.forEachIndexed { index, slice ->
            if (index < sliceAngles.size) {
                val (startAngle, sweepAngle) = sliceAngles[index]
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )

                if (selectedIndex == index) {
                    val midAngle = startAngle + sweepAngle / 2f
                    val midAngleRad = Math.toRadians(midAngle.toDouble())
                    
                    val tooltipRadius = radius + strokeWidth
                    val tooltipX = center.x + cos(midAngleRad).toFloat() * tooltipRadius
                    val tooltipY = center.y + sin(midAngleRad).toFloat() * tooltipRadius
                    
                    val sliceAmount = format.format(slice.amount).replace(".00", "")
                    val pct = (slice.percentage * 100).toInt()
                    val tooltipText = "    $sliceAmount ($pct%)"
                    val ttStyle = TextStyle(fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    val ttMeasurer = textMeasurer.measure(tooltipText, ttStyle)
                    
                    val paddingX = 8.dp.toPx()
                    val paddingY = 4.dp.toPx()
                    
                    val ttWidth = ttMeasurer.size.width + paddingX * 2
                    val ttHeight = ttMeasurer.size.height + paddingY * 2
                    
                    var rectX = tooltipX
                    if (cos(midAngleRad) < 0) rectX -= ttWidth
                    var rectY = tooltipY - ttHeight / 2
                    
                    rectX = rectX.coerceIn(0f, size.width - ttWidth)
                    rectY = rectY.coerceIn(0f, size.height - ttHeight)
                    
                    drawRoundRect(
                        color = Color(0xFF1F2937),
                        topLeft = Offset(rectX, rectY),
                        size = Size(ttWidth, ttHeight),
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                    
                    drawCircle(
                        color = slice.color,
                        radius = 4.dp.toPx(),
                        center = Offset(rectX + paddingX + 8.dp.toPx(), rectY + ttHeight/2)
                    )
                    
                    drawText(
                        textMeasurer = textMeasurer,
                        text = tooltipText,
                        style = ttStyle,
                        topLeft = Offset(rectX + paddingX, rectY + paddingY)
                    )
                }
            }
        }
    }
}

@Composable
fun RevenueView(transactions: List<AccountTransaction>) {
    var selectedPeriod by remember { mutableStateOf(RevenuePeriod.WEEKLY) }
    
    val incomes = transactions.filter { it.amount > 0 }
    
    val rawBars = remember(incomes, selectedPeriod) {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)
        
        when (selectedPeriod) {
            RevenuePeriod.WEEKLY -> {
                val result = mutableListOf<RevenueBar>()
                val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                var prevValue = 0.0
                for (i in 6 downTo 0) {
                    val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                    val start = day.apply { 
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) 
                    }.timeInMillis
                    val end = day.apply { 
                        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) 
                    }.timeInMillis
                    
                    val dayTxs = incomes.filter { it.timestamp in start..end }
                    val value = dayTxs.sumOf { it.amount }
                    val change = if (prevValue > 0) ((value - prevValue) / prevValue) * 100 else 0.0
                    val dayIndex = (6 - i) % 7
                    result.add(RevenueBar(dayLabels[dayIndex], value, change))
                    prevValue = value
                }
                result
            }
            RevenuePeriod.MONTHLY -> {
                val result = mutableListOf<RevenueBar>()
                var prevValue = 0.0
                for (i in 3 downTo 0) {
                    val start = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(i+1)*7) }.timeInMillis
                    val end = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i*7) }.timeInMillis
                    val weekTxs = incomes.filter { it.timestamp in start..end }
                    val value = weekTxs.sumOf { it.amount }
                    val change = if (prevValue > 0) ((value - prevValue) / prevValue) * 100 else 0.0
                    result.add(RevenueBar("W${4-i}", value, change))
                    prevValue = value
                }
                result
            }
            RevenuePeriod.YEARLY -> {
                val result = mutableListOf<RevenueBar>()
                var prevValue = 0.0
                for (i in 5 downTo 0) {
                    val m = (currentMonth - i + 12) % 12
                    val y = if (currentMonth - i < 0) currentYear - 1 else currentYear
                    val monthTxs = incomes.filter { 
                        val tCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        tCal.get(Calendar.YEAR) == y && tCal.get(Calendar.MONTH) == m
                    }
                    val value = monthTxs.sumOf { it.amount }
                    val change = if (prevValue > 0) ((value - prevValue) / prevValue) * 100 else 0.0
                    
                    val monthName = SimpleDateFormat("MMM", Locale.getDefault()).apply {
                        calendar = Calendar.getInstance().apply { set(Calendar.MONTH, m) }
                    }.format(Date())
                    
                    result.add(RevenueBar(monthName, value, change))
                    prevValue = value
                }
                result
            }
        }
    }
    
    val bars = rawBars
    
    val totalRevenue = bars.sumOf { it.value }
    val previousTotalRevenue = remember(incomes, selectedPeriod) {
        when (selectedPeriod) {
            RevenuePeriod.YEARLY -> {
                var prevSum = 0.0
                for (i in 11 downTo 6) {
                    val y = Calendar.getInstance().get(Calendar.YEAR) - (i / 12)
                    val m = (Calendar.getInstance().get(Calendar.MONTH) - (i % 12) + 12) % 12
                    val monthTxs = incomes.filter {
                        val tCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        tCal.get(Calendar.YEAR) == y && tCal.get(Calendar.MONTH) == m
                    }
                    prevSum += monthTxs.sumOf { it.amount }
                }
                prevSum
            }
            RevenuePeriod.MONTHLY -> {
                var prevSum = 0.0
                for (i in 7 downTo 4) {
                    val start = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(i+1)*7) }.timeInMillis
                    val end = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i*7) }.timeInMillis
                    val weekTxs = incomes.filter { it.timestamp in start..end }
                    prevSum += weekTxs.sumOf { it.amount }
                }
                prevSum
            }
            RevenuePeriod.WEEKLY -> {
                var prevSum = 0.0
                for (i in 13 downTo 7) {
                    val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                    val start = day.apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                    }.timeInMillis
                    val end = day.apply {
                        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
                    }.timeInMillis
                    val dayTxs = incomes.filter { it.timestamp in start..end }
                    prevSum += dayTxs.sumOf { it.amount }
                }
                prevSum
            }
        }
    }
    
    val totalChange = when {
        previousTotalRevenue > 0 -> ((totalRevenue - previousTotalRevenue) / previousTotalRevenue) * 100
        else -> 0.0
    } 
    
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    val totalStr = format.format(totalRevenue).replace(".00", "")

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            // Material 3 Expressive Period Selector Distributed Across Full Width
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3F0F8), RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                val options = listOf(RevenuePeriod.WEEKLY, RevenuePeriod.MONTHLY, RevenuePeriod.YEARLY)
                options.forEach { period ->
                    val isSel = selectedPeriod == period
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSel) Color.White else Color.Transparent)
                            .clickable { selectedPeriod = period }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val text = when(period) {
                            RevenuePeriod.WEEKLY -> stringResource(R.string.filter_weekly)
                            RevenuePeriod.MONTHLY -> stringResource(R.string.filter_monthly)
                            RevenuePeriod.YEARLY -> stringResource(R.string.filter_yearly)
                        }
                        Text(
                            text = text,
                            fontSize = 13.sp,
                            color = if (isSel) PinkPrimary else TextSecondary,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(totalStr, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.width(12.dp))
                val isPositive = totalChange >= 0
                val badgeColor = if (isPositive) BadgeGreen else Color.Red
                val badgeIcon = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
                val sign = if (isPositive) "+" else ""
                
                Row(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("$sign${String.format(Locale.US, "%.1f", totalChange)}%", color = badgeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vs_last_period), color = TextSecondary, fontSize = 12.sp)
            }
            
            Spacer(Modifier.height(24.dp))
            
            RevenueBarChart(bars = bars)
        }
    }
}

@Composable
fun RevenueBarChart(bars: List<RevenueBar>) {
    var selectedIndex by remember { mutableStateOf(bars.lastIndex.coerceAtLeast(0)) }
    val textMeasurer = rememberTextMeasurer()
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    
    val maxBarValue = bars.maxOfOrNull { it.value } ?: 15000.0
    val yMax = if (maxBarValue <= 0) 15000.0 else (Math.ceil(maxBarValue / 5000.0) * 5000.0).coerceAtLeast(15000.0)
    val targetValue = if (yMax >= 15000.0) 10000.0 else (yMax * 0.67)

    // Design Palette matching uploaded reference image:
    val regularBarColor = Color(0xFF5C45A0)    // Deep Purple capsule
    val targetBarColor = Color(0xFF70DC88)     // Mint Green capsule
    val targetLineColor = Color(0xFFA58BC0)    // Lavender / Purple target line
    val targetTextColor = Color(0xFF7A4FA0)    // Purple text for target label
    val starburstFillColor = Color(0xFFDCF8DA)  // Light mint flower badge
    val starburstCheckColor = Color(0xFF135D2A) // Dark green checkmark

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .pointerInput(bars) {
                detectTapGestures { offset ->
                    val width = size.width
                    val rightPadding = 48.dp.toPx()
                    val leftPadding = 16.dp.toPx()
                    val chartWidth = width - leftPadding - rightPadding
                    val barWidth = 36.dp.toPx()
                    val totalSpacing = chartWidth - (bars.size * barWidth)
                    val spacing = (totalSpacing / (bars.size + 1)).coerceAtLeast(4.dp.toPx())
                    
                    var hit = -1
                    for (i in bars.indices) {
                        val x = leftPadding + spacing + i * (barWidth + spacing)
                        if (offset.x >= x - spacing / 2 && offset.x <= x + barWidth + spacing / 2) {
                            hit = i
                            break
                        }
                    }
                    if (hit != -1) {
                        selectedIndex = hit
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val bottomPadding = 32.dp.toPx()
        val topPadding = 24.dp.toPx() 
        val rightPadding = 48.dp.toPx()
        val leftPadding = 16.dp.toPx()
        
        val chartWidth = width - leftPadding - rightPadding
        val chartHeight = height - bottomPadding - topPadding
        
        val labelStyle = TextStyle(fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        val targetLabelStyle = TextStyle(fontSize = 12.sp, color = targetTextColor, fontWeight = FontWeight.Bold)

        // 1. Draw Right Y-Axis grid lines & labels (0k, 5k, 10k, 15k)
        val steps = listOf(0.0, yMax * 0.333, targetValue, yMax)
        steps.forEach { v ->
            val y = topPadding + chartHeight - ((v / yMax) * chartHeight).toFloat()
            val isTarget = Math.abs(v - targetValue) < 1.0
            
            val vStr = if (v >= 1000) "${(v / 1000).toInt()}k" else v.toInt().toString()
            val textRes = textMeasurer.measure(vStr, if (isTarget) targetLabelStyle else labelStyle)
            
            // Draw Right Axis label
            drawText(
                textMeasurer = textMeasurer,
                text = vStr,
                style = if (isTarget) targetLabelStyle else labelStyle,
                topLeft = Offset(width - rightPadding + 10.dp.toPx(), y - textRes.size.height / 2f)
            )

            if (isTarget) {
                // Target horizontal purple line across full chart width
                drawLine(
                    color = targetLineColor,
                    start = Offset(leftPadding, y),
                    end = Offset(width - rightPadding + 4.dp.toPx(), y),
                    strokeWidth = 2.dp.toPx()
                )
            } else if (v > 0) {
                // Subtle horizontal background grid line
                drawLine(
                    color = Color(0xFFF3F0F8),
                    start = Offset(leftPadding, y),
                    end = Offset(width - rightPadding, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
        
        // 2. Draw Capsule Bars
        val barWidth = 36.dp.toPx()
        val totalSpacing = chartWidth - (bars.size * barWidth)
        val spacing = (totalSpacing / (bars.size + 1)).coerceAtLeast(4.dp.toPx())
        val capsuleRadius = barWidth / 2f
        
        bars.forEachIndexed { i, bar ->
            val x = leftPadding + spacing + i * (barWidth + spacing)
            val isSelected = i == selectedIndex
            val isTargetReached = bar.value >= targetValue
            
            // X-Axis day/period label
            val xLabelStyle = if (isSelected) {
                TextStyle(fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            } else {
                TextStyle(fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            }
            val xRes = textMeasurer.measure(bar.label, xLabelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = bar.label,
                style = xLabelStyle,
                topLeft = Offset(x + barWidth / 2f - xRes.size.width / 2f, height - bottomPadding + 8.dp.toPx())
            )
            
            // Bar height calculation
            val barHeight = (((bar.value / yMax) * chartHeight).toFloat()).coerceAtLeast(barWidth)
            val y = topPadding + chartHeight - barHeight
            
            // Choose Bar Color based on target threshold
            val barColor = if (isTargetReached) targetBarColor else regularBarColor
            
            // Draw Capsule Pill Bar
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(capsuleRadius, capsuleRadius)
            )
            
            // 3. Draw Starburst Flower Badge on Target Bars
            if (isTargetReached) {
                val badgeCenter = Offset(x + barWidth / 2f, y + barWidth * 0.75f)
                val badgeRadius = barWidth * 0.38f
                
                drawStarburstBadge(
                    drawScope = this,
                    center = badgeCenter,
                    badgeRadius = badgeRadius,
                    badgeColor = starburstFillColor,
                    checkColor = starburstCheckColor
                )
            }

            // 4. Draw Floating Tooltip for Selected Bar
            if (isSelected) {
                val valStr = format.format(bar.value).replace(".00", "")
                val tooltipStyle = TextStyle(fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                val ttRes = textMeasurer.measure(valStr, tooltipStyle)
                
                val ttW = ttRes.size.width + 16.dp.toPx()
                val ttH = ttRes.size.height + 8.dp.toPx()
                val ttX = (x + barWidth / 2f - ttW / 2f).coerceIn(leftPadding, width - rightPadding - ttW)
                var ttY = y - ttH - 6.dp.toPx()
                if (ttY < 0f) ttY = y + 8.dp.toPx()
                
                drawRoundRect(
                    color = Color(0xFF1F2937),
                    topLeft = Offset(ttX, ttY),
                    size = Size(ttW, ttH),
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
                
                drawText(
                    textMeasurer = textMeasurer,
                    text = valStr,
                    style = tooltipStyle,
                    topLeft = Offset(ttX + 8.dp.toPx(), ttY + 4.dp.toPx())
                )
            }
        }
    }
}

private fun drawStarburstBadge(
    drawScope: DrawScope,
    center: Offset,
    badgeRadius: Float,
    badgeColor: Color,
    checkColor: Color
) {
    val petals = 12
    val path = Path()
    val angleStep = (Math.PI / petals).toFloat()
    for (i in 0 until petals * 2) {
        val angle = i * angleStep - (Math.PI / 2).toFloat()
        val r = if (i % 2 == 0) badgeRadius else badgeRadius * 0.82f
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawScope.drawPath(path, color = badgeColor)

    // Checkmark inside badge
    val checkPath = Path().apply {
        moveTo(center.x - badgeRadius * 0.35f, center.y + badgeRadius * 0.05f)
        lineTo(center.x - badgeRadius * 0.08f, center.y + badgeRadius * 0.30f)
        lineTo(center.x + badgeRadius * 0.35f, center.y - badgeRadius * 0.22f)
    }
    drawScope.drawPath(
        path = checkPath,
        color = checkColor,
        style = Stroke(width = badgeRadius * 0.24f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}
