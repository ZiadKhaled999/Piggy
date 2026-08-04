package com.oryno.piggy_ledger.service

import android.content.Context
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.PendingTransaction
import com.oryno.piggy_ledger.data.Account

object SmsProcessor {
    private fun isPromotionalSms(body: String, sender: String): Boolean {
        val cleanSender = sender.lowercase()
        val cleanBody = body.lowercase()

        val promoSenders = listOf(
            "orange misr", "vodafone offers", "we offers", "etisalat offers",
            "1112", "8585", "8888"
        )
        if (promoSenders.any { cleanSender.contains(it) }) {
            return true
        }

        val promoKeywords = listOf(
            "offer", "promo", "promotion", "discount", "bonus", "campaign", "free", "cashback", "reward",
            "points", "coupon", "voucher", "exclusive", "limited time", "special offer", "enjoy", "celebrate",
            "win", "gift", "redemption", "subscribe", "opt out", "unsubscribe",
            "عرض", "عروض", "خصم", "خصومات", "كاش باك", "مكافأة", "نقاط", "كوبون", "هدية", "أحصل", "احصل",
            "فرصة", "ترقية", "كود", "رمز", "تخفيض", "بطاقة", "استمتع", "اشترك", "اكسب", "ربح", "مسابقة",
            "شروط", "أحكام", "وفرلي", "شحنة", "باقة", "وحدات", "تحويلية", "فيلوس", "اشترك واكسب", "gold", "شحن",
            "إلغاء", "الاشتراك", "قف", "stop"
        )
        if (promoKeywords.any { cleanBody.contains(it) }) {
            return true
        }

        val promoUrls = listOf(
            "myo.orange.eg", "web.vodafone.com.eg", "te.eg", "promo", "offer", "campaign"
        )
        if (promoUrls.any { cleanBody.contains(it) }) {
            return true
        }

        return false
    }

    suspend fun process(context: Context, sender: String, rawBody: String) {
        if (isPromotionalSms(rawBody, sender)) {
            android.util.Log.d("SmsProcessor", "Ignoring promotional SMS from $sender")
            return
        }

        val parsedSms = SmsParser.parse(rawBody)
        val amount = parsedSms.amount
        val merchant = parsedSms.merchant
        val isIncome = parsedSms.isIncome
        val actionType = parsedSms.actionType

        android.util.Log.d("SmsProcessor", "Transaction from/for: $merchant, Amount: $amount, Income: $isIncome")

        val db = PiggyLedgerDatabase.getInstance(context)
        val dao = db.piggyLedgerDao()

        if (amount == 0.0) {
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
                    .setSmallIcon(com.oryno.piggy_ledger.R.drawable.img_app_logo)
                    .setContentTitle("Failed to Parse SMS")
                    .setContentText("An SMS from $sender could not be parsed automatically. Tap to review.")
                    .setContentIntent(pendingIntent)
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            } catch (e: Exception) {
                android.util.Log.e("SmsProcessor", "Failed to show parse failure notification", e)
            }
            return
        }

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
            
            try {
                com.oryno.piggy_ledger.ui.NotificationHelper(context).showTransactionProcessedNotification(
                    accountName = matchedAccount.name,
                    currency = "EGP",
                    amount = amount,
                    actionType = actionType
                )
            } catch (e: Exception) {
                android.util.Log.e("SmsProcessor", "Failed to show processed notification", e)
            }
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

                val logoBitmap = try {
                    android.graphics.BitmapFactory.decodeResource(context.resources, com.oryno.piggy_ledger.R.drawable.img_app_logo)
                } catch (e: Exception) { null }

                val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(com.oryno.piggy_ledger.R.drawable.img_app_logo)
                    .apply { if (logoBitmap != null) setLargeIcon(logoBitmap) }
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
