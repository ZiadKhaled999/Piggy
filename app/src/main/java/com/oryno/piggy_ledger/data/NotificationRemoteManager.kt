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

    suspend fun fetchAndShowNotifications(context: Context, isPremium: Boolean): Boolean = withContext(Dispatchers.IO) {
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
                        val config = jsonFormatter.decodeFromString<NotificationConfigResponse>(jsonBody)
                        processNotifications(context, config.notifications, isPremium)
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

    private fun processNotifications(context: Context, notifications: List<NotificationItem>, isPremium: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val seenIds = prefs.getStringSet(KEY_SEEN_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        
        var hasNew = false
        
        for (item in notifications) {
            if (item.status != "active") continue
            if (seenIds.contains(item.id)) continue
            
            // Check expiry
            if (!item.expiresAt.isNullOrBlank()) {
                try {
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val expiryDate = format.parse(item.expiresAt)
                    if (expiryDate != null && Date().after(expiryDate)) {
                        continue
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Date parse error for ${item.id}", e)
                }
            }
            
            // Target audience check
            val target = item.targetAudience.lowercase()
            if (target == "pro" && !isPremium) continue
            if (target == "free" && isPremium) continue
            
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
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            item.id.hashCode(), 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Using app icon as fallback if no specific small icon exists
        val iconRes = context.resources.getIdentifier("ic_notification", "drawable", context.packageName).takeIf { it != 0 } 
            ?: R.mipmap.ic_launcher

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
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
