package com.oryno.piggy_ledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class JournalType {
    EXPENSE, INCOME, TRANSFER, ASSET_BUY, CERT_PURCHASE
}

@Serializable
enum class AssetType {
    GOLD, SILVER, CERTIFICATE
}

@Serializable
@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val type: AssetType,
    val name: String,
    val weightGrams: Double = 0.0,
    val purity: String = "21k", // "24k", "21k", "18k", "999", "925", "900"
    val principalAmount: Double = 0.0,
    val annualRate: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val startDate: Long = System.currentTimeMillis(),
    val maturityDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val is_deleted: Boolean = false
)

@Serializable
@Entity(tableName = "live_market_rates")
data class LiveMarketRatesEntity(
    @PrimaryKey val id: String = "singleton_rates",
    val gold24k: Double = 4120.0,
    val gold21k: Double = 3605.0,
    val gold18k: Double = 3090.0,
    val silver999: Double = 52.0,
    val silver925: Double = 48.0,
    val silver900: Double = 46.8,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "portfolio_history")
data class PortfolioHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: Long = System.currentTimeMillis(),
    val totalPortfolioValue: Double = 0.0
)

@Serializable
@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val date: Long = System.currentTimeMillis(),
    val description: String,
    val type: JournalType,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val is_deleted: Boolean = false
)

@Serializable
@Entity(
    tableName = "postings",
    indices = [
        androidx.room.Index("journalEntryId"),
        androidx.room.Index("accountId")
    ]
)
data class PostingEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val journalEntryId: String,
    val accountId: String,
    val amount: Double,
    val isDebit: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val is_deleted: Boolean = false
)

@Serializable
@Entity(
    tableName = "subscriptions",
    indices = [androidx.room.Index("accountId")]
)
data class SubscriptionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String,
    val logoUrl: String? = null,
    val amount: Double,
    val currency: String = "EGP",
    val billingCycle: String = "MONTHLY",
    val nextRenewalDate: Long = System.currentTimeMillis(),
    val accountId: String = "",
    val autoDebit: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val is_deleted: Boolean = false
)

@Serializable
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String,
    val targetAmount: Double,
    val backedByAssetId: String? = null,
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
    BANK, CARD, CASH, WALLET, INVESTMENT, ASSET_GOLD, ASSET_SILVER, ASSET_PROPERTY, CERTIFICATE, LIABILITY, EXPENSE_SYSTEM, INCOME_SYSTEM, EQUITY_SYSTEM
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
    val goldWeightGrams: Double? = null,
    val goldKarat: Int? = null,
    val isAutoUpdateGoldPrice: Boolean = false,
    val certificatePrincipal: Double? = null,
    val certificateInterestRate: Double? = null,
    val certificatePayoutFrequency: String? = null,
    val isSystemAccount: Boolean = false,
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
    val preferredAccountId: String? = null,
    // FIX(sync): appCurrency must ride the wire. @Transient excluded it from the
    // Ktor JSON payload, so currency never synced (server column exists). Room
    // column already exists, so removing it needs no migration.
    val appCurrency: String = "EGP",
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
