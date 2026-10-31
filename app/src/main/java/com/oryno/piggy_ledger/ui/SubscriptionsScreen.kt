package com.oryno.piggy_ledger.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.sin
import kotlin.random.Random
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.PiggyLedgerRepository
import com.oryno.piggy_ledger.data.SubscriptionEntity
import com.oryno.piggy_ledger.ui.subscription.CatalogItem
import com.oryno.piggy_ledger.ui.subscription.SubscriptionTab
import com.oryno.piggy_ledger.ui.subscription.SubscriptionViewModel
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

enum class MainSubNavTab {
    UPCOMING,
    CALENDAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { PiggyLedgerDatabase.getInstance(context).piggyLedgerDao() }
    val repository = remember { PiggyLedgerRepository(dao, context) }

    val viewModel: SubscriptionViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return SubscriptionViewModel(repository, dao) as T
        }
    })

    val uiState by viewModel.uiState.collectAsState()

    var activeMainTab by remember { mutableStateOf(MainSubNavTab.UPCOMING) }
    var isAddScreenOpen by remember { mutableStateOf(false) }
    var selectedCatalogItem by remember { mutableStateOf<CatalogItem?>(null) }
    var editingSub by remember { mutableStateOf<SubscriptionEntity?>(null) }
    var subToPayNow by remember { mutableStateOf<SubscriptionEntity?>(null) }
    var subForOptions by remember { mutableStateOf<SubscriptionEntity?>(null) }
    var selectedDetailSubId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Toast feedback
    LaunchedEffect(uiState.paymentSuccessMessage) {
        uiState.paymentSuccessMessage?.let { msg ->
            Toast.makeText(context, context.getString(R.string.sub_paid_success, msg), Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { err ->
            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    // 1. DEDICATED SUBSCRIPTION DETAIL PAGE
    val activeDetailSub = remember(uiState.allSubscriptions, selectedDetailSubId) {
        if (selectedDetailSubId == null) null
        else uiState.allSubscriptions.find { it.id == selectedDetailSubId }
    }

    if (activeDetailSub != null) {
        SubscriptionDetailScreen(
            subscription = activeDetailSub,
            accounts = uiState.accounts,
            onBack = { selectedDetailSubId = null },
            onPause = { viewModel.pauseSubscription(activeDetailSub, context) },
            onResume = { viewModel.resumeSubscription(activeDetailSub, context) },
            onArchive = { viewModel.archiveSubscription(activeDetailSub, context) },
            onReactivate = { viewModel.reactivateSubscription(activeDetailSub, context) },
            onEdit = {
                editingSub = activeDetailSub
                selectedCatalogItem = null
                isAddScreenOpen = true
            },
            onDelete = {
                viewModel.deleteSubscription(activeDetailSub.id, context)
                selectedDetailSubId = null
            },
            onToggleAutoRenew = { viewModel.toggleAutoRenew(activeDetailSub) },
            onUpdateReminder = { enabled, leadDays, dailyRepeat ->
                viewModel.updateReminderSettings(activeDetailSub, enabled, leadDays, dailyRepeat, context)
            },
            onUpdateAccount = { newAccountId ->
                viewModel.updateLinkedAccount(activeDetailSub, newAccountId)
            },
            onPayNow = { subToPayNow = activeDetailSub }
        )
        return
    }

    // 2. DEDICATED FULL-SCREEN SLIDE FLOW FOR ADDING / EDITING
    if (isAddScreenOpen) {
        AddSubscriptionSlideScreen(
            catalogItem = selectedCatalogItem,
            existingSub = editingSub,
            accounts = uiState.accounts,
            catalogServices = viewModel.catalogServices,
            onBackToDashboard = {
                isAddScreenOpen = false
                selectedCatalogItem = null
                editingSub = null
            },
            onSaveSubscription = { name, domain, amount, currency, cycle, accountId, category, hasTrial, trialDays ->
                if (editingSub != null) {
                    viewModel.updateSubscription(
                        id = editingSub!!.id,
                        name = name,
                        domain = domain,
                        amount = amount,
                        currency = currency,
                        billingCycle = cycle,
                        accountId = accountId,
                        category = category
                    )
                } else {
                    viewModel.addSubscription(
                        name = name,
                        domain = domain,
                        amount = amount,
                        currency = currency,
                        billingCycle = cycle,
                        accountId = accountId,
                        category = category,
                        hasFreeTrial = hasTrial,
                        trialDays = trialDays
                    )
                }
                isAddScreenOpen = false
                selectedCatalogItem = null
                editingSub = null
            }
        )
        return
    }

    // MAIN SUBSCRIPTION DASHBOARD
    BackHandler { onBack() }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.subscriptions_title),
                        fontWeight = FontWeight.Bold,
                        color = NavyDark,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("sub_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_btn),
                            tint = NavyDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // Sleek Floating Capsule Navigation Bar matching uploaded image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Subscriptions Tab (Normal list view)
                        val isUpcomingActive = activeMainTab == MainSubNavTab.UPCOMING && !isSearchActive
                        FloatingCapsuleCircleButton(
                            icon = Icons.Default.ReceiptLong,
                            contentDescription = stringResource(R.string.sub_nav_subscriptions),
                            isSelected = isUpcomingActive,
                            onClick = {
                                activeMainTab = MainSubNavTab.UPCOMING
                                isSearchActive = false
                            }
                        )

                        // 2. Calendar Tab
                        val isCalendarActive = activeMainTab == MainSubNavTab.CALENDAR
                        FloatingCapsuleCircleButton(
                            icon = Icons.Default.CalendarMonth,
                            contentDescription = stringResource(R.string.sub_nav_calendar),
                            isSelected = isCalendarActive,
                            onClick = {
                                activeMainTab = MainSubNavTab.CALENDAR
                                isSearchActive = false
                            }
                        )

                        // 3. Search Button
                        FloatingCapsuleCircleButton(
                            icon = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_subscriptions_hint),
                            isSelected = isSearchActive,
                            onClick = {
                                isSearchActive = !isSearchActive
                                if (isSearchActive) {
                                    activeMainTab = MainSubNavTab.UPCOMING
                                }
                            }
                        )

                        // 4. Add New Button (+)
                        FloatingCapsuleCircleButton(
                            icon = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_subscription),
                            isSelected = false,
                            onClick = {
                                selectedCatalogItem = null
                                editingSub = null
                                isAddScreenOpen = true
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isSearchActive) {
                // VIEW 0: DEDICATED FULL SCREEN SEARCH VIEW
                SubscriptionsSearchView(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onCloseSearch = {
                        isSearchActive = false
                        searchQuery = ""
                    },
                    uiState = uiState,
                    onSubscriptionClick = { sub -> selectedDetailSubId = sub.id },
                    onOptionsClick = { sub -> subForOptions = sub }
                )
            } else if (activeMainTab == MainSubNavTab.UPCOMING) {
                // VIEW 1: UPCOMING SUBSCRIPTIONS DASHBOARD
                SubscriptionsUpcomingView(
                    uiState = uiState,
                    searchQuery = searchQuery,
                    isSearchActive = isSearchActive,
                    onSearchQueryChange = { searchQuery = it },
                    onCloseSearch = {
                        isSearchActive = false
                        searchQuery = ""
                    },
                    onSelectTab = { viewModel.selectTab(it) },
                    onSubscriptionClick = { sub -> selectedDetailSubId = sub.id },
                    onOptionsClick = { sub -> subForOptions = sub }
                )
            } else {
                // VIEW 2: CALENDAR VIEW
                SubscriptionsCalendarView(
                    activeSubscriptions = uiState.activeSubscriptions,
                    onSubscriptionClick = { sub -> selectedDetailSubId = sub.id }
                )
            }
        }
    }

    // Options Bottom Sheet
    if (subForOptions != null) {
        SubscriptionOptionsBottomSheet(
            subscription = subForOptions!!,
            tab = uiState.selectedTab,
            onDismiss = { subForOptions = null },
            onPause = {
                viewModel.pauseSubscription(subForOptions!!)
                subForOptions = null
            },
            onResume = {
                viewModel.resumeSubscription(subForOptions!!)
                subForOptions = null
            },
            onArchive = {
                viewModel.archiveSubscription(subForOptions!!)
                subForOptions = null
            },
            onReactivate = {
                viewModel.reactivateSubscription(subForOptions!!)
                subForOptions = null
            },
            onEdit = {
                editingSub = subForOptions
                selectedCatalogItem = null
                subForOptions = null
                isAddScreenOpen = true
            },
            onDelete = {
                viewModel.deleteSubscription(subForOptions!!.id)
                subForOptions = null
            }
        )
    }

    // Pay Now Confirmation Modal Bottom Sheet
    if (subToPayNow != null) {
        PayNowConfirmationBottomSheet(
            subscription = subToPayNow!!,
            accounts = uiState.accounts,
            onDismiss = { subToPayNow = null },
            onConfirmPay = { selectedAccountId ->
                viewModel.payNow(subToPayNow!!, selectedAccountId, context)
                subToPayNow = null
            }
        )
    }
}

