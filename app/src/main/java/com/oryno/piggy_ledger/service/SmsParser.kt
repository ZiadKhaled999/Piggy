package com.oryno.piggy_ledger.service

data class ParsedSms(val amount: Double, val merchant: String, val date: String?, val isIncome: Boolean = false, val actionType: SmsActionType = SmsActionType.UNKNOWN)

object SmsParser {
    fun convertArabicDigitsAndSymbols(input: String): String {
        var result = input
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        for (i in 0..9) {
            result = result.replace(arabicDigits[i], '0' + i)
        }
        result = result.replace('٫', '.')
        
        return result
    }

    fun parse(rawBody: String): ParsedSms {
        val body = convertArabicDigitsAndSymbols(rawBody)
        
        // ===== AMOUNT EXTRACTION (PRIORITIZED) =====
        val currencyTokens = """\bEGP\b|\bLE\b|L\.E\.|\bUSD\b|\$|\bEUR\b|€|£|₩|\bAED\b|\bSAR\b|\bKWD\b|\bQAR\b|\bBHD\b|\bOMR\b|ج\.م|جم|جنيه|جنيها|جنيهًا|ريال|درهم|دينار"""
        
        // 1. Try transaction keywords first (including خصم for deductions)
        var amountMatch = Regex("""(?i)(?:amount|paid|purchase|pay|سداد|دفع|مبلغ|بمبلغ|تحويل|transfer|استقبلت|خصم|تم خصم)(?:\s*is|:|\s*)\s*([\d,]+(?:\.\d{1,2})?)""").find(body)
        
        // 2. Try currency-prefixed numbers
        if (amountMatch == null) {
            amountMatch = Regex("""(?i)(?:$currencyTokens)\s*([\d,]+(?:\.\d{1,2})?)|([\d,]+(?:\.\d{1,2})?)\s*(?:$currencyTokens)""").find(body)
        }
        
        // 3. Try balance as fallback
        if (amountMatch == null) {
            amountMatch = Regex("""(?i)(?:balance|رصيد|رصيدك)(?:\s*is|:|\s*)\s*([\d,]+(?:\.\d{1,2})?)""").find(body)
        }
        
        val amountStr = amountMatch?.groups?.get(1)?.value 
            ?: amountMatch?.groups?.get(2)?.value 
            ?: amountMatch?.groups?.get(3)?.value
            
        // Clean the amount string – remove appended dates like "8/3/26"
        val cleanAmountStr = amountStr
            ?.replace(",", "")
            ?.replace(Regex("""\s+\d{1,2}/\d{1,2}/\d{2,4}.*"""), "")
            ?.trim()
        val amount = cleanAmountStr?.toDoubleOrNull() ?: 0.0
        
        // ===== MERCHANT / PERSON NAME EXTRACTION =====
        var merchant = ""

        // 1. Try standard merchant regex first (after prepositions)
        val merchantRegex = Regex("""(?i)(?:\bat\b|\bto\b|\bfrom\b|في|من|من حسابك لدى|حسابك لدى|لدى|من|إلى|الي)\s+([A-Za-z0-9\s\u0600-\u06FF\-.',&]{2,50}?)(?:\s+on\b|\s+value\b|\.|بتاريخ|يوم|\d{1,2}/\d{1,2}|\$)""")
        val merchantMatch = merchantRegex.find(body)
        merchant = merchantMatch?.groups?.get(1)?.value?.trim() ?: ""

        // 2. NEW: Extract channel from "/ ATM" or "/ POS" pattern
        if (merchant.isBlank()) {
            val channelRegex = Regex("""/\s*([A-Za-z0-9\s]{2,20})(?:\s*\(|$)""")
            val channelMatch = channelRegex.find(body)
            if (channelMatch != null) {
                merchant = channelMatch.groups[1]?.value?.trim() ?: ""
            }
        }

        // 3. Special: InstaPay transfers with "من" (from) for person names
        if (merchant.isBlank() || merchant.contains("حسابك")) {
            val instaPayNameRegex = Regex("""من\s+([\u0600-\u06FF\s]{3,50}?)(?:\s+يوم|\s+في|\s+رقم)""")
            val instaPayMatch = instaPayNameRegex.find(body)
            if (instaPayMatch != null) {
                merchant = instaPayMatch.groups[1]?.value?.trim() ?: merchant
            }
        }

        // If still blank, set default
        if (merchant.isBlank()) {
            merchant = "Unknown SMS Merchant"
        }

        // ===== DATE EXTRACTION =====
        val dateRegex = Regex("""(\d{1,2}[/-]\d{1,2}([/-]\d{2,4})?)""")
        val dateMatch = dateRegex.find(body)
        val date = dateMatch?.value 

        // ===== INCOME vs EXPENSE DETECTION =====
        var isIncome = false

        // Check for DEBIT/EXPENSE patterns FIRST
        if (body.contains("تم خصم", ignoreCase = true) ||
            body.contains("خصم", ignoreCase = true) ||
            body.contains("سحب", ignoreCase = true) ||
            body.contains("debited", ignoreCase = true) ||
            body.contains("withdrawn", ignoreCase = true)) {
            isIncome = false
        } 
        // Then check for INCOME patterns
        else if (body.contains("استقبلت تحويل لحظي", ignoreCase = true) ||
                 body.contains("لقد استقبلت", ignoreCase = true) ||
                 body.contains("تم اضافة", ignoreCase = true) ||
                 body.contains("ايداع", ignoreCase = true) ||
                 body.contains("credited", ignoreCase = true) ||
                 body.contains("deposit", ignoreCase = true)) {
            isIncome = true
        } else {
            // Fallback to keyword list
            val incomeKeywords = listOf("استقبلت", "ايداع", "إيداع", "إضافة", "اضافة", "استرداد", "received", "credited", "refunded", "deposit", "added")
            isIncome = incomeKeywords.any { body.contains(it, ignoreCase = true) }
        }

        // ===== ACTION TYPE DETECTION =====
        val actionType = when {
            // Incoming InstaPay
            body.contains("استقبلت تحويل لحظي", ignoreCase = true) ||
            body.contains("لقد استقبلت", ignoreCase = true) -> SmsActionType.DEPOSIT
            
            // Outgoing InstaPay
            body.contains("قمت بتحويل لحظي", ignoreCase = true) ||
            body.contains("تم تحويل", ignoreCase = true) -> SmsActionType.TRANSFER_OUT
            
            // WITHDRAWAL (ATM cash)
            body.contains("سحب", ignoreCase = true) ||
            body.contains("withdrawal", ignoreCase = true) ||
            body.contains("cash withdrawal", ignoreCase = true) -> SmsActionType.WITHDRAWAL
            
            // PURCHASE (card payments)
            body.contains("شراء", ignoreCase = true) ||
            body.contains("purchase", ignoreCase = true) ||
            body.contains("paid", ignoreCase = true) -> SmsActionType.PURCHASE
            
            else -> SmsActionType.UNKNOWN
        }

        return ParsedSms(amount, merchant, date, isIncome, actionType)
    }
}
