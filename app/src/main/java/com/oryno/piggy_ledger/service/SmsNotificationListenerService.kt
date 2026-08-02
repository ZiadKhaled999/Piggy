package com.oryno.piggy_ledger.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SmsNotificationListenerService : NotificationListenerService() {

    // Common SMS apps packages
    private val messagingApps = listOf(
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
        "com.sonyericsson.conversations",
        "com.htc.sense.mms"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        
        // Only process notifications from known SMS apps
        if (!messagingApps.contains(packageName)) return

        val extras = sbn.notification.extras
        val sender = extras.getString(Notification.EXTRA_TITLE) ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

        // Sometimes the text might be empty or we are getting a summary notification
        if (text.isBlank()) return

        Log.d("SmsNotifListener", "Intercepted SMS notification from: $sender, Body: $text")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PiggyLedgerDatabase.getInstance(applicationContext)
                val accounts = db.piggyLedgerDao().getAllAccountsSync()

                // Gather dynamic identifiers from user's configured accounts
                val dynamicIdentifiers = mutableListOf<String>()
                accounts.forEach { account ->
                    account.provider?.takeIf { it.isNotBlank() }?.let { dynamicIdentifiers.add(it) }
                    account.name.takeIf { it.isNotBlank() }?.let { dynamicIdentifiers.add(it) }
                    account.label?.takeIf { it.isNotBlank() }?.let { dynamicIdentifiers.add(it) }
                    account.card_numbers?.takeIf { it.isNotBlank() }?.let { dynamicIdentifiers.add(it) }
                    account.bank_account_no?.takeIf { it.isNotBlank() }?.let { dynamicIdentifiers.add(it) }
                }

                // Gather user's custom keywords from UserPreferences
                val userPrefs = UserPreferences(applicationContext)
                val customJsonStr = userPrefs.customIdentifiersJson.firstOrNull() ?: "{}"
                val customKeywordsList = mutableListOf<String>()
                try {
                    val customMap = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<Map<String, List<String>>>(customJsonStr)
                    customMap.values.forEach { customKeywordsList.addAll(it) }
                } catch (e: Exception) {
                    Log.e("SmsNotifListener", "Error parsing custom keywords", e)
                }

                // Default providers copied from SmsReceiver
                val defaultProviders = listOf(
                    "VF-Cash", "OrangeCash", "e&Cash", "WEPay",
                    "NBE", "NationalBankOfEgypt", "NBEg", "BanqueMisr", "BM",
                    "CIB", "CIBEgypt", "BanqueDuCaire", "BDC", "QNB", "QNBAlahli",
                    "AlexBank", "HSBC", "HSBCEgypt", "Faisal", "FaisalBank",
                    "AAIB", "ADIB", "ADIBEgypt", "CreditAgricole", "CAE",
                    "EmiratesNBD", "ENBD", "HDB", "HousingDevelopmentBank",
                    "EGBank", "EGB", "SAIB", "SAIBBank", "AlBaraka", "ABG",
                    "Attijariwafa", "AWB", "ArabBank", "ADCB", "ADCBEgypt",
                    "EBank", "EDBE", "UnitedBank", "UB", "SuezCanal", "SCB",
                    "Mashreq", "MashreqBank", "Citibank", "Citi",
                    "FAB", "FABEgypt", "BankAudi", "ABK", "ABKEgypt",
                    "NBK", "NBKEgypt", "BankABC", "ABCBank", "aiBank", "AIBank",
                    "MIDBANK", "MDB", "AgriculturalBank", "EAB", "PBDAC",
                    "IDB", "IDBEgypt", "AIB", "ArabIntBank", "BlomBank", "Blom",
                    "StandardChartered", "StanChart", "NasserBank", "REEB", "RealEstateBank",
                    "Piraeus", "PiraeusBank", "CBE", "InstaPay", "SmartWallet", "Telda", "Nexta"
                )

                val allTrustedTokens = (defaultProviders + dynamicIdentifiers + customKeywordsList).flatMap { normalize(it) }.distinct()

                val cleanSender = sender.replace(" ", "").replace("-", "").replace("_", "")

                val isTrusted = allTrustedTokens.any { token ->
                    cleanSender.contains(token, ignoreCase = true) ||
                    sender.contains(token, ignoreCase = true)
                }

                if (isTrusted) {
                    Log.d("SmsNotifListener", "Sender $sender is verified as trusted. Processing SMS...")
                    SmsProcessor.process(applicationContext, sender, text)
                } else if (text.contains("EGP", ignoreCase = true) || text.contains("جنيه") || text.contains("تحويل") || text.contains("purchase", ignoreCase = true) || text.contains("LE", ignoreCase = true)) {
                    Log.d("SmsNotifListener", "Sender $sender not in whitelist, but contains financial keywords. Processing as pending...")
                    SmsProcessor.process(applicationContext, sender, text)
                } else {
                    Log.d("SmsNotifListener", "SMS from $sender ignored (no financial keywords)")
                }
            } catch (e: Exception) {
                Log.e("SmsNotifListener", "Error processing incoming SMS notification", e)
            }
        }
    }
    
    private fun normalize(token: String): List<String> {
        val clean = token.trim()
        val noSpaces = clean.replace(" ", "").replace("-", "").replace("_", "")
        val words = clean.split(" ", "-", "_").filter { it.length >= 2 }
        return listOf(clean, noSpaces) + words
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Not used
    }
}
