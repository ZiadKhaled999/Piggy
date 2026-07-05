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
    val social: String? = null,
    val note: String,
    val isPaidOff: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val deadline: Long? = null
)

@Serializable
enum class LoanType {
    LENT, BORROWED
}

@Serializable
data class BackupData(
    val goals: List<Goal>,
    val transactions: List<Transaction>,
    val loans: List<Loan>
)
