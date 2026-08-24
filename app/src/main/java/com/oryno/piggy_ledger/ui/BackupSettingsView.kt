package com.oryno.piggy_ledger.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.*
import java.time.LocalDate

@Composable
fun BackupSettingsView(
    viewModel: PiggyLedgerViewModel,
    isPremium: Boolean,
    createJsonLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var exportType by remember { mutableStateOf("JSON") }
    
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

    // Color requirements: Yellow for JSON, Green for Excel, Blue for CSV
    val headerBgColor by animateColorAsState(
        targetValue = when (exportType) {
            "JSON" -> Color(0xFFEAB308)  // Vibrant Warm Yellow
            "EXCEL" -> Color(0xFF10B981) // Vibrant Emerald Green
            "CSV" -> Color(0xFF2563EB)   // Vibrant Royal Blue
            else -> Color(0xFFEAB308)
        },
        animationSpec = tween(400),
        label = "HeaderColor"
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Full-Bleed Dynamic Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBgColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    // Top Bar inside vibrant header banner
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 12.dp, end = 24.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back_icon),
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.backup_data_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Banner main content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when (exportType) {
                                "JSON" -> Icons.Default.Code
                                "EXCEL" -> Icons.Default.TableChart
                                else -> Icons.Default.Article
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // BIG WHITE TEXT for JSON / EXCEL / CSV
                        Text(
                            text = exportType,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = when (exportType) {
                                "JSON" -> stringResource(R.string.export_json_subtitle)
                                "EXCEL" -> stringResource(R.string.export_excel_subtitle)
                                else -> stringResource(R.string.export_csv_subtitle)
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.92f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Bottom Fade-Out Blend Effect
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    headerBgColor.copy(alpha = 0f),
                                    headerBgColor.copy(alpha = 0.5f),
                                    Color.White
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Export Type Selector (Segmented Tabs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("JSON", "CSV", "EXCEL").forEach { type ->
                    val isSelected = exportType == type
                    
                    val tabBg = if (isSelected) headerBgColor else Color(0xFFF1F5F9)
                    val tabTextColor = if (isSelected) Color.White else TextLight

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(tabBg)
                            .then(
                                if (!isSelected) Modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                else Modifier
                            )
                            .clickable { exportType = type }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = tabTextColor,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.export_data_subtitle),
                fontSize = 12.sp,
                color = TextLight,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Options List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            ) {
                ExportOptionRow(stringResource(R.string.export_include_pending), includePending) { includePending = it }
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                ExportOptionRow(stringResource(R.string.export_include_balances), includeBalances) { includeBalances = it }
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                ExportOptionRow(stringResource(R.string.export_include_goals), includeGoals) { includeGoals = it }
            }

            Spacer(modifier = Modifier.height(20.dp))
        } // End of scrollable column

        // Bottom Action Area
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
                    .padding(horizontal = 20.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PinkPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = stringResource(R.string.export_action),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                checkedTrackColor = PinkPrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFCBD5E1),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