// Dedicated Full-Screen Search View with Big Pink Capsule Search Bar
@Composable
private fun SubscriptionsSearchView(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    uiState: com.oryno.piggy_ledger.ui.subscription.SubscriptionUiState,
    onSubscriptionClick: (SubscriptionEntity) -> Unit,
    onOptionsClick: (SubscriptionEntity) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val allCategories = remember(uiState.allSubscriptions) {
        listOf("All") + uiState.allSubscriptions
            .mapNotNull { it.category.trim().ifBlank { null } }
            .distinct()
            .sorted()
    }

    val filteredSubscriptions = remember(uiState.allSubscriptions, searchQuery, selectedCategory) {
        uiState.allSubscriptions.filter { sub ->
            val matchesQuery = searchQuery.isBlank() ||
                    sub.name.contains(searchQuery, ignoreCase = true) ||
                    sub.category.contains(searchQuery, ignoreCase = true) ||
                    sub.billingCycle.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategory.isNullOrEmpty() || selectedCategory == "All" ||
                    sub.category.equals(selectedCategory, ignoreCase = true)

            matchesQuery && matchesCategory
        }
    }

    val linkedMap = remember(uiState.accounts) {
        uiState.accounts.associateBy { it.id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // BIG PINK CAPSULE SEARCH BAR
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = PinkPrimary,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_subscriptions_hint),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sub_full_search_input")
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Surface(
                    onClick = onCloseSearch,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.search_close),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Category Filter Chips
        if (allCategories.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(allCategories) { category ->
                    val isSelected = (selectedCategory == category) || (selectedCategory == null && category == "All")
                    Surface(
                        onClick = {
                            selectedCategory = if (category == "All") null else category
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) Color(0xFF0F172A) else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0)),
                        shadowElevation = if (isSelected) 2.dp else 0.dp
                    ) {
                        Text(
                            text = if (category == "All") stringResource(R.string.search_all) else category,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Search Results Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (searchQuery.isBlank() && selectedCategory == null) stringResource(R.string.sub_nav_subscriptions) else stringResource(R.string.search_results),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )
            Text(
                text = stringResource(R.string.search_found_count, filteredSubscriptions.size),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextLight
            )
        }

        if (filteredSubscriptions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.search_no_results_found),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.search_no_results),
                        fontSize = 13.sp,
                        color = TextLight
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredSubscriptions, key = { it.id }) { sub ->
                    val tab = when (sub.status.uppercase(Locale.getDefault())) {
                        "PAUSED" -> SubscriptionTab.PAUSED
                        "ARCHIVED" -> SubscriptionTab.ARCHIVED
                        else -> SubscriptionTab.ACTIVE
                    }
                    SubscriptionCardItem(
                        subscription = sub,
                        linkedAccount = linkedMap[sub.accountId],
                        tab = tab,
                        onCardClick = { onSubscriptionClick(sub) },
                        onOptionsClick = { onOptionsClick(sub) }
                    )
                }
            }
        }
    }
}

