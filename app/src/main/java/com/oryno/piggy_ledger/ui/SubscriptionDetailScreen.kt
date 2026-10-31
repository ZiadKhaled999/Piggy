package com.oryno.piggy_ledger.ui

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
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
    onUpdateAccount: (String) -> Unit,
    onPayNow: () -> Unit
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

    var showDeleteSheet by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, context.getString(R.string.allow_notif_hint), Toast.LENGTH_SHORT).show()
        }
    }

    val formatter = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }
    val now = System.currentTimeMillis()
    val diffDays = TimeUnit.MILLISECONDS.toDays(subscription.nextRenewalDate - now).toInt()

    val isPaused = subscription.status.equals("PAUSED", ignoreCase = true) || !subscription.isActive
    val isArchived = subscription.status.equals("ARCHIVED", ignoreCase = true)

    val cycleLabel = when (subscription.billingCycle.uppercase()) {
        "YEARLY" -> stringResource(R.string.sub_cycle_yearly_label)
        "WEEKLY" -> stringResource(R.string.sub_cycle_weekly_label)
        "QUARTERLY" -> stringResource(R.string.sub_cycle_quarterly_label)
        else -> stringResource(R.string.sub_cycle_monthly_label)
    }

    val categoryLabel = when (subscription.category.lowercase().replace("cat_", "")) {
        "entertainment" -> stringResource(R.string.cat_entertainment)
        "shopping" -> stringResource(R.string.cat_shopping)
        "work", "business" -> stringResource(R.string.cat_business)
        "education" -> stringResource(R.string.cat_education)
        "health" -> stringResource(R.string.cat_health)
        "music" -> stringResource(R.string.cat_music)
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
                    // Three dots and Delete button removed as per request
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAFC)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Summary Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Logo
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

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = subscription.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDark
                                )
                                Text(
                                    text = "$categoryLabel • ${linkedAccount?.name ?: stringResource(R.string.sub_no_wallet_linked)}",
                                    fontSize = 13.sp,
                                    color = TextLight
                                )
                            }

                            // Circular Countdown
                            Box(
                                modifier = Modifier.size(50.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { 
                                        val totalDays = when(subscription.billingCycle.uppercase()) {
                                            "YEARLY" -> 365
                                            "WEEKLY" -> 7
                                            "QUARTERLY" -> 90
                                            else -> 30
                                        }
                                        (diffDays.coerceAtLeast(0).toFloat() / totalDays).coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    color = if (diffDays <= 3) Color.Red else PinkPrimary,
                                    strokeWidth = 4.dp,
                                    trackColor = Color(0xFFF1F5F9)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = diffDays.coerceAtLeast(0).toString(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark
                                    )
                                    Text(
                                        text = "days",
                                        fontSize = 8.sp,
                                        color = TextLight
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Three columns metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SummaryMetric(
                                icon = Icons.Default.Wallet,
                                label = stringResource(R.string.sub_total_spent),
                                value = "${String.format(Locale.getDefault(), "%,.2f", subscription.totalSpent)} ${subscription.currency}",
                                subValue = stringResource(R.string.sub_over_n_payments, subscription.totalPaidCount),
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetric(
                                icon = Icons.Default.CalendarMonth,
                                label = cycleLabel,
                                value = "${String.format(Locale.getDefault(), "%,.2f", subscription.amount)} ${subscription.currency}",
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetric(
                                icon = Icons.Default.CheckCircle,
                                label = stringResource(R.string.sub_status),
                                value = if (isPaused) stringResource(R.string.sub_tab_paused) else if (isArchived) stringResource(R.string.sub_tab_archived) else stringResource(R.string.sub_tab_active),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Timeline
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.sub_timeline),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        Icon(
                            Icons.Default.ArrowDownward,
                            null,
                            modifier = Modifier.size(16.dp).padding(start = 4.dp),
                            tint = TextLight
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Timeline Items - Capped at one future renewal
                            val trialStarted = subscription.createdAt
                            val subStarted = if (subscription.hasFreeTrial) subscription.trialEndsAt else subscription.createdAt
                            
                            // 4. One Future Renewal only
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = subStarted
                            
                            val cycleField = when(subscription.billingCycle.uppercase()) {
                                "YEARLY" -> Calendar.YEAR
                                "WEEKLY" -> Calendar.WEEK_OF_YEAR
                                "QUARTERLY" -> Calendar.MONTH
                                else -> Calendar.MONTH
                            }
                            val cycleAmount = if (subscription.billingCycle.uppercase() == "QUARTERLY") 3 else 1

                            // Add only ONE renewal
                            if (subscription.billingCycle.uppercase() == "QUARTERLY") {
                                calendar.add(Calendar.MONTH, 3)
                            } else {
                                calendar.add(cycleField, cycleAmount)
                            }
                            val renewalDate = calendar.timeInMillis
                            val isPast = now >= renewalDate
                            
                            val isApproachingRenewal = now >= (renewalDate - TimeUnit.DAYS.toMillis(30)) && now < renewalDate && !isPast
                            val isDuringTrial = subscription.hasFreeTrial && now >= trialStarted && now < subStarted
                            val isDuringSub = if (subscription.hasFreeTrial) (now >= subStarted && now < renewalDate && !isApproachingRenewal) else (now >= trialStarted && now < renewalDate && !isApproachingRenewal)

                            // 1. Trial Started / Created
                            TimelineItem(
                                date = formatter.format(Date(trialStarted)),
                                title = if (subscription.hasFreeTrial) stringResource(R.string.sub_timeline_trial_started) else stringResource(R.string.sub_timeline_sub_started),
                                amount = if (subscription.hasFreeTrial) "$0.00 ${subscription.currency}" else "${String.format(Locale.getDefault(), "%,.2f", subscription.amount)} ${subscription.currency}",
                                isCompleted = true,
                                isFirst = true,
                                showTodayMarker = if (subscription.hasFreeTrial) isDuringTrial else isDuringSub
                            )
                            
                            // 2. Trial Ended (if applicable)
                            if (subscription.hasFreeTrial) {
                                TimelineItem(
                                    date = formatter.format(Date(subStarted)),
                                    title = stringResource(R.string.sub_timeline_trial_ended),
                                    amount = "$0.00 ${subscription.currency}",
                                    isCompleted = now >= subStarted,
                                    showTodayMarker = false
                                )
                                // 3. Actual Subscription Started
                                TimelineItem(
                                    date = formatter.format(Date(subStarted)),
                                    title = stringResource(R.string.sub_timeline_sub_started),
                                    amount = "${String.format(Locale.getDefault(), "%,.2f", subscription.amount)} ${subscription.currency}",
                                    isCompleted = now >= subStarted,
                                    showTodayMarker = isDuringSub
                                )
                            }
                            
                            TimelineItem(
                                date = formatter.format(Date(renewalDate)),
                                title = stringResource(R.string.sub_timeline_renewal),
                                amount = "${String.format(Locale.getDefault(), "%,.2f", subscription.amount)} ${subscription.currency}",
                                isCompleted = isPast,
                                isLast = true,
                                showTodayMarker = isApproachingRenewal
                            )
                        }
                    }
                }
            }

            // Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Pay Now button removed as per request
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isPaused) {
                            Button(
                                onClick = onResume,
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.sub_resume), fontWeight = FontWeight.Bold)
                            }
                        } else if (!isArchived) {
                            Button(
                                onClick = onPause,
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                            ) {
                                Icon(Icons.Default.Pause, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.sub_pause), fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!isArchived) {
                            Button(
                                onClick = onArchive,
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                            ) {
                                Icon(Icons.Default.Archive, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.sub_archive), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onReactivate,
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                            ) {
                                Icon(Icons.Default.Unarchive, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.sub_reactivate), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteSheet) {
        DeleteConfirmationBottomSheet(
            subName = subscription.name,
            onDismiss = { showDeleteSheet = false },
            onConfirm = {
                onDelete()
                showDeleteSheet = false
            },
            onCopyName = {
                clipboardManager?.setText(AnnotatedString(subscription.name))
                Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
            }
        )
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

@Composable
private fun SummaryMetric(
    icon: ImageVector,
    label: String,
    value: String,
    subValue: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NavyDark,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextLight,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = NavyDark,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (subValue != null) {
            Text(
                text = subValue,
                fontSize = 11.sp,
                color = TextLight,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun TimelineItem(
    date: String,
    title: String,
    amount: String,
    isCompleted: Boolean,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    showTodayMarker: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        // Date
        Text(
            text = date.split(",").first(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isCompleted) NavyDark else TextLight,
            modifier = Modifier.width(60.dp).padding(top = 2.dp)
        )

        // Vertical Line and Node
        Column(
            modifier = Modifier.fillMaxHeight().width(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isFirst) {
                VerticalDivider(
                    modifier = Modifier.weight(1f),
                    color = if (isCompleted) PinkPrimary else Color(0xFFE2E8F0),
                    thickness = 2.dp
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) PinkPrimary else Color.White)
                    .border(2.dp, if (isCompleted) PinkPrimary else Color(0xFFE2E8F0), CircleShape)
            )

            if (showTodayMarker) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Today",
                    fontSize = 10.sp,
                    color = PinkPrimary,
                    fontWeight = FontWeight.Bold
                )
                VerticalDivider(
                    modifier = Modifier.height(16.dp),
                    color = PinkPrimary,
                    thickness = 2.dp
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PinkPrimary)
                )
                Canvas(modifier = Modifier.weight(1f).width(2.dp)) {
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
            } else if (!isLast) {
                VerticalDivider(
                    modifier = Modifier.weight(1f),
                    color = if (isCompleted) PinkPrimary else Color(0xFFE2E8F0),
                    thickness = 2.dp
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Content
        Row(
            modifier = Modifier.weight(1f).padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCompleted) NavyDark else TextLight
            )
            Text(
                text = amount,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) NavyDark else TextLight
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteConfirmationBottomSheet(
    subName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onCopyName: () -> Unit
) {
    var confirmText by remember { mutableStateOf("") }
    val isConfirmed = confirmText.trim().equals(subName.trim(), ignoreCase = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = Color(0xFFFEF2F2)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.sub_confirm_delete_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )

            Text(
                text = stringResource(R.string.sub_confirm_delete_msg),
                fontSize = 14.sp,
                color = TextLight,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = subName,
                        modifier = Modifier.weight(1f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                    TextButton(onClick = onCopyName) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.sub_copy_name), fontSize = 12.sp)
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.sub_confirm_delete_hint),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextLight,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                OutlinedTextField(
                    value = confirmText,
                    onValueChange = { confirmText = it },
                    placeholder = { Text(subName, color = TextLight.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isConfirmed) Color(0xFF10B981) else Color.Red,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
            }

            Button(
                onClick = onConfirm,
                enabled = isConfirmed,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    disabledContainerColor = Color.Red.copy(alpha = 0.3f)
                )
            ) {
                Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
