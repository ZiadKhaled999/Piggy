package com.oryno.piggy_ledger.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    // Default static whitelist of all 40 Egyptian banks, 4 e-wallets, and national instant payment providers
    private val defaultProviders = listOf(
        // 4 E-Wallets
        "VF-Cash",
        "OrangeCash",
        "e&Cash",
        "WEPay",

        // 40 Egyptian Banks & Licensed Financial Institutions
        "NBE", "NationalBankOfEgypt", "NBEg",                 // 1. National Bank of Egypt
        "BanqueMisr", "BM",                                    // 2. Banque Misr
        "CIB", "CIBEgypt",                                     // 3. Commercial International Bank
        "BanqueDuCaire", "BDC",                                // 4. Banque du Caire
        "QNB", "QNBAlahli",                                    // 5. QNB Alahli
        "AlexBank",                                            // 6. Bank of Alexandria
        "HSBC", "HSBCEgypt",                                   // 7. HSBC Egypt
        "Faisal", "FaisalBank",                                // 8. Faisal Islamic Bank of Egypt
        "AAIB",                                                // 9. Arab African International Bank
        "ADIB", "ADIBEgypt",                                   // 10. Abu Dhabi Islamic Bank
        "CreditAgricole", "CAE",                               // 11. Crédit Agricole Egypt
        "EmiratesNBD", "ENBD",                                 // 12. Emirates NBD Egypt
        "HDB", "HousingDevelopmentBank",                       // 13. Housing & Development Bank
        "EGBank", "EGB",                                       // 14. EG Bank (Egyptian Gulf Bank)
        "SAIB", "SAIBBank",                                    // 15. SAIB Bank
        "AlBaraka", "ABG",                                     // 16. Al Baraka Bank Egypt
        "Attijariwafa", "AWB",                                 // 17. Attijariwafa Bank Egypt
        "ArabBank",                                            // 18. Arab Bank Egypt
        "ADCB", "ADCBEgypt",                                   // 19. Abu Dhabi Commercial Bank
        "EBank", "EDBE",                                       // 20. Export Development Bank of Egypt
        "UnitedBank", "UB",                                    // 21. The United Bank
        "SuezCanal", "SCB",                                    // 22. Suez Canal Bank
        "Mashreq", "MashreqBank",                              // 23. Mashreq Bank Egypt
        "Citibank", "Citi",                                    // 24. Citibank Egypt
        "FAB", "FABEgypt", "BankAudi",                         // 25. First Abu Dhabi Bank (FAB)
        "ABK", "ABKEgypt",                                     // 26. Al Ahli Bank of Kuwait
        "NBK", "NBKEgypt",                                     // 27. National Bank of Kuwait
        "BankABC", "ABCBank",                                  // 28. Bank ABC Egypt
        "aiBank", "AIBank",                                    // 29. aiBank (Arab Investment Bank)
        "MIDBANK", "MDB",                                      // 30. MIDBANK
        "AgriculturalBank", "EAB", "PBDAC",                    // 31. Egyptian Agricultural Bank
        "IDB", "IDBEgypt",                                     // 32. Industrial Development Bank
        "AIB", "ArabIntBank",                                  // 33. Arab International Bank
        "BlomBank", "Blom",                                    // 34. Blom Bank Egypt
        "StandardChartered", "StanChart",                      // 35. Standard Chartered Bank Egypt
        "NasserBank",                                          // 36. Nasser Social Bank
        "REEB", "RealEstateBank",                              // 37. Egyptian Real Estate Bank
        "Piraeus", "PiraeusBank",                              // 38. Piraeus Bank Egypt
        "CBE",                                                 // 39. Central Bank of Egypt
        "InstaPay", "SmartWallet", "Telda", "Nexta"            // 40. National Instant Payment Switch (InstaPay) & Financial Apps
    )

    private fun normalize(token: String): List<String> {
        val clean = token.trim()
        val noSpaces = clean.replace(" ", "").replace("-", "").replace("_", "")
        val words = clean.split(" ", "-", "_").filter { it.length >= 2 }
        return listOf(clean, noSpaces) + words
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val sender = messages[0]?.originatingAddress ?: return
            val body = messages.mapNotNull { it?.messageBody }.joinToString("")
            if (body.isBlank()) return

            Log.d("SmsReceiver", "Received SMS from: $sender, Body: $body")

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = PiggyLedgerDatabase.getInstance(context)
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
                    val userPrefs = UserPreferences(context)
                    val customJsonStr = userPrefs.customIdentifiersJson.firstOrNull() ?: "{}"
                    val customKeywordsList = mutableListOf<String>()
                    try {
                        val customMap = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<Map<String, List<String>>>(customJsonStr)
                        customMap.values.forEach { customKeywordsList.addAll(it) }
                    } catch (e: Exception) {
                        Log.e("SmsReceiver", "Error parsing custom keywords in SmsReceiver", e)
                    }

                    // Build comprehensive list of keywords to match sender against
                    val allTrustedTokens = (defaultProviders + dynamicIdentifiers + customKeywordsList).flatMap { normalize(it) }.distinct()

                    val cleanSender = sender.replace(" ", "").replace("-", "").replace("_", "")

                    val isTrusted = allTrustedTokens.any { token ->
                        cleanSender.contains(token, ignoreCase = true) ||
                        sender.contains(token, ignoreCase = true)
                    }

                    if (isTrusted) {
                        Log.d("SmsReceiver", "Sender $sender is verified as trusted. Processing SMS...")
                        SmsProcessor.process(context, sender, body)
                    } else {
                        Log.d("SmsReceiver", "SMS from $sender ignored (not matched in trusted providers or user accounts)")
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error processing incoming SMS in receiver", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

