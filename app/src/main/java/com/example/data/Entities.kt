package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String,
    val name: String,
    val targetAmount: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    val goalId: String,
    val amount: Double,
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    val deadline: Long? = null
)

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

enum class LoanType {
    LENT, BORROWED
}
