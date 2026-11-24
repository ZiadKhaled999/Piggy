package com.oryno.piggy_ledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.AccountType
import com.oryno.piggy_ledger.data.NotificationParserPrefs
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AppFilterTab {
    FINANCIAL, ALL, WATCHED
}

@Composable
fun NotificationParserSettingsView(
    viewModel: PiggyLedgerViewModel? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { NotificationParserPrefs(context.applicationContext) }
    val enabled by prefs.enabled.collectAsStateWithLifecycle(initialValue = false)
    val watched by prefs.watchedPackages.collectAsStateWithLifecycle(initialValue = emptySet())
    val packageAccountMap by prefs.packageAccountMap.collectAsStateWithLifecycle(initialValue = emptyMap())
    val allAccounts by viewModel?.allAccounts?.collectAsStateWithLifecycle(initialValue = emptyList())
        ?: remember { mutableStateOf(emptyList()) }
    val userAccounts = remember(allAccounts) {
        allAccounts.filter { !it.isSystemAccount && !it.is_deleted && it.type != AccountType.CASH }
    }

    var hasAccess by remember { mutableStateOf(isListenerAccessGranted(context)) }
    var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(AppFilterTab.FINANCIAL) }
    var editingAppForLink by remember { mutableStateOf<InstalledAppInfo?>(null) }
    val scope = rememberCoroutineScope()

    BackHandler(onBack = onBack)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccess = isListenerAccessGranted(context)
                scope.launch {
                    apps = withContext(Dispatchers.IO) { loadInstalledApps(context) }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadInstalledApps(context) }
    }

    val displayedApps = remember(apps, watched, searchQuery, activeFilter) {
        val tabFiltered = when (activeFilter) {
            AppFilterTab.FINANCIAL -> apps.filter { it.isFinancial || it.packageName in watched }
            AppFilterTab.WATCHED -> apps.filter { it.packageName in watched }
            AppFilterTab.ALL -> apps
        }
        if (searchQuery.isBlank()) tabFiltered
        else tabFiltered.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("notification_parser_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_icon),
                    tint = NavyDark
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.notification_parser_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Access Banner Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (hasAccess) Color(0xFFF0FDF4) else Color(0xFFFFF7ED),
                border = BorderStroke(
                    1.dp,
                    if (hasAccess) Color(0xFFBBF7D0) else Color(0xFFFED7AA)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (hasAccess) Color(0xFFDCFCE7) else Color(0xFFFFEDD5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasAccess) Icons.Default.VerifiedUser else Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (hasAccess) Color(0xFF16A34A) else Color(0xFFEA580C),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasAccess)
                                stringResource(R.string.notification_access_granted)
                            else
                                stringResource(R.string.notification_access_required),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NavyDark
                        )
                        Text(
                            text = stringResource(R.string.notification_parser_select_apps_desc),
                            fontSize = 11.5.sp,
                            color = TextLight,
                            lineHeight = 16.sp
                        )
                    }
                    if (!hasAccess) {
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { openNotificationAccessSettings(context) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("grant_notification_access_button")
                        ) {
                            Text(
                                text = stringResource(R.string.grant_notification_access),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Master Enable Toggle
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.notification_parser_enable),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.notification_parser_enable_desc),
                            fontSize = 12.sp,
                            color = TextLight,
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = enabled && hasAccess,
                        enabled = hasAccess,
                        onCheckedChange = { value ->
                            scope.launch { prefs.setEnabled(value) }
                        },
                        modifier = Modifier.testTag("notification_parser_master_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PinkPrimary
                        )
                    )
                }
            }

            // Apps Header + Search + Filter Chips
            Text(
                text = stringResource(R.string.notification_parser_select_apps),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search_installed_apps_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextLight) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notification_parser_search"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = PinkPrimary,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = activeFilter == AppFilterTab.FINANCIAL,
                        onClick = { activeFilter = AppFilterTab.FINANCIAL },
                        label = { Text(stringResource(R.string.filter_financial_apps)) },
                        modifier = Modifier.testTag("notif_filter_financial")
                    )
                }
                item {
                    FilterChip(
                        selected = activeFilter == AppFilterTab.WATCHED,
                        onClick = { activeFilter = AppFilterTab.WATCHED },
                        label = { Text(stringResource(R.string.filter_watched_apps, watched.size)) },
                        modifier = Modifier.testTag("notif_filter_watched")
                    )
                }
                item {
                    FilterChip(
                        selected = activeFilter == AppFilterTab.ALL,
                        onClick = { activeFilter = AppFilterTab.ALL },
                        label = { Text(stringResource(R.string.filter_all_apps)) },
                        modifier = Modifier.testTag("notif_filter_all")
                    )
                }
            }

            // Installed Apps List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(displayedApps, key = { it.packageName }) { app ->
                    val checked = app.packageName in watched
                    val linkedIds = packageAccountMap[app.packageName].orEmpty()
                    val linkedAccounts = userAccounts.filter { it.id in linkedIds }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = hasAccess) {
                                scope.launch {
                                    if (checked) prefs.removePackage(app.packageName)
                                    else prefs.addPackage(app.packageName)
                                }
                            }
                            .testTag("watch_app_${app.packageName}"),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(
                            width = if (checked) 1.5.dp else 1.dp,
                            color = if (checked) PinkPrimary else Color(0xFFE2E8F0)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppPackageIcon(
                                    packageName = app.packageName,
                                    contentDescription = app.label,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(Modifier.width(12.dp))
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
                                        if (app.isFinancial) {
                                            Spacer(Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFEFF6FF)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.finance_app_badge),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1D4ED8),
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
                                Checkbox(
                                    checked = checked,
                                    enabled = hasAccess,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = PinkPrimary)
                                )
                            }

                            if (checked && userAccounts.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { editingAppForLink = app }
                                        .testTag("link_app_accounts_${app.packageName}"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Link,
                                                contentDescription = null,
                                                tint = PinkPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = if (linkedAccounts.isEmpty()) {
                                                    stringResource(R.string.linked_account_auto_match)
                                                } else {
                                                    stringResource(
                                                        R.string.linked_account_names,
                                                        linkedAccounts.joinToString(", ") { it.name }
                                                    )
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = NavyDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.change_linked_account),
                                            tint = TextLight,
                                            modifier = Modifier.size(15.dp)
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

    editingAppForLink?.let { targetApp ->
        AccountLinkBottomSheet(
            app = targetApp,
            accounts = userAccounts,
            currentlyLinkedAccountIds = packageAccountMap[targetApp.packageName].orEmpty(),
            onSaveLinks = { selectedIds ->
                scope.launch {
                    prefs.setAccountsForPackage(targetApp.packageName, selectedIds)
                }
            },
            onDismiss = { editingAppForLink = null }
        )
    }
}
