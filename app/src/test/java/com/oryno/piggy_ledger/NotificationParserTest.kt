package com.oryno.piggy_ledger

import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.data.AccountType
import com.oryno.piggy_ledger.data.NotificationParserPrefs
import com.oryno.piggy_ledger.service.BankNotificationListenerService
import com.oryno.piggy_ledger.service.BankProviderRegistry
import com.oryno.piggy_ledger.service.SmsActionType
import com.oryno.piggy_ledger.service.SmsParser
import com.oryno.piggy_ledger.service.TransactionIngestor
import com.oryno.piggy_ledger.ui.inferCurrencyFromPendingBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationParserTest {

    @Test
    fun buildNotificationText_deduplicatesBigTextAndTitle() {
        val prefixed = BankNotificationListenerService.buildNotificationText(
            title = "Telda",
            text = "You spent EGP 145.00",
            bigText = "You spent EGP 145.00 at Talabat using card ending 8821",
            subText = "Telda"
        )
        assertEquals(
            "Telda | You spent EGP 145.00 at Talabat using card ending 8821",
            prefixed
        )

        val noRepeatTitle = BankNotificationListenerService.buildNotificationText(
            title = "Telda",
            text = "You spent EGP 145.00 at Talabat",
            bigText = "You spent EGP 145.00 at Talabat using your Telda card ending 8821",
            subText = "Telda"
        )
        assertEquals(
            "You spent EGP 145.00 at Talabat using your Telda card ending 8821",
            noRepeatTitle
        )
    }

    @Test
    fun isOneTimeCode_blocksOtpButAllowsTransactionsWithCurrency() {
        assertTrue(BankNotificationListenerService.isOneTimeCode("Your verification code is 482910. Do not share."))
        assertTrue(BankNotificationListenerService.isOneTimeCode("رمز التحقق الخاص بك هو 992104"))
        assertFalse(BankNotificationListenerService.isOneTimeCode("You paid EGP 1250 at Carrefour. Code: POS"))
        assertFalse(BankNotificationListenerService.isOneTimeCode("Spent $25.00 at Uber"))
        assertFalse(BankNotificationListenerService.isOneTimeCode("Payment of ₹1500 to Amazon"))
    }

    @Test
    fun hasTransactionIntent_filtersNonTransactionalAlerts() {
        assertFalse(TransactionIngestor.hasTransactionIntent("Your monthly statement is now ready to view"))
        assertFalse(TransactionIngestor.hasTransactionIntent("New device login detected on your account"))
        assertTrue(TransactionIngestor.hasTransactionIntent("You spent $18.50 at Starbucks"))
        assertTrue(TransactionIngestor.hasTransactionIntent("تم دفع 320 جنيه لدى فوري"))
    }

    @Test
    fun parsePushNotifications_globalCurrenciesAndArabic() {
        val revParsed = SmsParser.parse("You spent £42.75 at Pret A Manger")
        assertEquals(42.75, revParsed.amount, 0.01)
        assertFalse(revParsed.isIncome)
        assertEquals(SmsActionType.PURCHASE, revParsed.actionType)

        val usdIncome = SmsParser.parse("You received $250.00 from Alex")
        assertEquals(250.0, usdIncome.amount, 0.01)
        assertTrue(usdIncome.isIncome)
        assertEquals(SmsActionType.DEPOSIT, usdIncome.actionType)

        val arPush = SmsParser.parse("تم دفع 180.00 جنيه لدى طلبات")
        assertEquals(180.0, arPush.amount, 0.01)
        assertFalse(arPush.isIncome)
        assertEquals(SmsActionType.PURCHASE, arPush.actionType)
    }

    @Test
    fun resolveAccountPure_explicitLinkedPackageTakesPriorityOverAmbiguity() {
        val cib = Account(
            id = "acc-cib",
            name = "CIB Checking",
            type = AccountType.BANK,
            icon_color = "#1E3A8A",
            currency = "EGP",
            starting_balance = 5000.0,
            provider = "Commercial International Bank (CIB)"
        )
        val customService = Account(
            id = "acc-custom",
            name = "My Work Wallet",
            type = AccountType.WALLET,
            icon_color = "#0D9488",
            currency = "EGP",
            starting_balance = 1200.0,
            provider = "Custom Service"
        )

        val resolved = BankProviderRegistry.resolveAccountPure(
            allAccounts = listOf(cib, customService),
            sender = "Some App Label",
            rawBody = "You paid EGP 95.00 at Metro Market",
            actionType = SmsActionType.PURCHASE,
            notificationPackage = "com.custom.wallet",
            linkedAccountIds = listOf("acc-custom")
        )

        assertEquals("acc-custom", resolved?.id)
    }

    @Test
    fun resolveAccountPure_multipleLinkedAccountsDisambiguateByCardDigits() {
        val checking = Account(
            id = "acc-chk",
            name = "CIB Checking",
            type = AccountType.BANK,
            icon_color = "#1E3A8A",
            currency = "EGP",
            starting_balance = 5000.0,
            bank_account_no = "99001122",
            provider = "Commercial International Bank (CIB)"
        )
        val card = Account(
            id = "acc-card",
            name = "CIB Titanium",
            type = AccountType.CARD,
            icon_color = "#8B5CF6",
            currency = "EGP",
            starting_balance = 0.0,
            card_numbers = "5544",
            provider = "Commercial International Bank (CIB)"
        )

        val resolved = BankProviderRegistry.resolveAccountPure(
            allAccounts = listOf(checking, card),
            sender = "CIB Mobile",
            rawBody = "Purchase of EGP 350.00 on card ending 5544 at Carrefour",
            actionType = SmsActionType.PURCHASE,
            notificationPackage = "com.cib.cibmobile",
            linkedAccountIds = listOf("acc-chk", "acc-card")
        )

        assertEquals("acc-card", resolved?.id)
    }

    @Test
    fun resolveAccountPure_matchesGlobalProviderByPackageAndCustomKeywords() {
        val revolut = Account(
            id = "acc-rev",
            name = "Travel Card",
            type = AccountType.WALLET,
            icon_color = "#0D9488",
            currency = "EUR",
            starting_balance = 400.0,
            provider = "Revolut"
        )
        val localBank = Account(
            id = "acc-nbe",
            name = "NBE",
            type = AccountType.BANK,
            icon_color = "#005B41",
            currency = "EGP",
            starting_balance = 9000.0,
            provider = "National Bank of Egypt (NBE)"
        )

        val matchedByPackage = BankProviderRegistry.resolveAccountPure(
            allAccounts = listOf(revolut, localBank),
            sender = "Revolut",
            rawBody = "You spent €14.50 at Cafe",
            actionType = SmsActionType.PURCHASE,
            notificationPackage = "com.revolut.revolut"
        )
        assertEquals("acc-rev", matchedByPackage?.id)

        val matchedByKeyword = BankProviderRegistry.resolveAccountPure(
            allAccounts = listOf(revolut, localBank),
            sender = "MyCustomSender",
            rawBody = "Paid EGP 60.00",
            actionType = SmsActionType.PURCHASE,
            customKeywordsMap = mapOf("National Bank of Egypt (NBE)" to listOf("MyCustomSender"))
        )
        assertEquals("acc-nbe", matchedByKeyword?.id)

        val unmatched = BankProviderRegistry.resolveAccountPure(
            allAccounts = listOf(revolut, localBank),
            sender = "Unknown Bank",
            rawBody = "Paid EGP 60.00",
            actionType = SmsActionType.PURCHASE
        )
        assertNull(unmatched)
    }

    @Test
    fun providerScoringAndFinancialAppDetection() {
        assertEquals(
            100,
            BankProviderRegistry.scorePackageMatchForProvider("Telda", "app.telda", "Telda")
        )
        assertTrue(
            BankProviderRegistry.scorePackageMatchForProvider("PayPal", "com.paypal.android.p2pmobile", "PayPal") >= 90
        )
        assertTrue(BankProviderRegistry.isLikelyFinancialApp("com.google.android.apps.walletnfcrel", "Google Wallet"))
        assertTrue(BankProviderRegistry.isLikelyFinancialApp("com.unknown.mybank", "My Bank"))
        assertFalse(BankProviderRegistry.isLikelyFinancialApp("com.spotify.music", "Spotify"))
    }

    @Test
    fun packageAccountsJsonRoundTrip() {
        val original = mapOf(
            "app.telda" to listOf("acc-1"),
            "com.cib.cibmobile" to listOf("acc-2", "acc-3")
        )
        val encoded = NotificationParserPrefs.encodePackageAccounts(original)
        val decoded = NotificationParserPrefs.decodePackageAccounts(encoded)
        assertEquals(original, decoded)
        assertEquals(emptyMap<String, List<String>>(), NotificationParserPrefs.decodePackageAccounts("{bad-json"))
    }

    @Test
    fun inferCurrencyFromPendingBody_usesMessageCurrencyOrFallback() {
        assertEquals("USD", inferCurrencyFromPendingBody("You spent $19.99 at Apple", "EGP"))
        assertEquals("EUR", inferCurrencyFromPendingBody("Paid EUR 45.00 at Zara", "EGP"))
        assertEquals("SAR", inferCurrencyFromPendingBody("تم خصم 120 ريال سعودي", "EGP"))
        assertEquals("GBP", inferCurrencyFromPendingBody("No currency token", "GBP"))
    }
}
