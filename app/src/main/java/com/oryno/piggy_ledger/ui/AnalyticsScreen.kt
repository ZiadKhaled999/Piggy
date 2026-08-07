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
enum class RevenuePeriod(val label: String) {
    DAYS_7("7D"),
    WEEKS_4("4W"),
    MONTHS_6("6M"),
    YEAR_1("1Y")
}

data class SpendingSlice(
    val categoryName: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
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

@Composable
fun SpendingView(transactions: List<AccountTransaction>, isPrivacyMode: Boolean = false) {
    var selectedPeriod by remember { mutableStateOf(SpendingPeriod.LAST_30_DAYS) }
    var showDropdown by remember { mutableStateOf(false) }
    
    val expenses = remember(transactions, selectedPeriod) {
        val startTime = when (selectedPeriod) {
            SpendingPeriod.LAST_7_DAYS -> {
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            SpendingPeriod.LAST_30_DAYS -> {
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -30)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            SpendingPeriod.THIS_YEAR -> {
                Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
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
                        DonutChart(slices = slices, total = totalSpending, isPrivacyMode = isPrivacyMode)
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
fun DonutChart(slices: List<SpendingSlice>, total: Double, isPrivacyMode: Boolean = false) {
    var selectedIndex by remember { mutableStateOf(-1) }
    val textMeasurer = rememberTextMeasurer()
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    val totalText = if (isPrivacyMode) "••••••" else format.format(total).replace(".00", "")
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
fun RevenueView(transactions: List<AccountTransaction>, isPrivacyMode: Boolean = false) {
    var selectedPeriod by remember { mutableStateOf(RevenuePeriod.DAYS_7) }
    
    val incomes = transactions.filter { it.amount > 0 }
    
    val points = remember(incomes, selectedPeriod) {
        val nowCal = Calendar.getInstance()
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH)
        
        when (selectedPeriod) {
            RevenuePeriod.DAYS_7 -> {
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
            RevenuePeriod.WEEKS_4 -> {
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
            RevenuePeriod.MONTHS_6 -> {
                val result = mutableListOf<RevenuePoint>()
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                val fullMonthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                for (i in 5 downTo 0) {
                    var m = currentMonth - i
                    var y = currentYear
                    if (m < 0) {
                        m += 12
                        y -= 1
                    }
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
            RevenuePeriod.YEAR_1 -> {
                val result = mutableListOf<RevenuePoint>()
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                val fullMonthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                for (i in 11 downTo 0) {
                    var m = currentMonth - i
                    var y = currentYear
                    while (m < 0) {
                        m += 12
                        y -= 1
                    }
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
    val previousTotalRevenue = remember(incomes, selectedPeriod) {
        val nowCal = Calendar.getInstance()
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH)
        
        when (selectedPeriod) {
            RevenuePeriod.DAYS_7 -> {
                val startPrev = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -13)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val endPrev = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                incomes.filter { it.timestamp in startPrev..endPrev }.sumOf { it.amount }
            }
            RevenuePeriod.WEEKS_4 -> {
                val startPrev = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -8 * 7)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val endPrev = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -4 * 7)
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                incomes.filter { it.timestamp in startPrev..endPrev }.sumOf { it.amount }
            }
            RevenuePeriod.MONTHS_6 -> {
                var prevSum = 0.0
                for (i in 11 downTo 6) {
                    var m = currentMonth - i
                    var y = currentYear
                    while (m < 0) { m += 12; y -= 1 }
                    val monthTxs = incomes.filter {
                        val tCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        tCal.get(Calendar.YEAR) == y && tCal.get(Calendar.MONTH) == m
                    }
                    prevSum += monthTxs.sumOf { it.amount }
                }
                prevSum
            }
            RevenuePeriod.YEAR_1 -> {
                var prevSum = 0.0
                for (i in 23 downTo 12) {
                    var m = currentMonth - i
                    var y = currentYear
                    while (m < 0) { m += 12; y -= 1 }
                    val monthTxs = incomes.filter {
                        val tCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        tCal.get(Calendar.YEAR) == y && tCal.get(Calendar.MONTH) == m
                    }
                    prevSum += monthTxs.sumOf { it.amount }
                }
                prevSum
            }
        }
    }
    
    val totalChange = when {
        previousTotalRevenue > 0 -> ((totalRevenue - previousTotalRevenue) / previousTotalRevenue) * 100
        else -> 0.0
    } 

    val periodAvgUnit = when (selectedPeriod) {
        RevenuePeriod.DAYS_7 -> "/day"
        RevenuePeriod.WEEKS_4 -> "/week"
        RevenuePeriod.MONTHS_6 -> "/month"
        RevenuePeriod.YEAR_1 -> "/month"
    }

    val divisor = points.size.toDouble().coerceAtLeast(1.0)
    val avgValue = totalRevenue / divisor
    val peakPoint = points.maxByOrNull { it.value }

    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    val totalStr = if (isPrivacyMode) "••••••" else format.format(totalRevenue).replace(".00", "")

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            // Header Row: Section Title and Compact Period Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.tab_revenue),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier
                        .background(Color(0xFFF3F0F8), RoundedCornerShape(20.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    RevenuePeriod.values().forEach { period ->
                        val isSel = selectedPeriod == period
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSel) Color.White else Color.Transparent)
                                .clickable { selectedPeriod = period }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = period.label,
                                fontSize = 12.sp,
                                color = if (isSel) PinkPrimary else TextSecondary,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Headline Amount & Change Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(totalStr, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.width(10.dp))
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
                    Text(
                        text = "$sign${String.format(Locale.US, "%.1f", totalChange)}%",
                        color = badgeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vs_last_period), color = TextSecondary, fontSize = 12.sp)
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Clean Trend Line Chart
            RevenueLineChart(points = points)

            Spacer(Modifier.height(16.dp))

            // Tiny Insights Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9FAFB), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(PinkPrimary, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Average: ${format.format(avgValue).replace(".00", "")} $periodAvgUnit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                if (peakPoint != null && peakPoint.value > 0) {
                    val peakStr = format.format(peakPoint.value).replace(".00", "")
                    Text(
                        text = "Highest: ${peakPoint.label} · $peakStr",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun RevenueLineChart(points: List<RevenuePoint>) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    
    val maxVal = points.maxOfOrNull { it.value } ?: 0.0
    val yMax = if (maxVal <= 0) 100.0 else maxVal * 1.15

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .pointerInput(points) {
                detectTapGestures { offset ->
                    if (points.isEmpty()) return@detectTapGestures
                    val width = size.width
                    val leftPadding = 16.dp.toPx()
                    val rightPadding = 16.dp.toPx()
                    val chartWidth = width - leftPadding - rightPadding
                    
                    val stepX = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth
                    var closestIdx = 0
                    var minDistance = Float.MAX_VALUE
                    
                    for (i in points.indices) {
                        val px = leftPadding + (if (points.size > 1) i * stepX else chartWidth / 2f)
                        val dist = abs(offset.x - px)
                        if (dist < minDistance) {
                            minDistance = dist
                            closestIdx = i
                        }
                    }
                    selectedIndex = if (selectedIndex == closestIdx) null else closestIdx
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val leftPadding = 16.dp.toPx()
        val rightPadding = 16.dp.toPx()
        val topPadding = 28.dp.toPx()
        val bottomPadding = 32.dp.toPx()
        
        val chartWidth = width - leftPadding - rightPadding
        val chartHeight = height - topPadding - bottomPadding
        
        if (points.isEmpty()) return@Canvas

        val stepX = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth

        fun getX(i: Int): Float = leftPadding + (if (points.size > 1) i * stepX else chartWidth / 2f)
        fun getY(value: Double): Float = topPadding + chartHeight - ((value / yMax) * chartHeight).toFloat()

        // Horizontal grid lines
        val gridRatios = listOf(0.33f, 0.66f)
        gridRatios.forEach { ratio ->
            val gy = topPadding + chartHeight * (1f - ratio)
            drawLine(
                color = Color(0xFFF3F0F8),
                start = Offset(leftPadding, gy),
                end = Offset(width - rightPadding, gy),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Line and Fill paths
        val linePath = Path()
        val fillPath = Path()

        val p0X = getX(0)
        val p0Y = getY(points[0].value)

        linePath.moveTo(p0X, p0Y)
        fillPath.moveTo(p0X, height - bottomPadding)
        fillPath.lineTo(p0X, p0Y)

        for (i in 0 until points.size - 1) {
            val x1 = getX(i)
            val y1 = getY(points[i].value)
            val x2 = getX(i + 1)
            val y2 = getY(points[i + 1].value)

            val cx1 = x1 + (x2 - x1) / 2f
            val cy1 = y1
            val cx2 = x1 + (x2 - x1) / 2f
            val cy2 = y2

            linePath.cubicTo(cx1, cy1, cx2, cy2, x2, y2)
            fillPath.cubicTo(cx1, cy1, cx2, cy2, x2, y2)
        }

        val lastX = getX(points.lastIndex)
        fillPath.lineTo(lastX, height - bottomPadding)
        fillPath.close()

        // Draw Area Gradient Fill
        drawPath(
            path = fillPath,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(PinkPrimary.copy(alpha = 0.22f), Color.Transparent),
                startY = topPadding,
                endY = height - bottomPadding
            )
        )

        // Draw Line
        drawPath(
            path = linePath,
            color = PinkPrimary,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw Data Point Nodes & X Labels
        val xLabelStyle = TextStyle(fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        val selXLabelStyle = TextStyle(fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)

        points.forEachIndexed { i, point ->
            val px = getX(i)
            val py = getY(point.value)
            val isSelected = selectedIndex == i

            val labelText = point.label
            val res = textMeasurer.measure(
                text = labelText,
                style = if (isSelected) selXLabelStyle else xLabelStyle
            )
            drawText(
                textMeasurer = textMeasurer,
                text = labelText,
                style = if (isSelected) selXLabelStyle else xLabelStyle,
                topLeft = Offset(px - res.size.width / 2f, height - bottomPadding + 8.dp.toPx())
            )

            if (isSelected) {
                drawCircle(
                    color = PinkPrimary.copy(alpha = 0.2f),
                    radius = 9.dp.toPx(),
                    center = Offset(px, py)
                )
                drawCircle(
                    color = PinkPrimary,
                    radius = 5.dp.toPx(),
                    center = Offset(px, py)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = Offset(px, py)
                )
            } else {
                drawCircle(
                    color = PinkPrimary,
                    radius = 3.5.dp.toPx(),
                    center = Offset(px, py)
                )
                drawCircle(
                    color = Color.White,
                    radius = 1.5.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }

        // Floating Tooltip
        selectedIndex?.let { idx ->
            if (idx in points.indices) {
                val pt = points[idx]
                val px = getX(idx)
                val py = getY(pt.value)
                val valStr = format.format(pt.value).replace(".00", "")

                val ttText = if (pt.fullDateLabel.isNotEmpty()) "${pt.fullDateLabel}\n$valStr" else valStr
                val ttStyle = TextStyle(fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                val ttRes = textMeasurer.measure(ttText, ttStyle)

                val paddingX = 10.dp.toPx()
                val paddingY = 6.dp.toPx()
                val ttW = ttRes.size.width + paddingX * 2
                val ttH = ttRes.size.height + paddingY * 2

                var ttX = px - ttW / 2f
                ttX = ttX.coerceIn(leftPadding, width - rightPadding - ttW)

                var ttY = py - ttH - 8.dp.toPx()
                if (ttY < 0) ttY = py + 10.dp.toPx()

                drawRoundRect(
                    color = Color(0xFF1F2937),
                    topLeft = Offset(ttX, ttY),
                    size = Size(ttW, ttH),
                    cornerRadius = CornerRadius(10.dp.toPx())
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = ttText,
                    style = ttStyle,
                    topLeft = Offset(ttX + paddingX, ttY + paddingY)
                )
            }
        }
    }
}
