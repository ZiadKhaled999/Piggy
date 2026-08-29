package com.oryno.piggy_ledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String,
    val targetAmount: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val is_deleted: Boolean = false
)

@Serializable
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val goalId: String,
    val amount: Double,
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val is_deleted: Boolean = false
)

@Serializable
@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val type: LoanType,
    val amount: Double,
    val contactName: String,
    val phone: String? = null,
    val email: String? = null,
    val photoUri: String? = null,
    val social: String? = null,
    val note: String,
    val isPaidOff: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val is_deleted: Boolean = false
)

@Serializable
@Entity(
    tableName = "loan_payments",
    indices = [androidx.room.Index("loanId")]
)
data class LoanPayment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val loanId: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val is_deleted: Boolean = false
)

@Serializable
enum class LoanType {
    LENT, BORROWED
}

@Serializable
enum class AccountType {
    BANK, CARD, CASH, WALLET
}

@Serializable
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String,
    val type: AccountType,
    val icon_color: String,
    val icon_name: String = "AccountBalance",
    val logo_url: String? = null,
    val local_logo_path: String? = null,
    val currency: String,
    val starting_balance: Double,
    val current_balance: Double = starting_balance,
    val exclude_from_all: Boolean = false,
    val credit_limit: Double? = null,
    val available_credit: Double? = null,
    val payment_due_day: Int? = null,
    val card_numbers: String? = null,
    val bank_account_no: String? = null,
    val provider: String? = null,
    val insta_pay_fee: Boolean = false,
    val label: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val is_deleted: Boolean = false
)

@Serializable
@Entity(
    tableName = "account_transactions",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = androidx.room.ForeignKey.NO_ACTION
        )
    ],
    indices = [androidx.room.Index("account_id")]
)
data class AccountTransaction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val account_id: String,
    val amount: Double,
    val merchant: String,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "AUTOMATIC_SMS",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val is_deleted: Boolean = false
)

@Serializable
@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val userId: String = "local_user",
    val hasOnboarded: Boolean = false,
    val hasLanguageSelected: Boolean = false,
    val hasHeardAboutUs: Boolean = false,
    val personalizedIntent: Int = -1,
    val personalizedIntensity: Int = -1,
    val savingMode: String = "piggy",
    val customIdentifiersJson: String = "{}",
    val isBiometricLockEnabled: Boolean = false,
    val isScreenshotProtectionEnabled: Boolean = false,
    val isPremium: Boolean = false,
    val premiumExpiryTimestamp: Long = 0L,
    val isLifetimePremium: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val is_deleted: Boolean = false
)

@Serializable
@Entity(tableName = "streak_dates")
data class StreakDateEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val dateStr: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val is_deleted: Boolean = false
)

@Serializable
@Entity(tableName = "ai_conversations")
data class AiConversation(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "New Conversation",
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val is_deleted: Boolean = false
)

@Serializable
@Entity(tableName = "ai_chat_messages")
data class AiChatMessage(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val conversationId: String = "default",
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val is_deleted: Boolean = false
)

@Serializable
data class BackupData(
    val goals: List<Goal>,
    val transactions: List<Transaction>,
    val loans: List<Loan>,
    val loanPayments: List<LoanPayment> = emptyList(),
    val streakDates: Set<String> = emptySet()
)

@Serializable
@Entity(tableName = "pending_transactions")
data class PendingTransaction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val amount: Double,
    val merchant: String,
    val raw_sms_body: String,
    val sender: String,
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val is_deleted: Boolean = false
)

@Serializable
@Entity(tableName = "onboarding_answers")
data class OnboardingAnswer(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val key: String = "",
    val value: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val is_deleted: Boolean = false
)
