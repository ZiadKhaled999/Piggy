package com.oryno.piggy_ledger.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Text
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.oryno.piggy_ledger.ui.theme.PiggyLedgerTheme

@Composable
fun PiggyLedgerApp(factory: ViewModelFactory) {
    val navController = rememberNavController()
    val viewModel: PiggyLedgerViewModel = viewModel(factory = factory)
    
    val hasOnboarded by viewModel.hasOnboarded.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(navController = navController, startDestination = Screen.Splash) {
            composable<Screen.Splash> {
                LaunchedEffect(hasOnboarded) {
                    if (hasOnboarded == false) {
                        navController.navigate(Screen.Onboarding) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    } else if (hasOnboarded == true) {
                        navController.navigate(Screen.Dashboard) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    }
                }
            }
            
            composable<Screen.Onboarding> {
                OnboardingScreen(
                    onComplete = {
                        viewModel.completeOnboarding()
                        navController.navigate(Screen.Dashboard) {
                            popUpTo(Screen.Onboarding) { inclusive = true }
                        }
                    }
                )
            }
            
            composable<Screen.Dashboard> {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToCreateGoal = { navController.navigate(Screen.CreateGoal) },
                    onNavigateToMyGoals = { navController.navigate(Screen.MyGoals) },
                    onNavigateToLoans = { navController.navigate(Screen.Loans) }
                )
            }
            
            composable<Screen.CreateGoal> {
                CreateGoalScreen(
                    onGoalCreated = { name, amount ->
                        viewModel.addGoal(name, amount)
                        navController.navigate(Screen.MyGoals) {
                            popUpTo(Screen.CreateGoal) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable<Screen.MyGoals> {
                MyGoalsScreen(
                    viewModel = viewModel,
                    onNavigateToGoal = { id -> navController.navigate(Screen.GoalDetail(id)) },
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable<Screen.GoalDetail> { backStackEntry ->
                val screen = backStackEntry.toRoute<Screen.GoalDetail>()
                GoalDetailScreen(
                    goalId = screen.goalId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable<Screen.Loans> {
                LoansScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
