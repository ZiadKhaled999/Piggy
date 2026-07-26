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

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Deadline Channel
            val deadlineName = context.getString(R.string.deadline_channel_name)
            val deadlineDesc = context.getString(R.string.deadline_channel_desc)
            val deadlineChannel = NotificationChannel(CHANNEL_DEADLINE_ID, deadlineName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = deadlineDesc
                enableLights(true)
                lightColor = AndroidColor.RED
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(deadlineChannel)

            // General Reminders Channel
            val remindersName = context.getString(R.string.channel_reminders_name)
            val remindersDesc = context.getString(R.string.channel_reminders_desc)
            val remindersChannel = NotificationChannel(CHANNEL_REMINDERS_ID, remindersName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = remindersDesc
                enableLights(true)
                lightColor = androidx.core.content.ContextCompat.getColor(context, R.color.pink_primary)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(remindersChannel)
        }
    }

    private fun getMainActivityPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun showDeadlineNotification(contactName: String, amount: Double) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = context.getString(R.string.deadline_title)
        val message = context.getString(R.string.repayment_deadline_over, contactName, "$$amount")

        val builder = NotificationCompat.Builder(context, CHANNEL_DEADLINE_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(androidx.core.content.ContextCompat.getColor(context, R.color.pink_primary))
            .setContentIntent(getMainActivityPendingIntent())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager.notify(contactName.hashCode(), builder.build())
    }

    fun showStreakWarningNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = context.getString(R.string.notif_streak_warning_title)
        val message = context.getString(R.string.notif_streak_warning_msg)

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_notification)
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
        val title = context.getString(R.string.notif_goal_title)
        val message = context.getString(R.string.notif_goal_msg, amountLeft)

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_notification)
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
        val title = context.getString(R.string.notif_motivation_title)

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_notification)
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
        val title = context.getString(if (isSignedIn) R.string.notif_auth_signin_title else R.string.notif_auth_signout_title)
        val message = context.getString(if (isSignedIn) R.string.notif_auth_signin_msg else R.string.notif_auth_signout_msg)

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
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
        
        val titleRes = when (actionType) {
            com.oryno.piggy_ledger.service.SmsActionType.DEPOSIT -> R.string.notif_tx_title_deposit
            com.oryno.piggy_ledger.service.SmsActionType.WITHDRAWAL -> R.string.notif_tx_title_withdrawal
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

        val title = context.getString(titleRes)
        val message = context.getString(msgRes, amount, currency, accountName)
        
        val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_notification)
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

    companion object {
        const val CHANNEL_DEADLINE_ID = "deadline_reminders"
        const val CHANNEL_REMINDERS_ID = "daily_reminders"
        const val NOTIF_ID_STREAK = 1001
        const val NOTIF_ID_GOAL = 1002
        const val NOTIF_ID_MOTIVATION = 1003
        const val NOTIF_ID_AUTH = 1004
    }
}
