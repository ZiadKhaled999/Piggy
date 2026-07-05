package com.oryno.piggy_ledger.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.oryno.piggy_ledger.R

class NotificationHelper(private val context: Context) {

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Deadline Reminders"
            val descriptionText = "Notifications for loan repayment deadlines"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showDeadlineNotification(contactName: String, amount: Double) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Deadline Over!")
            .setContentText("The repayment deadline for $contactName is over. Outstanding amount: $$amount.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", null)
            .addAction(android.R.drawable.ic_popup_reminder, "Snooze", null)

        notificationManager.notify(contactName.hashCode(), builder.build())
    }

    companion object {
        const val CHANNEL_ID = "deadline_reminders"
    }
}
