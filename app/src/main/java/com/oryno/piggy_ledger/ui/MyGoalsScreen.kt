package com.oryno.piggy_ledger.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Goal
import com.oryno.piggy_ledger.data.Transaction
import com.oryno.piggy_ledger.ui.theme.*

import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast

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
                            Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
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
                        goalToDelete?.let { viewModel.deleteGoal(it.id) }
                        goalToDelete = null
                        confirmNameInput = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = canDelete,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF4D4D), // Danger Red
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
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
            
            Text(stringResource(R.string.my_goals), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = NavyDark, modifier = Modifier.weight(1f))

            if (goals.isNotEmpty()) {
                IconButton(
                    onClick = onNavigateToCreateGoal,
                    modifier = Modifier
                        .size(48.dp)
                        .background(PinkPrimary.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_goal),
                        tint = PinkPrimary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (goals.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.img_no_goals),
                        contentDescription = null,
                        modifier = Modifier.size(280.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        stringResource(R.string.no_goals_yet),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.start_first_goal),
                        fontSize = 16.sp,
                        color = TextLight
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    OutlinedButton(
                        onClick = onNavigateToCreateGoal,
                        modifier = Modifier
                            .height(60.dp)
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(30.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, PinkPrimary.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = PinkPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.add_goal),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(goals) { goal ->
                    val goalTransactions = remember(allTransactions, goal.id) {
                        allTransactions.filter { it.goalId == goal.id }
                    }
                    GoalCard(
                        goal = goal, 
                        transactions = goalTransactions, 
                        onClick = { onNavigateToGoal(goal.id) },
                        onDeleteClick = { goalToDelete = goal }
                    )
                }
            }
        }
    }
}

@Composable
fun GoalCard(goal: Goal, transactions: List<Transaction>, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    val savedAmount = transactions.sumOf { it.amount }
    val isOpenSavings = goal.targetAmount <= 0.0
    val progress = if (goal.targetAmount > 0) (savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val isCompleted = !isOpenSavings && savedAmount >= goal.targetAmount
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PinkPrimary.copy(alpha = 0.05f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, PinkPrimary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name, 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = NavyDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isOpenSavings) stringResource(R.string.open_savings) else stringResource(R.string.target) + " \$${String.format("%.0f", goal.targetAmount)}",
                        fontSize = 12.sp,
                        color = TextLight
                    )
                }
                
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle, 
                        contentDescription = stringResource(R.string.completed_badge),
                        tint = PinkAccent,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (isOpenSavings) {
                    Surface(
                        color = PinkPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.open_badge),
                            color = PinkPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = NavyDark.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            if (!isOpenSavings) {
                Spacer(modifier = Modifier.height(16.dp))
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PinkAccent,
                    trackColor = PinkPrimary.copy(alpha = 0.1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val remaining = goal.targetAmount - savedAmount
                val savedText = if (isOpenSavings) {
                    stringResource(R.string.amount_saved, String.format("%.2f", savedAmount))
                } else if (remaining < 0) {
                    stringResource(R.string.amount_total_extra, String.format("%.2f", savedAmount), String.format("%.2f", -remaining))
                } else {
                    stringResource(R.string.amount_saved, String.format("%.2f", savedAmount))
                }
                Text(
                    text = savedText,
                    color = if (remaining < 0 && !isOpenSavings) PinkAccent else TextLight,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                
                if (remaining > 0 && !isOpenSavings) {
                    Text(
                        text = stringResource(R.string.remaining_left, String.format("%.2f", remaining)),
                        color = NavyDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
