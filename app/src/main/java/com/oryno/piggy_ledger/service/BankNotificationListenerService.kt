package com.oryno.piggy_ledger.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.oryno.piggy_ledger.data.NotificationParserPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BankNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val seenKeys = mutableSetOf<String>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        if (sbn.packageName == packageName) return

        scope.launch {
            val prefs = NotificationParserPrefs(applicationContext)

            if (!prefs.enabled.first()) return@launch

            val watched = prefs.watchedPackages.first()
            if (sbn.packageName !in watched) return@launch

            val n = sbn.notification ?: return@launch
            val flags = n.flags

            if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return@launch
            if (flags and Notification.FLAG_ONGOING_EVENT != 0) return@launch
            if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return@launch

            val extras = n.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()

            val fullText = listOf(title, text, bigText)
                .filter { it.isNotBlank() }
                .joinToString(" | ")

            if (fullText.isBlank()) return@launch

            if (isOneTimeCode(fullText)) return@launch

            synchronized(seenKeys) {
                if (!seenKeys.add(sbn.key)) return@launch
                if (seenKeys.size > 500) seenKeys.clear()
            }

            TransactionIngestor.ingest(
                context = applicationContext,
                senderLabel = sbn.packageName,
                rawBody = fullText,
                source = TransactionIngestor.SOURCE_NOTIFICATION,
                sourceKey = sbn.key,
                notificationPackage = sbn.packageName
            )
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    }

    private fun isOneTimeCode(text: String): Boolean {
        val lower = text.lowercase()
        val codeWords = listOf(
            "otp", "one time", "one-time", "verification", "verify",
            "code", "رمز", "كود", "تحقق", "التحقق", "كلمة المرور"
        )
        if (codeWords.none { lower.contains(it) }) return false

        val bareNumber = Regex("""(?<![EGP$€£]\s*)\b\d{4,8}\b""").find(text)
        return bareNumber != null
    }
}
