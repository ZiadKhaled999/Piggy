package com.oryno.piggy_ledger.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.BackupHelper
import com.oryno.piggy_ledger.data.FullBackupData
import com.oryno.piggy_ledger.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed class ImportState {
    object Idle : ImportState()
    object Scanning : ImportState()
    data class Preview(val backupData: FullBackupData) : ImportState()
    object Success : ImportState()
}

@Composable
fun BackupSettingsView(
    viewModel: PiggyLedgerViewModel,
    isPremium: Boolean,
    createJsonLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isImportMode by remember { mutableStateOf(false) }
    var exportType by remember { mutableStateOf("JSON") }
    var importState by remember { mutableStateOf<ImportState>(ImportState.Idle) }

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

    // Launcher for file import
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { fileUri ->
            coroutineScope.launch {
                importState = ImportState.Scanning
                try {
                    val content = context.contentResolver.openInputStream(fileUri)?.use { stream ->
                        stream.bufferedReader().use { it.readText() }
                    } ?: ""

                    // Smooth scanning animation delay
                    delay(1200)

                    if (content.isBlank()) {
                        ToastUtil.show(context, "Selected file is empty.", android.widget.Toast.LENGTH_LONG)
                        importState = ImportState.Idle
                        return@launch
                    }

                    val backupData = BackupHelper.parseBackupFile(content, exportType)
                    if (backupData.goals.isEmpty() &&
                        backupData.loans.isEmpty() &&
                        backupData.accounts.isEmpty() &&
                        backupData.transactions.isEmpty() &&
                        backupData.accountTransactions.isEmpty() &&
                        backupData.pendingTransactions.isEmpty()
                    ) {
                        ToastUtil.show(context, "No valid ledger entries found in the file.", android.widget.Toast.LENGTH_LONG)
                        importState = ImportState.Idle
                    } else {
                        importState = ImportState.Preview(backupData)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BackupSettingsView", "Import file parse failed", e)
                    ToastUtil.show(context, context.getString(R.string.import_failed_msg, e.localizedMessage ?: "Unknown error"), android.widget.Toast.LENGTH_LONG)
                    importState = ImportState.Idle
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
                        IconButton(onClick = {
                            if (isImportMode) {
                                if (importState is ImportState.Preview) {
                                    importState = ImportState.Idle
                                } else {
                                    isImportMode = false
                                    importState = ImportState.Idle
                                }
                            } else {
                                onBack()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back_icon),
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isImportMode) stringResource(R.string.import_data_title) else stringResource(R.string.backup_data_title),
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
                            imageVector = when {
                                isImportMode -> Icons.Default.CloudUpload
                                exportType == "JSON" -> Icons.Default.Code
                                exportType == "EXCEL" -> Icons.Default.TableChart
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
                            text = if (isImportMode) {
                                when (exportType) {
                                    "JSON" -> stringResource(R.string.import_json_subtitle)
                                    "EXCEL" -> stringResource(R.string.import_excel_subtitle)
                                    else -> stringResource(R.string.import_csv_subtitle)
                                }
                            } else {
                                when (exportType) {
                                    "JSON" -> stringResource(R.string.export_json_subtitle)
                                    "EXCEL" -> stringResource(R.string.export_excel_subtitle)
                                    else -> stringResource(R.string.export_csv_subtitle)
                                }
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

            // Format Type Selector (Segmented Tabs)
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
                            .clickable {
                                exportType = type
                                if (isImportMode) importState = ImportState.Idle
                            }
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

            if (!isImportMode) {
                // EXPORT MODE CONTENT
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

                // NEW "IMPORT YOUR BACKUP" BUTTON IN WHITE SPACE SECTION BELOW TOGGLES & ABOVE EXPORT BUTTON
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (!isPremium) {
                                ToastUtil.show(context, context.getString(R.string.export_upgrade_pro), android.widget.Toast.LENGTH_SHORT)
                            } else {
                                isImportMode = true
                                importState = ImportState.Idle
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, PinkPrimary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(PinkPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = PinkPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.import_your_backup),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.import_your_backup_subtitle),
                                fontSize = 12.sp,
                                color = TextLight
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = PinkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

            } else {
                // IMPORT MODE CONTENT
                when (val state = importState) {
                    is ImportState.Idle -> {
                        ImportIdleView(
                            exportType = exportType,
                            onSelectFile = {
                                if (!isPremium) {
                                    ToastUtil.show(context, context.getString(R.string.export_upgrade_pro), android.widget.Toast.LENGTH_SHORT)
                                } else {
                                    importFileLauncher.launch("*/*")
                                }
                            }
                        )
                    }
                    is ImportState.Scanning -> {
                        ImportScanningView()
                    }
                    is ImportState.Preview -> {
                        ImportPreviewCategorizedView(
                            data = state.backupData,
                            onConfirmRestore = {
                                viewModel.restoreBackup(state.backupData) { success ->
                                    if (success) {
                                        importState = ImportState.Success
                                        ToastUtil.show(context, context.getString(R.string.import_success_msg), android.widget.Toast.LENGTH_LONG)
                                    } else {
                                        ToastUtil.show(context, "Failed to restore backup data.", android.widget.Toast.LENGTH_LONG)
                                    }
                                }
                            },
                            onPickAnother = {
                                importState = ImportState.Idle
                            }
                        )
                    }
                    is ImportState.Success -> {
                        ImportSuccessView(
                            onDone = {
                                isImportMode = false
                                importState = ImportState.Idle
                            }
                        )
                    }
                }
            }

        } // End of scrollable column

        // Bottom Action Area (Export Mode Only)
        if (!isImportMode) {
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
}

@Composable
fun ImportIdleView(
    exportType: String,
    onSelectFile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable { onSelectFile() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(PinkPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = PinkPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Upload $exportType Backup File",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (exportType) {
                        "JSON" -> "Select a .json backup file exported from Piggy Ledger."
                        "EXCEL" -> "Select an .xls Excel spreadsheet backup file."
                        else -> "Select a .csv backup file to restore."
                    },
                    fontSize = 13.sp,
                    color = TextLight,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onSelectFile,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color.White)
                ) {
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.select_backup_file),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ImportScanningView() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(rotation)
                            .border(3.dp, Brush.sweepGradient(listOf(PinkPrimary, Color.Transparent, PinkPrimary)), CircleShape)
                    )
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = PinkPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.scanning_backup_file),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.categorizing_data),
                    fontSize = 13.sp,
                    color = TextLight,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = PinkPrimary,
                    trackColor = Color(0xFFF1F5F9)
                )
            }
        }
    }
}

