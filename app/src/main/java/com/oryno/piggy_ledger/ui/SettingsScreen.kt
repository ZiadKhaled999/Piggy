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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import com.oryno.piggy_ledger.ui.theme.AccentBlue

@Composable
fun SettingsScreen(
    viewModel: PiggyLedgerViewModel,
    initialMode: SettingsMode = SettingsMode.MAIN,
    onNavigateToPendingTransactions: () -> Unit,
    onBackClick: (() -> Unit)? = null
) {
    var settingsMode by remember { mutableStateOf(initialMode) }
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
                    com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.export_success), Toast.LENGTH_SHORT)
                } catch (e: Exception) {
                    com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.export_failed, e.message ?: ""), Toast.LENGTH_LONG)
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
                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.restore_success), Toast.LENGTH_SHORT)
                        },
                        onError = { error ->
                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.restore_failed, error), Toast.LENGTH_LONG)
                        }
                    )
                }
            } catch (e: Exception) {
                com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.read_file_failed, e.message ?: ""), Toast.LENGTH_LONG)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        when (settingsMode) {
            SettingsMode.MAIN -> {
                android.util.Log.d("SettingsScreen", "Showing SettingsMainContent")
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
                android.util.Log.d("SettingsScreen", "Showing DetailSettingsView with mode: $settingsMode")
                // Detail views handled inside when block below for simplicity in this refactor
                DetailSettingsView(
                    mode = settingsMode,
                    viewModel = viewModel,
                    onBack = {
                        if (initialMode != SettingsMode.MAIN) {
                            onBackClick?.invoke()
                        } else {
                            settingsMode = SettingsMode.MAIN
                        }
                    },
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
            title = stringResource(R.string.account_identifiers),
            iconRes = R.drawable.img_settings_identifiers_1784901671596,
            onClick = {
                android.util.Log.d("SettingsMainContent", "Account Identifiers clicked")
                onModeChange(SettingsMode.ACCOUNT_IDENTIFIERS)
            }
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
        
        SettingsItem(
            title = stringResource(R.string.piggy_ledger_pro),
            iconRes = null,
            iconVector = Icons.Default.Star,
            onClick = { onModeChange(SettingsMode.PRO) }
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
    android.util.Log.d("DetailSettingsView", "Entering DetailSettingsView with mode: $mode")
    val context = LocalContext.current
    val isPremium by viewModel.isPremium.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
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
                    SettingsMode.PRO -> stringResource(R.string.piggy_ledger_pro)
                    SettingsMode.ACCOUNT_IDENTIFIERS -> stringResource(R.string.account_identifiers)
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
                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.browser_error), Toast.LENGTH_SHORT)
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
                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.email_error), Toast.LENGTH_SHORT)
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
                                com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.csv_export_success), Toast.LENGTH_SHORT)
                            } catch (e: Exception) {
                                com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.csv_export_failed, e.message ?: ""), Toast.LENGTH_LONG)
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
                                com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.excel_export_success), Toast.LENGTH_SHORT)
                            } catch (e: Exception) {
                                com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.excel_export_failed, e.message ?: ""), Toast.LENGTH_LONG)
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
                                        if (isPremium) {
                                            createExcelLauncher.launch("piggy_ledger_backup.xls")
                                        } else {
                                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Upgrade to Pro to export your data", Toast.LENGTH_SHORT)
                                        }
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
                                        if (isPremium) {
                                            createCsvLauncher.launch("piggy_ledger_backup.csv")
                                        } else {
                                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Upgrade to Pro to export your data", Toast.LENGTH_SHORT)
                                        }
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
                                        if (isPremium) {
                                            createDocumentLauncher.launch("piggy_ledger_backup.json")
                                        } else {
                                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Upgrade to Pro to export your data", Toast.LENGTH_SHORT)
                                        }
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
                                        com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.csv_restore_success), Toast.LENGTH_SHORT)
                                    },
                                    onError = { error ->
                                        com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.csv_restore_failed, error), Toast.LENGTH_LONG)
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.csv_restore_failed, e.message ?: ""), Toast.LENGTH_LONG)
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
            SettingsMode.PRO -> {
                PiggyLedgerProView(viewModel = viewModel)
            }
            SettingsMode.ACCOUNT_IDENTIFIERS -> {
                android.util.Log.d("DetailSettingsView", "Calling AccountIdentifiersView")
                AccountIdentifiersView(viewModel = viewModel)
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

    var showTimeoutDialog by remember { mutableStateOf(false) }

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

@Composable
fun PiggyLedgerProView(viewModel: PiggyLedgerViewModel) {
    val isPremiumState by viewModel.isPremium.collectAsStateWithLifecycle()
    var isPro by remember { mutableStateOf<Boolean?>(null) }
    var customerInfo by remember { mutableStateOf<com.revenuecat.purchases.CustomerInfo?>(null) }

    LaunchedEffect(isPremiumState) {
        if (isPremiumState) {
            isPro = true
        }
    }

    LaunchedEffect(Unit) {
        try {
            com.revenuecat.purchases.Purchases.sharedInstance.getCustomerInfo(
                object : com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback {
                    override fun onReceived(info: com.revenuecat.purchases.CustomerInfo) {
                        customerInfo = info
                        val active = info.entitlements["Piggy Ledger Pro"]?.isActive == true
                        isPro = active || isPremiumState
                        if (active) {
                            viewModel.setPremiumStatus(true)
                        }
                    }
                    override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                        isPro = isPremiumState
                    }
                }
            )
        } catch (e: Exception) {
            isPro = isPremiumState
        }
    }

    if (isPro == null) {
        Box(modifier = Modifier.fillMaxSize().height(200.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PinkPrimary)
        }
    } else if (isPro == true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val context = LocalContext.current

            // 1. Premium Minimalist Light-Themed Hero Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(PinkPrimary.copy(alpha = 0.08f), CircleShape)
                            .border(1.dp, PinkPrimary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = PinkPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Piggy Ledger Pro",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyDark,
                        letterSpacing = 0.3.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = PinkPrimary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PinkPrimary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(PinkPrimary, CircleShape)
                            )
                            Text(
                                text = "Pro Active",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PinkPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "All premium features are fully unlocked.",
                        fontSize = 13.sp,
                        color = TextLight,
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // 2. Subscription Details
            val entitlement = remember(customerInfo) {
                customerInfo?.entitlements?.get("Piggy Ledger Pro")
            }

            if (entitlement != null) {
                val originalDate = entitlement.originalPurchaseDate
                val latestDate = entitlement.latestPurchaseDate
                val expirationDate = entitlement.expirationDate

                val prodId = entitlement.productIdentifier.lowercase()
                val planType = when {
                    prodId.contains("lifetime") || prodId.contains("life") || prodId.contains("lt") || expirationDate == null -> "Premium (Lifetime)"
                    prodId.contains("yearly") || prodId.contains("annual") || prodId.contains("yr") -> "Premium (Yearly)"
                    prodId.contains("monthly") || prodId.contains("mth") || prodId.contains("mo") -> "Premium (Monthly)"
                    else -> "Premium"
                }

                val isLifetime = planType.contains("Lifetime")

                val isVeryShortCycle = remember(originalDate, latestDate, expirationDate) {
                    val start = latestDate ?: originalDate ?: java.util.Date()
                    val end = expirationDate
                    end != null && (end.time - start.time < 24L * 60L * 60L * 1000L)
                }

                val dateFormat = remember(isVeryShortCycle) {
                    if (isVeryShortCycle) {
                        java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
                    } else {
                        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    }
                }

                val latestDateStr = remember(latestDate, originalDate, dateFormat) { 
                    val d = latestDate ?: originalDate
                    d?.let { dateFormat.format(it) } ?: "N/A" 
                }
                val expirationDateStr = remember(expirationDate, dateFormat) { expirationDate?.let { dateFormat.format(it) } ?: "N/A" }

                val remainingTimeStr = remember(expirationDate) {
                    expirationDate?.let { expDate ->
                        val diffMs = expDate.time - System.currentTimeMillis()
                        when {
                            diffMs <= 0 -> "Expired"
                            diffMs >= 24L * 60L * 60L * 1000L -> "${diffMs / (24L * 60L * 60L * 1000L)} days left"
                            diffMs >= 60L * 60L * 1000L -> "${diffMs / (60L * 60L * 1000L)} hours left"
                            else -> "${diffMs / (60L * 1000L)} minutes left"
                        }
                    } ?: "N/A"
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Subscription Plan", fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(planType, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                            }
                            if (!isLifetime) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Remaining Time", fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(remainingTimeStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PinkPrimary)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Status", fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Lifetime Access", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PinkPrimary)
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Subscription Date", fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(latestDateStr, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = NavyDark)
                            }
                            if (!isLifetime) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Expiration Date", fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(expirationDateStr, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = NavyDark)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { 
                    try {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/account/subscriptions")
                        )
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Unable to open subscriptions page", Toast.LENGTH_SHORT)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Subscription & Billing", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        PiggyLedgerPaywall(
            viewModel = viewModel,
            onPurchaseSuccess = { info ->
                customerInfo = info
                isPro = true
            }
        )
    }
}

@Composable
fun PremiumFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFFFF1F2), RoundedCornerShape(10.dp)), // Soft rose red pink background
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PinkPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = TextLight,
                lineHeight = 16.sp
            )
        }

        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF10B981), // Green checkmark
            modifier = Modifier.size(18.dp)
        )
    }
}

