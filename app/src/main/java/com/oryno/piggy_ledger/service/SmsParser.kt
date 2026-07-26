package com.oryno.piggy_ledger.service

data class ParsedSms(val amount: Double, val merchant: String, val date: String?, val isIncome: Boolean = false)

object SmsParser {
    fun convertArabicDigitsAndSymbols(input: String): String {
        var result = input
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        for (i in 0..9) {
            result = result.replace(arabicDigits[i], '0' + i)
        }
        result = result.replace('٫', '.')
        
        // Common text numbers that might appear or be transcribed
        val wordToNumber = mapOf(
            "واحد" to "1",
            "اثنين" to "2",
            "إثنين" to "2",
            "ثلاثة" to "3",
            "تلاتة" to "3",
            "اربعة" to "4",
            "أربعة" to "4",
            "خمسة" to "5",
            "ستة" to "6",
            "سبعة" to "7",
            "ثمانية" to "8",
            "تمانية" to "8",
            "تسعة" to "9",
            "عشرة" to "10",
            "عشره" to "10"
        )
        for ((word, number) in wordToNumber) {
            result = result.replace(word, number)
        }
        
        return result
    }

    fun parse(rawBody: String): ParsedSms {
        val body = convertArabicDigitsAndSymbols(rawBody)
        
        // Step 1: Parse Amount (English and Arabic currencies)
        val currencyTokens = """EGP|LE|L\.E\.|USD|\$|EUR|€|£|₩|AED|SAR|KWD|QAR|BHD|OMR|ج\.م|جنيه|جنيها|جنيهًا|ريال|درهم|دينار"""
        val amountRegex = Regex("""(?i)(?:$currencyTokens)\s*([\d,]+(?:\.\d{1,2})?)|([\d,]+(?:\.\d{1,2})?)\s*(?:$currencyTokens)|(?:amount|paid|transfer|balance|purchase|pay|سداد|دفع|تحويل|مبلغ|رصيد|رصيدك|مبلغ وقدره)(?:\s*is|:|\s*)\s*([\d,]+(?:\.\d{1,2})?)""")
        val amountMatch = amountRegex.find(body)
        
        val amountStr = amountMatch?.groups?.get(1)?.value 
            ?: amountMatch?.groups?.get(2)?.value 
            ?: amountMatch?.groups?.get(3)?.value
        val amount = amountStr?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        
        // Step 2: Extract Merchant (English and Arabic prepositions)
        val merchantRegex = Regex("""(?i)(?:at|to|from|في|من حسابك لدى|حسابك لدى|لدى|من|إلى)\s+([A-Za-z0-9\s\u0600-\u06FF]{2,50}?)(?:\s+on|\s+value|\.|\d|بتاريخ|$)""")
        val merchantMatch = merchantRegex.find(body)
        val merchant = merchantMatch?.groups?.get(1)?.value?.trim() ?: "Unknown SMS Merchant"

        // Step 3: Extract Date
        val dateRegex = Regex("""(\d{1,2}[/-]\d{1,2}([/-]\d{2,4})?)""")
        val dateMatch = dateRegex.find(body)
        val date = dateMatch?.value 

        // Step 4: Detect Income vs Expense
        val incomeKeywords = listOf("استقبلت", "ايداع", "إيداع", "إضافة", "اضافة", "استرداد", "received", "credited", "refunded", "deposit", "added")
        val isIncome = incomeKeywords.any { body.contains(it, ignoreCase = true) }

        return ParsedSms(amount, merchant, date, isIncome)
    }
}
