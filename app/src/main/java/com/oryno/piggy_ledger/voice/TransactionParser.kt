package com.oryno.piggy_ledger.voice

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ParsedTransaction(
    val amount: Double,
    val accountName: String?,
    val goalName: String?,
    val isExpense: Boolean
)

object TransactionParser {

    fun parse(text: String, accounts: List<String>, goals: List<String>): ParsedTransaction {
        val lowerText = text.lowercase()

        // Extract amount (first number found)
        val amountRegex = Regex("(?i)(\\d+(\\.\\d+)?)")
        val amountMatch = amountRegex.find(text)
        val amount = amountMatch?.value?.toDoubleOrNull() ?: 0.0

        // Determine type based on keywords
        val isExpense = if (lowerText.contains("add") || lowerText.contains("income") || lowerText.contains("deposit") || lowerText.contains("earned")) {
            false
        } else {
            true
        }

        // Find best match for account
        var bestAccount: String? = null
        for (account in accounts) {
            if (lowerText.contains(account.lowercase())) {
                bestAccount = account
                break
            }
        }

        // Find best match for category/goal
        var bestGoal: String? = null
        for (goal in goals) {
            if (lowerText.contains(goal.lowercase())) {
                bestGoal = goal
                break
            }
        }

        return ParsedTransaction(
            amount = amount,
            accountName = bestAccount,
            goalName = bestGoal,
            isExpense = isExpense
        )
    }
}
