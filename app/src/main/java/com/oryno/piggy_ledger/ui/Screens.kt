package com.oryno.piggy_ledger.ui

import kotlinx.serialization.Serializable

enum class SettingsMode {
    MAIN, FEEDBACK, RATING, BACKUP, RESTORE, LANGUAGE, SECURITY, PRO, ACCOUNT_IDENTIFIERS
}

sealed class Screen {
    @Serializable data object Splash : Screen()
    @Serializable data object LanguageSelection : Screen()
    @Serializable data object HearAboutUs : Screen()
    @Serializable data object Onboarding : Screen()
    @Serializable data object Auth : Screen()
    @Serializable data object MainContainer : Screen()
    @Serializable data object Dashboard : Screen()
    @Serializable data class Settings(val modeName: String = "MAIN") : Screen()
    @Serializable data object CreateGoal : Screen()
    @Serializable data object MyGoals : Screen()
    @Serializable data class GoalDetail(val goalId: String) : Screen()
    @Serializable data object Loans : Screen()
    @Serializable data object Accounts : Screen()
    @Serializable data object AddAccount : Screen()
    @Serializable data class EditAccount(val accountId: String) : Screen()
    @Serializable data object Analytics : Screen()
    @Serializable data object PendingTransactions : Screen()
    @Serializable data object StreakAchievements : Screen()
    @Serializable data object AiChat : Screen()
}
