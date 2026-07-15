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

@Composable
fun PiggyLedgerApp(factory: ViewModelFactory) {
    val navController = rememberNavController()
    val viewModel: PiggyLedgerViewModel = viewModel(factory = factory)
    val voiceViewModel: VoiceLedgerViewModel = viewModel(factory = factory)
    
    val hasOnboarded by viewModel.hasOnboarded.collectAsState()
    val hasLanguageSelected by viewModel.hasLanguageSelected.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    PiggyLedgerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(navController = navController, startDestination = Screen.Splash) {
                composable<Screen.Splash> {
                    LaunchedEffect(hasOnboarded, hasLanguageSelected, isAuthenticated) {
                        if (hasLanguageSelected == false) {
                            navController.navigate(Screen.LanguageSelection) {
                                popUpTo(Screen.Splash) { inclusive = true }
                            }
                        } else if (hasOnboarded == false) {
                            navController.navigate(Screen.Onboarding) {
                                popUpTo(Screen.Splash) { inclusive = true }
                            }
                        } else if (isAuthenticated == false) {
                            navController.navigate(Screen.Auth) {
                                popUpTo(Screen.Splash) { inclusive = true }
                            }
                        } else if (hasOnboarded == true && hasLanguageSelected == true && isAuthenticated == true) {
                            navController.navigate(Screen.MainContainer) {
                                popUpTo(Screen.Splash) { inclusive = true }
                            }
                        }
                    }
                }
                
                composable<Screen.LanguageSelection> {
                    LaunchedEffect(Unit) {
                        PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Language Selection"))
                    }
                    LanguageSelectionScreen(
                        onLanguageSelected = {
                            viewModel.completeLanguageSelection()
                            navController.navigate(Screen.Onboarding) {
                                popUpTo(Screen.LanguageSelection) { inclusive = true }
                            }
                        }
                    )
                }
                
                composable<Screen.Onboarding> {
                    LaunchedEffect(Unit) {
                        PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Onboarding"))
                    }
                    OnboardingScreen(
                        onComplete = {
                            viewModel.completeOnboarding()
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
                    MainContainer(viewModel = viewModel, voiceViewModel = voiceViewModel, appNavController = navController)
                }

                // Sub-screens that are not part of the main tabs but need to be accessible
                composable<Screen.CreateGoal> {
                    LaunchedEffect(Unit) {
                        PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Create Goal"))
                    }
                    CreateGoalScreen(
                        onGoalCreated = { name, amount ->
                            viewModel.addGoal(name, amount)
                            navController.popBackStack()
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
            }
        }
    }
}

@Composable
fun MainContainer(
    viewModel: PiggyLedgerViewModel,
    voiceViewModel: VoiceLedgerViewModel,
    appNavController: NavHostController
) {
    val bottomNavController = rememberNavController()
    val overdueLoans by viewModel.overdueLoans.collectAsState()
    var dismissedAlerts by remember { mutableStateOf(setOf<String>()) }
    
    val activeOverdue = overdueLoans.filter { it.id !in dismissedAlerts }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                FloatingNavBar(navController = bottomNavController)
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
                        voiceViewModel = voiceViewModel,
                        onNavigateToCreateGoal = { appNavController.navigate(Screen.CreateGoal) },
                        onNavigateToMyGoals = { bottomNavController.navigate(Screen.MyGoals) },
                        onNavigateToLoans = { bottomNavController.navigate(Screen.Loans) },
                        onNavigateToAccounts = { bottomNavController.navigate(Screen.Accounts) },
                        onNavigateToAnalytics = { bottomNavController.navigate(Screen.Analytics) }
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
                        onBack = { bottomNavController.popBackStack() }
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
                
                composable<Screen.Settings> {
                    LaunchedEffect(Unit) {
                        PostHog.capture(event = "screen_view", properties = mapOf("screen_name" to "Settings"))
                    }
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToPendingTransactions = { appNavController.navigate(Screen.PendingTransactions) }
                    )
                }
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
                Text(
                    text = stringResource(R.string.repayment_deadline_over, loanName, "$$amount"),
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
fun FloatingNavBar(navController: NavHostController) {
    val items = remember {
        listOf(
            NavItem(Screen.Dashboard, Icons.Default.Home),
            NavItem(Screen.MyGoals, Icons.Default.Dashboard),
            NavItem(Screen.Loans, Icons.Default.AccountBalance),
            NavItem(Screen.Accounts, Icons.Default.AccountTree),
            NavItem(Screen.Analytics, Icons.Default.PieChart),
            NavItem(Screen.Settings, Icons.Default.Settings)
        )
    }
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF1E1E2E).copy(alpha = 0.95f)) // Dark translucent background
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