enum class PaywallPlan {
    MONTHLY, YEARLY, LIFETIME
}

@Composable
fun PiggyLedgerPaywall(
    viewModel: PiggyLedgerViewModel,
    onPurchaseSuccess: (com.revenuecat.purchases.CustomerInfo?) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) break
            ctx = ctx.baseContext
        }
        ctx as? android.app.Activity
    }

    var selectedPlan by remember { mutableStateOf(PaywallPlan.YEARLY) }
    var isPurchasing by remember { mutableStateOf(false) }
    var offerings by remember { mutableStateOf<com.revenuecat.purchases.Offerings?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        com.revenuecat.purchases.Purchases.sharedInstance.getOfferings(
            object : com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback {
                override fun onReceived(receivedOfferings: com.revenuecat.purchases.Offerings) {
                    offerings = receivedOfferings
                }
                override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                    // Handled gracefully with default values
                }
            }
        )
    }

    val monthlyPrice = "$9.99"
    val yearlyPrice = "$99.99"
    val lifetimePrice = "$299.99"

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val isSmallScreen = maxWidth < 360.dp
        val horizontalPadding = if (isSmallScreen) 16.dp else 20.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Clean Top Bar with Close Button (NO image header)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = horizontalPadding, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PinkPrimary.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PinkPrimary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(14.dp))
                            Text("PRO ACCESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PinkPrimary)
                        }
                    }

                    IconButton(
                        onClick = { /* Handled by parent or BackHandler */ },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NavyDark, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Glowing Crown/Star Badge Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(PinkPrimary.copy(alpha = 0.18f), PinkPrimary.copy(alpha = 0.02f))
                        ),
                        CircleShape
                    )
                    .border(1.5.dp, PinkPrimary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PinkPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Upgrade to Pro",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PinkPrimary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Unlock Your Smarter\nFinancial Routine",
                fontSize = if (isSmallScreen) 22.sp else 26.sp,
                fontWeight = FontWeight.Black,
                color = NavyDark,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = if (isSmallScreen) 28.sp else 32.sp
            )

            Spacer(modifier = Modifier.height(if (isSmallScreen) 20.dp else 24.dp))

            // Comparison Table
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FEATURES",
                            modifier = Modifier.weight(1.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "FREE",
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight
                        )
                        Text(
                            text = "PRO",
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PinkPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(6.dp))

                    val comparisonRows = listOf(
                        Triple("SMS Tracking", "14 Days", "Unlimited"),
                        Triple("Security Suite", "Basic", "Full"),
                        Triple("Accounts", "2", "Unlimited"),
                        Triple("Budgets", "1", "Unlimited"),
                        Triple("Goals", "2", "Unlimited"),
                        Triple("Loans/Debts", "5", "Unlimited"),
                        Triple("Analytics", "30 Days", "Unlimited")
                    )

                    comparisonRows.forEach { (feature, free, pro) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = feature,
                                modifier = Modifier.weight(1.5f),
                                fontSize = if (isSmallScreen) 12.sp else 13.sp,
                                color = NavyDark,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = free,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = if (isSmallScreen) 11.sp else 12.sp,
                                color = TextLight
                            )
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PinkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isSmallScreen) 20.dp else 24.dp))

            // Subscription Plans Title
            Text(
                text = "SELECT A PLAN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Refined & Reshaped 3 Plan Cards (Vertical Stacked Cards)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. YEARLY PLAN (MOST POPULAR)
                ReshapedPlanCard(
                    title = "Yearly Plan",
                    price = "$99.99 / year",
                    subtitle = "$8.33 / month — Billed annually",
                    tag = "MOST POPULAR • SAVE 17%",
                    tagBgColor = PinkPrimary,
                    tagTextColor = Color.White,
                    isSelected = selectedPlan == PaywallPlan.YEARLY,
                    onClick = { selectedPlan = PaywallPlan.YEARLY }
                )

                // 2. MONTHLY PLAN
                ReshapedPlanCard(
                    title = "Monthly Plan",
                    price = "$9.99 / month",
                    subtitle = "Flexible month-to-month billing",
                    tag = "FLEXIBLE",
                    tagBgColor = Color(0xFFF1F5F9),
                    tagTextColor = NavyDark,
                    isSelected = selectedPlan == PaywallPlan.MONTHLY,
                    onClick = { selectedPlan = PaywallPlan.MONTHLY }
                )

                // 3. LIFETIME PLAN
                ReshapedPlanCard(
                    title = "Lifetime Access",
                    price = "$299.99 one-time",
                    subtitle = "Pay once, access all current & future Pro features forever",
                    tag = "BEST VALUE",
                    tagBgColor = Color(0xFF10B981),
                    tagTextColor = Color.White,
                    isSelected = selectedPlan == PaywallPlan.LIFETIME,
                    onClick = { selectedPlan = PaywallPlan.LIFETIME }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Slide to Purchase
            SlideToPurchase(
                isPurchasing = isPurchasing,
                onCompleted = {
                    val pkgToPurchase = when (selectedPlan) {
                        PaywallPlan.MONTHLY -> offerings?.current?.monthly
                        PaywallPlan.YEARLY -> offerings?.current?.annual
                        PaywallPlan.LIFETIME -> offerings?.current?.lifetime
                    }

                    if (pkgToPurchase != null && activity != null) {
                        isPurchasing = true
                        com.revenuecat.purchases.Purchases.sharedInstance.purchase(
                            com.revenuecat.purchases.PurchaseParams.Builder(activity, pkgToPurchase).build(),
                            object : com.revenuecat.purchases.interfaces.PurchaseCallback {
                                override fun onCompleted(storeTransaction: com.revenuecat.purchases.models.StoreTransaction, info: com.revenuecat.purchases.CustomerInfo) {
                                    isPurchasing = false
                                    if (info.entitlements["Piggy Ledger Pro"]?.isActive == true) {
                                        viewModel.setPremiumStatus(true)
                                        onPurchaseSuccess(info)
                                        com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Pro features unlocked!", Toast.LENGTH_LONG)
                                    }
                                }
                                override fun onError(error: com.revenuecat.purchases.PurchasesError, userCancelled: Boolean) {
                                    isPurchasing = false
                                }
                            }
                        )
                    } else {
                        // Simulation for dev environments / tests
                        coroutineScope.launch {
                            isPurchasing = true
                            delay(1000)
                            isPurchasing = false
                            viewModel.setPremiumStatus(true)
                            onPurchaseSuccess(null)
                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Welcome to Pro! Pro features unlocked.", Toast.LENGTH_SHORT)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Policy Links
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Restore Purchases",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PinkPrimary,
                    modifier = Modifier.clickable {
                        com.revenuecat.purchases.Purchases.sharedInstance.restorePurchases(
                            object : com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback {
                                override fun onReceived(info: com.revenuecat.purchases.CustomerInfo) {
                                    if (info.entitlements["Piggy Ledger Pro"]?.isActive == true) {
                                        viewModel.setPremiumStatus(true)
                                        onPurchaseSuccess(info)
                                        com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Pro features restored!", Toast.LENGTH_LONG)
                                    } else {
                                        com.oryno.piggy_ledger.ui.ToastUtil.show(context, "No active subscription found.", Toast.LENGTH_LONG)
                                    }
                                }
                                override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                                    com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Restore failed: ${error.message}", Toast.LENGTH_LONG)
                                }
                            }
                        )
                    }
                )
                Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(TextLight.copy(alpha = 0.3f)))
                Text(
                    "Terms",
                    fontSize = 12.sp,
                    color = TextLight,
                    modifier = Modifier.clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.oryno.com/piggy-ledger/terms"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Terms unavailable", Toast.LENGTH_SHORT)
                        }
                    }
                )
                Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(TextLight.copy(alpha = 0.3f)))
                Text(
                    "Privacy",
                    fontSize = 12.sp,
                    color = TextLight,
                    modifier = Modifier.clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.oryno.com/piggy-ledger/privacy"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Privacy policy unavailable", Toast.LENGTH_SHORT)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ReshapedPlanCard(
    title: String,
    price: String,
    subtitle: String,
    tag: String? = null,
    tagBgColor: Color = PinkPrimary,
    tagTextColor: Color = Color.White,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) PinkPrimary.copy(alpha = 0.06f) else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) PinkPrimary else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) PinkPrimary else Color.Transparent)
                            .border(
                                width = if (isSelected) 0.dp else 2.dp,
                                color = if (isSelected) Color.Transparent else Color(0xFFCBD5E1),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                }

                if (tag != null) {
                    Surface(
                        color = if (isSelected) tagBgColor else tagBgColor.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = tagTextColor,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = TextLight,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = price,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) PinkPrimary else NavyDark
                )
            }
        }
    }
}

