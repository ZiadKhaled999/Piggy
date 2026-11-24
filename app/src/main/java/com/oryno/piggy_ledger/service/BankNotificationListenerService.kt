package com.oryno.piggy_ledger.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.oryno.piggy_ledger.data.NotificationParserPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BankNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val seenKeys = object : LinkedHashMap<String, String>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 200
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.packageName == packageName) return

        val n = sbn.notification ?: return
        val flags = n.flags

        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return

        val extras = n.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty().trim()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty().trim()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty().trim()

        val fullText = buildNotificationText(title = title, text = text, bigText = bigText, subText = subText)
        if (fullText.isBlank()) return
        if (isOneTimeCode(fullText)) return

        val pkg = sbn.packageName
        val postTime = sbn.postTime
        val key = sbn.key

        scope.launch {
            val prefs = NotificationParserPrefs(applicationContext)
            if (!prefs.enabled.first()) return@launch

            val watched = prefs.watchedPackages.first()
            if (pkg !in watched) return@launch

            val contentHash = fullText.hashCode().toString()
            val derivedId = "$key|$contentHash|$postTime"

            synchronized(seenKeys) {
                if (seenKeys.containsKey(derivedId)) return@launch
                seenKeys[derivedId] = contentHash
            }

            val appLabel = resolveAppLabel(pkg)

            TransactionIngestor.ingest(
                context = applicationContext,
                senderLabel = appLabel,
                rawBody = fullText,
                source = TransactionIngestor.SOURCE_NOTIFICATION,
                sourceKey = derivedId,
                notificationPackage = pkg
            )
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    }

    private fun resolveAppLabel(pkg: String): String {
        return try {
            val ai = packageManager.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
            packageManager.getApplicationLabel(ai).toString().takeIf { it.isNotBlank() } ?: pkg
        } catch (_: Throwable) {
            try {
                @Suppress("DEPRECATION")
                val ai = packageManager.getApplicationInfo(pkg, 0)
                packageManager.getApplicationLabel(ai).toString().takeIf { it.isNotBlank() } ?: pkg
            } catch (_: Throwable) {
                pkg
            }
        }
    }

    companion object {
        private val bareNumberRegex = Regex("""(?<![EGP$€£₹₦₺]\s{0,2})\b\d{4,8}\b""")

        internal fun buildNotificationText(
            title: String,
            text: String,
            bigText: String,
            subText: String = ""
        ): String {
            val primaryBody = when {
                bigText.isNotBlank() && text.isNotBlank() && bigText.contains(text, ignoreCase = true) -> bigText
                bigText.isNotBlank() && text.isNotBlank() && text.contains(bigText, ignoreCase = true) -> text
                bigText.isNotBlank() && text.isBlank() -> bigText
                text.isNotBlank() && bigText.isBlank() -> text
                bigText.isNotBlank() && text.isNotBlank() -> "$text | $bigText"
                else -> ""
            }

            val parts = mutableListOf<String>()
            if (title.isNotBlank() && !primaryBody.contains(title, ignoreCase = true)) {
                parts.add(title)
            }
            if (primaryBody.isNotBlank()) {
                parts.add(primaryBody)
            }
            if (subText.isNotBlank() &&
                !primaryBody.contains(subText, ignoreCase = true) &&
                !title.contains(subText, ignoreCase = true)
            ) {
                parts.add(subText)
            }
            return parts.joinToString(" | ").trim()
        }

        internal fun isOneTimeCode(text: String): Boolean {
            val lower = text.lowercase()
            val codeWords = listOf(
                "otp", "one time", "one-time", "verification", "verify", "passcode",
                "pin code", "security code", "cvv",
                "code", "رمز", "كود", "تحقق", "التحقق", "كلمة المرور", "رقم سري"
            )
            if (codeWords.none { lower.contains(it) }) return false

            return bareNumberRegex.find(text) != null
        }
    }
}
