package com.oryno.piggy_ledger

import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.data.AccountType
import com.oryno.piggy_ledger.service.BankProviderRegistry
import com.oryno.piggy_ledger.service.SmsActionType
import com.oryno.piggy_ledger.service.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParsingAndMatchingTest {

    @Test
    fun testCibCardPurchaseSms() {
        val sms = "Purchase with card ending 1234 for EGP 250.00 at STARBUCKS on 2026-05-10. Available balance EGP 15,200.50"
        val parsed = SmsParser.parse(sms)

        assertEquals(250.0, parsed.amount, 0.01)
        assertEquals("STARBUCKS", parsed.merchant)
        assertFalse(parsed.isIncome)
        assertEquals(SmsActionType.PURCHASE, parsed.actionType)

        val cardTokens = BankProviderRegistry.extractCardOrAccountTokens(sms)
        assertTrue(cardTokens.contains("1234"))

        val account = Account(
            name = "CIB Titanium",
            type = AccountType.CARD,
            icon_color = "#3B82F6",
            currency = "EGP",
            starting_balance = 0.0,
            card_numbers = "1234",
            provider = "Commercial International Bank (CIB)"
        )

        assertTrue(BankProviderRegistry.matchesCardOrAccount(account, cardTokens, sms))
        assertTrue(BankProviderRegistry.matchesProviderOrAccount(account, "CIB-ALERT", sms, emptyList()))
    }

    @Test
    fun testNbeArabicDebitSms() {
        val sms = "تم خصم مبلغ 450.00 جم من حسابكم رقم **4321 طرف تاجر AMAZON، الرصيد المتاح 12500.00 جم"
        val parsed = SmsParser.parse(sms)

        assertEquals(450.0, parsed.amount, 0.01)
        assertEquals("AMAZON", parsed.merchant)
        assertFalse(parsed.isIncome)

        val tokens = BankProviderRegistry.extractCardOrAccountTokens(sms)
        assertTrue(tokens.contains("4321"))

        val nbeAccount = Account(
            name = "NBE Checking",
            type = AccountType.BANK,
            icon_color = "#005B41",
            currency = "EGP",
            starting_balance = 10000.0,
            bank_account_no = "123456784321",
            provider = "National Bank of Egypt (NBE)"
        )

        assertTrue(BankProviderRegistry.matchesCardOrAccount(nbeAccount, tokens, sms))
        assertTrue(BankProviderRegistry.matchesProviderOrAccount(nbeAccount, "AhlyBank", sms, emptyList()))
    }

    @Test
    fun testVodafoneCashTransferSms() {
        val sms = "تم تحويل 500.00 جنيه لرقم 01012345678. مصاريف الخدمة 5.00 جنيه. رصيدك الحالي هو 1200.00 جنيه."
        val parsed = SmsParser.parse(sms)

        assertEquals(500.0, parsed.amount, 0.01)
        assertFalse(parsed.isIncome)
        assertEquals(SmsActionType.TRANSFER_OUT, parsed.actionType)

        val vfAccount = Account(
            name = "My Vodafone Wallet",
            type = AccountType.WALLET,
            icon_color = "#E11D48",
            currency = "EGP",
            starting_balance = 1700.0,
            provider = "Vodafone Cash"
        )

        assertTrue(BankProviderRegistry.matchesProviderOrAccount(vfAccount, "VF-Cash", sms, emptyList()))
    }

    @Test
    fun testInstaPayIncomingSms() {
        val sms = "لقد استقبلت تحويل لحظي بمبلغ 1,500.00 جم من محمد حسن على حسابك في CIB"
        val parsed = SmsParser.parse(sms)

        assertEquals(1500.0, parsed.amount, 0.01)
        assertTrue(parsed.isIncome)
        assertEquals(SmsActionType.DEPOSIT, parsed.actionType)
        assertEquals("محمد حسن", parsed.merchant)

        val cibAccount = Account(
            name = "Main CIB Account",
            type = AccountType.BANK,
            icon_color = "#1E3A8A",
            currency = "EGP",
            starting_balance = 5000.0,
            provider = "Commercial International Bank (CIB)"
        )

        assertTrue(BankProviderRegistry.matchesProviderOrAccount(cibAccount, "InstaPay", sms, emptyList()))
    }
}
