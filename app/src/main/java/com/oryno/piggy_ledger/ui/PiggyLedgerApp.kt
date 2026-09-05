package com.oryno.piggy_ledger.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.PiggyLedgerTheme
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.posthog.PostHog
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.widget.Toast

@Composable
fun PiggyLedgerApp(
    factory: ViewModelFactory,
    initialDestination: Screen = Screen.LanguageSelection,
    openNotificationId: String? = null,
    shortcutAction: String? = null,
    onConsumeShortcut: () -> Unit = {}
) {
    val navController = rememberNavController()
    val viewModel: PiggyLedgerViewModel = viewModel(factory = factory)
    
    val isPremium by viewModel.isPremium.collectAsState()
    
    val hasOnboarded by viewModel.hasOnboarded.collectAsState()
    val hasLanguageSelected by viewModel.hasLanguageSelected.collectAsState()
    val hasHeardAboutUs by viewModel.hasHeardAboutUs.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    var showSignOutBottomSheet by remember { mutableStateOf(false) }

    if (showSignOutBottomSheet) {
        SignOutBottomSheet(
            viewModel = viewModel,
            onDismiss = { showSignOutBottomSheet = false },
            onSignOutSuccess = {
                showSignOutBottomSheet = false
                navController.navigate(Screen.Auth) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }

    LaunchedEffect(isAuthenticated, hasLanguageSelected, hasOnboarded) {
        if (isAuthenticated == false && hasLanguageSelected == true && hasOnboarded == true) {
            if (navController.currentDestination?.hasRoute(Screen.Auth::class) != true) {
                navController.navigate(Screen.Auth) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    LaunchedEffect(openNotificationId) {
        if (openNotificationId != null && initialDestination == Screen.MainContainer) {
            navController.navigate(Screen.Notifications)
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val isPreAuthFlow = currentRoute?.contains("LanguageSelection") == true ||
            currentRoute?.contains("HearAboutUs") == true ||
            currentRoute?.contains("Onboarding") == true ||
            currentRoute?.contains("Auth") == true

    if (!isPreAuthFlow && isAuthenticated == true && hasOnboarded == true) {
        PermissionHealthChecker()
    }

    PiggyLedgerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(navController = navController, startDestination = initialDestination) {
                composable<Screen.LanguageSelection> {
                    LaunchedEffect(Unit) {
                        PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Language Selection"))
                    }
                    LanguageSelectionScreen(
                        onLanguageSelected = {
                            viewModel.completeLanguageSelection()
                            navController.navigate(Screen.HearAboutUs) {
                                popUpTo(Screen.LanguageSelection) { inclusive = true }
                            }
                        },
                        onAlreadyHaveAccount = {
                            viewModel.completeLanguageSelection()
                            viewModel.completeHearAboutUs("already_have_account")
                            viewModel.completeOnboarding(1, 1, "Balanced")
                            navController.navigate(Screen.Auth) {
                                popUpTo(Screen.LanguageSelection) { inclusive = true }
                            }
                        }
                    )
                }
                

                composable<Screen.HearAboutUs> {
                    LaunchedEffect(Unit) {
                        PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Hear About Us"))
                    }
                    HearAboutUsScreen(
                        onContinue = { source ->
                            viewModel.completeHearAboutUs(source)
                            navController.navigate(Screen.Onboarding) {
                                popUpTo(Screen.HearAboutUs) { inclusive = true }
                            }
                        }
                    )
                }

                composable<Screen.Onboarding> {
                    LaunchedEffect(Unit) {
                        PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Onboarding"))
                    }
                    OnboardingScreen(
                        onComplete = { intent, intensity, savingMode, relatesLoans, relatesAccounts, relatesEmergency ->
                            viewModel.completeOnboarding(intent, intensity, savingMode, relatesLoans, relatesAccounts, relatesEmergency)
                            navController.navigate(Screen.Auth) {
                                popUpTo(Screen.Onboarding) { inclusive = true }
                            }
                        }
                    )
                }

                composable<Screen.Auth> {
                    LaunchedEffect(Unit) {
                        PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Authentication"))
                    }
                    AuthScreen(
                        viewModel = viewModel,
                        onAuthSuccess = {
                            navController.navigate(Screen.MainContainer) {
                                popUpTo(Screen.Auth) { inclusive = true }
                            }
                        }
                    )
                }
                
                composable<Screen.MainContainer> {
                    MainContainer(
                        viewModel = viewModel,
                        appNavController = navController,
                        shortcutAction = shortcutAction,
                        onConsumeShortcut = onConsumeShortcut,
                        onSignOutClick = { showSignOutBottomSheet = true }
                    )
                }

                // Sub-screens that are not part of the main tabs but need to be accessible
                composable<Screen.CreateGoal> {
                    val goals by viewModel.goals.collectAsState()
                    val isPremium by viewModel.isPremium.collectAsState()
                    val appCurrency by viewModel.appCurrency.collectAsState()
                    val context = LocalContext.current

                    LaunchedEffect(Unit) {
                        PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Create Goal"))
                    }
                    CreateGoalScreen(
                        appCurrency = appCurrency,
                        onGoalCreated = { name, amount ->
                            if (viewModel.canAddGoal(goals.size)) {
                                viewModel.addGoal(name, amount)
                                navController.popBackStack()
                            } else {
                                com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Upgrade to Pro to add more goals", Toast.LENGTH_SHORT)
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.GoalDetail> { backStackEntry ->
                    val screen = backStackEntry.toRoute<Screen.GoalDetail>()
                    LaunchedEffect(screen.goalId) {
                        PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Goal Detail", "goal_id" to screen.goalId))
                    }
                    GoalDetailScreen(
                        goalId = screen.goalId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<Screen.AddAccount> {
                    LaunchedEffect(Unit) {
                        PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Add Account"))
                    }
                    AddAccountScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<Screen.EditAccount> { backStackEntry ->
                    val screen = backStackEntry.toRoute<Screen.EditAccount>()
                    LaunchedEffect(screen.accountId) {
                        PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Edit Account", "account_id" to screen.accountId))
                    }
                    EditAccountScreen(
                        accountId = screen.accountId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<Screen.PendingTransactions> {
                    LaunchedEffect(Unit) {
                        PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Pending Transactions"))
                    }
                    PendingTransactionsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<Screen.StreakAchievements> {
                    LaunchedEffect(Unit) {
                        PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Streak Achievements"))
                    }
                    StreakAchievementsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<Screen.AiChat> {
                    LaunchedEffect(Unit) {
                        PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "AiChat"))
                    }
                    val aiChatViewModel: com.oryno.piggy_ledger.ai.AiChatViewModel = viewModel(factory = factory)
                    AiChatScreen(
                        viewModel = aiChatViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPaywall = {
                            navController.navigate(Screen.Settings(modeName = SettingsMode.PRO.name))
                        }
                    )
                }

                composable<Screen.Notifications> {
                    LaunchedEffect(Unit) {
                        PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Notifications"))
                    }
                    NotificationsScreen(
                        onBack = { navController.popBackStack() },
                        isPremium = isPremium,
                        openNotificationId = openNotificationId
                    )
                }

                composable<Screen.Settings> { backStackEntry ->
                    val screen = backStackEntry.toRoute<Screen.Settings>()
                    val mode = remember(screen.modeName) {
                        try {
                            SettingsMode.valueOf(screen.modeName)
                        } catch (e: Exception) {
                            SettingsMode.MAIN
                        }
                    }
                    LaunchedEffect(mode) {
                        try {
                            PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Settings", "mode" to mode.name))
                        } catch (e: Exception) {
                            android.util.Log.e("PostHog", "Failed to capture screen view", e)
                        }
                    }
                    SettingsScreen(
                        viewModel = viewModel,
                        initialMode = mode,
                        onNavigateToPendingTransactions = { navController.navigate(Screen.PendingTransactions) },
                        onBackClick = { navController.popBackStack() },
                        onSignOutClick = { showSignOutBottomSheet = true }
                    )
                }
            }
        }
    }
}

@Composable
fun MainContainer(
    viewModel: PiggyLedgerViewModel,
    appNavController: NavHostController,
    shortcutAction: String? = null,
    onConsumeShortcut: () -> Unit = {},
    onSignOutClick: () -> Unit = {}
) {
    val bottomNavController = rememberNavController()
    val overdueLoans by viewModel.overdueLoans.collectAsState()
    val allAccounts by viewModel.allAccounts.collectAsState()
    var dismissedAlerts by remember { mutableStateOf(setOf<String>()) }
    var showAddTransactionSheet by remember { mutableStateOf(false) }

    LaunchedEffect(shortcutAction) {
        if (shortcutAction != null) {
            when (shortcutAction) {
                "add_transaction" -> {
                    bottomNavController.navigate(Screen.Dashboard) {
                        popUpTo(bottomNavController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                    showAddTransactionSheet = true
                    onConsumeShortcut()
                }
                "savings_goals" -> {
                    bottomNavController.navigate(Screen.MyGoals) {
                        popUpTo(bottomNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    onConsumeShortcut()
                }
                "analytics" -> {
                    bottomNavController.navigate(Screen.Analytics) {
                        popUpTo(bottomNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    onConsumeShortcut()
                }
            }
        }
    }
    
    val activeOverdue = overdueLoans.filter { it.id !in dismissedAlerts }

    var isDrawerOpen by remember { mutableStateOf(false) }
    val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    val drawerProgress by animateFloatAsState(
        targetValue = if (isDrawerOpen) 1f else 0f,
        animationSpec = tween(
            durationMillis = 220,
            easing = FastOutSlowInEasing
        ),
        label = "DrawerAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NavyDark,
                        Color(0xFF1E1B4B)
                    )
                )
            )
    ) {
        // Main Content Card Layer (Layered at bottom of Box, shifted right)
        val density = LocalDensity.current
        val translationXPx = with(density) { (drawerProgress * 265.dp.value).dp.toPx() }

        var totalDrag by remember { mutableStateOf(0f) }

        Card(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isDrawerOpen) {
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragEnd = {
                            if (isRtl) {
                                if (totalDrag < -100f && !isDrawerOpen) {
                                    isDrawerOpen = true
                                } else if (totalDrag > 100f && isDrawerOpen) {
                                    isDrawerOpen = false
                                }
                            } else {
                                if (totalDrag > 100f && !isDrawerOpen) {
                                    isDrawerOpen = true
                                } else if (totalDrag < -100f && isDrawerOpen) {
                                    isDrawerOpen = false
                                }
                            }
                        },
                        onDragCancel = { totalDrag = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            totalDrag += dragAmount
                        }
                    )
                }
                .graphicsLayer {
                    scaleX = 1f - (drawerProgress * 0.18f)
                    scaleY = 1f - (drawerProgress * 0.18f)
                    translationX = if (isRtl) -translationXPx else translationXPx
                    rotationY = if (isRtl) drawerProgress * 12f else -drawerProgress * 12f
                    cameraDistance = 16f * density.density
                    shadowElevation = (drawerProgress * 24f).coerceAtLeast(0f).dp.toPx()
                    clip = true
                    shape = RoundedCornerShape((drawerProgress * 28f).coerceAtLeast(0f).dp)
                },
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    bottomBar = {
                        FloatingNavBar(
                            navController = bottomNavController,
                            onAiClick = { appNavController.navigate(Screen.AiChat) }
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = bottomNavController,
                        startDestination = Screen.Dashboard,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable<Screen.Dashboard> {
                            LaunchedEffect(Unit) {
                                PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Dashboard"))
                            }
                            DashboardScreen(
                                viewModel = viewModel,
                                onMenuClick = { isDrawerOpen = true },
                                onNavigateToCreateGoal = { appNavController.navigate(Screen.CreateGoal) },
                                onNavigateToMyGoals = { bottomNavController.navigate(Screen.MyGoals) },
                                onNavigateToLoans = { bottomNavController.navigate(Screen.Loans) },
                                onNavigateToAccounts = { bottomNavController.navigate(Screen.Accounts) },
                                onNavigateToAnalytics = { bottomNavController.navigate(Screen.Analytics) },
                                onNavigateToSettingsPro = { appNavController.navigate(Screen.Settings(SettingsMode.PRO.name)) },
                                onNavigateToStreak = { appNavController.navigate(Screen.StreakAchievements) },
                                onNotificationsClick = { appNavController.navigate(Screen.Notifications) }
                            )
                        }
                        
                        composable<Screen.MyGoals> {
                            LaunchedEffect(Unit) {
                                PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "My Goals"))
                            }
                            MyGoalsScreen(
                                viewModel = viewModel,
                                onNavigateToGoal = { id -> appNavController.navigate(Screen.GoalDetail(id)) },
                                onNavigateToCreateGoal = { appNavController.navigate(Screen.CreateGoal) },
                                onBack = { bottomNavController.popBackStack() }
                            )
                        }
                        
                        composable<Screen.Loans> {
                            LaunchedEffect(Unit) {
                                PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Loans"))
                            }
                            LoansScreen(
                                viewModel = viewModel,
                                onBack = { bottomNavController.popBackStack() }
                            )
                        }

                        composable<Screen.Accounts> {
                            LaunchedEffect(Unit) {
                                PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Accounts"))
                            }
                            AccountsScreen(
                                viewModel = viewModel,
                                onNavigateToAddAccount = { appNavController.navigate(Screen.AddAccount) },
                                onNavigateToEditAccount = { id -> appNavController.navigate(Screen.EditAccount(id)) },
                                onBack = { bottomNavController.popBackStack() },
                                onNavigateToSettingsPro = { appNavController.navigate(Screen.Settings(SettingsMode.PRO.name)) }
                            )
                        }

                        composable<Screen.Analytics> {
                            LaunchedEffect(Unit) {
                                PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Analytics"))
                            }
                            AnalyticsScreen(
                                viewModel = viewModel,
                                onBack = { bottomNavController.popBackStack() }
                            )
                        }
                    }
                }

                // Overlay to intercept clicks and dismiss drawer when it is open
                if (drawerProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { isDrawerOpen = false }
                            )
                    )
                }
            }
        }

        // Drawer Menu Layer (Rendered on top but restricted to left side, so it intercepts touches perfectly)
        if (drawerProgress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxHeight()
                    .width(265.dp)
                    .graphicsLayer {
                        alpha = drawerProgress
                        translationX = (if (isRtl) 60f else -60f) * (1f - drawerProgress)
                    }
                    .statusBarsPadding()
                    .padding(top = 8.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
            ) {
                DrawerSettingsContent(
                    viewModel = viewModel,
                    appNavController = appNavController,
                    onClose = { isDrawerOpen = false },
                    onSignOutClick = onSignOutClick
                )
            }
        }

        // Premium In-App Deadline Alert
        AnimatedVisibility(
            visible = activeOverdue.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            activeOverdue.firstOrNull()?.let { loan ->
                DeadlineInAppAlert(
                    loanName = loan.contactName,
                    amount = loan.amount,
                    onDismiss = { dismissedAlerts = dismissedAlerts + loan.id },
                    onAction = {
                        dismissedAlerts = dismissedAlerts + loan.id
                        bottomNavController.navigate(Screen.Loans)
                    }
                )
            }
        }

        if (showAddTransactionSheet) {
            AddTransactionScreen(
                viewModel = viewModel,
                selectedAccountId = null,
                accounts = allAccounts,
                onDismiss = { showAddTransactionSheet = false },
                onNavigateToAddAccount = {
                    showAddTransactionSheet = false
                    appNavController.navigate(Screen.AddAccount)
                },
                onNavigateToSettingsPro = {
                    showAddTransactionSheet = false
                    appNavController.navigate(Screen.Settings(SettingsMode.PRO.name))
                }
            )
        }
    }
}

@Composable
fun DeadlineInAppAlert(
    loanName: String,
    amount: Double,
    onDismiss: () -> Unit,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAction() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavyDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, PinkPrimary.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PinkPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PriorityHigh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.deadline_title),
                    color = PinkPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                val formattedAmount = remember(amount) {
                    try {
                        String.format(java.util.Locale.US, "%.2f", amount)
                    } catch (e: Exception) {
                        amount.toString()
                    }
                }
                Text(
                    text = stringResource(R.string.repayment_deadline_over, loanName, ""),
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 2
                )
            }
            
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun FloatingNavBar(navController: NavHostController, onAiClick: () -> Unit = {}) {
    val items = remember {
        listOf(
            NavItem(Screen.Dashboard, Icons.Default.Home),
            NavItem(Screen.MyGoals, Icons.Default.Dashboard),
            NavItem(Screen.Loans, Icons.Default.AccountBalance),
            NavItem(Screen.Accounts, Icons.Default.AccountTree),
            NavItem(Screen.Analytics, Icons.Default.PieChart)
        )
    }
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF1E1E2E).copy(alpha = 0.95f))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentDestination?.hierarchy?.any { it.hasRoute(item.screen::class) } == true
                
                NavBarItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = {
                        if (item.screen == Screen.Dashboard) {
                            navController.navigate(Screen.Dashboard) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate(item.screen) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
        
        FloatingActionButton(
            onClick = onAiClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "Sovereign AI")
        }
    }
}

data class NavItem(val screen: Screen, val icon: ImageVector)

@Composable
fun NavBarItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier.background(PinkPrimary)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun DrawerSettingsContent(
    viewModel: PiggyLedgerViewModel,
    appNavController: NavHostController,
    onClose: () -> Unit,
    onSignOutClick: () -> Unit = {}
) {
    val authUserName by viewModel.authUserName.collectAsState()
    val authUserEmail by viewModel.authUserEmail.collectAsState()
    val authUserPhotoUrl by viewModel.authUserPhotoUrl.collectAsState()
    val pendingTransactions by viewModel.allPendingTransactions.collectAsState()

    val userFullName = authUserName.ifBlank { "User" }
    val userEmail = authUserEmail.ifBlank { "user@example.com" }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Profile Info Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable {
                            onClose()
                            appNavController.navigate(Screen.Settings(SettingsMode.PROFILE.name))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (authUserPhotoUrl.isNotBlank()) {
                        AsyncImage(
                            model = authUserPhotoUrl,
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userFullName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = userEmail,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            // Menu Items List (clean text-only layout)
            val menuItems = listOf(
                DrawerMenuItem(
                    title = stringResource(R.string.pending_transactions),
                    badgeCount = pendingTransactions.size,
                    onClick = {
                        onClose()
                        appNavController.navigate(Screen.PendingTransactions)
                    }
                ),
                DrawerMenuItem(
                    title = stringResource(R.string.account_identifiers),
                    onClick = {
                        onClose()
                        appNavController.navigate(Screen.Settings(SettingsMode.ACCOUNT_IDENTIFIERS.name))
                    }
                ),
                DrawerMenuItem(
                    title = stringResource(R.string.language),
                    onClick = {
                        onClose()
                        appNavController.navigate(Screen.Settings(SettingsMode.LANGUAGE.name))
                    }
                ),
                /* DrawerMenuItem(
                    title = stringResource(R.string.currency),
                    onClick = {
                        onClose()
                        appNavController.navigate(Screen.Settings(SettingsMode.CURRENCY.name))
                    }
                ), */
                DrawerMenuItem(
                    title = stringResource(R.string.give_feedback),
                    onClick = {
                        onClose()
                        appNavController.navigate(Screen.Settings(SettingsMode.FEEDBACK.name))
                    }
                ),
                DrawerMenuItem(
                    title = stringResource(R.string.rate_app),
                    onClick = {
                        onClose()
                        appNavController.navigate(Screen.Settings(SettingsMode.RATING.name))
                    }
                ),
                DrawerMenuItem(
                    title = stringResource(R.string.backup_data),
                    onClick = {
                        onClose()
                        appNavController.navigate(Screen.Settings(SettingsMode.BACKUP.name))
                    }
                ),
                DrawerMenuItem(
                    title = stringResource(R.string.security),
                    onClick = {
                        onClose()
                        appNavController.navigate(Screen.Settings(SettingsMode.SECURITY.name))
                    }
                ),
                DrawerMenuItem(
                    title = stringResource(R.string.piggy_ledger_pro),
                    onClick = {
                        onClose()
                        appNavController.navigate(Screen.Settings(SettingsMode.PRO.name))
                    }
                ),
                DrawerMenuItem(
                    title = stringResource(R.string.share_app),
                    onClick = {
                        onClose()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Check out Piggy Ledger! https://play.google.com/store/apps/details?id=com.oryno.piggy_ledger")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }
                )
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                menuItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = item.onClick
                            )
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (item.badgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(PinkPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.badgeCount.toString(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom section: Logout Button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onClose()
                    onSignOutClick()
                },
            shape = RoundedCornerShape(14.dp),
            color = Color.White.copy(alpha = 0.1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.auth_sign_out),
                    color = PinkPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

data class DrawerMenuItem(
    val title: String,
    val badgeCount: Int = 0,
    val onClick: () -> Unit
)
