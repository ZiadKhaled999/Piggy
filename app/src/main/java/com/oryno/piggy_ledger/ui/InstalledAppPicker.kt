package com.oryno.piggy_ledger.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.service.BankProviderRegistry
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val isFinancial: Boolean,
    val matchScore: Int = 0
)

fun isListenerAccessGranted(context: Context): Boolean {
    return try {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    } catch (_: Exception) {
        false
    }
}

fun openNotificationAccessSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (_: Exception) {}
    }
}

private val excludedSystemPrefixes = listOf(
    "com.android.systemui",
    "com.android.providers.",
    "com.android.internal.",
    "com.google.android.gms",
    "com.google.android.gsf",
    "com.google.android.ext.",
    "com.android.bluetooth",
    "com.android.nfc",
    "com.android.phone",
    "com.android.settings"
)

fun loadInstalledApps(context: Context, providerOrName: String? = null): List<InstalledAppInfo> {
    val pm = context.packageManager
    val packages = try {
        pm.getInstalledPackages(0)
    } catch (_: Exception) {
        return emptyList()
    }

    return packages.mapNotNull { pkg ->
        val ai = pkg.applicationInfo ?: return@mapNotNull null
        val pkgName = pkg.packageName ?: return@mapNotNull null
        if (pkgName == context.packageName) return@mapNotNull null
        if (excludedSystemPrefixes.any { pkgName.startsWith(it) }) return@mapNotNull null

        val label = try {
            pm.getApplicationLabel(ai).toString().trim().ifBlank { pkgName }
        } catch (_: Exception) {
            pkgName
        }

        val isUserApp = (ai.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        val isUpdatedSystemApp = (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        val isLikelyFin = BankProviderRegistry.isLikelyFinancialApp(pkgName, label)

        if (!isUserApp && !isUpdatedSystemApp && !isLikelyFin) {
            return@mapNotNull null
        }

        val score = BankProviderRegistry.scorePackageMatchForProvider(providerOrName, pkgName, label)
        InstalledAppInfo(
            packageName = pkgName,
            label = label,
            isFinancial = isLikelyFin || score >= 40,
            matchScore = score
        )
    }.sortedWith(
        compareByDescending<InstalledAppInfo> { it.matchScore }
            .thenByDescending { it.isFinancial }
            .thenBy { it.label.lowercase() }
    )
}

@Composable
fun rememberAppIconBitmap(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(packageName) {
        try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawableToBitmap(drawable)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap? {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

@Composable
fun AppPackageIcon(
    packageName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val bitmap = rememberAppIconBitmap(packageName)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier.clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = contentDescription,
                tint = NavyDark.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun NotificationAutomationSection(
    providerOrName: String,
    customKeywords: String,
    onCustomKeywordsChange: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasListenerAccess by remember { mutableStateOf(isListenerAccessGranted(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasListenerAccess = isListenerAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Minimalist efficient Notification Access Banner if not granted
        if (!hasListenerAccess) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PinkPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = PinkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.notification_access_banner_title),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        Text(
                            text = stringResource(R.string.notification_access_banner_desc),
                            fontSize = 11.sp,
                            color = TextLight,
                            lineHeight = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { openNotificationAccessSettings(context) },
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .testTag("grant_notification_access_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.grant_notification_access),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Custom SMS / Notification Sender Keywords
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.custom_alert_keywords_label),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )
            OutlinedTextField(
                value = customKeywords,
                onValueChange = onCustomKeywordsChange,
                placeholder = {
                    val hintExample = providerOrName.takeIf { it.isNotBlank() } ?: "CIB"
                    Text(stringResource(R.string.custom_alert_keywords_hint, hintExample))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        tint = TextLight,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_keywords_input"),
                shape = RoundedCornerShape(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
            )
            Text(
                text = stringResource(R.string.custom_alert_keywords_desc),
                fontSize = 11.sp,
                color = TextLight
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerBottomSheet(
    apps: List<InstalledAppInfo>,
    selectedPackage: String?,
    onAppSelected: (InstalledAppInfo?) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAllApps by remember { mutableStateOf(false) }

    val hasSuggestedApps = remember(apps) { apps.any { it.isFinancial || it.matchScore > 0 } }
    val baseList = remember(apps, showAllApps, hasSuggestedApps, searchQuery) {
        if (showAllApps || !hasSuggestedApps || searchQuery.isNotBlank()) {
            apps
        } else {
            apps.filter { it.isFinancial || it.matchScore > 0 || it.packageName == selectedPackage }
        }
    }

    val filteredApps = remember(baseList, searchQuery) {
        if (searchQuery.isBlank()) baseList
        else baseList.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFCBD5E1)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.choose_service_app_title),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyDark
                    )
                    Text(
                        text = stringResource(R.string.choose_service_app_subtitle),
                        fontSize = 12.sp,
                        color = TextLight
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search_installed_apps_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextLight) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_picker_search_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedBorderColor = PinkPrimary,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showAllApps,
                    onClick = { showAllApps = false },
                    label = { Text(stringResource(R.string.filter_suggested_apps)) },
                    modifier = Modifier.testTag("app_picker_filter_suggested")
                )
                FilterChip(
                    selected = showAllApps,
                    onClick = { showAllApps = true },
                    label = { Text(stringResource(R.string.filter_all_apps)) },
                    modifier = Modifier.testTag("app_picker_filter_all")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedPackage != null) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onAppSelected(null)
                                    onDismiss()
                                }
                                .testTag("app_picker_clear_option"),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null, tint = TextLight)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.app_picker_none_sms_only),
                                    fontWeight = FontWeight.SemiBold,
                                    color = NavyDark,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                if (filteredApps.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.no_matching_apps_found),
                                fontSize = 14.sp,
                                color = TextLight
                            )
                        }
                    }
                } else {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isSelected = app.packageName == selectedPackage
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onAppSelected(app)
                                    onDismiss()
                                }
                                .testTag("app_picker_item_${app.packageName}"),
                            color = if (isSelected) PinkPrimary.copy(alpha = 0.08f) else Color.White,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) PinkPrimary else Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppPackageIcon(
                                    packageName = app.packageName,
                                    contentDescription = app.label,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = app.label,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = NavyDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (app.matchScore >= 75) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFDCFCE7)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.best_match_badge),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF15803D),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = app.packageName,
                                        fontSize = 11.5.sp,
                                        color = TextLight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = PinkPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountLinkBottomSheet(
    app: InstalledAppInfo,
    accounts: List<Account>,
    currentlyLinkedAccountIds: List<String>,
    onSaveLinks: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIds by remember(app.packageName, currentlyLinkedAccountIds) {
        mutableStateOf(currentlyLinkedAccountIds.toSet())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFCBD5E1)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppPackageIcon(
                    packageName = app.packageName,
                    contentDescription = app.label,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.link_accounts_for_app, app.label),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyDark
                    )
                    Text(
                        text = stringResource(R.string.link_accounts_for_app_desc),
                        fontSize = 12.sp,
                        color = TextLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Auto-match option
            val isAutoMatch = selectedIds.isEmpty()
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { selectedIds = emptySet() }
                    .testTag("account_link_auto_match"),
                color = if (isAutoMatch) PinkPrimary.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isAutoMatch) PinkPrimary else Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isAutoMatch,
                        onClick = { selectedIds = emptySet() },
                        colors = RadioButtonDefaults.colors(selectedColor = PinkPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.auto_match_account_option),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NavyDark
                        )
                        Text(
                            text = stringResource(R.string.auto_match_account_desc),
                            fontSize = 11.5.sp,
                            color = TextLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(accounts, key = { it.id }) { account ->
                    val checked = account.id in selectedIds
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedIds = if (checked) selectedIds - account.id else selectedIds + account.id
                            }
                            .testTag("account_link_item_${account.id}"),
                        color = if (checked) PinkPrimary.copy(alpha = 0.08f) else Color.White,
                        border = BorderStroke(1.dp, if (checked) PinkPrimary else Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = PinkPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BrandLogo(
                                provider = account.provider ?: account.name,
                                accountType = account.type,
                                iconColorHex = account.icon_color,
                                logoUrl = account.logo_url,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = NavyDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = account.provider ?: account.type.name,
                                    fontSize = 11.5.sp,
                                    color = TextLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onSaveLinks(selectedIds.toList())
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_account_links_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
            ) {
                Text(
                    text = stringResource(R.string.save),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
            }
        }
    }
}
