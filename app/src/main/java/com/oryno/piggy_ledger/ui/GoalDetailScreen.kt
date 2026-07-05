package com.oryno.piggy_ledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.oryno.piggy_ledger.data.Transaction
import kotlinx.coroutines.launch
import com.oryno.piggy_ledger.ui.theme.GreenAccent
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import com.patrykandpatrick.vico.core.formatter.ValueFormatter
import com.patrykandpatrick.vico.core.chart.values.ChartValues
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.draw.ChartDrawContext
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.chart.dimensions.HorizontalDimensions
import android.graphics.RectF
import android.graphics.Path
import com.patrykandpatrick.vico.core.entry.entryOf
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: String,
    viewModel: PiggyLedgerViewModel,
    onBack: () -> Unit
) {
    val goals by viewModel.goals.collectAsState()
    val goal = goals.find { it.id == goalId }
    val allTransactions by viewModel.allTransactions.collectAsState()
    val transactions = remember(allTransactions, goalId) {
        allTransactions.filter { it.goalId == goalId }
    }
    
    var showDepositDialog by remember { mutableStateOf(false) }
    
    if (goal == null) return

    val savedAmount = transactions.sumOf { it.amount }
    val progress = if (goal.targetAmount > 0) (savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val establishedDate = remember(goal.createdAt) { dateFormatter.format(Date(goal.createdAt)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NavyDark)
            }
            Text(
                text = "BUDGETING",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = TextLight,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TOTAL BALANCE",
                color = TextLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            if (savedAmount > goal.targetAmount) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "+$${String.format("%.2f", savedAmount - goal.targetAmount)} EXTRA",
                    color = GreenAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "$${String.format("%.2f", savedAmount)}",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )
            if (goal.targetAmount > 0.0) {
                Text(
                    text = " / $${String.format("%.2f", goal.targetAmount)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextLight,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            } else {
                Text(
                    text = " (Open Savings)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextLight,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("YOUR BUDGET", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    
                    val isCompleted = goal.targetAmount > 0.0 && savedAmount >= goal.targetAmount
                    Surface(
                        color = if (isCompleted) androidx.compose.ui.graphics.Color(0xFFDCFCE7) else if (goal.targetAmount <= 0.0) androidx.compose.ui.graphics.Color(0xFFEFF6FF) else androidx.compose.ui.graphics.Color(0xFFE0E7FF),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (isCompleted) "COMPLETED" else if (goal.targetAmount <= 0.0) "OPEN SAVINGS" else "IN PROGRESS",
                            color = if (isCompleted) androidx.compose.ui.graphics.Color(0xFF15803D) else if (goal.targetAmount <= 0.0) androidx.compose.ui.graphics.Color(0xFF1D4ED8) else androidx.compose.ui.graphics.Color(0xFF4338CA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(androidx.compose.ui.graphics.Color.White, androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = NavyDark) // Placeholder icon
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(goal.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                if (goal.targetAmount > 0.0) {
                                    Text("$${String.format("%.2f", savedAmount)} / $${String.format("%.2f", goal.targetAmount)}", color = TextLight, fontSize = 14.sp)
                                } else {
                                    Text("$${String.format("%.2f", savedAmount)} saved (Open Savings)", color = TextLight, fontSize = 14.sp)
                                }
                            }
                        }
                        
                        if (goal.targetAmount > 0.0) {
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = NavyDark,
                                trackColor = Color(0xFFCBD5E1)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${(progress * 100).toInt()}%", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                val remaining = goal.targetAmount - savedAmount
                                val remainingText = if (remaining < 0) {
                                    "$${String.format("%.2f", -remaining)} extra"
                                } else if (remaining == 0.0) {
                                    "Goal reached"
                                } else {
                                    "$${String.format("%.2f", remaining)} left"
                                }
                                Text(remainingText, color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        if (goal.targetAmount <= 0.0 || savedAmount < goal.targetAmount) {
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = { showDepositDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Deposit", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = GreenAccent.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenAccent)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Goal Completed", color = GreenAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                var selectedTab by remember { mutableStateOf(0) }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(24.dp))
                        .border(1.dp, androidx.compose.ui.graphics.Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabButton(text = "Overview", isSelected = selectedTab == 0, onClick = { selectedTab = 0 }, modifier = Modifier.weight(1f))
                    TabButton(text = "Progress", isSelected = selectedTab == 1, onClick = { selectedTab = 1 }, modifier = Modifier.weight(1f))
                    TabButton(text = "History", isSelected = selectedTab == 2, onClick = { selectedTab = 2 }, modifier = Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                when (selectedTab) {
                    0 -> {
                        OverviewContent(
                            goal = goal,
                            savedAmount = savedAmount,
                            transactions = transactions,
                            establishedDate = establishedDate
                        )
                    }
                    1 -> {
                        ProgressContent(transactions = transactions, goal = goal)
                    }
                    2 -> {
                        TransactionsContent(transactions = transactions)
                    }
                }
            }
        }
    }
    
    if (showDepositDialog) {
        var amountStr by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        
        ModalBottomSheet(
            onDismissRequest = { showDepositDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.oryno.piggy_ledger.R.drawable.img_piggy_pool),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Add Deposit",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Text(
                    "GROWTH YOUR SAVINGS BALANCE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextLight,
                    letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("DEPOSIT AMOUNT", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("$ 0.00") },
                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = PinkPrimary.copy(alpha = 0.5f),
                        focusedBorderColor = PinkPrimary,
                        focusedLabelColor = PinkPrimary,
                        unfocusedLabelColor = TextLight,
                        cursorColor = PinkPrimary,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("NOTE (REQUIRED)", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("e.g. Monthly contribution") },
                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = PinkPrimary.copy(alpha = 0.5f),
                        focusedBorderColor = PinkPrimary,
                        focusedLabelColor = PinkPrimary,
                        unfocusedLabelColor = TextLight,
                        cursorColor = PinkPrimary,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        val amount = amountStr.replace("$", "").trim().toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            viewModel.addTransaction(goalId, amount, note)
                            showDepositDialog = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                ) {
                    Text("Confirm Deposit", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun OverviewContent(
    goal: com.oryno.piggy_ledger.data.Goal,
    savedAmount: Double,
    transactions: List<Transaction>,
    establishedDate: String
) {
    val now = System.currentTimeMillis()
    val daysRunning = remember(goal.createdAt) {
        val diff = now - goal.createdAt
        TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(1)
    }
    val avgDaily = savedAmount / daysRunning
    val remaining = (goal.targetAmount - savedAmount).coerceAtLeast(0.0)
    val estDaysToComplete = if (avgDaily > 0) (remaining / avgDaily).toInt() else null
    
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val estCompletionDate = remember(estDaysToComplete) {
        if (estDaysToComplete != null) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, estDaysToComplete)
            dateFormatter.format(calendar.time)
        } else "N/A"
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            MetadataCard(label = "ESTABLISHED DATE", value = establishedDate, icon = Icons.Default.Info)
        }
        item {
            MetadataCard(label = "DAYS SINCE START", value = "$daysRunning days", icon = Icons.Default.Timeline)
        }
        item {
            MetadataCard(label = "AVG. DAILY SAVING", value = "$${String.format("%.2f", avgDaily)}", icon = Icons.AutoMirrored.Filled.TrendingUp)
        }
        item {
            MetadataCard(
                label = "EST. COMPLETION DATE", 
                value = if (savedAmount >= goal.targetAmount) "Goal Reached!" else estCompletionDate, 
                icon = Icons.Default.CheckCircle,
                valueColor = if (savedAmount >= goal.targetAmount) GreenAccent else NavyDark
            )
        }
    }
}

@Composable
fun MetadataCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valueColor: androidx.compose.ui.graphics.Color = NavyDark
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(androidx.compose.ui.graphics.Color(0xFFF1F5F9), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = NavyDark, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ProgressContent(transactions: List<Transaction>, goal: com.oryno.piggy_ledger.data.Goal) {
    if (transactions.isEmpty()) {
        EmptyState(message = "Start saving to see your progress chart!")
        return
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val chartColors = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFFF59E0B), // Amber
        Color(0xFF10B981), // Emerald
        Color(0xFF3B82F6), // Blue
        Color(0xFFF43F5E)  // Rose
    )

    val totalSaved = transactions.sumOf { it.amount }

    val depositData = remember(transactions) {
        transactions.map { it.amount }
    }
    
    val maxDeposit = remember(depositData) {
        depositData.maxOrNull()?.toFloat() ?: 0f
    }

    val barChartEntryModelProducer = remember(depositData) {
        // Create entries for each deposit
        val entries = depositData.mapIndexed { index, value ->
            listOf(entryOf(index.toFloat(), value.toFloat()))
        }
        ChartEntryModelProducer(entries)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Savings Challenge", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(4.dp))
                Text("$${String.format("%.0f", totalSaved)}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = NavyDark)
            }
            
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(4.dp)
            ) {
                val coroutineScope = rememberCoroutineScope()
                IconButton(
                    onClick = { 
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (pagerState.currentPage == 0) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Timeline, contentDescription = "Bar Chart", tint = if (pagerState.currentPage == 0) NavyDark else TextLight, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { 
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (pagerState.currentPage == 1) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Polar Area Chart", tint = if (pagerState.currentPage == 1) NavyDark else TextLight, modifier = Modifier.size(20.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (page == 0) {
                    Chart(
                        modifier = Modifier.fillMaxSize(),
                        chart = columnChart(
                            columns = chartColors.map { color ->
                                lineComponent(
                                    color = color,
                                    thickness = if (depositData.size > 8) 16.dp else 32.dp,
                                    shape = Shapes.roundedCornerShape(allPercent = 40)
                                )
                            },
                            spacing = if (depositData.size > 8) 6.dp else 10.dp,
                            dataLabel = axisLabelComponent(
                                color = PinkPrimary,
                                textSize = 9.sp,
                                fontWeight = FontWeight.Black
                            ),
                            dataLabelValueFormatter = object : ValueFormatter {
                                override fun formatValue(value: Float, chartValues: ChartValues): CharSequence {
                                    return if (value > 0f && value >= maxDeposit) "Max" else ""
                                }
                            }
                        ),
                        chartModelProducer = barChartEntryModelProducer,
                        startAxis = null,
                        bottomAxis = rememberBottomAxis(
                            label = axisLabelComponent(
                                color = TextLight.copy(alpha = 0.6f),
                                textSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            valueFormatter = { value, _ -> 
                                if (depositData.size > 10) "" else "D${value.toInt() + 1}"
                            },
                            guideline = null
                        ),
                        isZoomEnabled = false,
                        chartScrollSpec = rememberChartScrollSpec(isScrollEnabled = depositData.size > 7)
                    )
                } else {
                    PolarAreaChart(
                        data = depositData,
                        labels = depositData.mapIndexed { index, _ -> "D${index + 1}" },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Pager Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(2) { iteration ->
                val color = if (pagerState.currentPage == iteration) NavyDark else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(color)
                        .size(6.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Milestones", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)
        Spacer(modifier = Modifier.height(8.dp))
        Text("You've made ${transactions.size} contributions so far. Keep it up!", fontSize = 14.sp, color = TextLight)
    }
}

@Composable
fun PolarAreaChart(
    data: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(data) { data.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f }
    val colors = listOf(
        NavyDark,
        Color(0xFF6366F1),
        Color(0xFF8B5CF6),
        Color(0xFFEC4899),
        Color(0xFFF59E0B),
        Color(0xFF10B981),
        Color(0xFF3B82F6)
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val center = center
        val maxRadius = size.minDimension / 2.2f
        val anglePerSlice = 360f / data.size

        data.forEachIndexed { index, value ->
            val radius = (value.toFloat() / maxVal) * maxRadius + (maxRadius * 0.1f) // Ensure min radius
            val startAngle = index * anglePerSlice - 90f
            
            drawArc(
                color = colors[index % colors.size].copy(alpha = 0.8f),
                startAngle = startAngle,
                sweepAngle = anglePerSlice - 2f, // Gap between slices
                useCenter = true,
                topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )

            // Draw labels
            val textAngle = (startAngle + anglePerSlice / 2) * (PI / 180f).toFloat()
            val labelRadius = maxRadius + 20.dp.toPx()
            val x = center.x + cos(textAngle) * labelRadius
            val y = center.y + sin(textAngle) * labelRadius

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText(labels[index], x, y, paint)
            }
        }
        
        // Draw concentric circles for scale
        for (i in 1..3) {
            val r = (maxRadius / 3) * i
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.3f),
                radius = r,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
fun axisLabelComponent(
    color: Color,
    textSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight
) = com.patrykandpatrick.vico.compose.component.textComponent(
    color = color,
    textSize = textSize,
    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, fontWeight.weight)
)

@Composable
fun TransactionsContent(transactions: List<Transaction>) {
    if (transactions.isEmpty()) {
        EmptyState(message = "No contributions yet.")
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(transactions.sortedByDescending { it.timestamp }) { tx ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(GreenAccent.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.CallMade, contentDescription = null, tint = GreenAccent, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(tx.note.takeIf { it.isNotBlank() } ?: "Deposit", fontWeight = FontWeight.SemiBold, color = NavyDark)
                            val txDate = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
                            Text(txDate, color = TextLight, fontSize = 12.sp)
                        }
                    }
                    Text("+$${String.format("%.2f", tx.amount)}", color = GreenAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.List, contentDescription = null, tint = TextLight.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = TextLight, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Color.White else Color.Transparent)
            .then(
                if (isSelected) Modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) NavyDark else TextLight
        )
    }
}
