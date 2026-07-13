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

    private fun convertArabicDigitsAndSymbols(input: String): String {
        var result = input
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        for (i in 0..9) {
            result = result.replace(arabicDigits[i], '0' + i)
        }
        result = result.replace('٫', '.')
        return result
    }

    override suspend fun doWork(): Result {
        val sender = inputData.getString("sender") ?: return Result.failure()
        val rawBody = inputData.getString("body") ?: return Result.failure()

        // Normalize Eastern Arabic digits and symbols to Western format
        val body = convertArabicDigitsAndSymbols(rawBody)

        val db = PiggyLedgerDatabase.getInstance(applicationContext)
        val dao = db.piggyLedgerDao()
        
        // Step 1: Parse Amount (English and Arabic currencies)
        val amountRegex = Regex("""(?i)(?:EGP|LE|L\.E\.|USD|\$|EUR|£|ج\.م|جنيه|جنيها|جنيهًا)\s*([\d,]+(?:\.\d{1,2})?)|([\d,]+(?:\.\d{1,2})?)\s*(?:EGP|LE|L\.E\.|USD|\$|EUR|£|ج\.م|جنيه|جنيها|جنيهًا)""")
        val amountMatch = amountRegex.find(body)
        
        val amountStr = amountMatch?.groups?.get(1)?.value ?: amountMatch?.groups?.get(2)?.value
        val amount = amountStr?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        
        if (amount == 0.0) {
            // Could not parse any valid transaction amount, discard or fail
            return Result.success()
        }

        // Step 2: Extract Merchant (English and Arabic prepositions)
        val merchantRegex = Regex("""(?i)(?:at|to|from|في|لدى|من|إلى)\s+([A-Za-z0-9\s\u0600-\u06FF]+?)(?:\s+on|\s+value|\.|\d|$)""")
        val merchantMatch = merchantRegex.find(body)
        val merchant = merchantMatch?.groups?.get(1)?.value?.trim() ?: "Unknown SMS Merchant"

        // Step 3: Accounts Retrieval and Smart Matching
        val accounts = dao.getAllAccountsSync()

        // Match accounts by exact card numbers or bank account numbers (Unique Identifiers)
        val uniqueMatches = accounts.filter { account ->
            val hasCard = account.card_numbers?.let { it.isNotBlank() && body.contains(it) } == true
            val hasBank = account.bank_account_no?.let { it.isNotBlank() && body.contains(it) } == true
            hasCard || hasBank
        }

        // Match accounts by bank provider or sender name
        val providerMatches = accounts.filter { account ->
            val prov = account.provider
            if (prov.isNullOrBlank()) false else {
                val cleanProv = prov.replace(" ", "").lowercase()
                val cleanSender = sender.replace(" ", "").replace("-", "").lowercase()
                cleanSender.contains(cleanProv) || 
                cleanProv.contains(cleanSender) || 
                body.lowercase().contains(prov.lowercase()) ||
                body.lowercase().contains(cleanProv)
            }
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
