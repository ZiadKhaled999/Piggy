package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NavyDark
import com.example.ui.theme.TextLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalScreen(
    onGoalCreated: (String, Double) -> Unit,
    onBack: () -> Unit
) {
    var goalName by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }

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
            placeholder = { Text("e.g. Dream Vacation, New PC") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = androidx.compose.ui.graphics.Color(0xFFE2E8F0),
                focusedBorderColor = NavyDark
            )
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        OutlinedTextField(
            value = targetAmount,
            onValueChange = { targetAmount = it },
            label = { Text("HOW MUCH DO YOU NEED? ($)") },
            placeholder = { Text("0.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = androidx.compose.ui.graphics.Color(0xFFE2E8F0),
                focusedBorderColor = NavyDark
            )
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = { 
                val amount = targetAmount.replace("$", "").trim().toDoubleOrNull()
                if (goalName.isNotBlank() && amount != null) {
                    onGoalCreated(goalName, amount)
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
