package com.oryno.piggy_ledger.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oryno.piggy_ledger.ui.theme.*
import java.time.LocalDate

@Composable
fun BackupSettingsView(
    viewModel: PiggyLedgerViewModel,
    isPremium: Boolean,
    createJsonLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    val context = LocalContext.current
    var exportType by remember { mutableStateOf("CSV") }
    
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var includePending by remember { mutableStateOf(true) }
    var includeBalances by remember { mutableStateOf(true) }
    var includeGoals by remember { mutableStateOf(true) }
    
    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            viewModel.exportCSVData { csvString ->
                try {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(csvString.toByteArray())
                    }
                    ToastUtil.show(context, "Data exported successfully to CSV", android.widget.Toast.LENGTH_SHORT)
                } catch (e: Exception) {
                    ToastUtil.show(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG)
                }
            }
        }
    }

    val createExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri ->
        uri?.let {
            viewModel.exportExcelData { excelString ->
                try {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(excelString.toByteArray())
                    }
                    ToastUtil.show(context, "Data exported successfully to EXCEL", android.widget.Toast.LENGTH_SHORT)
                } catch (e: Exception) {
                    ToastUtil.show(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG)
                }
            }
        }
    }

    val colorMap = mapOf(
        "CSV" to Color(0xFF10A37F),
        "JSON" to Color(0xFFF5A623),
        "EXCEL" to Color(0xFF005A9E)
    )
    val currentColor by animateColorAsState(targetValue = colorMap[exportType] ?: Color(0xFF10A37F), animationSpec = tween(400))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val scale by animateFloatAsState(
            targetValue = if (exportType == "CSV") 1.05f else if (exportType == "JSON") 0.95f else 1f,
            animationSpec = tween(300)
        )
        // Logo
        Box(
            modifier = Modifier.size(100.dp, 120.dp).scale(scale),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Article,
                contentDescription = null,
                tint = currentColor,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .border(4.dp, currentColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = exportType,
                    color = currentColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Export data",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = NavyDark
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Export your data for external spreadsheets.",
            fontSize = 14.sp,
            color = TextLight,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
            lineHeight = 20.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Export Type Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("JSON", "CSV", "EXCEL").forEach { type ->
                val isSelected = exportType == type
                
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) PinkPrimary.copy(alpha = 0.08f) else Color.Transparent,
                    animationSpec = tween(300)
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) PinkPrimary else Color(0xFFE0E0E0),
                    animationSpec = tween(300)
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) PinkPrimary else TextLight,
                    animationSpec = tween(300)
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(bgColor, RoundedCornerShape(12.dp))
                        .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { exportType = type }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        fontSize = 15.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Useful and enjoyable options
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
        ) {
            ExportOptionRow("Include Pending Transactions", includePending) { includePending = it }
            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
            ExportOptionRow("Include Account Balances", includeBalances) { includeBalances = it }
            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
            ExportOptionRow("Include Goal History", includeGoals) { includeGoals = it }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "FOUND ${transactions.size} TRANSACTIONS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextLight,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                if (!isPremium) {
                    ToastUtil.show(context, "Upgrade to Pro to export your data", android.widget.Toast.LENGTH_SHORT)
                    return@Button
                }
                
                val dateStr = LocalDate.now().toString()
                val baseName = "piggy_ledger_$dateStr"
                
                when (exportType) {
                    "JSON" -> createJsonLauncher.launch("$baseName.json")
                    "CSV" -> createCsvLauncher.launch("$baseName.csv")
                    "EXCEL" -> createExcelLauncher.launch("$baseName.xls")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PinkPrimary,
                contentColor = Color.White
            )
        ) {
            Text("EXPORT", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ExportOptionRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            color = NavyDark,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PinkPrimary
            )
        )
    }
}
