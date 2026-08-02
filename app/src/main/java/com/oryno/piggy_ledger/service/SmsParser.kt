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
        
        // Step 1: Parse Amount (English and Arabic currencies with proper word boundaries)
        val currencyTokens = """\bEGP\b|\bLE\b|L\.E\.|\bUSD\b|\$|\bEUR\b|€|£|₩|\bAED\b|\bSAR\b|\bKWD\b|\bQAR\b|\bBHD\b|\bOMR\b|ج\.م|جنيه|جنيها|جنيهًا|ريال|درهم|دينار"""
        
        var amountMatch = Regex("""(?i)(?:amount|paid|purchase|pay|سداد|دفع|مبلغ|تحويل|transfer)(?:\s*is|:|\s*)\s*([\d,]+(?:\.\d{1,2})?)""").find(body)
        if (amountMatch == null) {
            amountMatch = Regex("""(?i)(?:$currencyTokens)\s*([\d,]+(?:\.\d{1,2})?)|([\d,]+(?:\.\d{1,2})?)\s*(?:$currencyTokens)""").find(body)
        }
        if (amountMatch == null) {
            amountMatch = Regex("""(?i)(?:balance|رصيد|رصيدك)(?:\s*is|:|\s*)\s*([\d,]+(?:\.\d{1,2})?)""").find(body)
        }
        
        val amountStr = amountMatch?.groups?.get(1)?.value 
            ?: amountMatch?.groups?.get(2)?.value 
            ?: amountMatch?.groups?.get(3)?.value
        val amount = amountStr?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        
        // Step 2: Extract Merchant (English and Arabic prepositions with word boundaries and expanded character class)
        val merchantRegex = Regex("""(?i)(?:\bat\b|\bto\b|\bfrom\b|في|من حسابك لدى|حسابك لدى|لدى|من|إلى)\s+([A-Za-z0-9\s\u0600-\u06FF\-.',&]{2,50}?)(?:\s+on\b|\s+value\b|\.|بتاريخ|$)""")
        val merchantMatch = merchantRegex.find(body)
        val merchant = merchantMatch?.groups?.get(1)?.value?.trim() ?: "Unknown SMS Merchant"

        // Step 3: Extract Date
        val dateRegex = Regex("""(\d{1,2}[/-]\d{1,2}([/-]\d{2,4})?)""")
        val dateMatch = dateRegex.find(body)
        val date = dateMatch?.value 

        // Step 4: Detect Income vs Expense
        val incomeKeywords = listOf("استقبلت", "ايداع", "إيداع", "إضافة", "اضافة", "استرداد", "received", "credited", "refunded", "deposit", "added")
        val isIncome = incomeKeywords.any { body.contains(it, ignoreCase = true) }

        // Step 5: Detect Action Type
        val withdrawalKeywords = listOf("سحب", "withdrawal", "cash withdrawal", "withdrawn")
        val transferKeywords = listOf("تحويل", "transfer", "sent")
        val purchaseKeywords = listOf("شراء", "مدفوعات", "purchase", "paid", "payment", "bought")
        
        val actionType = when {
            isIncome -> SmsActionType.DEPOSIT
            withdrawalKeywords.any { body.contains(it, ignoreCase = true) } -> SmsActionType.WITHDRAWAL
            transferKeywords.any { body.contains(it, ignoreCase = true) } -> SmsActionType.TRANSFER_OUT
            purchaseKeywords.any { body.contains(it, ignoreCase = true) } -> SmsActionType.PURCHASE
            else -> SmsActionType.UNKNOWN
        }

        return ParsedSms(amount, merchant, date, isIncome, actionType)
    }
}
