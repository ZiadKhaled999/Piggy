package com.oryno.piggy_ledger.service

data class ParsedSms(val amount: Double, val merchant: String, val date: String?)

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
        
        // Step 1: Parse Amount (English and Arabic currencies)
        val amountRegex = Regex("""(?i)(?:EGP|LE|L\.E\.|USD|\$|EUR|£|ج\.م|جنيه|جنيها|جنيهًا)\s*([\d,]+(?:\.\d{1,2})?)|([\d,]+(?:\.\d{1,2})?)\s*(?:EGP|LE|L\.E\.|USD|\$|EUR|£|ج\.م|جنيه|جنيها|جنيهًا)""")
        val amountMatch = amountRegex.find(body)
        
        val amountStr = amountMatch?.groups?.get(1)?.value ?: amountMatch?.groups?.get(2)?.value
        val amount = amountStr?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        
        // Step 2: Extract Merchant (English and Arabic prepositions)
        val merchantRegex = Regex("""(?i)(?:at|to|from|في|من حسابك لدى|حسابك لدى|لدى|من|إلى)\s+([A-Za-z0-9\s\u0600-\u06FF]+?)(?:\s+on|\s+value|\.|\d|بتاريخ|$)""")
        val merchantMatch = merchantRegex.find(body)
        val merchant = merchantMatch?.groups?.get(1)?.value?.trim() ?: "Unknown SMS Merchant"

        // Step 3: Extract Date
        val dateRegex = Regex("""(\d{1,2}[/-]\d{1,2}([/-]\d{2,4})?)""")
        val dateMatch = dateRegex.find(body)
        val date = dateMatch?.value
 
        return ParsedSms(amount, merchant, date)
    }
}
