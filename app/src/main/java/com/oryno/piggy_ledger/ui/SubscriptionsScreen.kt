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
            }
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
                            contentDescription = "Close Search",
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
                            text = category,
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
                text = if (searchQuery.isBlank() && selectedCategory == null) stringResource(R.string.sub_nav_subscriptions) else "Search Results",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )
            Text(
                text = "${filteredSubscriptions.size} found",
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
                        text = "No subscriptions found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try searching for another name or category",
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

                // Options 3-Dots Button
                IconButton(
                    onClick = onOptionsClick,
                    modifier = Modifier.size(32.dp).testTag("sub_options_btn_${subscription.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = stringResource(R.string.sub_options_title),
                        tint = NavyDark,
                        modifier = Modifier.size(20.dp)
                    )
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

// 2-Slide Screen for Adding / Editing Subscription
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
    var currentSlideStep by remember { mutableIntStateOf(1) }

    var name by remember { mutableStateOf(existingSub?.name ?: catalogItem?.name ?: "") }
    var domain by remember { mutableStateOf(catalogItem?.domain ?: "") }
    var amountText by remember {
        mutableStateOf(
            existingSub?.amount?.toString()
                ?: catalogItem?.defaultAmountEgp?.toInt()?.toString()
                ?: ""
        )
    }
    var currency by remember { mutableStateOf(existingSub?.currency ?: "EGP") }
    var billingCycle by remember { mutableStateOf(existingSub?.billingCycle ?: "MONTHLY") }
    var selectedAccountId by remember {
        mutableStateOf(existingSub?.accountId ?: accounts.firstOrNull()?.id ?: "")
    }
    var selectedCategory by remember {
        mutableStateOf(existingSub?.category ?: catalogItem?.defaultCategory ?: "subscriptions")
    }
    var hasFreeTrial by remember { mutableStateOf(existingSub?.hasFreeTrial ?: false) }
    var trialDays by remember { mutableStateOf(if (existingSub?.trialDays ?: 0 > 0) existingSub!!.trialDays else 7) }

    BackHandler {
        if (currentSlideStep == 2) {
            currentSlideStep = 1
        } else {
            onBackToDashboard()
        }
    }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0

    val sortedAccounts = remember(accounts, currency) {
        accounts.sortedWith(
            compareByDescending<Account> { it.currency.equals(currency, ignoreCase = true) }
                .thenByDescending { it.current_balance }
        )
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (existingSub != null) stringResource(R.string.sub_edit) else stringResource(R.string.add_new_subscription),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        Text(
                            text = if (currentSlideStep == 1) stringResource(R.string.sub_step_1_title) else stringResource(R.string.sub_step_2_title),
                            fontSize = 12.sp,
                            color = PinkPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentSlideStep == 2) currentSlideStep = 1 else onBackToDashboard()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_btn), tint = NavyDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LinearProgressIndicator(
                progress = { if (currentSlideStep == 1) 0.5f else 1.0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = PinkPrimary,
                trackColor = Color(0xFFE2E8F0)
            )

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
                modifier = Modifier.weight(1f),
                label = "SlideTransition"
            ) { step ->
                if (step == 1) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Column {
                                Text(
                                    text = stringResource(R.string.popular_services_catalog),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDark
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(catalogServices, key = { it.domain }) { service ->
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable {
                                                    name = service.name
                                                    domain = service.domain
                                                    amountText = service.defaultAmountEgp.toInt().toString()
                                                    selectedCategory = service.defaultCategory
                                                },
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, if (name == service.name) PinkPrimary else Color(0xFFE2E8F0))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(buildLogoDevUrl(service.domain))
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = service.name,
                                                    modifier = Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(service.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text(stringResource(R.string.service_name)) },
                                modifier = Modifier.fillMaxWidth().testTag("add_sub_name"),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = domain,
                                onValueChange = { domain = it },
                                label = { Text(stringResource(R.string.website_domain_label)) },
                                placeholder = { Text(stringResource(R.string.website_domain_placeholder)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = amountText,
                                    onValueChange = { amountText = it },
                                    label = { Text(stringResource(R.string.subscription_cost_label)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f).testTag("add_sub_amount"),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true
                                )

                                var showCurrencyMenu by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                                    OutlinedButton(
                                        onClick = { showCurrencyMenu = true },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.height(56.dp)
                                    ) {
                                        Text(currency, fontWeight = FontWeight.Bold, color = NavyDark)
                                        Icon(Icons.Default.ArrowDropDown, null, tint = NavyDark)
                                    }
                                    DropdownMenu(
                                        expanded = showCurrencyMenu,
                                        onDismissRequest = { showCurrencyMenu = false }
                                    ) {
                                        listOf("EGP", "USD", "EUR", "GBP", "SAR", "AED").forEach { c ->
                                            DropdownMenuItem(
                                                text = { Text(c, fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    currency = c
                                                    showCurrencyMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Column {
                                Text(
                                    stringResource(R.string.sub_select_category),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NavyDark
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(listOf("subscriptions", "entertainment", "work", "utilities", "education", "shopping", "other")) { cat ->
                                        FilterChip(
                                            selected = selectedCategory == cat,
                                            onClick = { selectedCategory = cat },
                                            label = { Text(cat.replaceFirstChar { it.uppercase() }) }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { currentSlideStep = 2 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("add_sub_next_slide"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                                enabled = name.isNotBlank() && parsedAmount > 0
                            ) {
                                Text(stringResource(R.string.sub_continue_to_step2), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        item {
                            Column {
                                Text(
                                    stringResource(R.string.billing_frequency),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NavyDark
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        "MONTHLY" to stringResource(R.string.billing_cycle_monthly),
                                        "YEARLY" to stringResource(R.string.billing_cycle_yearly),
                                        "WEEKLY" to stringResource(R.string.billing_cycle_weekly)
                                    ).forEach { (code, label) ->
                                        FilterChip(
                                            selected = billingCycle == code,
                                            onClick = { billingCycle = code },
                                            label = { Text(label, fontWeight = FontWeight.Bold) }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Column {
                                Text(
                                    stringResource(R.string.sub_linked_wallet),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NavyDark
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                if (sortedAccounts.isEmpty()) {
                                    Text(
                                        stringResource(R.string.sub_no_wallet_linked),
                                        color = Color(0xFFDC2626),
                                        fontSize = 12.5.sp
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        sortedAccounts.forEach { acc ->
                                            val isSelected = acc.id == selectedAccountId
                                            val isSameCurrency = acc.currency.equals(currency, ignoreCase = true)
                                            val prospective = acc.current_balance - parsedAmount

                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .clickable { selectedAccountId = acc.id },
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (isSelected) PinkPrimary.copy(alpha = 0.08f) else Color.White,
                                                border = BorderStroke(
                                                    if (isSelected) 1.5.dp else 1.dp,
                                                    if (isSelected) PinkPrimary else Color(0xFFE2E8F0)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = isSelected,
                                                        onClick = { selectedAccountId = acc.id },
                                                        colors = RadioButtonDefaults.colors(selectedColor = PinkPrimary)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = acc.name,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 14.sp,
                                                                color = NavyDark
                                                            )
                                                            if (isSameCurrency) {
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Surface(
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    color = Color(0xFFD1FAE5)
                                                                ) {
                                                                    Text(
                                                                        text = stringResource(R.string.sub_no_conversion_fee),
                                                                        fontSize = 9.5.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color(0xFF065F46),
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "Current: ${String.format(Locale.getDefault(), "%,.2f", acc.current_balance)} ${acc.currency}",
                                                            fontSize = 12.sp,
                                                            color = TextLight
                                                        )
                                                        if (isSelected && parsedAmount > 0) {
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "After pay: ${String.format(Locale.getDefault(), "%,.2f", prospective)} ${acc.currency}",
                                                                fontSize = 11.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (prospective >= 0) Color(0xFF059669) else Color(0xFFDC2626)
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

                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(stringResource(R.string.sub_free_trial), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                            Text(stringResource(R.string.sub_trial_active), fontSize = 12.sp, color = TextLight)
                                        }
                                        Switch(
                                            checked = hasFreeTrial,
                                            onCheckedChange = { hasFreeTrial = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = PinkPrimary)
                                        )
                                    }

                                    if (hasFreeTrial) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(7, 14, 30).forEach { days ->
                                                FilterChip(
                                                    selected = trialDays == days,
                                                    onClick = { trialDays = days },
                                                    label = { Text(stringResource(R.string.sub_days_format, days)) }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, trialDays) }
                                        val fmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                        Text(
                                            text = stringResource(R.string.sub_first_charge_on, fmt.format(cal.time)),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF6D28D9)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { currentSlideStep = 1 },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(stringResource(R.string.sub_previous_step), fontWeight = FontWeight.Bold)
                                }

                                Button(
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
                                        }
                                    },
                                    modifier = Modifier.weight(1.4f).height(50.dp).testTag("add_sub_save_final"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                                    enabled = name.isNotBlank() && parsedAmount > 0
                                ) {
                                    Text(stringResource(R.string.save_subscription), fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val isUrgent: Boolean
)

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
                val initialSpeed = 50f + Random.nextFloat() * 50f
                val initVx = Math.cos(randomAngle.toDouble()).toFloat() * initialSpeed
                val initVy = Math.sin(randomAngle.toDouble()).toFloat() * initialSpeed

                val isUrgent = sub.isActive && (sub.nextRenewalDate - now) <= threeDaysMillis

                list.add(
                    BubbleBody(
                        subscription = sub,
                        radiusPx = radius,
                        x = initX,
                        y = initY,
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

                val restitution = 0.88f
                val width = containerWidthPx
                val height = containerHeightPx
                val targetMinSpeed = 40f
                val targetMaxSpeed = 120f

                for (b in bubbles) {
                    if (b.subscription.id == draggedBubbleId) continue

                    // Continuous floating motion
                    b.x += b.vx * dt
                    b.y += b.vy * dt

                    // Wall Collision (Left & Right)
                    if (b.x - b.radiusPx <= 0f) {
                        b.x = b.radiusPx + 0.5f
                        b.vx = Math.abs(b.vx) * restitution
                    } else if (b.x + b.radiusPx >= width) {
                        b.x = width - b.radiusPx - 0.5f
                        b.vx = -Math.abs(b.vx) * restitution
                    }

                    // Wall Collision (Top & Bottom)
                    if (b.y - b.radiusPx <= 0f) {
                        b.y = b.radiusPx + 0.5f
                        b.vy = Math.abs(b.vy) * restitution
                    } else if (b.y + b.radiusPx >= height) {
                        b.y = height - b.radiusPx - 0.5f
                        b.vy = -Math.abs(b.vy) * restitution
                    }

                    // Continuous drift speed floor & damping for high throws
                    val currentSpeed = sqrt(b.vx * b.vx + b.vy * b.vy)
                    if (currentSpeed > targetMaxSpeed) {
                        b.vx *= 0.985f
                        b.vy *= 0.985f
                    } else if (currentSpeed < targetMinSpeed) {
                        if (currentSpeed < 1f) {
                            val newAngle = (Random.nextFloat() * 2f * Math.PI).toFloat()
                            b.vx = Math.cos(newAngle.toDouble()).toFloat() * targetMinSpeed
                            b.vy = Math.sin(newAngle.toDouble()).toFloat() * targetMinSpeed
                        } else {
                            val scale = targetMinSpeed / currentSpeed
                            b.vx *= scale
                            b.vy *= scale
                        }
                    }
                }

                val size = bubbles.size
                for (i in 0 until size) {
                    val b1 = bubbles[i]
                    for (j in i + 1 until size) {
                        val b2 = bubbles[j]

                        val dx = b2.x - b1.x
                        val dy = b2.y - b1.y
                        val distSq = dx * dx + dy * dy
                        val minDist = b1.radiusPx + b2.radiusPx

                        if (distSq < minDist * minDist && distSq > 0.001f) {
                            val dist = sqrt(distSq)
                            val overlap = minDist - dist
                            val nx = dx / dist
                            val ny = dy / dist

                            val is1Dragged = b1.subscription.id == draggedBubbleId
                            val is2Dragged = b2.subscription.id == draggedBubbleId

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

                            val rvx = b2.vx - b1.vx
                            val rvy = b2.vy - b1.vy
                            val velAlongNormal = rvx * nx + rvy * ny

                            if (velAlongNormal < 0) {
                                val m1 = b1.radiusPx
                                val m2 = b2.radiusPx
                                val impulse = -(1f + restitution) * velAlongNormal / (m1 + m2)

                                if (!is1Dragged) {
                                    b1.vx -= impulse * m2 * nx
                                    b1.vy -= impulse * m2 * ny
                                }
                                if (!is2Dragged) {
                                    b2.vx += impulse * m1 * nx
                                    b2.vy += impulse * m1 * ny
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
