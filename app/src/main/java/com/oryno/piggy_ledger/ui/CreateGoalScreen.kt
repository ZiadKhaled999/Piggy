package com.oryno.piggy_ledger.ui
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.oryno.piggy_ledger.R


import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.TextLight
import com.oryno.piggy_ledger.ui.theme.SlateDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import androidx.compose.material.icons.filled.CheckCircle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalScreen(
    onGoalCreated: (String, Double) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
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
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_icon), tint = NavyDark)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(stringResource(R.string.new_goal), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = NavyDark)
        Text(stringResource(R.string.new_goal_subtitle), fontSize = 16.sp, color = TextLight)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = goalName,
            onValueChange = { goalName = it },
            label = { Text(stringResource(R.string.what_are_you_saving_for), fontWeight = FontWeight.Bold) },
            placeholder = { Text(stringResource(R.string.goal_name_placeholder)) },
            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 16.sp),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
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
        
        Spacer(modifier = Modifier.height(20.dp))

        Text(stringResource(R.string.goal_type), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val targetSelected = !isOpenedBalance
            val openedSelected = isOpenedBalance

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { isOpenedBalance = false },
                shape = RoundedCornerShape(16.dp),
                color = if (targetSelected) PinkPrimary.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (targetSelected) 2.5.dp else 1.2.dp,
                    color = if (targetSelected) PinkPrimary else Color(0xFFE2E8F0)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.target_goal_title),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (targetSelected) PinkPrimary else NavyDark,
                            fontSize = 14.sp
                        )
                        if (targetSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.selected_badge),
                                tint = PinkPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .border(1.2.dp, Color(0xFFCBD5E1), androidx.compose.foundation.shape.CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.target_goal_desc),
                        fontSize = 11.sp,
                        color = if (targetSelected) NavyDark.copy(alpha = 0.8f) else TextLight,
                        lineHeight = 14.sp
                    )
                }
            }
            
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { isOpenedBalance = true },
                shape = RoundedCornerShape(16.dp),
                color = if (openedSelected) PinkPrimary.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (openedSelected) 2.5.dp else 1.2.dp,
                    color = if (openedSelected) PinkPrimary else Color(0xFFE2E8F0)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.open_savings),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (openedSelected) PinkPrimary else NavyDark,
                            fontSize = 14.sp
                        )
                        if (openedSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.selected_badge),
                                tint = PinkPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .border(1.2.dp, Color(0xFFCBD5E1), androidx.compose.foundation.shape.CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.open_savings_desc),
                        fontSize = 11.sp,
                        color = if (openedSelected) NavyDark.copy(alpha = 0.8f) else TextLight,
                        lineHeight = 14.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!isOpenedBalance) {
            OutlinedTextField(
                value = targetAmount,
                onValueChange = { input ->
                    if (input.all { it.isDigit() || it == '.' }) {
                        targetAmount = input
                    }
                },
                label = { Text(stringResource(R.string.how_much_do_you_need), fontWeight = FontWeight.Bold) },
                placeholder = { Text(stringResource(R.string.zero_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 16.sp),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
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
                        text = stringResource(R.string.open_savings_hint),
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
                try {
                    com.posthog.PostHog.capture(event = "button_clicked", properties = mapOf("button_name" to "Create Goal", "screen" to "CreateGoalScreen"))
                } catch (e: Exception) {}
                if (goalName.isNotBlank()) {
                    if (isOpenedBalance) {
                        com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.toast_goal_created), Toast.LENGTH_SHORT)
                        onGoalCreated(goalName, 0.0)
                    } else {
                        val amount = targetAmount.replace("$", "").trim().toDoubleOrNull()
                        if (amount != null) {
                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.toast_goal_created), Toast.LENGTH_SHORT)
                            onGoalCreated(goalName, amount)
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
        ) {
            Text(stringResource(R.string.lets_get_saving), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