// UPCOMING SUBSCRIPTIONS VIEW (Body filters upcoming for the next week)
@Composable
private fun SubscriptionsUpcomingView(
    uiState: com.oryno.piggy_ledger.ui.subscription.SubscriptionUiState,
    searchQuery: String,
    isSearchActive: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onSelectTab: (SubscriptionTab) -> Unit,
    onSubscriptionClick: (SubscriptionEntity) -> Unit,
    onOptionsClick: (SubscriptionEntity) -> Unit
) {
    val now = remember { System.currentTimeMillis() }
    val nextWeekMillis = remember { now + TimeUnit.DAYS.toMillis(7) }
    var isBubbleViewMode by remember { mutableStateOf(true) }

    // Upcoming subscriptions for next week (active only), filtered by search query
    val upcomingNextWeek = remember(uiState.activeSubscriptions, now, searchQuery) {
        val base = uiState.activeSubscriptions.filter { sub ->
            sub.nextRenewalDate in (now - TimeUnit.HOURS.toMillis(12))..nextWeekMillis
        }
        if (searchQuery.isBlank()) {
            base.sortedBy { it.nextRenewalDate }
        } else {
            base.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }.sortedBy { it.nextRenewalDate }
        }
    }

    // Full list based on selected capsule tab, filtered by search query
    val currentTabList = remember(uiState.selectedTab, uiState.activeSubscriptions, uiState.pausedSubscriptions, uiState.archivedSubscriptions, searchQuery) {
        val rawList = when (uiState.selectedTab) {
            SubscriptionTab.ACTIVE -> uiState.activeSubscriptions
            SubscriptionTab.PAUSED -> uiState.pausedSubscriptions
            SubscriptionTab.ARCHIVED -> uiState.archivedSubscriptions
        }
        if (searchQuery.isBlank()) {
            rawList
        } else {
            rawList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Metric Header Card (Clean real data only)
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.monthly_recurring_expense),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextLight,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${String.format(Locale.getDefault(), "%,.2f", uiState.totalMonthlyCost)} ${stringResource(R.string.egp_per_mo)}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = NavyDark
                            )
                        }

                        // Overlapping Brand Avatars Stack
                        Column(horizontalAlignment = Alignment.End) {
                            OverlappingBrandAvatars(subscriptions = uiState.activeSubscriptions)
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PinkPrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = stringResource(R.string.active_count, uiState.activeSubscriptions.size),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PinkPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Alert Banner for upcoming renewal if due soon
                    if (uiState.dueSoonSubscription != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        val dueSub = uiState.dueSoonSubscription!!
                        val dueDays = uiState.dueSoonDays
                        val dueTimeText = when (dueDays) {
                            0 -> stringResource(R.string.sub_due_today)
                            1 -> stringResource(R.string.sub_due_tomorrow)
                            else -> stringResource(R.string.sub_due_in_days, dueDays)
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSubscriptionClick(dueSub) },
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(
                                        R.string.sub_due_alert_banner,
                                        dueSub.name,
                                        "${dueSub.amount} ${dueSub.currency}",
                                        dueTimeText
                                    ),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B),
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

            // 2. Navigation Capsule Tabs Bar (White label, no border on count badge)
            item {
                AccountsStyleCapsuleTabBar(
                    selectedTab = uiState.selectedTab,
                    activeCount = uiState.activeSubscriptions.size,
                    pausedCount = uiState.pausedSubscriptions.size,
                    archivedCount = uiState.archivedSubscriptions.size,
                    onSelectTab = onSelectTab
                )
            }

            // 3. UPCOMING SUBSCRIPTIONS FOR THE NEXT WEEK (Featured Section)
            if (uiState.selectedTab == SubscriptionTab.ACTIVE) {
                item {
                    Text(
                        text = stringResource(R.string.sub_upcoming_week),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (upcomingNextWeek.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = stringResource(R.string.sub_no_upcoming_week),
                                fontSize = 13.sp,
                                color = TextLight,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    items(upcomingNextWeek, key = { "upcoming_${it.id}" }) { sub ->
                        val linkedAccount = uiState.accounts.find { it.id == sub.accountId }
                        SubscriptionCardItem(
                            subscription = sub,
                            linkedAccount = linkedAccount,
                            tab = SubscriptionTab.ACTIVE,
                            onCardClick = { onSubscriptionClick(sub) },
                            onOptionsClick = { onOptionsClick(sub) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.sub_all_subscriptions_title),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(modifier = Modifier.padding(3.dp)) {
                                Surface(
                                    onClick = { isBubbleViewMode = true },
                                    shape = RoundedCornerShape(13.dp),
                                    color = if (isBubbleViewMode) Color(0xFF0F172A) else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🫧 " + stringResource(R.string.sub_view_bubbles),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isBubbleViewMode) Color.White else Color(0xFF64748B)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = { isBubbleViewMode = false },
                                    shape = RoundedCornerShape(13.dp),
                                    color = if (!isBubbleViewMode) Color(0xFF0F172A) else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "📋 " + stringResource(R.string.sub_view_cards),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!isBubbleViewMode) Color.White else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. GAMIFIED PHYSICS BUBBLE DASHBOARD or CLASSIC LIST CARDS
            if (isBubbleViewMode) {
                item {
                    PhysicsBubbleDashboard(
                        subscriptions = currentTabList,
                        onSubscriptionClick = onSubscriptionClick
                    )
                }
            } else {
                if (currentTabList.isEmpty()) {
                    item {
                        val emptyTitle = when (uiState.selectedTab) {
                            SubscriptionTab.ACTIVE -> stringResource(R.string.no_subscriptions_yet)
                            SubscriptionTab.PAUSED -> stringResource(R.string.sub_no_paused)
                            SubscriptionTab.ARCHIVED -> stringResource(R.string.sub_no_archived)
                        }
                        val emptyDesc = when (uiState.selectedTab) {
                            SubscriptionTab.ACTIVE -> stringResource(R.string.no_subscriptions_desc)
                            SubscriptionTab.PAUSED -> stringResource(R.string.sub_paused_desc)
                            SubscriptionTab.ARCHIVED -> stringResource(R.string.sub_archived_desc)
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (uiState.selectedTab) {
                                            SubscriptionTab.ACTIVE -> Icons.Default.EventRepeat
                                            SubscriptionTab.PAUSED -> Icons.Default.PauseCircleFilled
                                            SubscriptionTab.ARCHIVED -> Icons.Default.Archive
                                        },
                                        contentDescription = null,
                                        tint = PinkPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = emptyTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDark,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = emptyDesc,
                                    fontSize = 12.5.sp,
                                    color = TextLight,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                } else {
                    items(currentTabList, key = { it.id }) { sub ->
                        val linkedAccount = uiState.accounts.find { it.id == sub.accountId }
                        SubscriptionCardItem(
                            subscription = sub,
                            linkedAccount = linkedAccount,
                            tab = uiState.selectedTab,
                            onCardClick = { onSubscriptionClick(sub) },
                            onOptionsClick = { onOptionsClick(sub) }
                        )
                    }
                }
            }
        }
    }

// CALENDAR VIEW (Matching reference images image_3e5ad698.png & image_a7184111.png)
@Composable
private fun SubscriptionsCalendarView(
    activeSubscriptions: List<SubscriptionEntity>,
    onSubscriptionClick: (SubscriptionEntity) -> Unit
) {
    val context = LocalContext.current
    var calendarMonthOffset by remember { mutableIntStateOf(0) }

    val baseCalendar = remember(calendarMonthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, calendarMonthOffset)
        }
    }

    val currentYear = baseCalendar.get(Calendar.YEAR)
    val currentMonth = baseCalendar.get(Calendar.MONTH)
    val monthName = remember(baseCalendar) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(baseCalendar.time)
    }

    // Number of days in this month
    val daysInMonth = remember(baseCalendar) {
        baseCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // First day of week index (Sunday = 0, Monday = 1, ...)
    val firstDayOfWeek = remember(baseCalendar) {
        val temp = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        temp.get(Calendar.DAY_OF_WEEK) - 1
    }

    // Today's day of month if current viewing month is real current month
    val realNow = remember { Calendar.getInstance() }
    val isViewingCurrentMonth = realNow.get(Calendar.YEAR) == currentYear && realNow.get(Calendar.MONTH) == currentMonth
    val todayDay = if (isViewingCurrentMonth) realNow.get(Calendar.DAY_OF_MONTH) else 1

    var selectedDay by remember { mutableIntStateOf(todayDay) }

    // Map subscriptions to each day of month based on nextRenewalDate
    val subsByDay = remember(activeSubscriptions, currentMonth, currentYear) {
        val map = mutableMapOf<Int, MutableList<SubscriptionEntity>>()
        activeSubscriptions.forEach { sub ->
            val cal = Calendar.getInstance().apply { timeInMillis = sub.nextRenewalDate }
            val subDay = cal.get(Calendar.DAY_OF_MONTH)
            map.getOrPut(subDay) { mutableListOf() }.add(sub)
        }
        map
    }

    val selectedDaySubs = subsByDay[selectedDay] ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Switcher Header & Active Subscriptions Count
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { calendarMonthOffset -= 1 },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = NavyDark)
                    }
                    Text(
                        text = monthName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = NavyDark,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = { calendarMonthOffset += 1 },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = NavyDark)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PinkPrimary.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = stringResource(R.string.active_count, activeSubscriptions.size),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PinkPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Calendar Grid Surface (Using the authentic off-white color and rounded day tiles from image_3e5ad698.png)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.5.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
                    // Day of week labels: Sun Mon Tue Wed Thu Fri Sat
                    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        daysOfWeek.forEach { dayName ->
                            Text(
                                text = dayName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = NavyDark,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Grid of days (Strict 7-column grid: repeat(7, 1fr))
                    val totalTiles = firstDayOfWeek + daysInMonth
                    val rowsCount = (totalTiles + 6) / 7

                    for (row in 0 until rowsCount) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (col in 0..6) {
                                val tileIndex = row * 7 + col
                                val dayNum = tileIndex - firstDayOfWeek + 1

                                if (dayNum in 1..daysInMonth) {
                                    val isSelected = dayNum == selectedDay
                                    val isToday = isViewingCurrentMonth && dayNum == realNow.get(Calendar.DAY_OF_MONTH)
                                    val daySubs = subsByDay[dayNum] ?: emptyList()

                                    // Day Cell: 1:1 aspect-ratio square, 16dp rounded corners, light gray #F3F4F6 background
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(2.dp)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) PinkPrimary else Color(0xFFF3F4F6))
                                            .then(
                                                if (isToday && !isSelected) {
                                                    Modifier.border(1.5.dp, PinkPrimary, RoundedCornerShape(16.dp))
                                                } else Modifier
                                            )
                                            .clickable { selectedDay = dayNum }
                                    ) {
                                        // 1. Day Number in top-left corner with standard padding
                                        Text(
                                            text = "$dayNum",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) Color.White else NavyDark,
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(top = 5.dp, start = 6.dp)
                                        )

                                        // 2. Subscription Logos positioned at bottom-center with small margin-bottom
                                        if (daySubs.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .padding(bottom = 5.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (daySubs.size == 1) {
                                                    MiniBrandAvatar(
                                                        sub = daySubs[0],
                                                        context = context,
                                                        size = 20.dp,
                                                        borderColor = if (isSelected) PinkPrimary else Color.White
                                                    )
                                                } else {
                                                    // Overlapping stacked avatar group
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy((-7).dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        val displayCount = daySubs.size.coerceAtMost(2)
                                                        for (i in 0 until displayCount) {
                                                            MiniBrandAvatar(
                                                                sub = daySubs[i],
                                                                context = context,
                                                                size = 20.dp,
                                                                borderColor = Color.White
                                                            )
                                                        }
                                                        if (daySubs.size > 2) {
                                                            // Overlapping +N indicator badge
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(20.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isSelected) Color.White.copy(alpha = 0.35f) else PinkPrimary)
                                                                    .border(1.5.dp, Color.White, CircleShape),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = "+${daySubs.size - 2}",
                                                                    fontSize = 8.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color.White
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Empty Cell offset before 1st of month or after last day
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(2.dp)
                                            .aspectRatio(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Reminders Section Header (Matching image_a7184111.png)
        item {
            val selectedCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, currentMonth)
                set(Calendar.DAY_OF_MONTH, selectedDay)
            }
            val dayTitleFmt = SimpleDateFormat("EEEE d, yyyy", Locale.getDefault()).format(selectedCalendar.time).uppercase()
            val isSelectedToday = isViewingCurrentMonth && selectedDay == realNow.get(Calendar.DAY_OF_MONTH)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayTitleFmt,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = NavyDark,
                    letterSpacing = 0.5.sp
                )
                if (isSelectedToday) {
                    Text(
                        text = stringResource(R.string.sub_today_badge),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight
                    )
                } else if (selectedDaySubs.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.sub_n_reminders, selectedDaySubs.size),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextLight
                    )
                }
            }
        }

        // Timeline Cards List for Selected Date (Matching image_a7184111.png)
        if (selectedDaySubs.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.sub_no_subs_on_date),
                            fontSize = 13.sp,
                            color = TextLight,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(selectedDaySubs, key = { "cal_${it.id}" }) { sub ->
                val nodeColor = remember(sub.category) {
                    when (sub.category.lowercase().replace("cat_", "")) {
                        "entertainment" -> Color(0xFF6366F1) // Indigo ring
                        "shopping" -> Color(0xFFF59E0B)      // Amber ring
                        "work", "business" -> Color(0xFF10B981) // Emerald ring
                        else -> Color(0xFF8B5CF6)            // Purple ring
                    }
                }

                val cycleSuffix = when (sub.billingCycle.uppercase()) {
                    "YEARLY" -> stringResource(R.string.sub_cycle_per_year)
                    "WEEKLY" -> stringResource(R.string.sub_cycle_per_week)
                    else -> stringResource(R.string.sub_cycle_per_month)
                }

                val renewalTypeLabel = when (sub.billingCycle.uppercase()) {
                    "YEARLY" -> stringResource(R.string.sub_yearly_renewal)
                    "WEEKLY" -> stringResource(R.string.sub_weekly_renewal)
                    else -> stringResource(R.string.sub_monthly_renewal)
                }

                // Vertical timeline layout matching image_a7184111.png
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSubscriptionClick(sub) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Timeline Node (Circular ring with white interior)
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.5.dp, nodeColor, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Subscription Card (NO Pay Now button)
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Squircle Brand Logo
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!sub.logoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(sub.logoUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = sub.name,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                    )
                                } else {
                                    Text(
                                        text = sub.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Black,
                                        color = PinkPrimary,
                                        fontSize = 18.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Name & Renewal/Reminder info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sub.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "9:00 AM • $renewalTypeLabel",
                                    fontSize = 11.5.sp,
                                    color = TextLight,
                                    maxLines = 1
                                )
                            }

                            // Price Column
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${String.format(Locale.getDefault(), "%,.2f", sub.amount)} ${sub.currency}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NavyDark
                                )
                                Text(
                                    text = cycleSuffix,
                                    fontSize = 11.sp,
                                    color = TextLight
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
private fun MiniBrandAvatar(
    sub: SubscriptionEntity,
    context: android.content.Context,
    size: Dp = 20.dp,
    borderColor: Color = Color.White
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.5.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!sub.logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(sub.logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = sub.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Text(
                text = sub.name.take(1).uppercase(),
                fontSize = if (size > 20.dp) 11.sp else 9.sp,
                fontWeight = FontWeight.Bold,
                color = PinkPrimary
            )
        }
    }
}

// Dark Capsule Tab Bar (White active label, no red, no circle/ellipse on number)
@Composable
private fun AccountsStyleCapsuleTabBar(
    selectedTab: SubscriptionTab,
    activeCount: Int,
    pausedCount: Int,
    archivedCount: Int,
    onSelectTab: (SubscriptionTab) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF1E293B),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SubscriptionTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val (title, count, icon) = when (tab) {
                    SubscriptionTab.ACTIVE -> Triple(stringResource(R.string.sub_tab_active), activeCount, Icons.Default.CheckCircle)
                    SubscriptionTab.PAUSED -> Triple(stringResource(R.string.sub_tab_paused), pausedCount, Icons.Default.PauseCircleFilled)
                    SubscriptionTab.ARCHIVED -> Triple(stringResource(R.string.sub_tab_archived), archivedCount, Icons.Default.Archive)
                }

                val contentColor = if (isSelected) Color(0xFF0F172A) else Color(0xFF94A3B8)
                val backgroundColor = if (isSelected) Color.White else Color.Transparent

                Surface(
                    onClick = { onSelectTab(tab) },
                    shape = RoundedCornerShape(20.dp),
                    color = backgroundColor,
                    modifier = Modifier.weight(if (isSelected) 1.2f else 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Raw number without any circle, ellipse, or quote around it
                        Text(
                            text = "$count",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

// Sleek Floating Capsule Circle Button matching uploaded image
@Composable
private fun FloatingCapsuleCircleButton(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) PinkPrimary else Color(0xFFF1F5F9).copy(alpha = 0.95f)
    val contentColor = if (isSelected) Color.White else Color(0xFF334155)

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = backgroundColor,
        shadowElevation = if (isSelected) 4.dp else 0.dp,
        modifier = Modifier.size(46.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// Subscription Card Item with Click to Open Detail Page
@Composable
private fun SubscriptionCardItem(
    subscription: SubscriptionEntity,
    linkedAccount: Account?,
    tab: SubscriptionTab,
    onCardClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val renewalStr = formatter.format(Date(subscription.nextRenewalDate))

    val now = System.currentTimeMillis()
    val diffDays = TimeUnit.MILLISECONDS.toDays(subscription.nextRenewalDate - now).toInt()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onCardClick() },
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.5.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Top Status Tag + Options 3 Dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status / Relative Due Tag
                if (tab == SubscriptionTab.ARCHIVED) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F5F9)) {
                        Text(
                            text = stringResource(R.string.sub_tab_archived),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (tab == SubscriptionTab.PAUSED) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFEF3C7)) {
                        Text(
                            text = stringResource(R.string.sub_tab_paused),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    if (subscription.hasFreeTrial && diffDays >= 0) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFEDE9FE)) {
                            Text(
                                text = stringResource(R.string.sub_trial_days_left, diffDays),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6D28D9),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        val (badgeText, badgeColor, badgeBg) = when {
                            diffDays <= 0 -> Triple(stringResource(R.string.sub_due_today), Color(0xFFDC2626), Color(0xFFFEF2F2))
                            diffDays == 1 -> Triple(stringResource(R.string.sub_due_tomorrow), Color(0xFFD97706), Color(0xFFFFFBEB))
                            diffDays in 2..5 -> Triple(stringResource(R.string.sub_due_in_days, diffDays), Color(0xFF2563EB), Color(0xFFEFF6FF))
                            else -> Triple(renewalStr, TextLight, Color(0xFFF1F5F9))
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = badgeBg) {
                            Text(
                                text = badgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Service Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand Logo Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!subscription.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(subscription.logoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = subscription.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Text(
                            text = subscription.name.take(1).uppercase(),
                            fontWeight = FontWeight.Black,
                            color = PinkPrimary,
                            fontSize = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subscription.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${subscription.billingCycle.lowercase().replaceFirstChar { it.uppercase() }} • ${subscription.category.replace("cat_", "").replaceFirstChar { it.uppercase() }}",
                        fontSize = 12.sp,
                        color = TextLight
                    )
                }

                // Amount
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format(Locale.getDefault(), "%,.2f", subscription.amount)} ${subscription.currency}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = NavyDark
                    )
                    Text(
                        text = "/ per mo",
                        fontSize = 11.sp,
                        color = TextLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Info & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Linked Wallet Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (linkedAccount != null) Color(0xFFF8FAFC) else Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, if (linkedAccount != null) Color(0xFFE2E8F0) else Color(0xFFFECACA))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (linkedAccount != null) Icons.Default.AccountBalanceWallet else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (linkedAccount != null) NavyDark else Color(0xFFDC2626),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = linkedAccount?.name ?: stringResource(R.string.sub_no_wallet_linked),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (linkedAccount != null) NavyDark else Color(0xFFDC2626)
                        )
                    }
                }

                // Right side: subtle renewal info & chevron
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tab == SubscriptionTab.ARCHIVED) {
                        Text(
                            text = stringResource(R.string.sub_lifetime_spent, "${String.format(Locale.getDefault(), "%,.0f", subscription.totalSpent)} ${subscription.currency}", subscription.totalPaidCount),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight
                        )
                    } else {
                        Text(
                            text = renewalStr,
                            fontSize = 11.5.sp,
                            color = TextLight,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextLight,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Circular Stack of Brand Avatars
@Composable
private fun OverlappingBrandAvatars(subscriptions: List<SubscriptionEntity>) {
    val context = LocalContext.current
    val topList = remember(subscriptions) { subscriptions.take(4) }

    Row(horizontalArrangement = Arrangement.End) {
        topList.forEachIndexed { index, sub ->
            Box(
                modifier = Modifier
                    .offset(x = (-10).dp * index)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                if (!sub.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(sub.logoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = sub.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = sub.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = NavyDark
                    )
                }
            }
        }
    }
}

// 3-Dots Options BottomSheet using Vector Icons
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionOptionsBottomSheet(
    subscription: SubscriptionEntity,
    tab: SubscriptionTab,
    onDismiss: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onArchive: () -> Unit,
    onReactivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!subscription.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(subscription.logoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = subscription.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Text(
                            text = subscription.name.take(1).uppercase(),
                            fontWeight = FontWeight.Black,
                            color = PinkPrimary,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subscription.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                    Text(
                        text = "${String.format(Locale.getDefault(), "%,.2f", subscription.amount)} ${subscription.currency} • ${subscription.billingCycle.lowercase().replaceFirstChar { it.uppercase() }}",
                        fontSize = 12.sp,
                        color = TextLight
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            if (tab == SubscriptionTab.ACTIVE) {
                OptionSheetRow(
                    icon = Icons.Default.PauseCircleOutline,
                    iconTint = Color(0xFFD97706),
                    iconBg = Color(0xFFFFFBEB),
                    title = stringResource(R.string.sub_pause),
                    subtitle = stringResource(R.string.sub_pause_desc_short),
                    onClick = onPause
                )
            } else if (tab == SubscriptionTab.PAUSED) {
                OptionSheetRow(
                    icon = Icons.Default.PlayCircleOutline,
                    iconTint = Color(0xFF059669),
                    iconBg = Color(0xFFD1FAE5),
                    title = stringResource(R.string.sub_resume),
                    subtitle = stringResource(R.string.sub_resume_desc_short),
                    onClick = onResume
                )
            }

            if (tab != SubscriptionTab.ARCHIVED) {
                OptionSheetRow(
                    icon = Icons.Default.Archive,
                    iconTint = Color(0xFF2563EB),
                    iconBg = Color(0xFFEFF6FF),
                    title = stringResource(R.string.sub_archive),
                    subtitle = stringResource(R.string.sub_archive_desc_short),
                    onClick = onArchive
                )
            } else {
                OptionSheetRow(
                    icon = Icons.Default.Replay,
                    iconTint = PinkPrimary,
                    iconBg = PinkPrimary.copy(alpha = 0.12f),
                    title = stringResource(R.string.sub_reactivate),
                    subtitle = stringResource(R.string.sub_reactivate_desc_short),
                    onClick = onReactivate
                )
            }

            OptionSheetRow(
                icon = Icons.Default.Edit,
                iconTint = NavyDark,
                iconBg = Color(0xFFF1F5F9),
                title = stringResource(R.string.sub_edit),
                subtitle = stringResource(R.string.sub_edit_desc_short),
                onClick = onEdit
            )

            HorizontalDivider(color = Color(0xFFF1F5F9))

            OptionSheetRow(
                icon = Icons.Default.Delete,
                iconTint = Color(0xFFDC2626),
                iconBg = Color(0xFFFEF2F2),
                title = stringResource(R.string.delete),
                subtitle = stringResource(R.string.sub_delete_desc_short),
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun OptionSheetRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = TextLight
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextLight, modifier = Modifier.size(18.dp))
        }
    }
}

// Data structure for currencies with CDN flag support
private data class SubCurrencyOption(
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val symbol: String,
    val flagUrl: String
)

private val subAvailableCurrencies = listOf(
    SubCurrencyOption("EGP", "Egyptian Pound", "جنيه مصري", "EGP", "https://flagcdn.com/w80/eg.png"),
    SubCurrencyOption("USD", "US Dollar", "دولار أمريكي", "$", "https://flagcdn.com/w80/us.png"),
    SubCurrencyOption("EUR", "Euro", "يورو", "€", "https://flagcdn.com/w80/eu.png"),
    SubCurrencyOption("SAR", "Saudi Riyal", "ريال سعودي", "SR", "https://flagcdn.com/w80/sa.png"),
    SubCurrencyOption("AED", "UAE Dirham", "درهم إماراتي", "AED", "https://flagcdn.com/w80/ae.png"),
    SubCurrencyOption("GBP", "British Pound", "جنيه إسترليني", "£", "https://flagcdn.com/w80/gb.png"),
    SubCurrencyOption("KWD", "Kuwaiti Dinar", "دينار كويتي", "KD", "https://flagcdn.com/w80/kw.png"),
    SubCurrencyOption("QAR", "Qatari Riyal", "ريال قطري", "QR", "https://flagcdn.com/w80/qa.png"),
    SubCurrencyOption("CAD", "Canadian Dollar", "دولار كندي", "CA$", "https://flagcdn.com/w80/ca.png"),
    SubCurrencyOption("AUD", "Australian Dollar", "دولار أسترالي", "AU$", "https://flagcdn.com/w80/au.png"),
    SubCurrencyOption("JPY", "Japanese Yen", "ين ياباني", "¥", "https://flagcdn.com/w80/jp.png"),
    SubCurrencyOption("TRY", "Turkish Lira", "ليرة تركية", "₺", "https://flagcdn.com/w80/tr.png"),
    SubCurrencyOption("BHD", "Bahraini Dinar", "دينار بحريني", "BD", "https://flagcdn.com/w80/bh.png"),
    SubCurrencyOption("OMR", "Omani Rial", "ريال عماني", "OMR", "https://flagcdn.com/w80/om.png"),
    SubCurrencyOption("CHF", "Swiss Franc", "فرنك سويسري", "CHF", "https://flagcdn.com/w80/ch.png")
)

@Composable
private fun BottomSheetOptionCard(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    leadingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFFF1F5F9) else Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) PinkPrimary else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingContent != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) PinkPrimary.copy(alpha = 0.15f) else Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    leadingContent()
                }
                Spacer(modifier = Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) NavyDark else NavyDark // Keep text dark for better contrast on gray
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = if (isSelected) NavyDark.copy(alpha = 0.7f) else TextLight
                    )
                }
            }
            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = PinkPrimary,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(1.5.dp, Color(0xFFCBD5E1), CircleShape)
                )
            }
        }
    }
}

