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

import androidx.compose.animation.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import com.oryno.piggy_ledger.R
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
                    ToastUtil.show(context, context.getString(R.string.export_success, "CSV"), android.widget.Toast.LENGTH_SHORT)
                } catch (e: Exception) {
                    ToastUtil.show(context, context.getString(R.string.export_failed, e.message.toString()), android.widget.Toast.LENGTH_LONG)
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
                    ToastUtil.show(context, context.getString(R.string.export_success, "EXCEL"), android.widget.Toast.LENGTH_SHORT)
                } catch (e: Exception) {
                    ToastUtil.show(context, context.getString(R.string.export_failed, e.message.toString()), android.widget.Toast.LENGTH_LONG)
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
            .padding(top = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

        val scale by animateFloatAsState(
            targetValue = if (exportType == "CSV") 1.05f else if (exportType == "JSON") 0.95f else 1f,
            animationSpec = tween(300)
        )
        val configuration = LocalConfiguration.current
        val isSmallScreen = configuration.screenWidthDp < 360

        // Logo
        Box(
            modifier = Modifier.size(68.dp, 80.dp).scale(scale),
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
                    .border(2.5.dp, currentColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = exportType,
                    color = currentColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (isSmallScreen) 12.sp else 14.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = stringResource(R.string.export_data_title),
            fontSize = if (isSmallScreen) 16.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            color = NavyDark
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = stringResource(R.string.export_data_subtitle),
            fontSize = if (isSmallScreen) 11.sp else 12.sp,
            color = TextLight,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
            lineHeight = 16.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Export Type Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        .background(bgColor, RoundedCornerShape(10.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { exportType = type }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Useful and enjoyable options
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            ExportOptionRow(stringResource(R.string.export_include_pending), includePending) { includePending = it }
            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
            ExportOptionRow(stringResource(R.string.export_include_balances), includeBalances) { includeBalances = it }
            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
            ExportOptionRow(stringResource(R.string.export_include_goals), includeGoals) { includeGoals = it }
            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        } // End of scrollable column
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.export_found_transactions, transactions.size),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight,
                letterSpacing = 0.8.sp
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Button(
            onClick = {
                if (!isPremium) {
                    ToastUtil.show(context, context.getString(R.string.export_upgrade_pro), android.widget.Toast.LENGTH_SHORT)
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
                .height(46.dp),
            shape = RoundedCornerShape(23.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PinkPrimary,
                contentColor = Color.White
            )
        ) {
            Text(text = stringResource(R.string.export_action), fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
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
