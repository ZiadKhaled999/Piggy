package com.oryno.piggy_ledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String,
    val name: String,
    val targetAmount: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    val goalId: String,
    val amount: Double,
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    val deadline: Long? = null
)

@Serializable
@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey val id: String,
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
    val deadline: Long? = null
)

@Serializable
@Entity(
    tableName = "loan_payments",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = Loan::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("loanId")]
)
data class LoanPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null
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
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    val label: String? = null
)

@Serializable
@Entity(
    tableName = "account_transactions",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("account_id")]
)
data class AccountTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val account_id: Long,
    val amount: Double,
    val merchant: String,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "AUTOMATIC_SMS"
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
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val raw_sms_body: String,
    val sender: String,
    val timestamp: Long = System.currentTimeMillis()
)
