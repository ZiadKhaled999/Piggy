package com.oryno.piggy_ledger.ui

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable data object Splash : Screen()
    @Serializable data object LanguageSelection : Screen()
    @Serializable data object Onboarding : Screen()
    @Serializable data object Dashboard : Screen()
    @Serializable data object CreateGoal : Screen()
    @Serializable data object MyGoals : Screen()
    @Serializable data class GoalDetail(val goalId: String) : Screen()
    @Serializable data object Loans : Screen()
}