@Composable
fun SlideToPurchase(
    isPurchasing: Boolean,
    onCompleted: () -> Unit
) {
    var widthPx by remember { mutableStateOf(0) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val thumbWidth = 58.dp
    val thumbHeight = 52.dp
    val paddingDp = 6.dp
    val thumbWidthPx = with(density) { thumbWidth.toPx() }
    val paddingPx = with(density) { paddingDp.toPx() }

    val maxOffset = remember(widthPx, thumbWidthPx, paddingPx) {
        (widthPx - thumbWidthPx - (paddingPx * 2)).coerceAtLeast(0f)
    }

    val dragOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PinkPrimary.copy(alpha = 0.08f))
            .border(1.5.dp, PinkPrimary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .onSizeChanged { widthPx = it.width },
        contentAlignment = Alignment.CenterStart
    ) {
        // Target slot (drop zone box at the far right inside track)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(paddingDp)
                .size(width = thumbWidth, height = thumbHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(PinkPrimary.copy(alpha = 0.04f))
                .border(1.5.dp, PinkPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        )

        // Swiped filled track area - attached behind the knob as it slides
        val fillWidthDp = with(density) { (paddingPx + dragOffset.value + thumbWidthPx).toDp() }
        Box(
            modifier = Modifier
                .width(fillWidthDp)
                .fillMaxHeight()
                .padding(paddingDp)
                .clip(RoundedCornerShape(12.dp))
                .background(PinkPrimary)
        )

        // Base text (unswiped section in NavyDark)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isPurchasing) "Contacting Store..." else "Slide to Unlock Pro",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NavyDark
            )
        }

        // Overlay text layer clipped to swiped fill (transitions smoothly to White text)
        Box(
            modifier = Modifier
                .width(fillWidthDp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
        ) {
            Box(
                modifier = Modifier
                    .width(with(density) { widthPx.toDp() })
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPurchasing) "Contacting Store..." else "Slide to Unlock Pro",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        // Thumb Knob (Squarish rounded drag handle with double chevron >>)
        if (!isPurchasing) {
            Box(
                modifier = Modifier
                    .offset { IntOffset((paddingPx + dragOffset.value).roundToInt(), 0) }
                    .padding(vertical = paddingDp)
                    .size(width = thumbWidth, height = thumbHeight)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(NavyDark)
                    .pointerInput(maxOffset) {
                        if (maxOffset <= 0f) return@pointerInput
                        detectDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    if (dragOffset.value >= maxOffset * 0.82f) {
                                        dragOffset.animateTo(maxOffset, spring(stiffness = Spring.StiffnessMedium))
                                        onCompleted()
                                    } else {
                                        dragOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                                    }
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    dragOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    val newOffset = (dragOffset.value + dragAmount.x).coerceIn(0f, maxOffset)
                                    dragOffset.snapTo(newOffset)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Double Chevron Icon (>>)
                Canvas(modifier = Modifier.size(20.dp)) {
                    val strokeWidth = 2.5.dp.toPx()
                    val p1 = Path().apply {
                        moveTo(size.width * 0.12f, size.height * 0.2f)
                        lineTo(size.width * 0.45f, size.height * 0.5f)
                        lineTo(size.width * 0.12f, size.height * 0.8f)
                    }
                    val p2 = Path().apply {
                        moveTo(size.width * 0.48f, size.height * 0.2f)
                        lineTo(size.width * 0.81f, size.height * 0.5f)
                        lineTo(size.width * 0.48f, size.height * 0.8f)
                    }
                    drawPath(
                        path = p1,
                        color = Color.White,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = p2,
                        color = Color.White,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(paddingDp)
                    .size(width = thumbWidth, height = thumbHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PinkPrimary),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountIdentifiersView(viewModel: PiggyLedgerViewModel) {
    android.util.Log.d("AccountIdentifiersView", "Entering AccountIdentifiersView")
    val context = LocalContext.current
    val customIdentifiers by viewModel.customIdentifiers.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showBottomSheet by remember { mutableStateOf(false) }
    var targetProviderForSheet by remember { mutableStateOf("Vodafone Cash") }

    val providersList = remember {
        listOf(
            // 4 E-Wallets
            ProviderIdentifierInfo("Vodafone Cash", "E-Wallet", listOf("VF-Cash")),
            ProviderIdentifierInfo("Orange Cash", "E-Wallet", listOf("OrangeCash")),
            ProviderIdentifierInfo("e& Cash", "E-Wallet", listOf("e&Cash")),
            ProviderIdentifierInfo("WE Pay", "E-Wallet", listOf("WEPay")),

            // National Switch
            ProviderIdentifierInfo("InstaPay / Instant Switch", "Switch & Apps", listOf("InstaPay", "SmartWallet", "Telda", "Nexta")),

            // 40 Egyptian Banks
            ProviderIdentifierInfo("National Bank of Egypt (NBE)", "Bank", listOf("NBE", "NationalBankOfEgypt", "NBEg", "الأهلي")),
            ProviderIdentifierInfo("Banque Misr", "Bank", listOf("BanqueMisr", "BM", "مصر")),
            ProviderIdentifierInfo("Commercial International Bank (CIB)", "Bank", listOf("CIB", "CIBEgypt", "التجاري الدولي")),
            ProviderIdentifierInfo("Banque du Caire", "Bank", listOf("BanqueDuCaire", "BDC", "القاهرة")),
            ProviderIdentifierInfo("QNB Alahli", "Bank", listOf("QNB", "QNBAlahli")),
            ProviderIdentifierInfo("Bank of Alexandria (AlexBank)", "Bank", listOf("AlexBank")),
            ProviderIdentifierInfo("HSBC Egypt", "Bank", listOf("HSBC", "HSBCEgypt")),
            ProviderIdentifierInfo("Faisal Islamic Bank of Egypt", "Bank", listOf("Faisal", "FaisalBank")),
            ProviderIdentifierInfo("Arab African International Bank (AAIB)", "Bank", listOf("AAIB")),
            ProviderIdentifierInfo("Abu Dhabi Islamic Bank (ADIB)", "Bank", listOf("ADIB", "ADIBEgypt")),
            ProviderIdentifierInfo("Crédit Agricole Egypt", "Bank", listOf("CreditAgricole", "CAE")),
            ProviderIdentifierInfo("Emirates NBD Egypt", "Bank", listOf("EmiratesNBD", "ENBD")),
            ProviderIdentifierInfo("Housing & Development Bank (HDB)", "Bank", listOf("HDB", "HousingDevelopmentBank")),
            ProviderIdentifierInfo("EG Bank (Egyptian Gulf Bank)", "Bank", listOf("EGBank", "EGB")),
            ProviderIdentifierInfo("SAIB Bank", "Bank", listOf("SAIB", "SAIBBank")),
            ProviderIdentifierInfo("Al Baraka Bank Egypt", "Bank", listOf("AlBaraka", "ABG")),
            ProviderIdentifierInfo("Attijariwafa Bank Egypt", "Bank", listOf("Attijariwafa", "AWB")),
            ProviderIdentifierInfo("Arab Bank Egypt", "Bank", listOf("ArabBank")),
            ProviderIdentifierInfo("Abu Dhabi Commercial Bank (ADCB)", "Bank", listOf("ADCB", "ADCBEgypt")),
            ProviderIdentifierInfo("Export Development Bank of Egypt (EBank)", "Bank", listOf("EBank", "EDBE")),
            ProviderIdentifierInfo("The United Bank", "Bank", listOf("UnitedBank", "UB")),
            ProviderIdentifierInfo("Suez Canal Bank", "Bank", listOf("SuezCanal", "SCB")),
            ProviderIdentifierInfo("Mashreq Bank Egypt", "Bank", listOf("Mashreq", "MashreqBank")),
            ProviderIdentifierInfo("Citibank Egypt", "Bank", listOf("Citibank", "Citi")),
            ProviderIdentifierInfo("First Abu Dhabi Bank (FAB / Audi)", "Bank", listOf("FAB", "FABEgypt", "BankAudi")),
            ProviderIdentifierInfo("Al Ahli Bank of Kuwait (ABK)", "Bank", listOf("ABK", "ABKEgypt")),
            ProviderIdentifierInfo("National Bank of Kuwait (NBK)", "Bank", listOf("NBK", "NBKEgypt")),
            ProviderIdentifierInfo("Bank ABC Egypt", "Bank", listOf("BankABC", "ABCBank")),
            ProviderIdentifierInfo("aiBank (Arab Investment Bank)", "Bank", listOf("aiBank", "AIBank")),
            ProviderIdentifierInfo("MIDBANK", "Bank", listOf("MIDBANK", "MDB")),
            ProviderIdentifierInfo("Egyptian Agricultural Bank", "Bank", listOf("AgriculturalBank", "PBDAC")),
            ProviderIdentifierInfo("Industrial Development Bank (IDB)", "Bank", listOf("IDB", "IDBEgypt")),
            ProviderIdentifierInfo("Arab International Bank (AIB)", "Bank", listOf("AIB", "ArabIntBank")),
            ProviderIdentifierInfo("Blom Bank Egypt", "Bank", listOf("BlomBank", "Blom")),
            ProviderIdentifierInfo("Standard Chartered Bank Egypt", "Bank", listOf("StandardChartered", "StanChart")),
            ProviderIdentifierInfo("Nasser Social Bank", "Bank", listOf("NasserBank")),
            ProviderIdentifierInfo("Egyptian Real Estate Bank", "Bank", listOf("REEB", "RealEstateBank")),
            ProviderIdentifierInfo("Piraeus Bank Egypt", "Bank", listOf("Piraeus", "PiraeusBank")),
            ProviderIdentifierInfo("Central Bank of Egypt (CBE)", "Bank", listOf("CBE"))
        )
    }

    val filteredProviders = remember(searchQuery, selectedCategory, customIdentifiers) {
        val result = providersList.filter { provider ->
            val matchesCategory = when (selectedCategory) {
                "E-Wallets" -> provider.category == "E-Wallet"
                "Banks" -> provider.category == "Bank"
                "Switch & Apps" -> provider.category == "Switch & Apps"
                else -> true
            }
            val userKw = customIdentifiers[provider.name] ?: emptyList()
            val allKw = provider.defaultKeywords + userKw
            val matchesSearch = searchQuery.isBlank() ||
                    provider.name.contains(searchQuery, ignoreCase = true) ||
                    allKw.any { it.contains(searchQuery, ignoreCase = true) }
            matchesCategory && matchesSearch
        }
        android.util.Log.d("AccountIdentifiersView", "Filtered providers size: ${result.size}")
        result
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_provider_placeholder), fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextLight) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PinkPrimary,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        // Category Filter Chips
        val categoryOptions = listOf(
            "All" to stringResource(R.string.category_all),
            "E-Wallets" to stringResource(R.string.category_e_wallets),
            "Banks" to stringResource(R.string.category_banks),
            "Switch & Apps" to stringResource(R.string.category_switch_apps)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(categoryOptions) { option ->
                val (catKey, catLabel) = option
                val isSelected = selectedCategory == catKey
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = catKey },
                    label = { Text(catLabel, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PinkPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFFF1F5F9),
                        labelColor = NavyDark
                    ),
                    border = null
                )
            }
        }

        // Providers List
        android.util.Log.d("AccountIdentifiersView", "Starting LazyColumn with ${filteredProviders.size} providers")
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(filteredProviders) { provider ->
                android.util.Log.d("AccountIdentifiersView", "Rendering provider: ${provider.name}")
                val userKeywords = customIdentifiers[provider.name] ?: emptyList()
                val accountType = if (provider.category == "E-Wallet") com.oryno.piggy_ledger.data.AccountType.WALLET else com.oryno.piggy_ledger.data.AccountType.BANK

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            targetProviderForSheet = provider.name
                            showBottomSheet = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BrandLogo(
                                provider = provider.name,
                                accountType = accountType,
                                iconColorHex = null,
                                modifier = Modifier.size(36.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = provider.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = NavyDark
                                )
                                Text(
                                    text = when(provider.category) {
                                        "E-Wallet" -> stringResource(R.string.category_e_wallets)
                                        "Bank" -> stringResource(R.string.category_banks)
                                        else -> provider.category
                                    },
                                    fontSize = 11.sp,
                                    color = TextLight
                                )
                            }

                            IconButton(
                                onClick = {
                                    targetProviderForSheet = provider.name
                                    showBottomSheet = true
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add keyword", tint = PinkPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Default Built-in Keywords
                        Text(stringResource(R.string.default_identifiers), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextLight)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            provider.defaultKeywords.forEach { kw ->
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(kw, fontSize = 11.sp, color = NavyDark, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        // User Added Custom Keywords
                        if (userKeywords.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(stringResource(R.string.your_custom_keywords), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PinkPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                userKeywords.forEach { kw ->
                                    Box(
                                        modifier = Modifier
                                            .background(PinkPrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                            .border(1.dp, PinkPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(kw, fontSize = 11.sp, color = PinkPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        val targetProviderInfo = providersList.find { it.name == targetProviderForSheet }
        val category = targetProviderInfo?.category ?: "Bank"
        val accountType = if (category == "E-Wallet") com.oryno.piggy_ledger.data.AccountType.WALLET else com.oryno.piggy_ledger.data.AccountType.BANK

        val toastMessage = stringResource(R.string.toast_saved_keywords, targetProviderForSheet)

        AddIdentifierBottomSheet(
            providerName = targetProviderForSheet,
            accountType = accountType,
            onDismiss = { showBottomSheet = false },
            onSave = { newKeywords ->
                viewModel.addCustomIdentifierKeywords(targetProviderForSheet, newKeywords) {
                    com.oryno.piggy_ledger.ui.ToastUtil.show(context, toastMessage, Toast.LENGTH_SHORT)
                    showBottomSheet = false
                }
            }
        )
    }
}

data class ProviderIdentifierInfo(
    val name: String,
    val category: String,
    val defaultKeywords: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIdentifierBottomSheet(
    providerName: String,
    accountType: com.oryno.piggy_ledger.data.AccountType,
    onDismiss: () -> Unit,
    onSave: (keywords: List<String>) -> Unit
) {
    var inputKeywords by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                BrandLogo(
                    provider = providerName,
                    accountType = accountType,
                    iconColorHex = null,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        providerName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                    Text(
                        stringResource(R.string.add_custom_sms_identifiers),
                        fontSize = 12.sp,
                        color = TextLight
                    )
                }
            }

            Text(
                stringResource(R.string.enter_custom_keywords_desc, providerName),
                fontSize = 13.sp,
                color = TextLight,
                lineHeight = 18.sp
            )

            // Keyword Text Input
            OutlinedTextField(
                value = inputKeywords,
                onValueChange = { inputKeywords = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.keywords_placeholder, providerName), fontSize = 13.sp) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PinkPrimary,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }

                Button(
                    onClick = {
                        val parsed = inputKeywords.split(",", "\n", ";")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        if (parsed.isNotEmpty()) {
                            onSave(parsed)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = inputKeywords.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.save_btn), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}



