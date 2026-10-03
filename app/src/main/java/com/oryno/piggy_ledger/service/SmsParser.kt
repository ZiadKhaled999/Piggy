package com.oryno.piggy_ledger.service

data class ParsedSms(
    val amount: Double,
    val merchant: String,
    val date: String?,
    val isIncome: Boolean = false,
    val actionType: SmsActionType = SmsActionType.UNKNOWN
)

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

        // 1. Try transaction keywords with optional connectors (مبلغ, بقيمة, for, of, etc.) followed by optional currency and number
        var amountMatch = Regex("""(?i)(?:amount|paid|purchase|pay|debited|credited|transfer|withdrawn|سداد|دفع|مبلغ|بمبلغ|بقيمة|قيمة|تحويل|تم تحويل|استقبلت|استلام|تم استلام|خصم|تم خصم|حركة خصم|سحب|تم سحب|شراء|إيداع|ايداع)(?:\s*(?:is|of|for|with|value|مبلغ|بقيمة|بمبلغ|قيمة|:)\s*|\s+)(?:(?:$currencyTokens)\s*)?([\d,،]+(?:\.\d{1,2})?)""").find(body)

        // 2. Try currency-prefixed or currency-suffixed numbers
        if (amountMatch == null) {
            amountMatch = Regex("""(?i)(?:$currencyTokens)\s*([\d,،]+(?:\.\d{1,2})?)|([\d,،]+(?:\.\d{1,2})?)\s*(?:$currencyTokens)""").find(body)
        }

        // 3. Try balance / for / of as fallback
        if (amountMatch == null) {
            amountMatch = Regex("""(?i)(?:for|of|balance|رصيد|رصيدك)(?:\s*is|:|\s*)\s*([\d,،]+(?:\.\d{1,2})?)""").find(body)
        }

        // 4. Fallback: standard currency decimal pattern (e.g. 1500.00)
        if (amountMatch == null) {
            amountMatch = Regex("""\b([\d,،]{1,9}\.\d{1,2})\b""").find(body)
        }

        val amountStr = amountMatch?.groups?.get(1)?.value
            ?: amountMatch?.groups?.get(2)?.value
            ?: amountMatch?.groups?.get(3)?.value

        // Clean the amount string – remove commas, Arabic commas, appended dates
        val cleanAmountStr = amountStr
            ?.replace(",", "")
            ?.replace("،", "")
            ?.replace(Regex("""\s+\d{1,2}/\d{1,2}/\d{2,4}.*"""), "")
            ?.trim()
        val amount = cleanAmountStr?.toDoubleOrNull() ?: 0.0

        // ===== ACTION TYPE DETECTION =====
        val actionType = when {
            // Incoming InstaPay
            body.contains("استقبلت تحويل لحظي", ignoreCase = true) ||
            body.contains("لقد استقبلت", ignoreCase = true) ||
            body.contains("تم استلام", ignoreCase = true) -> SmsActionType.DEPOSIT

            // Outgoing InstaPay / Transfer
            body.contains("قمت بتحويل لحظي", ignoreCase = true) ||
            body.contains("تم تحويل", ignoreCase = true) ||
            body.contains("transfer", ignoreCase = true) -> SmsActionType.TRANSFER_OUT

            // WITHDRAWAL (ATM cash)
            body.contains("سحب", ignoreCase = true) ||
            body.contains("withdrawal", ignoreCase = true) ||
            body.contains("cash withdrawal", ignoreCase = true) -> SmsActionType.WITHDRAWAL

            // PURCHASE (card payments / shopping)
            body.contains("شراء", ignoreCase = true) ||
            body.contains("purchase", ignoreCase = true) ||
            body.contains("paid", ignoreCase = true) ||
            body.contains("pos", ignoreCase = true) -> SmsActionType.PURCHASE

            else -> SmsActionType.UNKNOWN
        }

        // ===== INCOME vs EXPENSE DETECTION =====
        var isIncome = false

        // Check for DEBIT/EXPENSE patterns FIRST
        if (body.contains("تم خصم", ignoreCase = true) ||
            body.contains("حركة خصم", ignoreCase = true) ||
            body.contains("خصم", ignoreCase = true) ||
            body.contains("سحب", ignoreCase = true) ||
            body.contains("debited", ignoreCase = true) ||
            body.contains("withdrawn", ignoreCase = true) ||
            body.contains("قمت بتحويل", ignoreCase = true) ||
            actionType == SmsActionType.PURCHASE ||
            actionType == SmsActionType.WITHDRAWAL) {
            isIncome = false
        }
        // Then check for INCOME patterns
        else if (body.contains("استقبلت تحويل لحظي", ignoreCase = true) ||
                 body.contains("لقد استقبلت", ignoreCase = true) ||
                 body.contains("تم اضافة", ignoreCase = true) ||
                 body.contains("تم إضافة", ignoreCase = true) ||
                 body.contains("ايداع", ignoreCase = true) ||
                 body.contains("إيداع", ignoreCase = true) ||
                 body.contains("credited", ignoreCase = true) ||
                 body.contains("deposit", ignoreCase = true) ||
                 body.contains("لحسابكم", ignoreCase = true) ||
                 body.contains("لحسابك", ignoreCase = true)) {
            isIncome = true
        } else {
            // Fallback to keyword list
            val incomeKeywords = listOf("استقبلت", "ايداع", "إيداع", "إضافة", "اضافة", "استرداد", "received", "credited", "refunded", "deposit", "added")
            isIncome = incomeKeywords.any { body.contains(it, ignoreCase = true) }
        }

        // ===== MERCHANT / PERSON NAME EXTRACTION =====
        var merchant = ""

        // 1. Try Egyptian merchant prepositions (طرف تاجر, طرف, لدى, at, to)
        val merchantRegex = Regex("""(?i)(?:طرف تاجر|طرف|تاجر|لدى|\bat\b|\bto\b|\bfrom\b|إلى|الي)\s*:?\s*([A-Za-z0-9\s\u0600-\u06FF\-.',&]{2,40}?)(?:\s+on\b|\s+value\b|\.|,|،|\s+بتاريخ|\s+يوم|\d{1,2}/\d{1,2}|\$|\s*الرصيد|\s*رصيدك|\s*available|$)""")
        val merchantMatch = merchantRegex.find(body)
        merchant = merchantMatch?.groups?.get(1)?.value?.trim()?.trimEnd(',', '،', '.', '-', ' ', ':', '/') ?: ""

        // Clean merchant if it captured boilerplate bank strings
        if (merchant.contains("حسابك") || merchant.contains("فروع البنك") || merchant.contains("ماكينة")) {
            merchant = ""
        }

        // 2. Channel from "/ ATM" or "/ POS" pattern
        if (merchant.isBlank()) {
            val channelRegex = Regex("""/\s*([A-Za-z0-9\s]{2,20})(?:\s*\(|$)""")
            val channelMatch = channelRegex.find(body)
            if (channelMatch != null) {
                merchant = channelMatch.groups[1]?.value?.trim() ?: ""
            }
        }

        // 3. InstaPay transfers with "من" or "إلى" for person names
        if (merchant.isBlank()) {
            val instaPayNameRegex = Regex("""(?:من|إلى|الي)\s+([\u0600-\u06FF\s]{3,40}?)(?:\s+يوم|\s+في|\s+رقم|\s+عبر|\s+على)""")
            val instaPayMatch = instaPayNameRegex.find(body)
            if (instaPayMatch != null) {
                merchant = instaPayMatch.groups[1]?.value?.trim() ?: ""
            }
        }

        // If ATM withdrawal
        if (merchant.isBlank() && (actionType == SmsActionType.WITHDRAWAL || body.contains("ATM", ignoreCase = true))) {
            merchant = "ATM Cash Withdrawal"
        }

        // Default fallback
        if (merchant.isBlank()) {
            merchant = if (isIncome) "Incoming Transfer" else "Card / Bank Transaction"
        }

        // ===== DATE EXTRACTION =====
        val dateRegex = Regex("""(\d{1,2}[/-]\d{1,2}([/-]\d{2,4})?)""")
        val dateMatch = dateRegex.find(body)
        val date = dateMatch?.value

        return ParsedSms(amount, merchant, date, isIncome, actionType)
    }
}
