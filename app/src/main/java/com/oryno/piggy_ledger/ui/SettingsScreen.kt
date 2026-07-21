package com.oryno.piggy_ledger.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.os.LocaleListCompat
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import com.oryno.piggy_ledger.ui.theme.AccentBlue

@Composable
fun SettingsScreen(
    viewModel: PiggyLedgerViewModel,
    onNavigateToPendingTransactions: () -> Unit
) {
    var settingsMode by remember { mutableStateOf(SettingsMode.MAIN) }
    val context = LocalContext.current

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportData { jsonString ->
                try {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(jsonString.toByteArray())
                    }
                    Toast.makeText(context, context.getString(R.string.export_success), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.export_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val jsonString = stream.bufferedReader().use { it.readText() }
                    viewModel.importData(
                        jsonString = jsonString,
                        onComplete = {
                            Toast.makeText(context, context.getString(R.string.restore_success), Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(context, context.getString(R.string.restore_failed, error), Toast.LENGTH_LONG).show()
                        }
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.read_file_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        when (settingsMode) {
            SettingsMode.MAIN -> {
                Text(
                    stringResource(R.string.settings),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                SettingsMainContent(
                    onModeChange = { settingsMode = it },
                    onNavigateToPendingTransactions = onNavigateToPendingTransactions
                )
            }
            else -> {
                // Detail views handled inside when block below for simplicity in this refactor
                DetailSettingsView(
                    mode = settingsMode,
                    viewModel = viewModel,
                    onBack = { settingsMode = SettingsMode.MAIN },
                    createDocumentLauncher = createDocumentLauncher,
                    openDocumentLauncher = openDocumentLauncher
                )
            }
        }
    }
}

@Composable
fun SettingsMainContent(
    onModeChange: (SettingsMode) -> Unit,
    onNavigateToPendingTransactions: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsItem(
            title = stringResource(R.string.pending_transactions),
            iconRes = R.drawable.img_settings_pending_1784465160290,
            onClick = onNavigateToPendingTransactions
        )

        SettingsItem(
            title = stringResource(R.string.language),
            iconRes = R.drawable.img_settings_language,
            onClick = { onModeChange(SettingsMode.LANGUAGE) }
        )
        
        SettingsItem(
            title = stringResource(R.string.give_feedback),
            iconRes = R.drawable.img_settings_feedback,
            onClick = { onModeChange(SettingsMode.FEEDBACK) }
        )
        
        SettingsItem(
            title = stringResource(R.string.rate_app),
            iconRes = R.drawable.img_settings_rate,
            onClick = { onModeChange(SettingsMode.RATING) }
        )
        
        SettingsItem(
            title = stringResource(R.string.backup_data),
            iconRes = R.drawable.img_settings_backup,
            onClick = { onModeChange(SettingsMode.BACKUP) }
        )
        
        SettingsItem(
            title = stringResource(R.string.restore_data),
            iconRes = R.drawable.img_settings_restore,
            onClick = { onModeChange(SettingsMode.RESTORE) }
        )

        SettingsItem(
            title = stringResource(R.string.security),
            iconRes = R.drawable.img_settings_security,
            onClick = { onModeChange(SettingsMode.SECURITY) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SettingsItem(
    title: String,
    iconRes: Int?,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PinkPrimary.copy(alpha = 0.03f)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, PinkPrimary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (iconRes != null || iconVector != null) 12.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
            } else if (iconVector != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(PinkPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = PinkPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyDark, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextLight)
        }
    }
}

@Composable
fun DetailSettingsView(
    mode: SettingsMode,
    viewModel: PiggyLedgerViewModel,
    onBack: () -> Unit,
    createDocumentLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    openDocumentLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
) {
    val context = LocalContext.current
    
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
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
                text = when(mode) {
                    SettingsMode.LANGUAGE -> stringResource(R.string.language)
                    SettingsMode.FEEDBACK -> stringResource(R.string.community_feedback)
                    SettingsMode.RATING -> stringResource(R.string.rate_app_title)
                    SettingsMode.BACKUP -> stringResource(R.string.backup_data_title)
                    SettingsMode.RESTORE -> stringResource(R.string.restore_data_title)
                    SettingsMode.SECURITY -> stringResource(R.string.security)
                    else -> ""
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        when (mode) {
            SettingsMode.LANGUAGE -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_settings_language),
                        contentDescription = stringResource(R.string.language_illustration),
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                
                SettingsLanguageOption(
                    title = stringResource(id = R.string.english),
                    subtitle = stringResource(id = R.string.united_states),
                    flagResId = R.drawable.ic_flag_us,
                    isSelected = currentLocale.startsWith("en"),
                    onClick = {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                SettingsLanguageOption(
                    title = stringResource(id = R.string.arabic),
                    subtitle = stringResource(id = R.string.saudi_arabia),
                    flagResId = R.drawable.ic_flag_sa,
                    isSelected = (currentLocale.startsWith("ar") && !currentLocale.contains("EG")) || (currentLocale.isEmpty() && java.util.Locale.getDefault().language == "ar"),
                    onClick = {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ar"))
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                SettingsLanguageOption(
                    title = stringResource(id = R.string.egyptian),
                    subtitle = stringResource(id = R.string.egypt),
                    flagResId = R.drawable.ic_flag_eg,
                    isSelected = currentLocale.contains("ar-EG"),
                    onClick = {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ar-EG"))
                    }
                )
            }
            SettingsMode.FEEDBACK -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_settings_feedback),
                        contentDescription = stringResource(R.string.feedback_illustration),
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.join_community_board),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.help_improve),
                            fontSize = 13.sp,
                            color = TextLight,
                            lineHeight = 18.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://piggy-ledger.featureos.app"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.browser_error), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PinkPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.open_feedback_board), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            SettingsMode.RATING -> {
                var rating by remember { mutableIntStateOf(0) }
                
                Box(
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_settings_rate),
                        contentDescription = stringResource(R.string.rate_illustration),
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.enjoying_piggy_ledger),
                    fontSize = 14.sp,
                    color = TextLight,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        IconButton(
                            onClick = { rating = i },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = if (i <= rating) Icons.Default.Star else Icons.Outlined.Star,
                                contentDescription = "Star $i",
                                tint = if (i <= rating) PinkPrimary else Color(0xFFCBD5E1),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        val mailtoUrl = "mailto:albhyrytwamrwhy@gmail.com" +
                                "?subject=" + Uri.encode("Piggy Ledger Rating") +
                                "&body=" + Uri.encode("I rated Piggy Ledger $rating/5 stars!")
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse(mailtoUrl)
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Send Rating"))
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.email_error), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PinkPrimary,
                        contentColor = Color.White
                    ),
                    enabled = rating > 0
                ) {
                    Text(stringResource(R.string.send_rating), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            SettingsMode.BACKUP -> {
                val createCsvLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("text/csv")
                ) { uri ->
                    uri?.let {
                        viewModel.exportCSVData { csvString ->
                            try {
                                context.contentResolver.openOutputStream(it)?.use { stream ->
                                    stream.write(csvString.toByteArray())
                                }
                                Toast.makeText(context, context.getString(R.string.csv_export_success), Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.csv_export_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
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
                                Toast.makeText(context, context.getString(R.string.excel_export_success), Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.excel_export_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_settings_backup),
                                contentDescription = stringResource(R.string.backup_illustration),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PinkPrimary.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                stringResource(R.string.secure_local_export),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.export_screen_desc),
                                fontSize = 13.sp,
                                color = TextLight,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Card 1: Beautiful Excel File
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TableChart,
                                        contentDescription = null,
                                        tint = AccentBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        stringResource(R.string.export_excel_subtitle),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NavyDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.export_excel_desc),
                                    fontSize = 12.sp,
                                    color = TextLight,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        createExcelLauncher.launch("piggy_ledger_backup.xls")
                                    },
                                    modifier = Modifier.fillMaxWidth().height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentBlue,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(stringResource(R.string.export_excel_title), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Card 2: Porting CSV File
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Article,
                                        contentDescription = null,
                                        tint = PinkPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        stringResource(R.string.export_csv_subtitle),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NavyDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.export_csv_desc),
                                    fontSize = 12.sp,
                                    color = TextLight,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        createCsvLauncher.launch("piggy_ledger_backup.csv")
                                    },
                                    modifier = Modifier.fillMaxWidth().height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PinkPrimary,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(stringResource(R.string.export_csv_title), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Card 3: Standard JSON Backup
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Backup,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        stringResource(R.string.export_json_subtitle),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NavyDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.export_json_desc),
                                    fontSize = 12.sp,
                                    color = TextLight,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        createDocumentLauncher.launch("piggy_ledger_backup.json")
                                    },
                                    modifier = Modifier.fillMaxWidth().height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF64748B),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(stringResource(R.string.create_backup_file), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            SettingsMode.RESTORE -> {
                val openCsvLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    uri?.let {
                        try {
                            context.contentResolver.openInputStream(it)?.use { stream ->
                                val csvString = stream.bufferedReader().use { it.readText() }
                                viewModel.importCSVData(
                                    csvString = csvString,
                                    onComplete = {
                                        Toast.makeText(context, context.getString(R.string.csv_restore_success), Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, context.getString(R.string.csv_restore_failed, error), Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.csv_restore_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
                        }
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_settings_restore),
                                contentDescription = stringResource(R.string.restore_illustration),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AccentBlue.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                stringResource(R.string.import_json_backup),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.restoring_data_replace),
                                fontSize = 13.sp,
                                color = TextLight,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Card 1: Restore from CSV File
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Article,
                                        contentDescription = null,
                                        tint = PinkPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        stringResource(R.string.restore_csv_subtitle),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NavyDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.restore_csv_desc),
                                    fontSize = 12.sp,
                                    color = TextLight,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        openCsvLauncher.launch(arrayOf("*/*"))
                                    },
                                    modifier = Modifier.fillMaxWidth().height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PinkPrimary,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(stringResource(R.string.select_csv_file), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Card 2: Restore from Legacy JSON File
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Backup,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        stringResource(R.string.restore_json_subtitle),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NavyDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.restore_json_desc),
                                    fontSize = 12.sp,
                                    color = TextLight,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        openDocumentLauncher.launch(arrayOf("application/json"))
                                    },
                                    modifier = Modifier.fillMaxWidth().height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF64748B),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(stringResource(R.string.select_backup_file), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            SettingsMode.SECURITY -> {
                SecuritySettingsView(viewModel = viewModel)
            }
            else -> {}
        }
    }
}

@Composable
fun SecuritySettingsView(viewModel: PiggyLedgerViewModel) {
    val isBiometricEnabled by viewModel.isBiometricLockEnabled.collectAsStateWithLifecycle()
    val isScreenshotProtected by viewModel.isScreenshotProtectionEnabled.collectAsStateWithLifecycle()
    val lockTimeout by viewModel.lockTimeoutSeconds.collectAsStateWithLifecycle()
    val pinLock by viewModel.pinLock.collectAsStateWithLifecycle()

    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Hero Image
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_settings_security),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Biometric Lock
        SecurityToggleItem(
            title = stringResource(R.string.biometric_lock),
            description = stringResource(R.string.biometric_lock_desc),
            checked = isBiometricEnabled,
            onCheckedChange = { viewModel.setBiometricLockEnabled(it) }
        )

        if (isBiometricEnabled) {
            // Lock Timeout
            SecurityClickItem(
                title = stringResource(R.string.lock_timeout),
                description = stringResource(R.string.lock_timeout_desc),
                value = formatTimeout(lockTimeout),
                onClick = { showTimeoutDialog = true }
            )
        }

        // Screenshot Protection
        SecurityToggleItem(
            title = stringResource(R.string.screenshot_protection),
            description = stringResource(R.string.screenshot_protection_desc),
            checked = isScreenshotProtected,
            onCheckedChange = { viewModel.setScreenshotProtectionEnabled(it) }
        )

        // PIN Lock
        SecurityToggleItem(
            title = stringResource(R.string.pin_lock_app),
            description = stringResource(R.string.pin_lock_app_desc),
            checked = pinLock != null,
            onCheckedChange = { 
                if (it) {
                    showPinDialog = true
                } else {
                    viewModel.setPinLock(null)
                }
            }
        )
    }

    if (showTimeoutDialog) {
        TimeoutSelectorDialog(
            currentTimeout = lockTimeout,
            onDismiss = { showTimeoutDialog = false },
            onSelect = { 
                viewModel.setLockTimeout(it)
                showTimeoutDialog = false
            }
        )
    }

    if (showPinDialog) {
        PinSetupDialog(
            onDismiss = { showPinDialog = false },
            onConfirm = { 
                viewModel.setPinLock(it)
                showPinDialog = false
            }
        )
    }
}

@Composable
fun SecurityToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, fontSize = 12.sp, color = TextLight, lineHeight = 16.sp)
            }
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
}

