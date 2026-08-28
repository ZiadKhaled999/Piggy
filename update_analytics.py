import re

with open('app/src/main/java/com/oryno/piggy_ledger/ui/AnalyticsScreen.kt', 'r') as f:
    content = f.read()

# Replace RevenuePeriod
old_enum = """enum class RevenuePeriod(val label: String) {
    DAYS_7("7D"),
    WEEKS_4("4W"),
    MONTHS_6("6M"),
    YEAR_1("1Y")
}"""

new_enum = """enum class RevenuePeriod(val label: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}"""

content = content.replace(old_enum, new_enum)

# Add imports
imports_to_add = """import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Check
"""
content = content.replace("import androidx.compose.material3.Text", imports_to_add + "import androidx.compose.material3.Text")

start_idx = content.find('@Composable\nfun RevenueView')
# Since RevenueLineChart is the last function, we can just replace everything from start_idx to the end
end_idx = content.find('fun AnalyticsEmptyState', start_idx)

if end_idx == -1:
    end_idx = len(content)
else:
    end_idx = content.rfind('@Composable', start_idx, end_idx)

new_revenue_code = """@OptIn(ExperimentalMaterial3Api::class)
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

    var weekdaysSum = 0.0
    var weekendsSum = 0.0
    
    val periodStart = when (selectedPeriod) {
        RevenuePeriod.WEEKLY -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6); set(Calendar.HOUR_OF_DAY, 0) }.timeInMillis
        RevenuePeriod.MONTHLY -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -28); set(Calendar.HOUR_OF_DAY, 0) }.timeInMillis
        RevenuePeriod.YEARLY -> Calendar.getInstance().apply { add(Calendar.MONTH, -11); set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0) }.timeInMillis
    }
    
    val periodTxs = incomes.filter { it.timestamp >= periodStart }
    periodTxs.forEach { tx ->
        val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            weekendsSum += tx.amount
        } else {
            weekdaysSum += tx.amount
        }
    }
    
    val insightText = if (weekendsSum > weekdaysSum) {
        val diff = if (weekdaysSum > 0) ((weekendsSum - weekdaysSum) / weekdaysSum * 100).toInt() else 100
        "You earn $diff% more on weekends"
    } else if (weekdaysSum > weekendsSum) {
        val diff = if (weekendsSum > 0) ((weekdaysSum - weekendsSum) / weekendsSum * 100).toInt() else 100
        "You earn $diff% more on weekdays"
    } else {
        "Your earnings are balanced"
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
                Text(
                    text = "Spending by Day",
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
            
            RevenueBarChart(points = points, isPrivacyMode = isPrivacyMode)
            
            Spacer(Modifier.height(24.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF9E6), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = "Insight",
                    tint = Color(0xFFD4A017),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = insightText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8B6914)
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Weekdays", fontSize = 14.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isPrivacyMode) "••••••" else format.format(weekdaysSum),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFE5E7EB)))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Weekends", fontSize = 14.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isPrivacyMode) "••••••" else format.format(weekendsSum),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
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
fun RevenueBarChart(points: List<RevenuePoint>, isPrivacyMode: Boolean) {
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
        
        points.forEachIndexed { i, point ->
            val isCurrent = i == points.size - 1 // Highlight last one
            val barColor = if (isCurrent) Color(0xFF6B4EFF) else Color(0xFFC7D0FF) // Dark purple / light purple
            
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
"""

new_content = content[:start_idx] + new_revenue_code
with open('app/src/main/java/com/oryno/piggy_ledger/ui/AnalyticsScreen.kt', 'w') as f:
    f.write(new_content)

