package com.oryno.piggy_ledger.service

import android.content.Context
import android.util.Log
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.data.PendingTransaction
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.ui.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TransactionIngestor {

    const val SOURCE_SMS = "AUTOMATIC_SMS"
    const val SOURCE_NOTIFICATION = "AUTOMATIC_NOTIFICATION"

    suspend fun ingest(
        context: Context,
        senderLabel: String,
        rawBody: String,
        source: String,
        sourceKey: String? = null,
        notificationPackage: String? = null
    ) = withContext(Dispatchers.IO) {
        if (isPromotional(rawBody, senderLabel)) {
            Log.d("TransactionIngestor", "Ignoring promotional content from $senderLabel")
            return@withContext
        }

        val parsed = SmsParser.parse(rawBody)
        val amount = parsed.amount
        val merchant = parsed.merchant
        val isIncome = parsed.isIncome
        val actionType = parsed.actionType

        Log.d(
            "TransactionIngestor",
            "Ingesting $source — Sender: $senderLabel, Merchant: $merchant, " +
                "Amount: $amount, Income: $isIncome, Action: $actionType"
        )

        val dao = PiggyLedgerDatabase.getInstance(context).piggyLedgerDao()

        if (amount == 0.0) {
            dao.insertPendingTransaction(
                PendingTransaction(
                    amount = 0.0,
                    merchant = merchant,
                    raw_sms_body = rawBody,
                    sender = senderLabel,
                    source = source,
                    notificationPackage = notificationPackage,
                    sourceKey = sourceKey,
                    reference = parsed.reference,
                    needsReview = true
                )
            )
            showUnparsedNotification(context, senderLabel)
            return@withContext
        }

        val matchedAccount: Account? = BankProviderRegistry.resolveAccount(
            context = context,
            sender = senderLabel,
            rawBody = rawBody,
            actionType = actionType
        )

        val isInstaPay = senderLabel.contains("instapay", ignoreCase = true) ||
            rawBody.contains("instapay", ignoreCase = true) ||
            rawBody.contains("انستاباي")

        val dedup = DuplicateDetector.evaluate(
            context = context,
            incoming = DuplicateDetector.IncomingRow(
                amount = amount,
                isIncome = isIncome,
                merchant = merchant,
                timestamp = System.currentTimeMillis(),
                source = source,
                sourceKey = sourceKey,
                rawText = rawBody
            )
        )

        when (dedup.action) {
            DuplicateDetector.Action.DROP_EXACT -> {
                Log.d("TransactionIngestor", "Dropped exact repost: $sourceKey")
                return@withContext
            }

            DuplicateDetector.Action.INSERT_FLAGGED -> {
                dao.insertPendingTransactionReturningId(
                    PendingTransaction(
                        amount = if (isIncome) amount else -amount,
                        merchant = merchant,
                        raw_sms_body = rawBody,
                        sender = senderLabel,
                        source = source,
                        notificationPackage = notificationPackage,
                        sourceKey = sourceKey,
                        reference = parsed.reference,
                        needsReview = true,
                        duplicateGroupId = dedup.groupId
                    )
                )
                dedup.matchedRowId?.let { existingId ->
                    dao.flagDuplicate(
                        table = dedup.matchedTable ?: "",
                        id = existingId,
                        groupId = dedup.groupId
                    )
                }
                Log.d("TransactionIngestor", "Flagged duplicate group ${dedup.groupId}")
                return@withContext
            }

            DuplicateDetector.Action.MERGE -> {
                val matchedTable = dedup.matchedTable ?: ""
                val matchedId = dedup.matchedRowId

                if (matchedTable == "pending_transactions" && matchedAccount != null) {
                    val newAccountTxnId = dao.processSmsTransaction(
                        accountId = matchedAccount.id,
                        amount = amount,
                        merchant = merchant,
                        applyInstaPayFee = matchedAccount.insta_pay_fee && isInstaPay,
                        isIncome = isIncome,
                        source = source,
                        sourceKey = sourceKey,
                        reference = parsed.reference
                    )
                    matchedId?.let { dao.flagPendingDuplicate(it, dedup.groupId) }
                    dao.flagAccountDuplicate(newAccountTxnId, dedup.groupId)
                    return@withContext
                }

                matchedId?.let {
                    dao.enrichFromDuplicate(
                        table = matchedTable,
                        id = it,
                        reference = parsed.reference,
                        rawText = rawBody
                    )
                }
                Log.d("TransactionIngestor", "Merged into existing row ${dedup.matchedRowId}")
                return@withContext
            }

            DuplicateDetector.Action.INSERT -> Unit
        }

        if (matchedAccount != null) {
            dao.processSmsTransaction(
                accountId = matchedAccount.id,
                amount = amount,
                merchant = merchant,
                applyInstaPayFee = matchedAccount.insta_pay_fee && isInstaPay,
                isIncome = isIncome,
                source = source,
                sourceKey = sourceKey,
                reference = parsed.reference
            )

            try {
                NotificationHelper(context).showTransactionProcessedNotification(
                    accountName = matchedAccount.name,
                    currency = matchedAccount.currency.ifBlank { "EGP" },
                    amount = amount,
                    actionType = actionType
                )
            } catch (e: Exception) {
                Log.e("TransactionIngestor", "Failed to show processed notification", e)
            }
        } else {
            Log.d("TransactionIngestor", "No account match for $senderLabel → pending")
            dao.insertPendingTransaction(
                PendingTransaction(
                    amount = if (isIncome) amount else -amount,
                    merchant = merchant,
                    raw_sms_body = rawBody,
                    sender = senderLabel,
                    source = source,
                    notificationPackage = notificationPackage,
                    sourceKey = sourceKey,
                    reference = parsed.reference
                )
            )
        }

    }

    private fun isPromotional(body: String, sender: String): Boolean {
        val cleanSender = sender.lowercase()
        val cleanBody = body.lowercase()

        val promoSenders = listOf(
            "orange misr", "vodafone offers", "we offers", "etisalat offers",
            "1112", "8585", "8888"
        )
        if (promoSenders.any { cleanSender.contains(it) }) return true

        val promoKeywords = listOf(
            "offer", "promo", "promotion", "discount", "bonus", "campaign",
            "free", "cashback", "reward", "points", "coupon", "voucher",
            "exclusive", "limited time", "special offer", "enjoy", "celebrate",
            "win", "gift", "redemption", "subscribe", "opt out", "unsubscribe",
            "عرض", "عروض", "خصم", "خصومات", "كاش باك", "مكافأة", "نقاط",
            "كوبون", "هدية", "أحصل", "احصل", "فرصة", "ترقية", "كود", "رمز",
            "تخفيض", "بطاقة", "استمتع", "اشترك", "اكسب", "ربح", "مسابقة",
            "شروط", "أحكام", "وفرلي", "شحنة", "باقة", "وحدات", "تحويلية",
            "فيلوس", "اشترك واكسب", "gold", "شحن", "إلغاء", "الاشتراك", "قف", "stop"
        )

        val financialIndicators = listOf(
            "تم خصم", "حركة خصم", "تم تحويل", "قمت بتحويل", "استقبلت",
            "تم سحب", "شراء بمبلغ", "debited", "credited", "purchase with",
            "transfer of", "withdrawn"
        )

        val hasFinancialIndicator = financialIndicators.any { cleanBody.contains(it) }
        if (!hasFinancialIndicator && promoKeywords.any { cleanBody.contains(it) }) {
            return true
        }

        val promoUrls = listOf("myo.orange.eg", "web.vodafone.com.eg", "te.eg")
        if (!hasFinancialIndicator && promoUrls.any { cleanBody.contains(it) }) {
            return true
        }

        return false
    }

    private fun showUnparsedNotification(context: Context, sender: String) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
            val channelId = "pending_transactions_channel"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Pending Transactions",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                )
                nm.createNotificationChannel(channel)
            }
            val intent = android.content.Intent(
                context,
                com.oryno.piggy_ledger.MainActivity::class.java
            ).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pi = android.app.PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val notification = androidx.core.app.NotificationCompat
                .Builder(context, channelId)
                .setSmallIcon(R.drawable.img_app_logo)
                .setContentTitle(context.getString(R.string.sms_notification_unparsed_title))
                .setContentText(context.getString(R.string.sms_notification_unparsed_desc, sender))
                .setContentIntent(pi)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            nm.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            Log.e("TransactionIngestor", "Failed to show unparsed notification", e)
        }
    }
}
