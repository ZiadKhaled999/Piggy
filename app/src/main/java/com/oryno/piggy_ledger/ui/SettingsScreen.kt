
package com.oryno.piggy_ledger.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity

import android.content.Intent

import android.net.Uri

import android.widget.Toast

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts

import androidx.appcompat.app.AppCompatDelegate

import com.oryno.piggy_ledger.BuildConfig
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
import androidx.compose.ui.graphics.RectangleShape

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.material.icons.filled.Logout
import androidx.activity.compose.BackHandler
import android.os.Build

import androidx.compose.foundation.lazy.items

import androidx.compose.ui.draw.rotate

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
    onBackClick: (() -> Unit)? = null,
    onSignOutClick: (() -> Unit)? = null
) {
    var modeStack by remember(initialMode) { mutableStateOf(listOf(initialMode)) }
    val settingsMode = modeStack.lastOrNull() ?: SettingsMode.MAIN
    val context = LocalContext.current

    val navigateBack: () -> Unit = {
        if (modeStack.size > 1) {
            modeStack = modeStack.dropLast(1)
        } else {
            onBackClick?.invoke()
        }
    }

    val navigateTo: (SettingsMode) -> Unit = { newMode ->
        modeStack = modeStack + newMode
    }

    BackHandler(enabled = true) {
        navigateBack()
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (settingsMode != SettingsMode.PRO && 
                    settingsMode != SettingsMode.PROFILE && 
                    settingsMode != SettingsMode.BACKUP && 
                    settingsMode != SettingsMode.FEEDBACK &&
                    settingsMode != SettingsMode.RATING
                ) Modifier.padding(horizontal = 24.dp) else Modifier
            )
    ) {
        if (settingsMode != SettingsMode.PROFILE && 
            settingsMode != SettingsMode.BACKUP && 
            settingsMode != SettingsMode.FEEDBACK &&
            settingsMode != SettingsMode.RATING
        ) {
            Spacer(modifier = Modifier.height(24.dp))
        }
        
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
                    onModeChange = { navigateTo(it) },
                    onNavigateToPendingTransactions = onNavigateToPendingTransactions,
                    onSignOutClick = onSignOutClick
                )
            }
            else -> {
                android.util.Log.d("SettingsScreen", "Showing DetailSettingsView with mode: $settingsMode")
                DetailSettingsView(
                    mode = settingsMode,
                    viewModel = viewModel,
                    onBack = navigateBack,
                    onNavigate = navigateTo,
                    createDocumentLauncher = createDocumentLauncher
                )
            }
        }
    }
}

