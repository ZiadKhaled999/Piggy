package com.oryno.piggy_ledger.service

import android.content.Context
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.PendingTransaction
import com.oryno.piggy_ledger.data.Account

object SmsProcessor {
    suspend fun process(context: Context, sender: String, rawBody: String) {
        val parsedSms = SmsParser.parse(rawBody)
        val amount = parsedSms.amount
        val merchant = parsedSms.merchant
        val isIncome = parsedSms.isIncome

        if (amount == 0.0) {
            return
        }

        val db = PiggyLedgerDatabase.getInstance(context)
        val dao = db.piggyLedgerDao()
        val body = SmsParser.convertArabicDigitsAndSymbols(rawBody)

        val accounts = dao.getAllAccountsSync()

        val uniqueMatches = accounts.filter { account ->
            val hasCard = account.card_numbers?.let { it.isNotBlank() && body.contains(it) } == true
            val hasBank = account.bank_account_no?.let { it.isNotBlank() && body.contains(it) } == true
            hasCard || hasBank
        }

        val providerMatches = accounts.filter { account ->
            val cleanSender = sender.replace(" ", "").replace("-", "").lowercase()
            val cleanBody = body.lowercase()
            val provMatch = account.provider?.takeIf { it.isNotBlank() }?.let { prov ->
                val cleanProv = prov.replace(" ", "").lowercase()
                cleanSender.contains(cleanProv) ||
                cleanProv.contains(cleanSender) ||
                cleanBody.contains(prov.lowercase()) ||
                cleanBody.contains(cleanProv)
            } ?: false
            val nameMatch = account.name.takeIf { it.isNotBlank() }?.let { name ->
                val cleanName = name.replace(" ", "").lowercase()
                cleanSender.contains(cleanName) ||
                cleanName.contains(cleanSender) ||
                cleanBody.contains(name.lowercase()) ||
                cleanBody.contains(cleanName)
            } ?: false
            val labelMatch = account.label?.takeIf { it.isNotBlank() }?.let { label ->
                val cleanLabel = label.replace(" ", "").lowercase()
                cleanSender.contains(cleanLabel) ||
                cleanBody.contains(label.lowercase())
            } ?: false
            provMatch || nameMatch || labelMatch
        }

        val matchedAccount: Account? = when {
            uniqueMatches.size == 1 -> uniqueMatches.first()
            uniqueMatches.isEmpty() && providerMatches.size == 1 -> providerMatches.first()
            accounts.size == 1 -> accounts.first()
            else -> null
        }

        if (matchedAccount != null) {
            dao.processSmsTransaction(
                accountId = matchedAccount.id,
                amount = amount,
                merchant = merchant,
                applyInstaPayFee = matchedAccount.insta_pay_fee,
                isIncome = isIncome
            )
        } else {
            dao.insertPendingTransaction(
                PendingTransaction(
                    amount = if (isIncome) amount else -amount,
                    merchant = merchant,
                    raw_sms_body = rawBody,
                    sender = sender
                )
            )

            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val channelId = "pending_transactions_channel"
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val channel = android.app.NotificationChannel(
                        channelId,
                        "Pending Transactions",
                        android.app.NotificationManager.IMPORTANCE_DEFAULT
                    )
                    notificationManager.createNotificationChannel(channel)
                }

                val intent = android.content.Intent(context, com.oryno.piggy_ledger.MainActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent: android.app.PendingIntent = android.app.PendingIntent.getActivity(
                    context, System.currentTimeMillis().toInt(), intent, android.app.PendingIntent.FLAG_IMMUTABLE
                )

                val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("New Pending Transaction")
                    .setContentText("A transaction of $amount from $merchant needs your review.")
                    .setContentIntent(pendingIntent)
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            } catch (e: Exception) {
                android.util.Log.e("SmsProcessor", "Failed to show notification", e)
            }
        }
    }
}
