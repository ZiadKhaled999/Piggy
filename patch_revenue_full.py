import re

with open('app/src/main/java/com/oryno/piggy_ledger/ui/AnalyticsScreen.kt', 'r') as f:
    content = f.read()

start_idx = content.find("@Composable\nfun RevenueView")
if start_idx != -1:
    content = content[:start_idx]

new_code = """@OptIn(ExperimentalMaterial3Api::class)
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Average", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isPrivacyMode) "••••••" else format.format(avgValue).replace(".00", ""),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0xFFE5E7EB)))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Highest", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isPrivacyMode) "••••••" else if (peakPoint != null) format.format(peakPoint.value).replace(".00", "") else "$0",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0xFFE5E7EB)))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isPrivacyMode) "••••••" else format.format(totalRevenue).replace(".00", ""),
                        fontSize = 14.sp,
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
"""

with open('app/src/main/java/com/oryno/piggy_ledger/ui/AnalyticsScreen.kt', 'w') as f:
    f.write(content + new_code)