@Composable
fun SettingsMainContent(
    onModeChange: (SettingsMode) -> Unit,
    onNavigateToPendingTransactions: () -> Unit,
    onSignOutClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
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
            title = stringResource(R.string.security),
            iconRes = R.drawable.img_settings_security,
            onClick = { onModeChange(SettingsMode.SECURITY) }
        )
        
        SettingsItem(
            title = stringResource(R.string.piggy_ledger_pro),
            iconRes = R.drawable.piggy_pro,
            iconVector = null,
            onClick = { onModeChange(SettingsMode.PRO) }
        )

        onSignOutClick?.let { onSignOut ->
            SettingsItem(
                title = stringResource(R.string.auth_sign_out),
                iconRes = null,
                iconVector = Icons.Default.Logout,
                onClick = onSignOut
            )
        }
        
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
    onNavigate: (SettingsMode) -> Unit = {},
    createDocumentLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    android.util.Log.d("DetailSettingsView", "Entering DetailSettingsView with mode: $mode")
    val context = LocalContext.current
    val isPremium by viewModel.isPremium.collectAsState()

    if (mode == SettingsMode.PRO) {
        PiggyLedgerProView(viewModel = viewModel, onClose = onBack)
        return
    }
    
    if (mode == SettingsMode.PROFILE) {
        Box(modifier = Modifier.fillMaxSize()) {
            ProfileSettingsView(viewModel = viewModel)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp)
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
                    text = stringResource(R.string.profile_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
            }
        }
        return
    }

    if (mode == SettingsMode.BACKUP) {
        BackupSettingsView(
            viewModel = viewModel,
            isPremium = isPremium,
            createJsonLauncher = createDocumentLauncher,
            onBack = onBack
        )
        return
    }

    if (mode == SettingsMode.FEEDBACK) {
        FeedbackSettingsView(
            viewModel = viewModel,
            onBack = onBack
        )
        return
    }

    if (mode == SettingsMode.PRO) {
        PiggyLedgerProView(
            viewModel = viewModel,
            onClose = onBack
        )
        return
    }

    if (mode == SettingsMode.RATING) {
        RateAppView(
            onBack = onBack,
            onNavigateToFeedback = { onNavigate(SettingsMode.FEEDBACK) }
        )
        return
    }
    
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
                    SettingsMode.CURRENCY -> stringResource(R.string.currency)
                    SettingsMode.FEEDBACK -> stringResource(R.string.community_feedback)
                    SettingsMode.RATING -> stringResource(R.string.rate_app_title)
                    SettingsMode.BACKUP -> stringResource(R.string.backup_data_title)
                    SettingsMode.SECURITY -> stringResource(R.string.security)
                    SettingsMode.PRO -> stringResource(R.string.piggy_ledger_pro)
                    SettingsMode.ACCOUNT_IDENTIFIERS -> stringResource(R.string.account_identifiers)
                    SettingsMode.PROFILE -> stringResource(R.string.profile_title)
                    else -> ""
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        when (mode) {

            SettingsMode.CURRENCY -> {
                CurrencySettingsView(viewModel = viewModel, onBack = onBack)
            }
            SettingsMode.LANGUAGE -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
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
            }
            SettingsMode.SECURITY -> {
                SecuritySettingsView(viewModel = viewModel)
            }
            SettingsMode.PRO -> {
                PiggyLedgerProView(viewModel = viewModel, onClose = onBack)
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
            colors = CardDefaults.cardColors(containerColor = Color.White)
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
fun PiggyLedgerProView(
    viewModel: PiggyLedgerViewModel,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
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
            if (com.revenuecat.purchases.Purchases.isConfigured) {
                com.revenuecat.purchases.Purchases.sharedInstance.getCustomerInfo(
                    object : com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback {
                        override fun onReceived(info: com.revenuecat.purchases.CustomerInfo) {
                            customerInfo = info
                            val active = info.entitlements.all.values.any { it.isActive } || info.entitlements["Piggy Ledger Pro"]?.isActive == true
                            isPro = active || isPremiumState
                            if (active && !isPremiumState) {
                                viewModel.setPremiumStatus(true)
                            }
                        }
                        override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                            isPro = isPremiumState
                        }
                    }
                )
            } else {
                isPro = isPremiumState
            }
        } catch (e: Exception) {
            isPro = isPremiumState
        }
    }

    if (isPro == null) {
        Box(modifier = Modifier.fillMaxSize().height(200.dp), contentAlignment = Alignment.Center) {
            ExpressiveLoadingIndicator(size = 40.dp)
        }
    } else if (isPro == true) {
        val configuration = LocalConfiguration.current
        val isCompact = configuration.screenWidthDp < 360

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (onClose != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_icon),
                            tint = NavyDark
                        )
                    }
                }
            }
            // 1. Top Hero Image (Clean, No Card/Border)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 180.dp else 220.dp)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.piggy_pro),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Pro Title & Description (Clean, No Card/Border)
            Text(
                text = stringResource(R.string.pro_title),
                fontSize = if (isCompact) 22.sp else 26.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.pro_desc),
                fontSize = if (isCompact) 13.sp else 15.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = if (isCompact) 18.sp else 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 3. Features List (Clean, borderless list rows)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val features = listOf(
                    stringResource(R.string.pro_feature_ai),
                    stringResource(R.string.pro_feature_1),
                    stringResource(R.string.pro_feature_2),
                    stringResource(R.string.pro_feature_3),
                    stringResource(R.string.pro_feature_4),
                    stringResource(R.string.pro_feature_5)
                )

                features.forEach { feature ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(PinkPrimary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = PinkPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = feature,
                            fontSize = if (isCompact) 14.sp else 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    } else {
        PiggyLedgerPaywall(
            viewModel = viewModel,
            onPurchaseSuccess = { info ->
                customerInfo = info
                isPro = true
            },
            onClose = onClose
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

private data class PlanMetadata(
    val tabLabel: String,
    val badgeName: String,
    val headerSubtitle: String,
    val priceText: String,
    val renewalCaption: String,
    val ctaText: String,
    val tag: String? = null,
    val accentColor: Color
)

private sealed class FeatureStatus {
    object Check : FeatureStatus()
    object Dash : FeatureStatus()
    data class TextValue(val text: String) : FeatureStatus()
}

private data class FeatureComparisonRow(
    val title: String,
    val freeValue: FeatureStatus,
    val proValue: FeatureStatus
)

@Composable
fun PiggyLedgerPaywall(
    viewModel: PiggyLedgerViewModel,
    onPurchaseSuccess: (com.revenuecat.purchases.CustomerInfo?) -> Unit,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    
    var offerings: com.revenuecat.purchases.Offerings? by remember { mutableStateOf(null) }
    var isLoadingOfferings by remember { mutableStateOf(true) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var refreshBillingTrigger by remember { mutableIntStateOf(0) }
    var selectedPlan by remember { mutableStateOf(PaywallPlan.YEARLY) }
    var isPurchasing by remember { mutableStateOf(false) }

    LaunchedEffect(refreshBillingTrigger) {
        isLoadingOfferings = true
        fetchError = null
        try {
            if (com.revenuecat.purchases.Purchases.isConfigured) {
                com.revenuecat.purchases.Purchases.sharedInstance.getOfferings(
                    object : com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback {
                        override fun onReceived(offeringsResult: com.revenuecat.purchases.Offerings) {
                            isLoadingOfferings = false
                            offerings = offeringsResult
                            fetchError = null
                        }
                        override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                            isLoadingOfferings = false
                            fetchError = BillingErrorHandler.formatPurchasesError(context, error)
                            android.util.Log.w("Paywall", "Offerings unavailable: ${error.message}")
                        }
                    }
                )
            } else {
                isLoadingOfferings = false
                fetchError = "In-App Billing is not configured."
            }
        } catch (e: Exception) {
            isLoadingOfferings = false
            fetchError = BillingErrorHandler.formatThrowable(context, e)
        }
    }

    val packagesList = remember(offerings) {
        val current = offerings?.current?.availablePackages
        if (!current.isNullOrEmpty()) {
            current
        } else {
            offerings?.all?.values?.flatMap { it.availablePackages } ?: emptyList()
        }
    }

    val monthlyPackage = packagesList.find { 
        it.packageType == com.revenuecat.purchases.PackageType.MONTHLY ||
        it.identifier.contains("month", ignoreCase = true) ||
        it.product.id.contains("month", ignoreCase = true)
    } ?: packagesList.getOrNull(0)

    val yearlyPackage = packagesList.find { 
        it.packageType == com.revenuecat.purchases.PackageType.ANNUAL ||
        it.identifier.contains("year", ignoreCase = true) ||
        it.identifier.contains("annual", ignoreCase = true) ||
        it.product.id.contains("year", ignoreCase = true) ||
        it.product.id.contains("annual", ignoreCase = true)
    } ?: packagesList.find { it != monthlyPackage } ?: packagesList.getOrNull(0)

    val lifetimePackage = packagesList.find { 
        it.packageType == com.revenuecat.purchases.PackageType.LIFETIME ||
        it.identifier.contains("life", ignoreCase = true) ||
        it.identifier.contains("lt", ignoreCase = true) ||
        it.product.id.contains("life", ignoreCase = true) ||
        it.product.id.contains("lt", ignoreCase = true)
    } ?: packagesList.find { it != monthlyPackage && it != yearlyPackage } ?: packagesList.getOrNull(0)

    val planMeta = when (selectedPlan) {
        PaywallPlan.MONTHLY -> PlanMetadata(
            tabLabel = stringResource(R.string.plan_monthly),
            badgeName = stringResource(R.string.plan_monthly),
            headerSubtitle = stringResource(R.string.plan_monthly_desc),
            priceText = monthlyPackage?.product?.price?.formatted ?: "$4.99 / mo",
            renewalCaption = stringResource(R.string.plan_monthly_renew, monthlyPackage?.product?.price?.formatted ?: "$4.99"),
            ctaText = stringResource(R.string.upgrade_monthly),
            accentColor = Color(0xFF2563EB)
        )
        PaywallPlan.YEARLY -> PlanMetadata(
            tabLabel = stringResource(R.string.plan_yearly),
            badgeName = stringResource(R.string.plan_yearly),
            headerSubtitle = stringResource(R.string.plan_yearly_desc),
            priceText = yearlyPackage?.product?.price?.formatted ?: "$49.99 / yr",
            renewalCaption = stringResource(R.string.plan_yearly_renew, yearlyPackage?.product?.price?.formatted ?: "$49.99"),
            ctaText = stringResource(R.string.upgrade_yearly),
            tag = stringResource(R.string.save_17_percent),
            accentColor = Color(0xFF7C3AED)
        )
        PaywallPlan.LIFETIME -> PlanMetadata(
            tabLabel = stringResource(R.string.plan_lifetime),
            badgeName = stringResource(R.string.plan_lifetime),
            headerSubtitle = stringResource(R.string.plan_lifetime_desc_2),
            priceText = lifetimePackage?.product?.price?.formatted ?: "$299.99",
            renewalCaption = stringResource(R.string.plan_lifetime_renew_2, lifetimePackage?.product?.price?.formatted ?: "$299.99"),
            ctaText = stringResource(R.string.upgrade_lifetime_2),
            tag = stringResource(R.string.best_value_caps),
            accentColor = PinkPrimary
        )
    }

    val comparisonFeatures = listOf(
        FeatureComparisonRow(stringResource(R.string.comp_piggy_ai), FeatureStatus.TextValue(stringResource(R.string.comp_piggy_ai_free)), FeatureStatus.TextValue(stringResource(R.string.comp_piggy_ai_pro))),
        FeatureComparisonRow(stringResource(R.string.comp_acc_goals), FeatureStatus.TextValue(stringResource(R.string.two_max)), FeatureStatus.Check),
        FeatureComparisonRow(stringResource(R.string.comp_budgets_loans), FeatureStatus.TextValue(stringResource(R.string.two_max)), FeatureStatus.Check),
        FeatureComparisonRow(stringResource(R.string.comp_adv_analytics), FeatureStatus.Dash, FeatureStatus.Check),
        FeatureComparisonRow(stringResource(R.string.comp_export), FeatureStatus.Dash, FeatureStatus.Check),
        FeatureComparisonRow(stringResource(R.string.comp_custom_categories), FeatureStatus.Dash, FeatureStatus.Check),
        FeatureComparisonRow(stringResource(R.string.comp_screenshot_protect), FeatureStatus.Check, FeatureStatus.Check),
        FeatureComparisonRow(stringResource(R.string.comp_cloud_sync), FeatureStatus.Check, FeatureStatus.Check),
        FeatureComparisonRow(stringResource(R.string.comp_priority_support), FeatureStatus.Dash, FeatureStatus.Check)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (onClose != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_icon),
                            tint = NavyDark
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Header Title & Subtitle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = androidx.compose.ui.text.buildAnnotatedString {
                        append("Piggy Ledger ")
                        withStyle(
                            androidx.compose.ui.text.SpanStyle(color = planMeta.accentColor)
                        ) {
                            append(planMeta.badgeName)
                        }
                    },
                    fontSize = if (LocalConfiguration.current.screenWidthDp < 360) 24.sp else 30.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = planMeta.headerSubtitle,
                    fontSize = if (LocalConfiguration.current.screenWidthDp < 360) 13.sp else 15.sp,
                    color = Color(0xFF64748B),
                    lineHeight = if (LocalConfiguration.current.screenWidthDp < 360) 18.sp else 22.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Segmented Bar Selector (Tabs: Monthly | Yearly | Lifetime)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(32.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PaywallPlan.values().forEach { plan ->
                        val isSelected = selectedPlan == plan
                        val pMeta = when (plan) {
                            PaywallPlan.MONTHLY -> Pair(stringResource(R.string.plan_monthly), null as String?)
                            PaywallPlan.YEARLY -> Pair(stringResource(R.string.plan_yearly), stringResource(R.string.save_17_percent_caps))
                            PaywallPlan.LIFETIME -> Pair(stringResource(R.string.plan_lifetime), stringResource(R.string.best_value_caps))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) Color(0xFFCBD5E1) else Color.Transparent,
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .clickable { selectedPlan = plan },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = pMeta.first,
                                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Feature Comparison Table Box (Light Mode Card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Column {
                    // Header row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.billing_table_features),
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = stringResource(R.string.billing_table_free),
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(72.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = planMeta.badgeName,
                            color = planMeta.accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(72.dp)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Comparison Rows
                    comparisonFeatures.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = row.title,
                                color = Color(0xFF0F172A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )

                            // Free Column Value
                            Box(
                                modifier = Modifier.width(72.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when (val f = row.freeValue) {
                                    is FeatureStatus.Check -> {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    is FeatureStatus.Dash -> {
                                        Text("—", color = Color(0xFFCBD5E1), fontSize = 14.sp)
                                    }
                                    is FeatureStatus.TextValue -> {
                                        Text(
                                            text = f.text,
                                            color = Color(0xFF64748B),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Pro Column Value
                            Box(
                                modifier = Modifier.width(72.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when (val p = row.proValue) {
                                    is FeatureStatus.Check -> {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = planMeta.accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    is FeatureStatus.Dash -> {
                                        Text("—", color = Color(0xFFCBD5E1), fontSize = 14.sp)
                                    }
                                    is FeatureStatus.TextValue -> {
                                        Text(
                                            text = p.text,
                                            color = planMeta.accentColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (fetchError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF1F2),
                    border = BorderStroke(1.dp, Color(0xFFFECDD3))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = fetchError ?: stringResource(R.string.billing_error_offline),
                            color = Color(0xFF9F1239),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { refreshBillingTrigger++ },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.retry),
                                    color = Color(0xFFE11D48),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Restore subscription button
            Text(
                text = stringResource(R.string.restore_subscription),
                color = Color(0xFF475569),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        if (!BillingErrorHandler.isOnline(context)) {
                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.billing_error_offline), Toast.LENGTH_LONG)
                            return@clickable
                        }
                        if (com.revenuecat.purchases.Purchases.isConfigured) {
                            try {
                                com.revenuecat.purchases.Purchases.sharedInstance.restorePurchases(
                                    object : com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback {
                                        override fun onReceived(info: com.revenuecat.purchases.CustomerInfo) {
                                            val active = info.entitlements.all.values.any { it.isActive } || info.entitlements["Piggy Ledger Pro"]?.isActive == true
                                            if (active) {
                                                viewModel.setPremiumStatus(true)
                                                onPurchaseSuccess(info)
                                                com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.billing_pro_restored), Toast.LENGTH_LONG)
                                            } else {
                                                com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.billing_no_active_sub), Toast.LENGTH_LONG)
                                            }
                                        }
                                        override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                                            val errorMsg = BillingErrorHandler.formatPurchasesError(context, error)
                                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, errorMsg, Toast.LENGTH_LONG)
                                        }
                                    }
                                )
                            } catch (e: Exception) {
                                val errorMsg = BillingErrorHandler.formatThrowable(context, e)
                                com.oryno.piggy_ledger.ui.ToastUtil.show(context, errorMsg, Toast.LENGTH_LONG)
                            }
                        } else {
                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.billing_not_available), Toast.LENGTH_LONG)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Normal Upgrade Button
            Button(
                onClick = {
                    if (isPurchasing) return@Button
                    if (!BillingErrorHandler.isOnline(context)) {
                        com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.billing_error_offline), Toast.LENGTH_LONG)
                        return@Button
                    }
                    val packageToBuy = when (selectedPlan) {
                        PaywallPlan.MONTHLY -> monthlyPackage
                        PaywallPlan.YEARLY -> yearlyPackage
                        PaywallPlan.LIFETIME -> lifetimePackage
                    } ?: packagesList.firstOrNull()

                    val activity = context.findActivity()
                    if (packageToBuy != null && activity != null) {
                        if (com.revenuecat.purchases.Purchases.isConfigured) {
                            try {
                                isPurchasing = true
                                com.revenuecat.purchases.Purchases.sharedInstance.purchase(
                                    com.revenuecat.purchases.PurchaseParams.Builder(activity, packageToBuy).build(),
                                    object : com.revenuecat.purchases.interfaces.PurchaseCallback {
                                        override fun onCompleted(storeTransaction: com.revenuecat.purchases.models.StoreTransaction, customerInfo: com.revenuecat.purchases.CustomerInfo) {
                                            isPurchasing = false
                                            val active = customerInfo.entitlements.all.values.any { it.isActive } || 
                                                         customerInfo.entitlements["Piggy Ledger Pro"]?.isActive == true ||
                                                         customerInfo.entitlements["pro"]?.isActive == true ||
                                                         customerInfo.entitlements["premium"]?.isActive == true
                                             if (active) {
                                                viewModel.setPremiumStatus(true)
                                                onPurchaseSuccess(customerInfo)
                                                com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.billing_welcome_pro), android.widget.Toast.LENGTH_SHORT)
                                            } else {
                                                com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.billing_purchase_completed), android.widget.Toast.LENGTH_SHORT)
                                            }
                                        }
                                        override fun onError(error: com.revenuecat.purchases.PurchasesError, userCancelled: Boolean) {
                                            isPurchasing = false
                                            if (!userCancelled) {
                                                val errorMsg = BillingErrorHandler.formatPurchasesError(context, error)
                                                if (errorMsg.isNotBlank()) {
                                                    com.oryno.piggy_ledger.ui.ToastUtil.show(context, errorMsg, android.widget.Toast.LENGTH_LONG)
                                                }
                                            }
                                        }
                                    }
                                )
                            } catch (e: Exception) {
                                isPurchasing = false
                                val errorMsg = BillingErrorHandler.formatThrowable(context, e)
                                com.oryno.piggy_ledger.ui.ToastUtil.show(context, errorMsg, android.widget.Toast.LENGTH_LONG)
                            }
                        } else {
                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.billing_initializing), android.widget.Toast.LENGTH_LONG)
                            refreshBillingTrigger++
                        }
                    } else {
                        val msg = when {
                            !BillingErrorHandler.isOnline(context) -> context.getString(R.string.billing_error_offline)
                            isLoadingOfferings -> context.getString(R.string.billing_loading_plans)
                            fetchError != null -> fetchError ?: context.getString(R.string.billing_error_general)
                            packagesList.isEmpty() -> context.getString(R.string.billing_no_products)
                            else -> context.getString(R.string.billing_error_product_unavailable)
                        }
                        com.oryno.piggy_ledger.ui.ToastUtil.show(context, msg, android.widget.Toast.LENGTH_LONG)
                        refreshBillingTrigger++
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = planMeta.accentColor,
                    contentColor = Color.White
                )
            ) {
                if (isPurchasing) {
                    ExpressiveLoadingIndicator(
                        size = 24.dp,
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${stringResource(R.string.paywall_cta_upgrade)} • ${planMeta.priceText}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer Subtitle / Renewal Text
            Text(
                text = planMeta.renewalCaption,
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "This plan includes all features. ",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "Learn more.",
                    fontSize = 12.sp,
                    color = planMeta.accentColor,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://piggyapp.top/pricing"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Could not open link", Toast.LENGTH_SHORT)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SwipeToUpgradeButton(
    planText: String,
    priceText: String,
    accentColor: Color,
    isPurchasing: Boolean,
    onSwipeComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val handleWidthPx = with(density) { 60.dp.toPx() }
    val maxOffset = (containerWidthPx - handleWidthPx).coerceAtLeast(1f)
    val progress = (offsetX / maxOffset).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFE2E8F0))
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(32.dp))
            .onGloballyPositioned { layoutCoordinates ->
                containerWidthPx = layoutCoordinates.size.width.toFloat()
            }
    ) {
        // Track fill behind handle
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(density) { (offsetX + handleWidthPx).toDp() })
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.4f),
                                accentColor
                            )
                        )
                    )
            )
        }

        // Text in track
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isPurchasing) {
                ExpressiveLoadingIndicator(
                    size = 26.dp,
                    color = accentColor,
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(
                    text = if (progress > 0.45f) "Release to Upgrade" else "Swipe to Upgrade ($priceText)",
                    color = if (progress > 0.5f) Color.White else Color(0xFF0F172A),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 48.dp)
                )
            }
        }

        // Handle
        if (!isPurchasing) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.toInt(), 0) }
                    .size(width = 60.dp, height = 60.dp)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                    .pointerInput(maxOffset) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX > maxOffset * 0.7f) {
                                    offsetX = maxOffset
                                    onSwipeComplete()
                                } else {
                                    offsetX = 0f
                                }
                            },
                            onDragCancel = { offsetX = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                val newOffset = (offsetX + dragAmount).coerceIn(0f, maxOffset)
                                offsetX = newOffset
                            }
                        )
                    }
                    .clickable {
                        onSwipeComplete()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Swipe handle",
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp).offset(x = (-12).dp)
                    )
                }
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
                ExpressiveLoadingIndicator(size = 24.dp, color = Color.White, strokeWidth = 2.dp)
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


fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}
