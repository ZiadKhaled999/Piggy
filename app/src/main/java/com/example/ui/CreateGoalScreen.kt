package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NavyDark
import com.example.ui.theme.TextLight
import com.example.ui.theme.SlateDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalScreen(
    onGoalCreated: (String, Double) -> Unit,
    onBack: () -> Unit
) {
    var goalName by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var isOpenedBalance by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding()
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.offset(x = (-12).dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NavyDark)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("New Goal", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = NavyDark)
        Text("What's the big plan? Set it up here.", fontSize = 16.sp, color = TextLight)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = goalName,
            onValueChange = { goalName = it },
            label = { Text("WHAT ARE YOU SAVING FOR?") },
            placeholder = { Text("e.g. Dream Vacation, New PC, General Savings") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedBorderColor = NavyDark
            )
        )
        
        Spacer(modifier = Modifier.height(20.dp))

        Text("Goal Type", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { isOpenedBalance = false },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!isOpenedBalance) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (!isOpenedBalance) 2.dp else 1.dp,
                    color = if (!isOpenedBalance) Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Target Goal", fontWeight = FontWeight.Bold, color = if (!isOpenedBalance) Color(0xFF3B82F6) else NavyDark, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Save for a specific amount (e.g. PC)", fontSize = 11.sp, color = TextLight)
                }
            }
            
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { isOpenedBalance = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOpenedBalance) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isOpenedBalance) 2.dp else 1.dp,
                    color = if (isOpenedBalance) Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Opened Balance", fontWeight = FontWeight.Bold, color = if (isOpenedBalance) Color(0xFF3B82F6) else NavyDark, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Deposit just for saving (No target limit)", fontSize = 11.sp, color = TextLight)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!isOpenedBalance) {
            OutlinedTextField(
                value = targetAmount,
                onValueChange = { targetAmount = it },
                label = { Text("HOW MUCH DO YOU NEED? ($)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedBorderColor = NavyDark
                )
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💡 Hint: Opened balance is for general/indefinite savings. You can deposit money here anytime for savings without setting any specific limit.",
                        fontSize = 13.sp,
                        color = SlateDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = { 
                if (goalName.isNotBlank()) {
                    if (isOpenedBalance) {
                        onGoalCreated(goalName, 0.0)
                    } else {
                        val amount = targetAmount.replace("$", "").trim().toDoubleOrNull()
                        if (amount != null) {
                            onGoalCreated(goalName, amount)
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
        ) {
            Text("Let's Get Saving!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
