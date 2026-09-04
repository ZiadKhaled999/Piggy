package com.oryno.piggy_ledger.ui
import android.widget.Toast

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.oryno.piggy_ledger.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.oryno.piggy_ledger.data.Transaction
import kotlinx.coroutines.launch
import com.oryno.piggy_ledger.ui.theme.PinkAccent
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GoalDetailScreen(
    goalId: String,
    viewModel: PiggyLedgerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val goals by viewModel.goals.collectAsState()
    val isPrivacyMode by viewModel.isPrivacyModeEnabled.collectAsState()
    val appCurrency by viewModel.appCurrency.collectAsState()
    val currencySymbol = getCurrencySymbol(appCurrency)
    val goal = goals.find { it.id == goalId }
    val allTransactions by viewModel.allTransactions.collectAsState()
    val transactions = remember(allTransactions, goalId) {
        allTransactions.filter { it.goalId == goalId }
    }
    
    var showDepositDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberLazyListState()
    
    if (goal == null) return

    val savedAmount = transactions.sumOf { it.amount }
    val progress = if (goal.targetAmount > 0) (savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val establishedDate = remember(goal.createdAt) { dateFormatter.format(Date(goal.createdAt)) }

    val shrinkOffset by remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex == 0) {
                scrollState.firstVisibleItemScrollOffset.toFloat()
            } else {
                500f
            }
        }
    }
    
    val shrinkThreshold = 300f
    val shrinkFactor = (shrinkOffset / shrinkThreshold).coerceIn(0f, 1f)
    
    val now = System.currentTimeMillis()
    val daysRunning = remember(goal.createdAt) {
        val diff = now - goal.createdAt
        TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(1)
    }
    
    val avgDaily = savedAmount / daysRunning
    val remaining = (goal.targetAmount - savedAmount).coerceAtLeast(0.0)
    val estDaysToComplete = if (avgDaily > 0) (remaining / avgDaily).toInt() else null
    
    val estCompletionDate = remember(estDaysToComplete) {
        if (estDaysToComplete != null) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, estDaysToComplete)
            dateFormatter.format(calendar.time)
        } else "N/A"
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Content
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                // Header Spacer
                Spacer(modifier = Modifier.height(180.dp))
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            Text(stringResource(R.string.your_budget_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                            
                            if (goal.targetAmount > 0.0) {
                                val isCompleted = savedAmount >= goal.targetAmount
                                Surface(
                                    color = if (isCompleted) androidx.compose.ui.graphics.Color(0xFFDCFCE7) else androidx.compose.ui.graphics.Color(0xFFE0E7FF),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = if (isCompleted) stringResource(R.string.completed_status) else stringResource(R.string.in_progress_status),
                                        color = if (isCompleted) androidx.compose.ui.graphics.Color(0xFF15803D) else androidx.compose.ui.graphics.Color(0xFF4338CA),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
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
                                if (goal.targetAmount <= 0.0 || savedAmount < goal.targetAmount) {
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
                                        Text(stringResource(R.string.add_deposit), fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = PinkAccent.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PinkAccent)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.goal_completed_msg), color = PinkAccent, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
            
            stickyHeader {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .height(48.dp)
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(24.dp))
                            .border(1.dp, androidx.compose.ui.graphics.Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabButton(text = stringResource(R.string.overview_tab), isSelected = selectedTab == 0, onClick = { selectedTab = 0 }, modifier = Modifier.weight(1f))
                        TabButton(text = stringResource(R.string.history_tab), isSelected = selectedTab == 1, onClick = { selectedTab = 1 }, modifier = Modifier.weight(1f))
                    }
                }
            }

            if (selectedTab == 0) {
                overviewContent(
                    goal = goal,
                    daysRunning = daysRunning,
                    avgDaily = avgDaily,
                    estCompletionDate = estCompletionDate,
                    establishedDate = establishedDate,
                    savedAmount = savedAmount,
                    isPrivacyMode = isPrivacyMode,
                    currencySymbol = currencySymbol
                )
            } else {
                transactionsContent(transactions = transactions, isPrivacyMode = isPrivacyMode, currencySymbol = currencySymbol)
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        
        // Shrinkable Header Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .graphicsLayer {
                    translationY = -shrinkOffset * 0.5f
                    alpha = (1f - shrinkFactor * 1.5f).coerceAtLeast(0f)
                    val scale = (1f - shrinkFactor * 0.3f).coerceAtLeast(0.7f)
                    scaleX = scale
                    scaleY = scale
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_icon), tint = NavyDark)
                }
                Text(
                    text = stringResource(R.string.budgeting_title),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = TextLight,
                    letterSpacing = 1.sp
                )
                IconButton(
                    onClick = { viewModel.togglePrivacyMode(context) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Privacy",
                        tint = NavyDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.total_balance_label),
                    color = TextLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (savedAmount > goal.targetAmount) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPrivacyMode) stringResource(R.string.amount_extra_simple, "$currencySymbol••••••") else stringResource(R.string.amount_extra_simple, "$currencySymbol${String.format("%.2f", savedAmount - goal.targetAmount)}"),
                        color = PinkAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontSize = 18.sp, color = NavyDark)) {
                        append(currencySymbol)
                    }
                    append(if (isPrivacyMode) "••••••" else String.format("%.2f", savedAmount))
                    if (goal.targetAmount > 0.0) {
                        withStyle(style = SpanStyle(fontSize = 20.sp, color = TextLight)) {
                            append(" / ")
                        }
                        withStyle(style = SpanStyle(fontSize = 14.sp, color = TextLight)) {
                            append(currencySymbol)
                        }
                        withStyle(style = SpanStyle(fontSize = 20.sp, color = TextLight)) {
                            append(if (isPrivacyMode) "••••••" else String.format("%.2f", goal.targetAmount))
                        }
                    }
                },
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
                lineHeight = 48.sp
            )
        }

        // Fixed Top Bar when scrolled
        if (shrinkFactor > 0.8f) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_icon), tint = NavyDark)
                    }
                    Text(
                        text = goal.name,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        color = NavyDark,
                        fontSize = 18.sp,
                        maxLines = 1
                    )
                    Text(
                        text = if (isPrivacyMode) "$currencySymbol••••••" else "$currencySymbol${String.format("%.0f", savedAmount)}",
                        fontWeight = FontWeight.ExtraBold,
                        color = PinkPrimary,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
    
    if (showDepositDialog) {
        var amountStr by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        
        ModalBottomSheet(
            onDismissRequest = { showDepositDialog = false },
            sheetState = sheetState,
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
                    stringResource(R.string.add_deposit),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Text(
                    stringResource(R.string.growth_savings_subtitle),
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
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) {
                            amountStr = input
                        }
                    },
                    leadingIcon = {
                        Text(
                            text = currencySymbol,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    },
                    label = { Text(stringResource(R.string.deposit_amount_label), fontWeight = FontWeight.Bold) },
                    placeholder = { Text(stringResource(R.string.zero_amount_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    onValueChange = { if (it.length <= 20) note = it },
                    label = { Text(stringResource(R.string.payment_note), fontWeight = FontWeight.Bold) },
                    placeholder = { Text(stringResource(R.string.monthly_contribution_placeholder)) },
                    supportingText = {
                        Text(
                            text = "${note.length}/20",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            color = TextLight,
                            fontSize = 11.sp
                        )
                    },
                    singleLine = true,
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
                        val amount = amountStr.replace(currencySymbol, "").replace("$", "").trim().toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            viewModel.addTransaction(goalId, amount, note)
                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.toast_savings_added), Toast.LENGTH_SHORT)
                            showDepositDialog = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                ) {
                    Text(stringResource(R.string.confirm_deposit_btn), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun LazyListScope.overviewContent(
    goal: com.oryno.piggy_ledger.data.Goal,
    daysRunning: Long,
    avgDaily: Double,
    estCompletionDate: String,
    establishedDate: String,
    savedAmount: Double,
    isPrivacyMode: Boolean,
    currencySymbol: String = "$"
) {
    item {
        MetadataCard(label = stringResource(R.string.established_date_label), value = establishedDate, icon = Icons.Default.Info)
        Spacer(modifier = Modifier.height(12.dp))
    }
    item {
        MetadataCard(label = stringResource(R.string.days_since_start_label), value = stringResource(R.string.days_count, daysRunning), icon = Icons.Default.Timeline)
        Spacer(modifier = Modifier.height(12.dp))
    }
    item {
        MetadataCard(label = stringResource(R.string.avg_daily_saving_label), value = if (isPrivacyMode) "$currencySymbol••••••" else "$currencySymbol${String.format("%.2f", avgDaily)}", icon = Icons.AutoMirrored.Filled.TrendingUp)
        Spacer(modifier = Modifier.height(12.dp))
    }
    item {
        MetadataCard(
            label = stringResource(R.string.est_completion_date_label), 
            value = if (isPrivacyMode) "••••••" else if (savedAmount >= goal.targetAmount && goal.targetAmount > 0) stringResource(R.string.goal_reached_success) else estCompletionDate, 
            icon = Icons.Default.CheckCircle,
            valueColor = if (!isPrivacyMode && savedAmount >= goal.targetAmount && goal.targetAmount > 0) PinkAccent else NavyDark
        )
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
fun ProgressContent(
    transactions: List<Transaction>,
    goal: com.oryno.piggy_ledger.data.Goal,
    isPrivacyMode: Boolean = false,
    currencySymbol: String = "$"
) {
    val context = LocalContext.current
    if (transactions.isEmpty()) {
        EmptyState(message = stringResource(R.string.start_saving_msg))
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
                Text(stringResource(R.string.savings_challenge_title), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isPrivacyMode) "$currencySymbol••••••" else "$currencySymbol${String.format("%.0f", totalSaved)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = NavyDark
                )
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
                    Icon(Icons.Default.Timeline, contentDescription = stringResource(R.string.bar_chart_desc), tint = if (pagerState.currentPage == 0) NavyDark else TextLight, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { 
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (pagerState.currentPage == 1) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Info, contentDescription = stringResource(R.string.polar_chart_desc), tint = if (pagerState.currentPage == 1) NavyDark else TextLight, modifier = Modifier.size(20.dp))
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
                                    return if (value > 0f && value >= maxDeposit) context.getString(R.string.max_label) else ""
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
        
        Text(stringResource(R.string.milestones_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.contributions_count_msg, transactions.size), fontSize = 14.sp, color = TextLight)
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

fun LazyListScope.transactionsContent(transactions: List<Transaction>, isPrivacyMode: Boolean, currencySymbol: String = "$") {
    if (transactions.isEmpty()) {
        item {
            EmptyState(message = stringResource(R.string.no_contributions_msg))
        }
        return
    }

    items(transactions.sortedByDescending { it.timestamp }) { tx ->
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PinkAccent.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.CallMade,
                        contentDescription = null,
                        tint = PinkAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val noteText = tx.note.trim().takeIf { it.isNotBlank() } ?: stringResource(R.string.deposit_tx_note)
                    Text(
                        text = noteText,
                        fontWeight = FontWeight.SemiBold,
                        color = NavyDark,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    val txDate = remember(tx.timestamp) {
                        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = txDate,
                            color = TextLight,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "|",
                            color = TextLight.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light
                        )
                        val amountFormatted = String.format(Locale.getDefault(), "%,.2f", tx.amount)
                        val amountStr = if (isPrivacyMode) "+$currencySymbol••••••" else "+$currencySymbol$amountFormatted"
                        Text(
                            text = amountStr,
                            color = PinkAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
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