// 3-Step Screen for Adding / Editing Subscription
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSubscriptionSlideScreen(
    catalogItem: CatalogItem?,
    existingSub: SubscriptionEntity?,
    accounts: List<Account>,
    catalogServices: List<CatalogItem>,
    onBackToDashboard: () -> Unit,
    onSaveSubscription: (
        name: String,
        domain: String,
        amount: Double,
        currency: String,
        cycle: String,
        accountId: String,
        category: String,
        hasTrial: Boolean,
        trialDays: Int
    ) -> Unit
) {
    val context = LocalContext.current
    var currentSlideStep by remember { mutableIntStateOf(if (existingSub != null) 3 else 1) }

    var name by remember { mutableStateOf(existingSub?.name ?: catalogItem?.name ?: "") }
    var domain by remember { mutableStateOf(existingSub?.logoUrl ?: catalogItem?.domain ?: "") }
    var amountText by remember {
        mutableStateOf(
            existingSub?.amount?.let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() }
                ?: catalogItem?.defaultAmountEgp?.toInt()?.toString()
                ?: "12"
        )
    }
    var currency by remember { mutableStateOf(existingSub?.currency ?: "USD") }
    var billingCycle by remember { mutableStateOf(existingSub?.billingCycle ?: "MONTHLY") }
    var selectedAccountId by remember {
        mutableStateOf(existingSub?.accountId ?: accounts.firstOrNull()?.id ?: "")
    }
    var selectedCategory by remember {
        mutableStateOf(existingSub?.category ?: catalogItem?.defaultCategory ?: "entertainment")
    }
    var noteText by remember { mutableStateOf("") }

    // Custom Provider intermediate state
    var customProviderName by remember { mutableStateOf(name) }
    var customProviderDomain by remember { mutableStateOf(domain) }

    // Start date & End date
    var startDateMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var endDateMs by remember {
        mutableStateOf<Long?>(
            java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, 1) }.timeInMillis
        )
    }

    // Trial state
    var hasFreeTrial by remember { mutableStateOf(existingSub?.hasFreeTrial ?: false) }
    var trialType by remember { mutableStateOf("Free") }
    var trialPeriod by remember { mutableStateOf("1 Week") }
    var trialDays by remember { mutableStateOf(if (existingSub?.trialDays ?: 0 > 0) existingSub!!.trialDays else 7) }

    // Reminders state
    var renewalReminderEnabled by remember { mutableStateOf(true) }
    var firstReminder by remember { mutableStateOf("1 day before") }
    var secondReminder by remember { mutableStateOf("None") }
    var thirdReminder by remember { mutableStateOf("None") }

    var trialReminderEnabled by remember { mutableStateOf(true) }
    var trialFirstReminder by remember { mutableStateOf("1 week before") }
    var trialSecondReminder by remember { mutableStateOf("3 days before") }
    var trialThirdReminder by remember { mutableStateOf("1 day before") }

    // Modal Sheet states
    var searchQuery by remember { mutableStateOf("") }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showCyclePicker by remember { mutableStateOf(false) }
    var showTrialPeriodPicker by remember { mutableStateOf(false) }
    var activeReminderTarget by remember { mutableStateOf<String?>(null) }

    @Composable
    fun mapReminderToDisplay(key: String): String {
        return when (key) {
            "1 day before" -> stringResource(R.string.sub_remind_1_day)
            "2 days before" -> stringResource(R.string.sub_remind_2_days)
            "3 days before" -> stringResource(R.string.sub_remind_3_days)
            "1 week before" -> stringResource(R.string.sub_remind_1_week)
            "On renewal day" -> stringResource(R.string.sub_due_today)
            "None" -> stringResource(R.string.sub_none)
            else -> key
        }
    }

    BackHandler {
        when (currentSlideStep) {
            3 -> {
                if (existingSub != null) onBackToDashboard()
                else if (customProviderName.isNotBlank() && domain == customProviderDomain) currentSlideStep = 2
                else currentSlideStep = 1
            }
            2 -> currentSlideStep = 1
            else -> onBackToDashboard()
        }
    }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0

    // Date Formatters
    val dateFmt = remember { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US) }
    val renewalFmt = remember { java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.US) }

    val currentCurrencyOption = subAvailableCurrencies.find { it.code.equals(currency, ignoreCase = true) }
        ?: SubCurrencyOption(currency, currency, currency, currency, "https://flagcdn.com/w80/us.png")

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentSlideStep) {
                            1 -> "Select company"
                            2 -> stringResource(R.string.custom_provider_title)
                            else -> if (existingSub != null) "Edit" else "New"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when (currentSlideStep) {
                                3 -> {
                                    if (existingSub != null) onBackToDashboard()
                                    else if (customProviderName.isNotBlank() && domain == customProviderDomain) currentSlideStep = 2
                                    else currentSlideStep = 1
                                }
                                2 -> currentSlideStep = 1
                                else -> onBackToDashboard()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NavyDark)
                    }
                },
                actions = {
                    when (currentSlideStep) {
                        1 -> {
                            IconButton(onClick = {
                                customProviderName = ""
                                customProviderDomain = ""
                                currentSlideStep = 2
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Add custom", tint = PinkPrimary)
                            }
                        }
                        2 -> {
                            IconButton(
                                onClick = {
                                    if (customProviderName.isNotBlank()) {
                                        name = customProviderName.trim()
                                        domain = customProviderDomain.trim()
                                        currentSlideStep = 3
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.enter_custom_provider_name), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = customProviderName.isNotBlank()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = if (customProviderName.isNotBlank()) PinkPrimary else TextLight)
                            }
                        }
                        3 -> {
                            IconButton(
                                onClick = {
                                    if (name.isNotBlank() && parsedAmount > 0) {
                                        onSaveSubscription(
                                            name,
                                            domain,
                                            parsedAmount,
                                            currency,
                                            billingCycle,
                                            selectedAccountId,
                                            selectedCategory,
                                            hasFreeTrial,
                                            trialDays
                                        )
                                    } else {
                                        Toast.makeText(context, "Please enter a valid service name and price", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Save", tint = PinkPrimary, modifier = Modifier.size(26.dp))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = currentSlideStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally(animationSpec = tween(300), initialOffsetX = { it }) + fadeIn() togetherWith
                                slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { -it }) + fadeOut()
                    } else {
                        slideInHorizontally(animationSpec = tween(300), initialOffsetX = { -it }) + fadeIn() togetherWith
                                slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { it }) + fadeOut()
                    }
                },
                modifier = Modifier.fillMaxSize(),
                label = "AddSubSlideTransition"
            ) { step ->
                when (step) {
                    1 -> {
                        // SLIDE 1: SELECT COMPANY (Searchable Popular Catalog)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF8FAFC))
                                .padding(horizontal = 18.dp)
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            // Search Outlined Bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(stringResource(R.string.search_subscriptions_hint), color = TextLight, fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextLight) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.search_clear), tint = TextLight)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = PinkPrimary,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) stringResource(R.string.search_popular) else stringResource(R.string.search_results_short),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val filteredServices = catalogServices.filter {
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                        it.domain.contains(searchQuery, ignoreCase = true)
                            }

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(filteredServices, key = { it.name + it.domain }) { service ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                name = service.name
                                                domain = service.domain
                                                amountText = service.defaultAmountEgp.toInt().toString()
                                                selectedCategory = service.defaultCategory
                                                currentSlideStep = 3 // Skip custom provider if chosen from catalog
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        shadowElevation = 1.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                modifier = Modifier.size(38.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFFF1F5F9)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(LocalContext.current)
                                                            .data(buildLogoDevUrl(service.domain))
                                                            .crossfade(true)
                                                            .build(),
                                                        contentDescription = service.name,
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Text(
                                                text = service.name,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = NavyDark,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = TextLight,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                customProviderName = ""
                                                customProviderDomain = ""
                                                currentSlideStep = 2
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                modifier = Modifier.size(38.dp),
                                                shape = CircleShape,
                                                color = PinkPrimary.copy(alpha = 0.12f)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Add, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(22.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Text(
                                                text = "Add custom subscription",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PinkPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                Icons.Default.ChevronRight,
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
                    2 -> {
                        // SLIDE 2: CUSTOM PROVIDER DETAILS (Name & Domain with logo.dev fetch)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF8FAFC))
                                .padding(horizontal = 20.dp)
                        ) {
                            Spacer(modifier = Modifier.height(24.dp))

                            // Large Logo Preview Card
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(2f),
                                shape = RoundedCornerShape(24.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shadowElevation = 2.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Surface(
                                            modifier = Modifier.size(80.dp),
                                            shape = RoundedCornerShape(20.dp),
                                            color = Color(0xFFF1F5F9)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (customProviderDomain.isNotBlank()) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(LocalContext.current)
                                                            .data(buildLogoDevUrl(customProviderDomain))
                                                            .crossfade(true)
                                                            .build(),
                                                        contentDescription = "Logo Preview",
                                                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp))
                                                    )
                                                } else {
                                                    Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(40.dp))
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = if (customProviderName.isNotBlank()) customProviderName else stringResource(R.string.custom_provider_logo_preview),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NavyDark
                                        )
                                        if (customProviderDomain.isNotBlank()) {
                                            Text(
                                                text = stringResource(R.string.custom_provider_logo_ready),
                                                fontSize = 12.sp,
                                                color = Color(0xFF10B981),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Input Fields
                            Text(stringResource(R.string.custom_provider_name_label), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextLight, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                            OutlinedTextField(
                                value = customProviderName,
                                onValueChange = { customProviderName = it },
                                placeholder = { Text(stringResource(R.string.custom_provider_name_placeholder), color = TextLight.copy(alpha = 0.6f)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = PinkPrimary,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(stringResource(R.string.custom_provider_domain_label), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextLight, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                            OutlinedTextField(
                                value = customProviderDomain,
                                onValueChange = { customProviderDomain = it },
                                placeholder = { Text(stringResource(R.string.custom_provider_domain_placeholder), color = TextLight.copy(alpha = 0.6f)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = PinkPrimary,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                )
                            )

                            Text(
                                text = stringResource(R.string.custom_provider_domain_hint),
                                fontSize = 11.sp,
                                color = TextLight,
                                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.custom_provider_quick_suggestions), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextLight, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf(".com", ".ai", ".eg", ".net", ".org", ".co")) { ext ->
                                    Surface(
                                        onClick = {
                                            if (customProviderDomain.isNotBlank() && !customProviderDomain.contains(".")) {
                                                customProviderDomain += ext
                                            } else if (!customProviderDomain.endsWith(ext)) {
                                                val base = if (customProviderDomain.contains(".")) customProviderDomain.substringBeforeLast(".") else customProviderDomain
                                                customProviderDomain = base + ext
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Text(ext, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyDark, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = {
                                    if (customProviderName.isNotBlank()) {
                                        name = customProviderName.trim()
                                        domain = customProviderDomain.trim()
                                        currentSlideStep = 3
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                                enabled = customProviderName.isNotBlank()
                            ) {
                                Text(stringResource(R.string.custom_provider_continue_btn), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                    3 -> {
                        // SLIDE 3: NEW / EDIT SUBSCRIPTION FORM (5 CARDS LIGHT THEME)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF8FAFC))
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
                        ) {
                            // CARD 1: BASIC INFO CARD
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shadowElevation = 1.dp
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        // Row 1: Service
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(stringResource(R.string.service_name), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NavyDark, modifier = Modifier.weight(1f))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable { currentSlideStep = 1 }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = name.ifBlank { "Custom" },
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = NavyDark
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    modifier = Modifier.size(32.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFF1F5F9)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        if (domain.isNotBlank()) {
                                                            AsyncImage(
                                                                model = ImageRequest.Builder(LocalContext.current)
                                                                    .data(buildLogoDevUrl(domain))
                                                                    .crossfade(true)
                                                                    .build(),
                                                                contentDescription = name,
                                                                modifier = Modifier
                                                                    .size(24.dp)
                                                                    .clip(RoundedCornerShape(6.dp))
                                                            )
                                                        } else {
                                                            Icon(
                                                                Icons.Default.Subscriptions,
                                                                contentDescription = null,
                                                                tint = PinkPrimary,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                                        // Row 2: Price
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(stringResource(R.string.amount), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NavyDark, modifier = Modifier.weight(1f))

                                            BasicTextField(
                                                value = amountText,
                                                onValueChange = { amountText = it },
                                                textStyle = TextStyle(
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NavyDark,
                                                    textAlign = TextAlign.End
                                                ),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                modifier = Modifier
                                                    .width(100.dp)
                                                    .padding(end = 12.dp)
                                            )

                                            // Currency Pill Badge
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable { showCurrencyPicker = true },
                                                shape = RoundedCornerShape(50),
                                                color = PinkPrimary
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val symbol = subAvailableCurrencies.find { it.code == currency }?.symbol ?: currency
                                                    Text(
                                                        text = "$currency ($symbol)",
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                                        // Row 3: Billing Cycle
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(stringResource(R.string.sub_billing_cycle), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NavyDark, modifier = Modifier.weight(1f))

                                            val cycleLabel = when (billingCycle.uppercase()) {
                                                "YEARLY" -> stringResource(R.string.sub_cycle_yearly_label)
                                                "WEEKLY" -> stringResource(R.string.sub_cycle_weekly_label)
                                                "QUARTERLY" -> stringResource(R.string.sub_cycle_quarterly_label)
                                                else -> stringResource(R.string.sub_cycle_monthly_label)
                                            }
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable { showCyclePicker = true },
                                                shape = RoundedCornerShape(50),
                                                color = PinkPrimary
                                            ) {
                                                Text(
                                                    text = cycleLabel,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                                )
                                            }
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                                        // Row 4: Start Date
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(stringResource(R.string.sub_start_date), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NavyDark, modifier = Modifier.weight(1f))

                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable {
                                                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = startDateMs }
                                                        android.app.DatePickerDialog(
                                                            context,
                                                            { _, year, month, dayOfMonth ->
                                                                cal.set(year, month, dayOfMonth)
                                                                startDateMs = cal.timeInMillis
                                                            },
                                                            cal.get(java.util.Calendar.YEAR),
                                                            cal.get(java.util.Calendar.MONTH),
                                                            cal.get(java.util.Calendar.DAY_OF_MONTH)
                                                        ).show()
                                                    },
                                                shape = RoundedCornerShape(50),
                                                color = PinkPrimary
                                            ) {
                                                Text(
                                                    text = dateFmt.format(java.util.Date(startDateMs)),
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                                )
                                            }
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                                        // Row 5: End Date
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(stringResource(R.string.sub_end_date), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NavyDark)

                                            Spacer(modifier = Modifier.weight(1f))

                                            if (endDateMs != null) {
                                                Text(
                                                    text = "Reset",
                                                    color = PinkPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .clickable { endDateMs = null }
                                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                                )
                                            }

                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable {
                                                        val cal = java.util.Calendar.getInstance().apply {
                                                            timeInMillis = endDateMs ?: (startDateMs + 30L * 24 * 3600 * 1000)
                                                        }
                                                        android.app.DatePickerDialog(
                                                            context,
                                                            { _, year, month, dayOfMonth ->
                                                                cal.set(year, month, dayOfMonth)
                                                                endDateMs = cal.timeInMillis
                                                            },
                                                            cal.get(java.util.Calendar.YEAR),
                                                            cal.get(java.util.Calendar.MONTH),
                                                            cal.get(java.util.Calendar.DAY_OF_MONTH)
                                                        ).show()
                                                    },
                                                shape = RoundedCornerShape(50),
                                                color = if (endDateMs != null) PinkPrimary else Color(0xFFE2E8F0)
                                            ) {
                                                Text(
                                                    text = if (endDateMs != null) dateFmt.format(java.util.Date(endDateMs!!)) else "None",
                                                    color = if (endDateMs != null) Color.White else TextLight,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // CARD 2: TRIAL CARD
                            item {
                                Column {
                                    Text(
                                        text = stringResource(R.string.sub_free_trial),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                    )

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        shadowElevation = 1.dp
                                    ) {
                                        @Suppress("UNUSED_VARIABLE")
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(stringResource(R.string.sub_has_trial), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NavyDark)
                                                Switch(
                                                    checked = hasFreeTrial,
                                                    onCheckedChange = { hasFreeTrial = it },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = PinkPrimary
                                                    )
                                                )
                                            }

                                            if (hasFreeTrial) {
                                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(stringResource(R.string.sub_trial_type), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NavyDark, modifier = Modifier.weight(1f))
                                                    Surface(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(50))
                                                            .clickable { trialType = if (trialType == "Free") "Paid" else "Free" },
                                                        shape = RoundedCornerShape(50),
                                                        color = PinkPrimary
                                                    ) {
                                                        Text(
                                                            text = if (trialType == "Free") stringResource(R.string.sub_trial_free) else trialType,
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }

                                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(stringResource(R.string.sub_trial_period), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NavyDark, modifier = Modifier.weight(1f))
                                                    Surface(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(50))
                                                            .clickable { showTrialPeriodPicker = true },
                                                        shape = RoundedCornerShape(50),
                                                        color = PinkPrimary
                                                    ) {
                                                        val periodDisplay = when(trialPeriod) {
                                                            "1 Week" -> stringResource(R.string.sub_period_1_week)
                                                            "1 Month" -> stringResource(R.string.sub_period_1_month)
                                                            "3 Months" -> stringResource(R.string.sub_period_3_months)
                                                            else -> trialPeriod
                                                        }
                                                        Text(
                                                            text = periodDisplay,
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(14.dp))

                                                val renewalCal = java.util.Calendar.getInstance().apply {
                                                    timeInMillis = startDateMs + (trialDays.toLong() * 24 * 3600 * 1000)
                                                }
                                                Text(
                                                    text = stringResource(R.string.sub_first_charge_on, renewalFmt.format(renewalCal.time)),
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF64748B),
                                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // CARD 3: MORE DETAILS CARD (EXCLUDING SPACE)
                            item {
                                Column {
                                    Text(
                                        text = stringResource(R.string.general_info),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                    )

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        shadowElevation = 1.dp
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            // Category Row
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(stringResource(R.string.sub_category), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NavyDark, modifier = Modifier.weight(1f))
                                                Surface(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50))
                                                        .clickable { showCategoryPicker = true },
                                                    shape = RoundedCornerShape(50),
                                                    color = PinkPrimary
                                                ) {
                                                    val catLabel = when (selectedCategory.lowercase().replace("cat_", "")) {
                                                        "entertainment" -> stringResource(R.string.cat_entertainment)
                                                        "shopping" -> stringResource(R.string.cat_shopping)
                                                        "work", "business" -> stringResource(R.string.cat_business)
                                                        "education" -> stringResource(R.string.cat_education)
                                                        "health" -> stringResource(R.string.cat_health)
                                                        "music" -> stringResource(R.string.cat_music)
                                                        else -> stringResource(R.string.cat_subscriptions)
                                                    }
                                                    Text(
                                                        text = catLabel,
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }

                                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                                            // Payment Method Row
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(stringResource(R.string.sub_payment_methods), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NavyDark, modifier = Modifier.weight(1f))
                                                val selectedAccount = accounts.find { it.id == selectedAccountId }
                                                Surface(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50))
                                                        .clickable { showAccountPicker = true },
                                                    shape = RoundedCornerShape(50),
                                                    color = PinkPrimary
                                                ) {
                                                    Text(
                                                        text = selectedAccount?.name ?: stringResource(R.string.sub_none),
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }

                                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                                            // Note Field
                                            Text(stringResource(R.string.payment_note), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NavyDark)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            BasicTextField(
                                                value = noteText,
                                                onValueChange = { noteText = it },
                                                textStyle = TextStyle(fontSize = 14.sp, color = NavyDark),
                                                decorationBox = { innerTextField ->
                                                    Box {
                                                        if (noteText.isEmpty()) {
                                                            Text(stringResource(R.string.add_note_placeholder), color = TextLight, fontSize = 14.sp)
                                                        }
                                                        innerTextField()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // CARD 4: NOTIFICATIONS CARD
                            item {
                                Column {
                                    Text(
                                        text = stringResource(R.string.sub_reminders_section),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                    )

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
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
                                                Text(stringResource(R.string.sub_renewal_reminder), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NavyDark)
                                                Switch(
                                                    checked = renewalReminderEnabled,
                                                    onCheckedChange = { renewalReminderEnabled = it },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = PinkPrimary
                                                    )
                                                )
                                            }

                                            if (renewalReminderEnabled) {
                                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(stringResource(R.string.sub_first_reminder), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NavyDark, modifier = Modifier.weight(1f))
                                                    Surface(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(50))
                                                            .clickable { activeReminderTarget = "first" },
                                                        shape = RoundedCornerShape(50),
                                                        color = PinkPrimary
                                                    ) {
                                                        Text(
                                                            text = mapReminderToDisplay(firstReminder),
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }

                                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(stringResource(R.string.sub_second_reminder), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NavyDark, modifier = Modifier.weight(1f))
                                                    Surface(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(50))
                                                            .clickable { activeReminderTarget = "second" },
                                                        shape = RoundedCornerShape(50),
                                                        color = PinkPrimary
                                                    ) {
                                                        Text(
                                                            text = mapReminderToDisplay(secondReminder),
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }

                                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(stringResource(R.string.sub_third_reminder), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NavyDark, modifier = Modifier.weight(1f))
                                                    Surface(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(50))
                                                            .clickable { activeReminderTarget = "third" },
                                                        shape = RoundedCornerShape(50),
                                                        color = PinkPrimary
                                                    ) {
                                                        Text(
                                                            text = mapReminderToDisplay(thirdReminder),
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // CARD 5: TRIAL ENDING REMINDER CARD (Only if Trial Enabled)
                            if (hasFreeTrial) {
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
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
                                                Text(stringResource(R.string.sub_trial_ending_reminder), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NavyDark)
                                                Switch(
                                                    checked = trialReminderEnabled,
                                                    onCheckedChange = { trialReminderEnabled = it },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = PinkPrimary
                                                    )
                                                )
                                            }

                                            if (trialReminderEnabled) {
                                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(stringResource(R.string.sub_first_reminder), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NavyDark, modifier = Modifier.weight(1f))
                                                    Surface(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(50))
                                                            .clickable { activeReminderTarget = "trialFirst" },
                                                        shape = RoundedCornerShape(50),
                                                        color = PinkPrimary
                                                    ) {
                                                        Text(
                                                            text = mapReminderToDisplay(trialFirstReminder),
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }

                                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(stringResource(R.string.sub_second_reminder), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NavyDark, modifier = Modifier.weight(1f))
                                                    Surface(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(50))
                                                            .clickable { activeReminderTarget = "trialSecond" },
                                                        shape = RoundedCornerShape(50),
                                                        color = PinkPrimary
                                                    ) {
                                                        Text(
                                                            text = mapReminderToDisplay(trialSecondReminder),
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }

                                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(stringResource(R.string.sub_third_reminder), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NavyDark, modifier = Modifier.weight(1f))
                                                    Surface(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(50))
                                                            .clickable { activeReminderTarget = "trialThird" },
                                                        shape = RoundedCornerShape(50),
                                                        color = PinkPrimary
                                                    ) {
                                                        Text(
                                                            text = mapReminderToDisplay(trialThirdReminder),
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
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
                }
            }
        }
    }

    // MODAL BOTTOM SHEETS FOR PICKERS
    if (showCategoryPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCategoryPicker = false },
            containerColor = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp).padding(bottom = 24.dp)) {
                Text(stringResource(R.string.select_category), fontSize = 18.sp, fontWeight = FontWeight.Black, color = NavyDark)
                Spacer(modifier = Modifier.height(18.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(listOf("entertainment", "music", "work", "utilities", "education", "shopping", "games", "health", "other")) { cat ->
                        val categoryResId = when(cat) {
                            "entertainment" -> R.string.cat_entertainment
                            "music" -> R.string.cat_music
                            "work" -> R.string.cat_business
                            "education" -> R.string.cat_education
                            "shopping" -> R.string.cat_shopping
                            "health" -> R.string.cat_health
                            else -> R.string.cat_subscriptions
                        }
                        BottomSheetOptionCard(
                            title = stringResource(categoryResId),
                            isSelected = selectedCategory.equals(cat, ignoreCase = true),
                            leadingContent = {
                                Icon(
                                    imageVector = when(cat) {
                                        "entertainment" -> Icons.Default.Movie
                                        "music" -> Icons.Default.MusicNote
                                        "work" -> Icons.Default.Work
                                        "utilities" -> Icons.Default.Bolt
                                        "education" -> Icons.Default.School
                                        "shopping" -> Icons.Default.ShoppingBag
                                        "games" -> Icons.Default.SportsEsports
                                        "health" -> Icons.Default.Favorite
                                        else -> Icons.Default.Category
                                    },
                                    contentDescription = null,
                                    tint = if (selectedCategory.equals(cat, ignoreCase = true)) PinkPrimary else NavyDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                selectedCategory = cat
                                showCategoryPicker = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAccountPicker) {
        ModalBottomSheet(
            onDismissRequest = { showAccountPicker = false },
            containerColor = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp).padding(bottom = 24.dp)) {
                Text(stringResource(R.string.select_payment_account), fontSize = 18.sp, fontWeight = FontWeight.Black, color = NavyDark)
                Spacer(modifier = Modifier.height(18.dp))
                if (accounts.isEmpty()) {
                    Text(stringResource(R.string.no_accounts_added), color = TextLight, fontSize = 14.sp)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(accounts) { acc ->
                            BottomSheetOptionCard(
                                title = acc.name,
                                subtitle = "${String.format(java.util.Locale.getDefault(), "%,.2f", acc.current_balance)} ${acc.currency}",
                                isSelected = selectedAccountId == acc.id,
                                leadingContent = {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(20.dp))
                                },
                                onClick = {
                                    selectedAccountId = acc.id
                                    showAccountPicker = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCurrencyPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCurrencyPicker = false },
            containerColor = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp).padding(bottom = 24.dp)) {
                Text(stringResource(R.string.select_currency), fontSize = 18.sp, fontWeight = FontWeight.Black, color = NavyDark)
                Spacer(modifier = Modifier.height(18.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(subAvailableCurrencies) { opt ->
                        BottomSheetOptionCard(
                            title = "${opt.code} (${opt.symbol})",
                            subtitle = if (Locale.getDefault().language == "ar") opt.nameAr else opt.nameEn,
                            isSelected = currency == opt.code,
                            leadingContent = {
                                AsyncImage(
                                    model = opt.flagUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp).clip(RoundedCornerShape(4.dp))
                                )
                            },
                            onClick = {
                                currency = opt.code
                                showCurrencyPicker = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCyclePicker) {
        ModalBottomSheet(
            onDismissRequest = { showCyclePicker = false },
            containerColor = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp).padding(bottom = 24.dp)) {
                Text(stringResource(R.string.select_billing_cycle), fontSize = 18.sp, fontWeight = FontWeight.Black, color = NavyDark)
                Spacer(modifier = Modifier.height(18.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val cycles = listOf(
                        Triple("MONTHLY", R.string.sub_cycle_monthly_label, Icons.Default.CalendarMonth),
                        Triple("YEARLY", R.string.sub_cycle_yearly_label, Icons.Default.Event),
                        Triple("WEEKLY", R.string.sub_cycle_weekly_label, Icons.Default.ViewWeek),
                        Triple("QUARTERLY", R.string.sub_cycle_quarterly_label, Icons.Default.Layers)
                    )
                    items(cycles) { (code, resId, icon) ->
                        BottomSheetOptionCard(
                            title = stringResource(resId),
                            isSelected = billingCycle == code,
                            leadingContent = {
                                Icon(icon, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                billingCycle = code
                                showCyclePicker = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (showTrialPeriodPicker) {
        ModalBottomSheet(
            onDismissRequest = { showTrialPeriodPicker = false },
            containerColor = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp).padding(bottom = 24.dp)) {
                Text(stringResource(R.string.select_trial_period), fontSize = 18.sp, fontWeight = FontWeight.Black, color = NavyDark)
                Spacer(modifier = Modifier.height(18.dp))
                val trialOptions = listOf(
                    Triple("1 Week", 7, Icons.Default.Timer),
                    Triple("2 Weeks", 14, Icons.Default.Timer),
                    Triple("1 Month", 30, Icons.Default.Update),
                    Triple("Custom", 14, Icons.Default.Edit)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(trialOptions) { (label, days, icon) ->
                        val displayLabel = when(label) {
                            "1 Week" -> stringResource(R.string.sub_period_1_week)
                            "1 Month" -> stringResource(R.string.sub_period_1_month)
                            "3 Months" -> stringResource(R.string.sub_period_3_months)
                            "Custom" -> stringResource(R.string.custom_category)
                            else -> label
                        }
                        BottomSheetOptionCard(
                            title = displayLabel,
                            isSelected = trialPeriod == label,
                            leadingContent = {
                                Icon(icon, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                trialPeriod = label
                                trialDays = days
                                showTrialPeriodPicker = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (activeReminderTarget != null) {
        ModalBottomSheet(
            onDismissRequest = { activeReminderTarget = null },
            containerColor = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp).padding(bottom = 24.dp)) {
                Text(stringResource(R.string.select_reminder_time), fontSize = 18.sp, fontWeight = FontWeight.Black, color = NavyDark)
                Spacer(modifier = Modifier.height(18.dp))
                val reminderOpts = listOf("1 day before", "2 days before", "3 days before", "1 week before", "On renewal day", "None")
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(reminderOpts) { opt ->
                        val isSelected = when (activeReminderTarget) {
                            "first" -> firstReminder == opt
                            "second" -> secondReminder == opt
                            "third" -> thirdReminder == opt
                            "trialFirst" -> trialFirstReminder == opt
                            "trialSecond" -> trialSecondReminder == opt
                            "trialThird" -> trialThirdReminder == opt
                            else -> false
                        }
                        BottomSheetOptionCard(
                            title = opt,
                            isSelected = isSelected,
                            leadingContent = {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = if (isSelected) PinkPrimary else TextLight, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                when (activeReminderTarget) {
                                    "first" -> firstReminder = opt
                                    "second" -> secondReminder = opt
                                    "third" -> thirdReminder = opt
                                    "trialFirst" -> trialFirstReminder = opt
                                    "trialSecond" -> trialSecondReminder = opt
                                    "trialThird" -> trialThirdReminder = opt
                                }
                                activeReminderTarget = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayNowConfirmationBottomSheet(
    subscription: SubscriptionEntity,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirmPay: (accountId: String) -> Unit
) {
    var chosenAccountId by remember {
        mutableStateOf(
            if (subscription.accountId.isNotBlank()) subscription.accountId
            else accounts.firstOrNull()?.id ?: ""
        )
    }

    val chosenAccount = accounts.find { it.id == chosenAccountId }
    val prospectiveBalance = (chosenAccount?.current_balance ?: 0.0) - subscription.amount
    val isInsufficient = prospectiveBalance < 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                if (!subscription.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
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

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = subscription.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${String.format(Locale.getDefault(), "%,.2f", subscription.amount)} ${subscription.currency}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = PinkPrimary
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.sub_paying_from),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextLight
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (accounts.isNotEmpty()) {
                        accounts.forEach { acc ->
                            val isSelected = acc.id == chosenAccountId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { chosenAccountId = acc.id }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { chosenAccountId = acc.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = PinkPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = acc.name,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark
                                    )
                                    Text(
                                        text = "Current: ${String.format(Locale.getDefault(), "%,.2f", acc.current_balance)} ${acc.currency}",
                                        fontSize = 11.5.sp,
                                        color = TextLight
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            stringResource(R.string.sub_no_wallet_linked),
                            color = Color(0xFFDC2626),
                            fontSize = 13.sp
                        )
                    }

                    if (chosenAccount != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(R.string.sub_balance_after),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NavyDark
                            )
                            Text(
                                text = "${String.format(Locale.getDefault(), "%,.2f", prospectiveBalance)} ${chosenAccount.currency}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isInsufficient) Color(0xFFDC2626) else Color(0xFF059669)
                            )
                        }
                        if (isInsufficient) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.sub_insufficient_balance),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { onConfirmPay(chosenAccountId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sub_confirm_pay_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                enabled = chosenAccountId.isNotBlank()
            ) {
                Text(
                    text = stringResource(R.string.sub_confirm_payment),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

private class BubbleBody(
    val subscription: SubscriptionEntity,
    val radiusPx: Float,
    initX: Float,
    initY: Float,
    var vx: Float,
    var vy: Float,
    val isUrgent: Boolean
) {
    var x by mutableFloatStateOf(initX)
    var y by mutableFloatStateOf(initY)
}

@Composable
private fun PhysicsBubbleDashboard(
    subscriptions: List<SubscriptionEntity>,
    onSubscriptionClick: (SubscriptionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var containerHeightPx by remember { mutableFloatStateOf(0f) }

    val now = remember { System.currentTimeMillis() }
    val threeDaysMillis = remember { TimeUnit.DAYS.toMillis(3) }

    val minRadiusPx = remember(density) { with(density) { 42.dp.toPx() } }
    val maxRadiusPx = remember(density) { with(density) { 100.dp.toPx() } }

    var draggedBubbleId by remember { mutableStateOf<String?>(null) }
    var topBubbleId by remember { mutableStateOf<String?>(null) }

    val subIdsKey = remember(subscriptions) { subscriptions.map { it.id }.sorted().joinToString(",") }
    val hasDimensions = containerWidthPx > 0f && containerHeightPx > 0f

    val bubbles = remember(subIdsKey, hasDimensions) {
        if (!hasDimensions || subscriptions.isEmpty()) {
            mutableStateListOf<BubbleBody>()
        } else {
            val minCost = subscriptions.minOfOrNull { it.amount } ?: 1.0
            val maxCost = subscriptions.maxOfOrNull { it.amount } ?: 100.0
            val costRange = (maxCost - minCost).coerceAtLeast(1.0)

            val centerX = containerWidthPx / 2f
            val centerY = containerHeightPx / 2f

            val list = mutableStateListOf<BubbleBody>()
            val count = subscriptions.size

            subscriptions.forEachIndexed { index, sub ->
                val normCost = ((sub.amount - minCost) / costRange).coerceIn(0.0, 1.0).toFloat()
                val radius = minRadiusPx + (normCost * (maxRadiusPx - minRadiusPx))

                val angle = index * (2f * Math.PI / count.coerceAtLeast(1)).toFloat() + (Random.nextFloat() * 0.2f)
                val dist = (index * 22f + 30f).coerceAtMost(containerWidthPx.coerceAtMost(containerHeightPx) * 0.35f)

                val initX = (centerX + Math.cos(angle.toDouble()).toFloat() * dist).coerceIn(radius, containerWidthPx - radius)
                val initY = (centerY + Math.sin(angle.toDouble()).toFloat() * dist).coerceIn(radius, containerHeightPx - radius)

                val randomAngle = (Random.nextFloat() * 2f * Math.PI).toFloat()
                val initialSpeed = 50f + Random.nextFloat() * 45f
                val initVx = Math.cos(randomAngle.toDouble()).toFloat() * initialSpeed
                val initVy = Math.sin(randomAngle.toDouble()).toFloat() * initialSpeed

                val isUrgent = sub.isActive && (sub.nextRenewalDate - now) <= threeDaysMillis

                list.add(
                    BubbleBody(
                        subscription = sub,
                        radiusPx = radius,
                        initX = initX,
                        initY = initY,
                        vx = initVx,
                        vy = initVy,
                        isUrgent = isUrgent
                    )
                )
            }
            list
        }
    }

    LaunchedEffect(subIdsKey, hasDimensions) {
        if (!hasDimensions || bubbles.isEmpty()) return@LaunchedEffect
        var lastNanos = System.nanoTime()

        while (isActive) {
            withFrameNanos { currentNanos ->
                val dt = ((currentNanos - lastNanos) / 1_000_000_000f).coerceIn(0.008f, 0.033f)
                lastNanos = currentNanos

                val restitution = 0.90f
                val width = containerWidthPx
                val height = containerHeightPx
                val minSpeed = 50f
                val maxSpeed = 160f

                // 1. Continuous Position Update & Wall Collision
                for (b in bubbles) {
                    if (b.subscription.id == draggedBubbleId) continue

                    b.x += b.vx * dt
                    b.y += b.vy * dt

                    // Strict Wall Collision with clamping (Zero-sticking)
                    if (b.x - b.radiusPx < 0f) {
                        b.x = b.radiusPx
                        b.vx = Math.abs(b.vx)
                    } else if (b.x + b.radiusPx > width) {
                        b.x = width - b.radiusPx
                        b.vx = -Math.abs(b.vx)
                    }

                    if (b.y - b.radiusPx < 0f) {
                        b.y = b.radiusPx
                        b.vy = Math.abs(b.vy)
                    } else if (b.y + b.radiusPx > height) {
                        b.y = height - b.radiusPx
                        b.vy = -Math.abs(b.vy)
                    }

                    // Autonomous Drift Speed Floor (Windows Screensaver style - constant speed)
                    val speed = sqrt(b.vx * b.vx + b.vy * b.vy)
                    val targetSpeed = (minSpeed + maxSpeed) / 2f
                    if (speed > 0.001f) {
                        val scale = targetSpeed / speed
                        b.vx *= scale
                        b.vy *= scale
                    } else {
                        val angle = (Random.nextFloat() * 2f * Math.PI).toFloat()
                        b.vx = Math.cos(angle.toDouble()).toFloat() * targetSpeed
                        b.vy = Math.sin(angle.toDouble()).toFloat() * targetSpeed
                    }
                }

                // 2. Rigid-Body Bubble-to-Bubble Collision (Step A: Positional Correction + Step B: Velocity Bounce)
                val size = bubbles.size
                for (i in 0 until size) {
                    val b1 = bubbles[i]
                    for (j in i + 1 until size) {
                        val b2 = bubbles[j]

                        val dx = b2.x - b1.x
                        val dy = b2.y - b1.y
                        val distSq = dx * dx + dy * dy
                        val minDist = b1.radiusPx + b2.radiusPx

                        if (distSq < minDist * minDist && distSq > 0.0001f) {
                            val dist = sqrt(distSq)
                            val overlap = minDist - dist
                            val nx = dx / dist
                            val ny = dy / dist

                            val is1Dragged = b1.subscription.id == draggedBubbleId
                            val is2Dragged = b2.subscription.id == draggedBubbleId

                            // Step A: Positional Correction (Guarantees no visual overlapping)
                            val w1 = if (is1Dragged) 0f else if (is2Dragged) 1f else 0.5f
                            val w2 = if (is2Dragged) 0f else if (is1Dragged) 1f else 0.5f

                            if (!is1Dragged) {
                                b1.x -= nx * overlap * w1
                                b1.y -= ny * overlap * w1
                            }
                            if (!is2Dragged) {
                                b2.x += nx * overlap * w2
                                b2.y += ny * overlap * w2
                            }

                            // Step B: Elastic Velocity Bounce (Strict reflection)
                            val rvx = b2.vx - b1.vx
                            val rvy = b2.vy - b1.vy
                            val velAlongNormal = rvx * nx + rvy * ny

                            if (velAlongNormal < 0f) {
                                // Perfectly elastic collision normal reflection
                                val impulse = -(2f) * velAlongNormal / (1f/b1.radiusPx + 1f/b2.radiusPx)
                                
                                if (!is1Dragged) {
                                    b1.vx -= nx * (impulse / b1.radiusPx)
                                    b1.vy -= ny * (impulse / b1.radiusPx)
                                }
                                if (!is2Dragged) {
                                    b2.vx += nx * (impulse / b2.radiusPx)
                                    b2.vy += ny * (impulse / b2.radiusPx)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(480.dp)
            .clip(RoundedCornerShape(24.dp))
            .onSizeChanged { size ->
                containerWidthPx = size.width.toFloat()
                containerHeightPx = size.height.toFloat()
            },
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 2.dp
    ) {
        if (subscriptions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BubbleChart,
                            contentDescription = null,
                            tint = TextLight,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.no_subscriptions_yet),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(bubbles) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startPos = down.position
                            val startTime = System.currentTimeMillis()

                            val touchedBubble = bubbles.findLast { b ->
                                val dx = down.position.x - b.x
                                val dy = down.position.y - b.y
                                (dx * dx + dy * dy) <= (b.radiusPx * b.radiusPx)
                            }

                            if (touchedBubble != null) {
                                draggedBubbleId = touchedBubble.subscription.id
                                topBubbleId = touchedBubble.subscription.id

                                var lastPos = startPos
                                var lastTime = startTime
                                var velX = 0f
                                var velY = 0f
                                var totalMoved = 0f

                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (change.pressed) {
                                        val currentPos = change.position
                                        val currentTime = System.currentTimeMillis()
                                        val dtSec = ((currentTime - lastTime).coerceAtLeast(1L)) / 1000f

                                        val dx = currentPos.x - lastPos.x
                                        val dy = currentPos.y - lastPos.y

                                        totalMoved += sqrt(dx * dx + dy * dy)

                                        touchedBubble.x = currentPos.x.coerceIn(touchedBubble.radiusPx, containerWidthPx - touchedBubble.radiusPx)
                                        touchedBubble.y = currentPos.y.coerceIn(touchedBubble.radiusPx, containerHeightPx - touchedBubble.radiusPx)

                                        velX = (dx / dtSec).coerceIn(-3000f, 3000f)
                                        velY = (dy / dtSec).coerceIn(-3000f, 3000f)

                                        lastPos = currentPos
                                        lastTime = currentTime
                                        change.consume()
                                    } else {
                                        break
                                    }
                                } while (true)

                                val totalTime = System.currentTimeMillis() - startTime
                                draggedBubbleId = null

                                if (totalTime < 250 && totalMoved < 14.dp.toPx()) {
                                    onSubscriptionClick(touchedBubble.subscription)
                                } else {
                                    touchedBubble.vx = velX * 0.85f
                                    touchedBubble.vy = velY * 0.85f
                                }
                            }
                        }
                    }
            ) {
                bubbles.forEach { b ->
                    val isTop = b.subscription.id == topBubbleId
                    val isDragged = b.subscription.id == draggedBubbleId

                    val diameterDp = with(density) { (b.radiusPx * 2f).toDp() }
                    val logoSizeDp = with(density) { (b.radiusPx * 0.75f).coerceIn(22.dp.toPx(), 46.dp.toPx()).toDp() }

                    val (bgColor, borderColor, priceColor) = remember(b.subscription, b.isUrgent) {
                        if (b.isUrgent) {
                            Triple(Color(0xFFFFE4E6), Color(0xFFFB7185), Color(0xFFE11D48))
                        } else {
                            // Sleek, modern cool gray bubble background
                            Triple(Color(0xFFF1F5F9), Color(0xFFCBD5E1), NavyDark)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (b.x - b.radiusPx).roundToInt(),
                                    (b.y - b.radiusPx).roundToInt()
                                )
                            }
                            .size(diameterDp)
                            .zIndex(if (isDragged) 10f else if (isTop) 5f else 1f),
                        shape = CircleShape,
                        color = bgColor,
                        border = BorderStroke(if (b.isUrgent) 2.dp else 1.2.dp, borderColor),
                        shadowElevation = 6.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            if (!b.subscription.logoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(b.subscription.logoUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = b.subscription.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(logoSizeDp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(logoSizeDp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = b.subscription.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Black,
                                        color = PinkPrimary,
                                        fontSize = if (b.radiusPx > 70f) 18.sp else 13.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "${String.format(Locale.getDefault(), "%,.2f", b.subscription.amount)} ${b.subscription.currency}",
                                fontSize = if (b.radiusPx > 70f) 12.5.sp else if (b.radiusPx > 50f) 11.sp else 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = priceColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