@Composable
fun SecurityClickItem(
    title: String,
    description: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, fontSize = 12.sp, color = TextLight, lineHeight = 16.sp)
            }
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PinkPrimary)
        }
    }
}

@Composable
fun TimeoutSelectorDialog(
    currentTimeout: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    val options = listOf(
        0L to stringResource(R.string.instant),
        60L to stringResource(R.string.minutes_1),
        120L to stringResource(R.string.minutes_2),
        180L to stringResource(R.string.minutes_3),
        300L to stringResource(R.string.minutes_5),
        600L to stringResource(R.string.minutes_10),
        1800L to stringResource(R.string.minutes_30),
        3600L to stringResource(R.string.hour_1),
        10800L to stringResource(R.string.hours_3)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lock_timeout)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (seconds, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(seconds) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTimeout == seconds,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = PinkPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_btn), color = PinkPrimary)
            }
        }
    )
}

@Composable
fun PinSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_pin)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text(stringResource(R.string.enter_pin)) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PinkPrimary,
                        focusedLabelColor = PinkPrimary,
                        cursorColor = PinkPrimary
                    )
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                    label = { Text(stringResource(R.string.confirm_pin)) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PinkPrimary,
                        focusedLabelColor = PinkPrimary,
                        cursorColor = PinkPrimary
                    )
                )
                if (error != null) {
                    Text(text = error!!, color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin == confirmPin && pin.length >= 4) {
                        onConfirm(pin)
                    } else if (pin.length < 4) {
                        error = context.getString(R.string.pin_min_digits)
                    } else {
                        error = context.getString(R.string.pins_dont_match)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_btn), color = PinkPrimary)
            }
        }
    )
}

@Composable
fun formatTimeout(seconds: Long): String {
    return when (seconds) {
        0L -> stringResource(R.string.instant)
        60L -> stringResource(R.string.minutes_1)
        120L -> stringResource(R.string.minutes_2)
        180L -> stringResource(R.string.minutes_3)
        300L -> stringResource(R.string.minutes_5)
        600L -> stringResource(R.string.minutes_10)
        1800L -> stringResource(R.string.minutes_30)
        3600L -> stringResource(R.string.hour_1)
        10800L -> stringResource(R.string.hours_3)
        else -> "$seconds s"
    }
}

@Composable
fun SettingsLanguageOption(
    title: String,
    subtitle: String,
    flagResId: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PinkPrimary.copy(alpha = 0.08f) else androidx.compose.ui.graphics.Color(0xFFF8FAFC)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) PinkPrimary else androidx.compose.ui.graphics.Color(0xFFE2E8F0)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = flagResId),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextLight
                )
            }
            if (isSelected) {
                androidx.compose.material3.RadioButton(
                    selected = true,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = PinkPrimary)
                )
            }
        }
    }
}
