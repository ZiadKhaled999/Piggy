package com.oryno.piggy_ledger.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.oryno.piggy_ledger.MainActivity
import com.oryno.piggy_ledger.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Serializable
data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    @SerialName("target_audience") val targetAudience: String = "all", // "free", "pro", "all"
    @SerialName("target_region") val targetRegion: String = "", // region code or "all"
    @SerialName("target_user") val targetUser: String = "", // specific user email/id
    val status: String = "active", // "active", "inactive"
    @SerialName("expires_at") val expiresAt: String? = null // ISO string or simple date
)

@Serializable
data class NotificationConfigResponse(
    val notifications: List<NotificationItem> = emptyList()
)

object NotificationRemoteManager {
    private const val TAG = "NotificationRemoteMgr"
    private const val PREFS_NAME = "piggy_notifications_prefs"
    private const val KEY_SEEN_IDS = "seen_notification_ids"
    private const val KEY_READ_IDS = "read_notification_ids"
    private const val KEY_DISMISSED_IDS = "dismissed_notification_ids"
    private const val KEY_NOTIFICATIONS_JSON = "notifications_json"

    fun getReadIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_READ_IDS, emptySet()) ?: emptySet()
    }

    fun getDismissedIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_DISMISSED_IDS, emptySet()) ?: emptySet()
    }

    fun markAsDismissed(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dismissedIds = prefs.getStringSet(KEY_DISMISSED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        dismissedIds.add(id)
        prefs.edit().putStringSet(KEY_DISMISSED_IDS, dismissedIds).apply()
    }

    fun markAsRead(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val readIds = prefs.getStringSet(KEY_READ_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        readIds.add(id)
        prefs.edit().putStringSet(KEY_READ_IDS, readIds).apply()
    }

    fun markAllAsRead(context: Context, ids: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val readIds = prefs.getStringSet(KEY_READ_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        readIds.addAll(ids)
        prefs.edit().putStringSet(KEY_READ_IDS, readIds).apply()
    }

    private const val DEFAULT_REMOTE_URL = "https://piggy-assets.vercel.app/notifications_config.json"
    private const val FALLBACK_GITHUB_RAW_URL = "https://raw.githubusercontent.com/ZiadKhaled999/piggy-assets/main/notifications_config.json"

    private val jsonFormatter = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
        
    private const val CHANNEL_ID = "piggy_custom_notifications"
    private const val CHANNEL_NAME = "Piggy Announcements"

    suspend fun fetchAndShowNotifications(
        context: Context,
        isPremium: Boolean,
        region: String = "",
        userEmail: String = "",
        userId: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val resolvedRegion = region.ifBlank { resolveDeviceCountryCode(context) }
        val (resolvedEmail, resolvedUserId) = if (userEmail.isBlank() && userId.isBlank()) {
            resolveCurrentUserIdentity(context)
        } else {
            userEmail to userId
        }

        val urlsToTry = listOf(
            DEFAULT_REMOTE_URL,
            FALLBACK_GITHUB_RAW_URL
        )

        for (configUrl in urlsToTry) {
            try {
                Log.d(TAG, "Fetching notifications config from: $configUrl")
                val request = Request.Builder().url(configUrl).build()
                val response = httpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val jsonBody = response.body?.string()
                    if (!jsonBody.isNullOrBlank()) {
                        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit().putString(KEY_NOTIFICATIONS_JSON, jsonBody).apply()
                        val config = jsonFormatter.decodeFromString<NotificationConfigResponse>(jsonBody)
                        processNotifications(
                            context = context,
                            notifications = config.notifications,
                            isPremium = isPremium,
                            region = resolvedRegion,
                            userEmail = resolvedEmail,
                            userId = resolvedUserId
                        )
                        return@withContext true
                    }
                } else {
                    Log.w(TAG, "Fetch from $configUrl returned code ${response.code}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching notifications from $configUrl", e)
            }
        }
        return@withContext false
    }

    fun getSavedNotifications(
        context: Context,
        isPremium: Boolean,
        region: String = "",
        userEmail: String = "",
        userId: String = ""
    ): List<NotificationItem> {
        val resolvedRegion = region.ifBlank { resolveDeviceCountryCode(context) }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_NOTIFICATIONS_JSON, null) ?: return emptyList()
        try {
            val config = jsonFormatter.decodeFromString<NotificationConfigResponse>(jsonString)
            return config.notifications.filter { item ->
                matchesNotificationTarget(
                    item = item,
                    isPremium = isPremium,
                    region = resolvedRegion,
                    userEmail = userEmail,
                    userId = userId
                )
            }.reversed() // Show newest first (assuming array order is chronological, or reverse it)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing saved notifications", e)
        }
        return emptyList()
    }

    internal fun matchesNotificationTarget(
        item: NotificationItem,
        isPremium: Boolean,
        region: String,
        userEmail: String,
        userId: String = "",
        now: Date = Date()
    ): Boolean {
        if (!item.status.equals("active", ignoreCase = true)) return false

        // Check expiry
        if (!item.expiresAt.isNullOrBlank()) {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val expiryDate = format.parse(item.expiresAt)
                if (expiryDate != null && now.after(expiryDate)) {
                    return false
                }
            } catch (e: Exception) {
                try {
                    Log.e(TAG, "Date parse error for ${item.id}", e)
                } catch (_: Throwable) {}
            }
        }

        // Target audience check
        val target = item.targetAudience.lowercase(Locale.ROOT)
        if (target == "pro" && !isPremium) return false
        if (target == "free" && isPremium) return false

        // Region check (supports single or comma-separated ISO codes, or "all"/blank)
        if (!matchesRegion(item.targetRegion, region)) return false

        // Specific user check (supports email or Clerk user ID, single or comma-separated)
        if (!matchesUser(item.targetUser, userEmail, userId)) return false

        return true
    }

    internal fun matchesRegion(targetRegion: String, deviceRegion: String): Boolean {
        val raw = targetRegion.trim()
        if (raw.isBlank() || raw.equals("all", ignoreCase = true)) return true

        val targets = raw.split(',', ';', ' ')
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (targets.isEmpty() || targets.any { it.equals("all", ignoreCase = true) }) {
            return true
        }

        val normalizedDevice = deviceRegion.trim().uppercase(Locale.ROOT)
        if (normalizedDevice.isBlank()) return false

        val deviceCountryName = try {
            Locale("", normalizedDevice).getDisplayCountry(Locale.ENGLISH)
        } catch (_: Exception) {
            ""
        }

        return targets.any { token ->
            token.equals(normalizedDevice, ignoreCase = true) ||
                (deviceCountryName.isNotBlank() && token.equals(deviceCountryName, ignoreCase = true))
        }
    }

    internal fun matchesUser(targetUser: String, userEmail: String, userId: String): Boolean {
        val raw = targetUser.trim()
        if (raw.isBlank() || raw.equals("all", ignoreCase = true)) return true

        val targets = raw.split(',', ';')
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (targets.isEmpty()) return true

        val cleanEmail = userEmail.trim()
        val cleanId = userId.trim()

        return targets.any { token ->
            (cleanEmail.isNotBlank() && token.equals(cleanEmail, ignoreCase = true)) ||
                (cleanId.isNotBlank() && token.equals(cleanId, ignoreCase = true))
        }
    }

    fun resolveDeviceCountryCode(context: Context): String {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            val networkCountry = tm?.networkCountryIso?.trim().orEmpty()
            if (networkCountry.length == 2 && networkCountry.all { it.isLetter() }) {
                return networkCountry.uppercase(Locale.ROOT)
            }
            val simCountry = tm?.simCountryIso?.trim().orEmpty()
            if (simCountry.length == 2 && simCountry.all { it.isLetter() }) {
                return simCountry.uppercase(Locale.ROOT)
            }
        } catch (_: Exception) {}

        try {
            val sysLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                android.content.res.Resources.getSystem().configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                android.content.res.Resources.getSystem().configuration.locale
            }
            val sysCountry = sysLocale?.country?.trim().orEmpty()
            if (sysCountry.length == 2 && sysCountry.all { it.isLetter() }) {
                return sysCountry.uppercase(Locale.ROOT)
            }
        } catch (_: Exception) {}

        return Locale.getDefault().country.trim().uppercase(Locale.ROOT)
    }

    suspend fun resolveCurrentUserIdentity(context: Context): Pair<String, String> {
        val clerkUser = try {
            com.clerk.api.Clerk.userFlow.value ?: com.clerk.api.Clerk.user
        } catch (_: Exception) {
            null
        }
        val clerkId = clerkUser?.id?.trim().orEmpty()
        val clerkEmail = try {
            clerkUser?.primaryEmailAddress?.emailAddress
                ?: clerkUser?.emailAddresses?.firstOrNull()?.emailAddress
                ?: ""
        } catch (_: Exception) {
            ""
        }.trim()

        if (clerkEmail.isNotBlank()) {
            return clerkEmail to clerkId
        }

        val storedEmail = try {
            UserPreferences(context).authUserEmail.firstOrNull().orEmpty().trim()
        } catch (_: Exception) {
            ""
        }
        return storedEmail to clerkId
    }

    private fun processNotifications(
        context: Context,
        notifications: List<NotificationItem>,
        isPremium: Boolean,
        region: String = "",
        userEmail: String = "",
        userId: String = ""
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val seenIds = prefs.getStringSet(KEY_SEEN_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        
        var hasNew = false
        
        for (item in notifications) {
            if (seenIds.contains(item.id)) continue
            if (!matchesNotificationTarget(item, isPremium, region, userEmail, userId)) continue

            // Show it
            showNotification(context, item)
            seenIds.add(item.id)
            hasNew = true
        }
        
        if (hasNew) {
            prefs.edit().putStringSet(KEY_SEEN_IDS, seenIds).apply()
        }
    }

    private fun showNotification(context: Context, item: NotificationItem) {
        createNotificationChannel(context)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_notification_id", item.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            item.id.hashCode(), 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val iconRes = context.resources.getIdentifier("ic_notification", "drawable", context.packageName).takeIf { it != 0 } 
            ?: R.mipmap.ic_launcher
            
        val largeIcon = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.img_app_logo)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setLargeIcon(largeIcon)
            .setContentTitle(item.title)
            .setContentText(item.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(item.id.hashCode(), builder.build())
            Log.d(TAG, "Displayed notification: ${item.title}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing notification permission", e)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Important announcements and updates"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