@Composable
fun ImportPreviewCategorizedView(
    data: FullBackupData,
    onConfirmRestore: () -> Unit,
    onPickAnother: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.backup_contents_preview),
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = NavyDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Categorized Breakdown List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category 1: Payoffs & Loans
            PreviewCategoryItem(
                title = stringResource(R.string.preview_payoffs_loans),
                icon = Icons.Default.Handshake,
                iconBg = Color(0xFF8B5CF6),
                primaryText = "${data.loans.size} Loans / Payoffs",
                secondaryText = if (data.loans.isNotEmpty()) {
                    "Contacts: " + data.loans.take(3).joinToString(", ") { it.contactName } +
                            if (data.loans.size > 3) "..." else "" +
                            " • ${data.loanPayments.size} Payment Logs"
                } else {
                    "No loans found in backup"
                }
            )

            // Category 2: Accounts & Banks
            PreviewCategoryItem(
                title = stringResource(R.string.preview_accounts_banks),
                icon = Icons.Default.AccountBalance,
                iconBg = Color(0xFF3B82F6),
                primaryText = "${data.accounts.size} Accounts Registered",
                secondaryText = if (data.accounts.isNotEmpty()) {
                    "Accounts: " + data.accounts.take(3).joinToString(", ") { it.name } +
                            if (data.accounts.size > 3) "..." else "" +
                            " • ${data.accountTransactions.size} Transactions"
                } else {
                    "No bank accounts found in backup"
                }
            )

            // Category 3: Savings Goals
            PreviewCategoryItem(
                title = stringResource(R.string.preview_goals_savings),
                icon = Icons.Default.Savings,
                iconBg = Color(0xFF10B981),
                primaryText = "${data.goals.size} Savings Goals",
                secondaryText = if (data.goals.isNotEmpty()) {
                    "Goals: " + data.goals.take(3).joinToString(", ") { it.name } +
                            if (data.goals.size > 3) "..." else "" +
                            " • ${data.transactions.size} Deposits Logged"
                } else {
                    "No savings goals found in backup"
                }
            )

            // Category 4: Pending SMS & Streaks
            PreviewCategoryItem(
                title = stringResource(R.string.preview_pending_streaks),
                icon = Icons.Default.Sms,
                iconBg = Color(0xFFF59E0B),
                primaryText = "${data.pendingTransactions.size} Pending SMS Queue",
                secondaryText = "${data.streakDates.size} Active Streak Days Recorded"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Confirmation Actions
        Button(
            onClick = onConfirmRestore,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color.White)
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.confirm_restore_action),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onPickAnother,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
        ) {
            Text(
                text = stringResource(R.string.choose_another_file),
                fontSize = 14.sp,
                color = TextLight,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PreviewCategoryItem(
    title: String,
    icon: ImageVector,
    iconBg: Color,
    primaryText: String,
    secondaryText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconBg,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconBg
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = secondaryText,
                    fontSize = 11.sp,
                    color = TextLight
                )
            }
        }
    }
}

@Composable
fun ImportSuccessView(
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.import_success_msg),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Done / Return to Ledger",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
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
