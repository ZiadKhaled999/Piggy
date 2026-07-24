package com.oryno.piggy_ledger.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.PendingTransaction
import com.oryno.piggy_ledger.data.Account

class SmsParsingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val sender = inputData.getString("sender") ?: return Result.failure()
        val rawBody = inputData.getString("body") ?: return Result.failure()

        val parsedSms = SmsParser.parse(rawBody)
        val amount = parsedSms.amount
        val merchant = parsedSms.merchant

        if (amount == 0.0) {
            // Could not parse any valid transaction amount, discard or fail
            return Result.success()
        }

        val db = PiggyLedgerDatabase.getInstance(applicationContext)
        val dao = db.piggyLedgerDao()
        val body = SmsParser.convertArabicDigitsAndSymbols(rawBody)


        // Step 3: Accounts Retrieval and Smart Matching
        val accounts = dao.getAllAccountsSync()

        // Match accounts by exact card numbers or bank account numbers (Unique Identifiers)
        val uniqueMatches = accounts.filter { account ->
            val hasCard = account.card_numbers?.let { it.isNotBlank() && body.contains(it) } == true
            val hasBank = account.bank_account_no?.let { it.isNotBlank() && body.contains(it) } == true
            hasCard || hasBank
        }

        // Match accounts by bank provider, account name, or label against sender/SMS body
        val providerMatches = accounts.filter { account ->
            val cleanSender = sender.replace(" ", "").replace("-", "").lowercase()
            val cleanBody = body.lowercase()

            val provMatch = account.provider?.takeIf { it.isNotBlank() }?.let { prov ->
                val cleanProv = prov.replace(" ", "").lowercase()
                cleanSender.contains(cleanProv) ||
                cleanProv.contains(cleanSender) ||
                cleanBody.contains(prov.lowercase()) ||
                cleanBody.contains(cleanProv)
            } ?: false

            val nameMatch = account.name.takeIf { it.isNotBlank() }?.let { name ->
                val cleanName = name.replace(" ", "").lowercase()
                cleanSender.contains(cleanName) ||
                cleanName.contains(cleanSender) ||
                cleanBody.contains(name.lowercase()) ||
                cleanBody.contains(cleanName)
            } ?: false

            val labelMatch = account.label?.takeIf { it.isNotBlank() }?.let { label ->
                val cleanLabel = label.replace(" ", "").lowercase()
                cleanSender.contains(cleanLabel) ||
                cleanBody.contains(label.lowercase())
            } ?: false

            provMatch || nameMatch || labelMatch
        }

        // Identify the correct destination account or route to Safety Hold (Pending Queue)
        val matchedAccount: Account? = when {
            // Case A: Exactly one unique card/bank number match
            uniqueMatches.size == 1 -> uniqueMatches.first()
            
            // Case B: No explicit unique matches, but exactly one provider match
            uniqueMatches.isEmpty() && providerMatches.size == 1 -> providerMatches.first()
            
            // Case C: Multi-account ambiguity or no match - Hold in Pending Transactions
            else -> null
        }

        if (matchedAccount != null) {
            // Direct hit: Safe automatic processing
            dao.processSmsTransaction(
                accountId = matchedAccount.id,
                amount = amount,
                merchant = merchant,
                applyInstaPayFee = matchedAccount.insta_pay_fee
            )
        } else {
            // Safety Hold: Route to the Human-in-the-Loop pending queue
            dao.insertPendingTransaction(
                PendingTransaction(
                    amount = amount,
                    merchant = merchant,
                    raw_sms_body = rawBody,
                    sender = sender
                )
            )
        }

        return Result.success()
    }
}
