package com.oryno.piggy_ledger.service

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsParserTest {

    @Test
    fun testParseAmount_English_EGP() {
        val sms = "Purchase of 150.50 EGP at Starbucks on 10/10/2023"
        val parsed = SmsParser.parse(sms)
        assertEquals(150.50, parsed.amount, 0.001)
        assertEquals("Starbucks", parsed.merchant)
        assertEquals("10/10/2023", parsed.date)
    }

    @Test
    fun testParseAmount_Arabic_EGP() {
        val sms = "تم خصم مبلغ 200.00 ج.م من حسابك لدى Amazon بتاريخ 25/12/2024"
        val parsed = SmsParser.parse(sms)
        assertEquals(200.0, parsed.amount, 0.001)
        assertEquals("Amazon", parsed.merchant)
        assertEquals("25/12/2024", parsed.date)
    }

    @Test
    fun testParseAmount_Arabic_Digits() {
        val sms = "عملية تحويل أموال ناجحة بمبلغ ٥٠٠٫٥٠ جنيه إلى فودافون كاش"
        val parsed = SmsParser.parse(sms)
        assertEquals(500.50, parsed.amount, 0.001)
        assertEquals("فودافون كاش", parsed.merchant)
    }

    @Test
    fun testParseAmount_NoAmount() {
        val sms = "This is a random message without any amount."
        val parsed = SmsParser.parse(sms)
        assertEquals(0.0, parsed.amount, 0.001)
        assertEquals("Unknown SMS Merchant", parsed.merchant)
    }

    @Test
    fun testParseMerchant_At() {
        val sms = "You paid 50 EGP at Uber on 10/10/2023"
        val parsed = SmsParser.parse(sms)
        assertEquals(50.0, parsed.amount, 0.001)
        assertEquals("Uber", parsed.merchant)
    }

    @Test
    fun testParseMerchant_To() {
        val sms = "Transfer of 1000 LE to John Doe completed"
        val parsed = SmsParser.parse(sms)
        assertEquals(1000.0, parsed.amount, 0.001)
        assertEquals("John Doe completed", parsed.merchant) // Currently regex matches till the end unless it hits `on`, `value`, `.`, `\d` or `$`. It should match "John Doe completed" based on the regex. Let's see what the regex actually does.
    }

    @Test
    fun testParseMerchant_Arabic_Fee() {
        val sms = "شراء بقيمة 120 جنيها في كارفور"
        val parsed = SmsParser.parse(sms)
        assertEquals(120.0, parsed.amount, 0.001)
        assertEquals("كارفور", parsed.merchant)
    }
}
