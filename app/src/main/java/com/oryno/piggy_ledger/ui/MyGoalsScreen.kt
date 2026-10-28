package com.oryno.piggy_ledger.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Goal
import com.oryno.piggy_ledger.data.Transaction
import com.oryno.piggy_ledger.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGoalsScreen(
    viewModel: PiggyLedgerViewModel,
    onNavigateToGoal: (String) -> Unit,
    onNavigateToCreateGoal: () -> Unit,
    onBack: () -> Unit
) {
    val goals by viewModel.goals.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val isPrivacyMode by viewModel.isPrivacyModeEnabled.collectAsState()
    val appCurrency by viewModel.appCurrency.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var goalToDelete by remember { mutableStateOf<Goal?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var confirmNameInput by remember { mutableStateOf("") }

    if (goalToDelete != null) {
        ModalBottomSheet(
            onDismissRequest = {
                goalToDelete = null
                confirmNameInput = ""
            },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = NavyDark.copy(alpha = 0.2f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.delete_goal_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = goalToDelete?.name ?: "",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PinkPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(goalToDelete?.name ?: ""))
                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.copied), Toast.LENGTH_SHORT)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = PinkPrimary.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.delete_goal_warning),
                    fontSize = 14.sp,
                    color = TextLight,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.delete_goal_confirm_hint),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = NavyDark,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmNameInput,
                    onValueChange = { confirmNameInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.delete_goal_confirm_placeholder)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PinkPrimary,
                        unfocusedBorderColor = NavyDark.copy(alpha = 0.1f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                val canDelete = confirmNameInput.trim() == goalToDelete?.name?.trim()

                Button(
                    onClick = {
                        goalToDelete?.let {
                            viewModel.deleteGoal(it.id)
                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.toast_goal_deleted), Toast.LENGTH_SHORT)
                        }
                        goalToDelete = null
                        confirmNameInput = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = canDelete,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF4D4D),
                        disabledContainerColor = Color(0xFFFF4D4D).copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.delete_button),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // TOP BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_icon),
                        tint = NavyDark
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.my_goals),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NavyDark,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { viewModel.togglePrivacyMode(context) },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        painter = if (isPrivacyMode) painterResource(id = R.drawable.ic_eye_closed) else painterResource(id = R.drawable.ic_eye_open),
                        contentDescription = "Toggle Privacy",
                        tint = NavyDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (goals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.img_no_goals),
                            contentDescription = null,
                            modifier = Modifier.size(240.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            stringResource(R.string.no_goals_yet),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.start_first_goal),
                            fontSize = 15.sp,
                            color = TextLight,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(goals) { goal ->
                        val goalTransactions = remember(allTransactions, goal.id) {
                            allTransactions.filter { it.goalId == goal.id }
                        }
                        GoalCard(
                            goal = goal,
                            transactions = goalTransactions,
                            isPrivacyMode = isPrivacyMode,
                            onClick = { onNavigateToGoal(goal.id) },
                            onDeleteClick = { goalToDelete = goal },
                            appCurrency = appCurrency
                        )
                    }
                }
            }
        }

        // FLOATING ACTION BUTTON FOR ADD GOAL (Floating above bottom navigation)
        FloatingActionButton(
            onClick = onNavigateToCreateGoal,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = 20.dp)
                .size(56.dp),
            shape = CircleShape,
            containerColor = PinkPrimary,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_goal), modifier = Modifier.size(26.dp))
        }
    }
}

// REDESIGNED CLEAN SLATE-GRAY GOAL CARD
@Composable
fun GoalCard(
    goal: Goal,
    transactions: List<Transaction>,
    isPrivacyMode: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    appCurrency: String = "USD"
) {
    val savedAmount = transactions.sumOf { it.amount }
    val isOpenSavings = goal.targetAmount <= 0.0
    val progress = if (goal.targetAmount > 0) (savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val isCompleted = !isOpenSavings && savedAmount >= goal.targetAmount
    val currencySym = getCurrencySymbol(appCurrency)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header Row: Goal Name, Badge & Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PinkPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOpenSavings) Icons.Default.Savings else Icons.Default.Flag,
                            contentDescription = null,
                            tint = PinkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = goal.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        Text(
                            text = if (isOpenSavings) stringResource(R.string.open_savings) else "${(progress * 100).toInt()}% ${stringResource(R.string.completed_status)}",
                            fontSize = 12.sp,
                            color = TextLight
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCompleted) {
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Done ✓",
                                color = Color(0xFF15803D),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chat_delete),
                            contentDescription = "Delete",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (!isOpenSavings) {
                Spacer(modifier = Modifier.height(14.dp))

                // Sleek Linear Progress Bar
                LinearProgressIndicator(
                    progress = { if (isPrivacyMode) 0f else progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PinkPrimary,
                    trackColor = Color(0xFFF1F5F9)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("SAVED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 0.5.sp)
                    Text(
                        text = if (isPrivacyMode) "$currencySym••••••" else "$currencySym${String.format(Locale.getDefault(), "%,.2f", savedAmount)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = NavyDark
                    )
                }

                if (!isOpenSavings) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("TARGET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 0.5.sp)
                        Text(
                            text = if (isPrivacyMode) "$currencySym••••••" else "$currencySym${String.format(Locale.getDefault(), "%,.0f", goal.targetAmount)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PinkPrimary
                        )
                    }
                }
            }
        }
    }
}
