package com.oryno.piggy_ledger.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.data.SubscriptionEntity
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionDetailScreen(
    subscription: SubscriptionEntity,
    accounts: List<Account>,
    onBack: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onArchive: () -> Unit,
    onReactivate: () -> Unit = onResume,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleAutoRenew: () -> Unit,
    onUpdateReminder: (enabled: Boolean, leadDays: Int, dailyRepeat: Boolean) -> Unit,
    onUpdateAccount: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val linkedAccount = remember(accounts, subscription.accountId) {
        accounts.find { it.id == subscription.accountId }
    }

    var showAccountPickerSheet by remember { mutableStateOf(false) }

    // Reminder state initialized from subscription entity
    var isReminderEnabled by remember(subscription) { mutableStateOf(subscription.reminderEnabled) }
    var selectedLeadDays by remember(subscription) { mutableIntStateOf(subscription.reminderLeadDays) }
    var isDailyRepeat by remember(subscription) { mutableStateOf(subscription.reminderDailyRepeat) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, context.getString(R.string.allow_notif_hint), Toast.LENGTH_SHORT).show()
        }
    }

    val formatter = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }
    val shortFmt = remember { SimpleDateFormat("MMM, yyyy", Locale.getDefault()) }
    val renewalStr = formatter.format(Date(subscription.nextRenewalDate))
    val createdStr = shortFmt.format(Date(subscription.createdAt))

    val now = System.currentTimeMillis()
    val diffDays = TimeUnit.MILLISECONDS.toDays(subscription.nextRenewalDate - now).toInt()

    val isPaused = subscription.status.equals("PAUSED", ignoreCase = true) || !subscription.isActive
    val isArchived = subscription.status.equals("ARCHIVED", ignoreCase = true)

    val cycleLabel = when (subscription.billingCycle.uppercase()) {
        "YEARLY" -> stringResource(R.string.sub_cycle_yearly_label)
        "WEEKLY" -> stringResource(R.string.sub_cycle_weekly_label)
        else -> stringResource(R.string.sub_cycle_monthly_label)
    }

    val cyclePriceSuffix = when (subscription.billingCycle.uppercase()) {
        "YEARLY" -> stringResource(R.string.sub_cycle_per_year)
        "WEEKLY" -> stringResource(R.string.sub_cycle_per_week)
        else -> stringResource(R.string.sub_cycle_per_month)
    }

    val categoryLabel = when (subscription.category.lowercase().replace("cat_", "")) {
        "entertainment" -> stringResource(R.string.cat_entertainment)
        "shopping" -> stringResource(R.string.cat_shopping)
        "work", "business" -> stringResource(R.string.cat_business)
        "education" -> stringResource(R.string.cat_education)
        "health" -> stringResource(R.string.cat_health)
        else -> stringResource(R.string.cat_subscriptions)
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.sub_manage_title),
                        fontWeight = FontWeight.Bold,
                        color = NavyDark,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_btn),
                            tint = NavyDark
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.sub_edit),
                            tint = NavyDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // 1. HERO CARD (Clean and focused without redundant action buttons)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Top row inside card: Status / Relative Due Tag
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (tagText, tagColor, tagBg) = when {
                                isArchived -> Triple(stringResource(R.string.sub_tab_archived), TextLight, Color(0xFFF1F5F9))
                                isPaused -> Triple(stringResource(R.string.sub_tab_paused), Color(0xFFB45309), Color(0xFFFEF3C7))
                                diffDays <= 0 -> Triple(stringResource(R.string.sub_due_today), Color(0xFFDC2626), Color(0xFFFEF2F2))
                                diffDays == 1 -> Triple(stringResource(R.string.sub_due_tomorrow), Color(0xFFD97706), Color(0xFFFFFBEB))
                                diffDays in 2..7 -> Triple(stringResource(R.string.sub_due_in_days, diffDays), Color(0xFFD97706), Color(0xFFFEF3C7))
                                else -> Triple(renewalStr, TextLight, Color(0xFFF1F5F9))
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = tagBg
                            ) {
                                Text(
                                    text = tagText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tagColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            if (!isArchived && !isPaused) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFD1FAE5)
                                ) {
                                    Text(
                                        text = stringResource(R.string.sub_tab_active),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Logo + Name + Description
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!subscription.logoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(subscription.logoUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = subscription.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                } else {
                                    Text(
                                        text = subscription.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Black,
                                        color = PinkPrimary,
                                        fontSize = 24.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = subscription.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NavyDark
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.sub_plan_format, cycleLabel, categoryLabel),
                                    fontSize = 12.5.sp,
                                    color = TextLight
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Price Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "${String.format(Locale.getDefault(), "%,.2f", subscription.amount)} ${subscription.currency}",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = NavyDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cyclePriceSuffix,
                                fontSize = 12.5.sp,
                                color = TextLight,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                }
            }

            // 2. 2x2 METRIC CARDS GRID (Next Payment, Billing Cycle, Total Spent, Category)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 1: Next Payment
                        val relativeDue = if (diffDays >= 0) {
                            stringResource(R.string.sub_in_days, diffDays)
                        } else {
                            stringResource(R.string.sub_overdue)
                        }
                        DetailMetricTile(
                            icon = Icons.Default.Event,
                            label = stringResource(R.string.sub_next_payment),
                            value = renewalStr,
                            subValue = relativeDue,
                            modifier = Modifier.weight(1f)
                        )

                        // Card 2: Billing Cycle
                        DetailMetricTile(
                            icon = Icons.Default.Autorenew,
                            label = stringResource(R.string.sub_billing_cycle),
                            value = cycleLabel,
                            subValue = stringResource(R.string.sub_since_date, createdStr),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 3: Total Spent
                        val effectivePaidCount = if (subscription.totalPaidCount > 0) {
                            subscription.totalPaidCount
                        } else if (subscription.hasFreeTrial) {
                            0
                        } else {
                            1
                        }
                        val effectiveTotalSpent = if (subscription.totalSpent > 0.0) {
                            subscription.totalSpent
                        } else {
                            effectivePaidCount * subscription.amount
                        }

                        DetailMetricTile(
                            icon = Icons.Default.Schedule,
                            label = stringResource(R.string.sub_total_spent),
                            value = "${String.format(Locale.getDefault(), "%,.1f", effectiveTotalSpent)} ${subscription.currency}",
                            subValue = stringResource(R.string.sub_over_n_payments, effectivePaidCount),
                            modifier = Modifier.weight(1f)
                        )

                        // Card 4: Category
                        DetailMetricTile(
                            icon = Icons.Default.Category,
                            label = stringResource(R.string.sub_category),
                            value = categoryLabel,
                            subValue = subscription.name,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. SETTINGS TOGGLES (Payment Reminder & Auto-Renew with customizable notification options)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Reminder switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsNone,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.sub_payment_reminder),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark
                                    )
                                    Text(
                                        text = stringResource(R.string.sub_reminder_desc),
                                        fontSize = 11.5.sp,
                                        color = TextLight
                                    )
                                }
                            }
                            Switch(
                                checked = isReminderEnabled,
                                onCheckedChange = { checked ->
                                    isReminderEnabled = checked
                                    if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                    onUpdateReminder(checked, selectedLeadDays, isDailyRepeat)
                                    Toast.makeText(context, context.getString(R.string.sub_reminder_saved), Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF4F46E5),
                                    checkedTrackColor = Color(0xFFC7D2FE)
                                )
                            )
                        }

                        // CUSTOMIZABLE REMINDER OPTIONS PICKER (When toggle is ON)
                        AnimatedVisibility(
                            visible = isReminderEnabled,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.sub_remind_lead_title),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDark
                                )

                                // Timing choice chips (1 day, 2 days, 3 days, 1 week)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val options = listOf(
                                        1 to stringResource(R.string.sub_remind_1_day),
                                        2 to stringResource(R.string.sub_remind_2_days),
                                        3 to stringResource(R.string.sub_remind_3_days),
                                        7 to stringResource(R.string.sub_remind_1_week)
                                    )

                                    options.forEach { (days, label) ->
                                        val isSelected = selectedLeadDays == days
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    selectedLeadDays = days
                                                    onUpdateReminder(true, days, isDailyRepeat)
                                                    Toast.makeText(context, context.getString(R.string.sub_reminder_saved), Toast.LENGTH_SHORT).show()
                                                },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) Color(0xFF4F46E5) else Color.White,
                                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0))
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else NavyDark,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFE2E8F0))

                                // Daily alerts toggle until renewal date
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.sub_daily_alerts),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = NavyDark
                                        )
                                        Text(
                                            text = stringResource(R.string.sub_daily_alerts_desc),
                                            fontSize = 11.sp,
                                            color = TextLight
                                        )
                                    }
                                    Switch(
                                        checked = isDailyRepeat,
                                        onCheckedChange = { repeatChecked ->
                                            isDailyRepeat = repeatChecked
                                            onUpdateReminder(true, selectedLeadDays, repeatChecked)
                                            Toast.makeText(context, context.getString(R.string.sub_reminder_saved), Toast.LENGTH_SHORT).show()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF4F46E5),
                                            checkedTrackColor = Color(0xFFC7D2FE)
                                        )
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // Auto-Renew switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF3E8FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = Color(0xFF7E22CE),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.sub_auto_renew),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark
                                    )
                                    Text(
                                        text = stringResource(R.string.sub_auto_renew_desc),
                                        fontSize = 11.5.sp,
                                        color = TextLight
                                    )
                                }
                            }
                            Switch(
                                checked = subscription.autoDebit,
                                onCheckedChange = { onToggleAutoRenew() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF4F46E5),
                                    checkedTrackColor = Color(0xFFC7D2FE)
                                )
                            )
                        }
                    }
                }
            }

            // 4. LINKED PAYMENT METHOD (WALLET)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.sub_payment_methods),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )
                            TextButton(onClick = { showAccountPickerSheet = true }) {
                                Text(
                                    text = "${stringResource(R.string.sub_change)} >",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4F46E5)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = NavyDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = linkedAccount?.name ?: stringResource(R.string.sub_no_wallet_linked),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (linkedAccount != null) NavyDark else Color(0xFFDC2626)
                                    )
                                    if (linkedAccount != null) {
                                        Text(
                                            text = stringResource(
                                                R.string.sub_wallet_balance,
                                                "${String.format(Locale.getDefault(), "%,.2f", linkedAccount.current_balance)} ${linkedAccount.currency}"
                                            ),
                                            fontSize = 12.sp,
                                            color = TextLight
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. BOTTOM ACTIONS (Pause & Cancel Buttons)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isPaused) {
                        Button(
                            onClick = onResume,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.sub_resume), fontWeight = FontWeight.Bold)
                        }
                    } else if (!isArchived) {
                        Button(
                            onClick = onPause,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                        ) {
                            Icon(Icons.Default.Pause, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.sub_pause), fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!isArchived) {
                        Button(
                            onClick = onArchive,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.sub_archive), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onReactivate,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.Restore, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.sub_reactivate), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet to change linked wallet
    if (showAccountPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccountPickerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.sub_choose_wallet),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                accounts.forEach { acc ->
                    val isSelected = acc.id == subscription.accountId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onUpdateAccount(acc.id)
                                showAccountPickerSheet = false
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) PinkPrimary.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSelected) PinkPrimary else Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = if (isSelected) PinkPrimary else NavyDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = acc.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDark
                                )
                                Text(
                                    text = stringResource(
                                        R.string.sub_wallet_balance,
                                        "${String.format(Locale.getDefault(), "%,.2f", acc.current_balance)} ${acc.currency}"
                                    ),
                                    fontSize = 12.sp,
                                    color = TextLight
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PinkPrimary,
                                    modifier = Modifier.size(20.dp)
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

@Composable
private fun DetailMetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    subValue: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NavyDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextLight,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = NavyDark,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subValue,
                fontSize = 11.5.sp,
                color = TextLight,
                maxLines = 1
            )
        }
    }
}
