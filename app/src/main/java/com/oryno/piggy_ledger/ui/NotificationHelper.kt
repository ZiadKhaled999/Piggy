package com.oryno.piggy_ledger.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color as AndroidColor
import androidx.core.app.NotificationCompat
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.MainActivity

class NotificationHelper(private val context: Context) {

    private val localizedContext: Context
        get() = getLocalizedContext(context)

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Deadline Channel
            val deadlineName = localizedContext.getString(R.string.deadline_channel_name)
            val deadlineDesc = localizedContext.getString(R.string.deadline_channel_desc)
            val deadlineChannel = NotificationChannel(CHANNEL_DEADLINE_ID, deadlineName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = deadlineDesc
                enableLights(true)
                lightColor = AndroidColor.RED
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(deadlineChannel)

            // General Reminders Channel
            val remindersName = localizedContext.getString(R.string.channel_reminders_name)
            val remindersDesc = localizedContext.getString(R.string.channel_reminders_desc)
            val remindersChannel = NotificationChannel(CHANNEL_REMINDERS_ID, remindersName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = remindersDesc
                enableLights(true)
                lightColor = androidx.core.content.ContextCompat.getColor(context, R.color.pink_primary)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(remindersChannel)
        }
    }

    private fun getMainActivityPendingIntent(
        shortcutAction: String? = null,
        requestCode: Int = 0
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!shortcutAction.isNullOrBlank()) {
                putExtra("shortcut_action", shortcutAction)
            }
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getAppLogoBitmap(): android.graphics.Bitmap? {
        return try {
            android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.img_app_logo)
        } catch (e: Exception) {
            null
        }
    }

    fun showDeadlineNotification(
        contactName: String,
        amount: Double,
        currencySymbol: String = "$",
        notificationId: Int = contactName.hashCode()
    ) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val title = try {
                localizedContext.getString(R.string.deadline_title)
            } catch (e: Exception) {
                "Deadline Over!"
            }
            val formattedAmount = try {
                String.format(java.util.Locale.US, "%.2f", amount)
            } catch (e: Exception) {
                amount.toString()
            }
            val message = try {
                localizedContext.getString(R.string.repayment_deadline_over, contactName, "$currencySymbol$formattedAmount")
            } catch (e: Exception) {
                "The repayment deadline for $contactName is over. Outstanding amount: $currencySymbol$formattedAmount"
            }
            val logoBitmap = getAppLogoBitmap()

            val builder = NotificationCompat.Builder(context, CHANNEL_DEADLINE_ID)
                .setSmallIcon(R.drawable.img_app_logo)
                .apply { if (logoBitmap != null) setLargeIcon(logoBitmap) }
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(androidx.core.content.ContextCompat.getColor(context, R.color.pink_primary))
                .setContentIntent(getMainActivityPendingIntent("loans", notificationId))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            notificationManager.notify(notificationId, builder.build())
        } catch (e: Throwable) {
            android.util.Log.e("NotificationHelper", "Failed to show deadline notification", e)
        }
    }

    fun cancelDeadlineNotification(loanId: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            notificationManager.cancel(loanId.hashCode())
        } catch (_: Throwable) { }
    }

    fun showStreakWarningNotification(daysMissed: Int = com.oryno.piggy_ledger.data.StreakManager.getConsecutiveMissedDays(context)) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = localizedContext.getString(R.string.notif_streak_warning_title)
        val message = localizedContext.getString(R.string.notif_streak_warning_msg, daysMissed.coerceAtLeast(1))
        val logoBitmap = getAppLogoBitmap()

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.img_app_logo)
            .apply { if (logoBitmap != null) setLargeIcon(logoBitmap) }
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setColor(androidx.core.content.ContextCompat.getColor(context, R.color.pink_primary))
            .setContentIntent(getMainActivityPendingIntent())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager.notify(NOTIF_ID_STREAK, builder.build())
    }

    fun showGoalReminderNotification(goalName: String, amountLeft: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = localizedContext.getString(R.string.notif_goal_title)
        val message = localizedContext.getString(R.string.notif_goal_msg, amountLeft)
        val logoBitmap = getAppLogoBitmap()

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.img_app_logo)
            .apply { if (logoBitmap != null) setLargeIcon(logoBitmap) }
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(AndroidColor.parseColor("#10B981")) // Emerald Green
            .setContentIntent(getMainActivityPendingIntent())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager.notify(NOTIF_ID_GOAL, builder.build())
    }

    fun showMotivationNotification(quote: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = localizedContext.getString(R.string.notif_motivation_title)
        val logoBitmap = getAppLogoBitmap()

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.img_app_logo)
            .apply { if (logoBitmap != null) setLargeIcon(logoBitmap) }
            .setContentTitle(title)
            .setContentText(quote)
            .setStyle(NotificationCompat.BigTextStyle().bigText(quote))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(AndroidColor.parseColor("#3B82F6")) // Blue
            .setContentIntent(getMainActivityPendingIntent())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager.notify(NOTIF_ID_MOTIVATION, builder.build())
    }

    fun showAuthNotification(isSignedIn: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = localizedContext.getString(if (isSignedIn) R.string.notif_auth_signin_title else R.string.notif_auth_signout_title)
        val message = localizedContext.getString(if (isSignedIn) R.string.notif_auth_signin_msg else R.string.notif_auth_signout_msg)
        val logoBitmap = getAppLogoBitmap()

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.img_app_logo)
            .apply { if (logoBitmap != null) setLargeIcon(logoBitmap) }
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(androidx.core.content.ContextCompat.getColor(context, R.color.pink_primary))
            .setContentIntent(getMainActivityPendingIntent())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager.notify(NOTIF_ID_AUTH, builder.build())
    }

    fun showTransactionProcessedNotification(
        accountName: String,
        currency: String,
        amount: Double,
        actionType: com.oryno.piggy_ledger.service.SmsActionType
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val logoBitmap = getAppLogoBitmap()
        
        val titleRes = when (actionType) {
            com.oryno.piggy_ledger.service.SmsActionType.DEPOSIT -> R.string.notif_tx_title_deposit
            com.oryno.piggy_ledger.service.SmsActionType.WITHDRAWAL -> R.string.notif_tx_msg_withdrawal
            com.oryno.piggy_ledger.service.SmsActionType.TRANSFER_OUT -> R.string.notif_tx_title_transfer
            com.oryno.piggy_ledger.service.SmsActionType.PURCHASE -> R.string.notif_tx_title_purchase
            com.oryno.piggy_ledger.service.SmsActionType.UNKNOWN -> R.string.notif_tx_title_unknown
        }
        
        val msgRes = when (actionType) {
            com.oryno.piggy_ledger.service.SmsActionType.DEPOSIT -> R.string.notif_tx_msg_deposit
            com.oryno.piggy_ledger.service.SmsActionType.WITHDRAWAL -> R.string.notif_tx_msg_withdrawal
            com.oryno.piggy_ledger.service.SmsActionType.TRANSFER_OUT -> R.string.notif_tx_msg_transfer
            com.oryno.piggy_ledger.service.SmsActionType.PURCHASE -> R.string.notif_tx_msg_purchase
            com.oryno.piggy_ledger.service.SmsActionType.UNKNOWN -> R.string.notif_tx_msg_unknown
        }

        val title = localizedContext.getString(titleRes)
        val message = localizedContext.getString(msgRes, amount, currency, accountName)
        
        val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.img_app_logo)
            .apply { if (logoBitmap != null) setLargeIcon(logoBitmap) }
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(androidx.core.content.ContextCompat.getColor(context, R.color.pink_primary))
            .setContentIntent(getMainActivityPendingIntent())
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    fun showSubscriptionDueNotification(
        subscriptionId: String,
        serviceName: String,
        amountStr: String,
        daysUntilDue: Int,
        isTrial: Boolean = false
    ) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val locCtx = localizedContext
            val dueRelativeText = when {
                daysUntilDue < 0 -> locCtx.getString(R.string.sub_notif_due_overdue_days, -daysUntilDue)
                daysUntilDue == 0 -> locCtx.getString(R.string.sub_due_today)
                daysUntilDue == 1 -> locCtx.getString(R.string.sub_due_tomorrow)
                else -> locCtx.getString(R.string.sub_due_in_days, daysUntilDue)
            }
            val title = if (isTrial) {
                locCtx.getString(R.string.sub_notif_trial_title, serviceName, dueRelativeText)
            } else {
                locCtx.getString(R.string.sub_notif_due_title, serviceName, dueRelativeText)
            }
            val message = if (isTrial) {
                locCtx.getString(R.string.sub_notif_trial_body, serviceName, amountStr, dueRelativeText)
            } else {
                locCtx.getString(R.string.sub_notif_due_body, amountStr, dueRelativeText)
            }
            val logoBitmap = getAppLogoBitmap()
            val notifId = NOTIF_ID_SUBSCRIPTION + (subscriptionId.hashCode() and 0x7FFF)

            val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
                .setSmallIcon(R.drawable.img_app_logo)
                .apply { if (logoBitmap != null) setLargeIcon(logoBitmap) }
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(androidx.core.content.ContextCompat.getColor(context, R.color.pink_primary))
                .setContentIntent(getMainActivityPendingIntent("subscriptions", notifId))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            notificationManager.notify(notifId, builder.build())
        } catch (e: Throwable) {
            android.util.Log.e("NotificationHelper", "Failed to show subscription due notification", e)
        }
    }

    fun cancelSubscriptionNotification(subscriptionId: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val notifId = NOTIF_ID_SUBSCRIPTION + (subscriptionId.hashCode() and 0x7FFF)
            notificationManager.cancel(notifId)
        } catch (_: Exception) { }
    }

    companion object {
        const val CHANNEL_DEADLINE_ID = "deadline_reminders"
        const val CHANNEL_REMINDERS_ID = "daily_reminders"
        const val NOTIF_ID_STREAK = 1001
        const val NOTIF_ID_GOAL = 1002
        const val NOTIF_ID_MOTIVATION = 1003
        const val NOTIF_ID_AUTH = 1004
        const val NOTIF_ID_SUBSCRIPTION = 1005

        fun getLocalizedContext(context: Context): Context {
            val savedLang = try {
                com.oryno.piggy_ledger.data.UserPreferences.getSavedAppLanguageSync(context)
            } catch (e: Exception) {
                null
            }?.takeIf { it.isNotBlank() }
                ?: try {
                    androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags()
                        .split(",")
                        .firstOrNull { it.isNotBlank() }
                } catch (e: Exception) {
                    null
                }

            if (savedLang.isNullOrBlank()) return context

            val locale = when {
                savedLang.equals("ar-EG", ignoreCase = true) || savedLang.lowercase().contains("eg") ->
                    java.util.Locale.forLanguageTag("ar-EG")
                savedLang.lowercase().startsWith("ar") ->
                    java.util.Locale.forLanguageTag("ar")
                else ->
                    java.util.Locale.forLanguageTag(savedLang)
            }

            val config = android.content.res.Configuration(context.resources.configuration)
            config.setLocale(locale)
            config.setLocales(android.os.LocaleList(locale))
            return context.createConfigurationContext(config)
        }

        fun getRandomMotivationQuote(context: Context): String {
            val localizedCtx = getLocalizedContext(context)
            val quotes = localizedCtx.resources.getStringArray(R.array.motivation_notification_phrases)
            if (quotes.isNotEmpty()) {
                return quotes.random()
            }
            val fallback = localizedCtx.resources.getStringArray(R.array.streak_statements_happy)
            return if (fallback.isNotEmpty()) fallback.random() else "Stay on track with Piggy Ledger! 🐷"
        }
    }
}
