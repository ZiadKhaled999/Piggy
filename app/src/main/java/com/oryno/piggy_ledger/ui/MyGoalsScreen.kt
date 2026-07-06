package com.oryno.piggy_ledger.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Goal
import com.oryno.piggy_ledger.data.Transaction
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.PurplePrimary
import com.oryno.piggy_ledger.ui.theme.PinkAccent
import com.oryno.piggy_ledger.ui.theme.TextLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGoalsScreen(
    viewModel: PiggyLedgerViewModel,
    onNavigateToGoal: (String) -> Unit,
    onBack: () -> Unit
) {
    val goals by viewModel.goals.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val context = LocalContext.current

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
                    contentDescription = "Back",
                    tint = NavyDark
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text("My Goals", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = NavyDark)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (goals.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(160.dp).alpha(0.6f).clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No goals yet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark.copy(alpha = 0.6f)
                    )
                    Text(
                        "Start your first goal today!",
                        fontSize = 14.sp,
                        color = TextLight
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(goals) { goal ->
                    val goalTransactions = remember(allTransactions, goal.id) {
                        allTransactions.filter { it.goalId == goal.id }
                    }
                    GoalCard(goal = goal, transactions = goalTransactions, onClick = { onNavigateToGoal(goal.id) })
                }
            }
        }
    }
}

@Composable
fun GoalCard(goal: Goal, transactions: List<Transaction>, onClick: () -> Unit) {
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
                        text = if (isOpenSavings) "Open Savings (General)" else "Target: $${String.format("%.0f", goal.targetAmount)}",
                        fontSize = 12.sp,
                        color = TextLight
                    )
                }
                
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle, 
                        contentDescription = "Completed",
                        tint = PinkAccent,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (isOpenSavings) {
                    Surface(
                        color = PinkPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "OPEN",
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
                    "$${String.format("%.2f", savedAmount)} saved"
                } else if (remaining < 0) {
                    "$${String.format("%.2f", savedAmount)} total ($${String.format("%.2f", -remaining)} extra)"
                } else {
                    "$${String.format("%.2f", savedAmount)} saved"
                }
                Text(
                    text = savedText,
                    color = if (remaining < 0 && !isOpenSavings) PinkAccent else TextLight,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                
                if (remaining > 0 && !isOpenSavings) {
                    Text(
                        text = "$${String.format("%.2f", remaining)} left",
                        color = NavyDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
