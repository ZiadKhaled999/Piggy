package com.oryno.piggy_ledger.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

@Serializable
data class RemoteMascotItem(
    val id: String = "",
    val name: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("start_time") val startTime: String = "00:00",
    @SerialName("end_time") val endTime: String = "23:59",
    @SerialName("days_of_week") val daysOfWeek: List<Int> = emptyList()
)

@Serializable
data class RemoteCategoryItem(
    @SerialName("category_id") val categoryId: Int? = null,
    val name: String = "",
    @SerialName("status_key") val statusKey: String = "",
    val statements: List<String> = emptyList(),
    val mascots: List<RemoteMascotItem> = emptyList()
)

@Serializable
data class RemoteWidgetConfig(
    @SerialName("default_image_url") val defaultImageUrl: String? = null,
    val categories: List<RemoteCategoryItem> = emptyList()
)

@Serializable
data class PiggyRemoteConfig(
    val version: Int = 1,
    @SerialName("updated_at") val lastUpdated: String? = null,
    @SerialName("widget_config") val widgetConfig: RemoteWidgetConfig? = null
)

data class ResolvedMascotResult(
    val bitmap: Bitmap? = null,
    val localDrawableRes: Int? = null,
    val phrase: String? = null
)

object PiggyRemoteConfigManager {
    private const val TAG = "PiggyRemoteConfig"
    private const val PREFS_NAME = "piggy_remote_config_prefs"
    private const val KEY_REMOTE_URL = "remote_config_url"
    private const val DEFAULT_REMOTE_URL = "https://piggy-assets.vercel.app/piggy_remote_config.json"
    private const val CACHE_FILE_NAME = "piggy_remote_config_cache.json"

    private val jsonFormatter = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val httpClient by lazy {
        OkHttpClient.Builder().build()
    }

    fun getRemoteConfigUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_REMOTE_URL, DEFAULT_REMOTE_URL) ?: DEFAULT_REMOTE_URL
    }

    fun setRemoteConfigUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_REMOTE_URL, url).apply()
    }

    suspend fun fetchAndSyncConfig(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val configUrl = getRemoteConfigUrl(context)
            val request = Request.Builder()
                .url(configUrl)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonBody = response.body?.string()
                if (!jsonBody.isNullOrBlank()) {
                    val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
                    cacheFile.writeText(jsonBody)

                    // Parse config and pre-download mascot images
                    val config = jsonFormatter.decodeFromString<PiggyRemoteConfig>(jsonBody)
                    val allMascots = config.widgetConfig?.categories?.flatMap { it.mascots } ?: emptyList()
                    downloadMascotImages(context, allMascots)
                    Log.d(TAG, "Successfully fetched and synced remote config with ${allMascots.size} mascots")
                    return@withContext true
                }
            } else {
                Log.w(TAG, "HTTP fetch failed with code: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote config", e)
        }
        return@withContext false
    }

    fun getCachedConfig(context: Context): PiggyRemoteConfig? {
        return try {
            val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
            if (cacheFile.exists()) {
                val jsonBody = cacheFile.readText()
                jsonFormatter.decodeFromString<PiggyRemoteConfig>(jsonBody)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading cached config", e)
            null
        }
    }

    private fun downloadMascotImages(context: Context, mascots: List<RemoteMascotItem>) {
        val mascotDir = File(context.filesDir, "mascots").apply { if (!exists()) mkdirs() }
        for (item in mascots) {
            val url = item.imageUrl ?: continue
            if (url.isBlank()) continue

            val fileName = getFileNameFromUrl(url, item.id)
            val targetFile = File(mascotDir, fileName)
            if (!targetFile.exists() || targetFile.length() == 0L) {
                try {
                    val req = Request.Builder().url(url).build()
                    val resp = httpClient.newCall(req).execute()
                    if (resp.isSuccessful) {
                        resp.body?.bytes()?.let { bytes ->
                            targetFile.writeBytes(bytes)
                            Log.d(TAG, "Downloaded mascot image: $fileName")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed downloading mascot image from $url", e)
                }
            }
        }
    }

    fun getLocalMascotBitmap(context: Context, urlOrFileName: String?, mascotId: String): Bitmap? {
        if (urlOrFileName.isNullOrBlank()) return null
        return try {
            val mascotDir = File(context.filesDir, "mascots")
            val fileName = getFileNameFromUrl(urlOrFileName, mascotId)
            val targetFile = File(mascotDir, fileName)
            if (targetFile.exists() && targetFile.length() > 0L) {
                BitmapFactory.decodeFile(targetFile.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed loading bitmap for $mascotId", e)
            null
        }
    }

    private fun getFileNameFromUrl(url: String, mascotId: String): String {
        val lastSegment = url.substringAfterLast('/').substringBefore('?')
        return if (lastSegment.isNotBlank() && lastSegment.contains(".")) {
            "${mascotId}_$lastSegment"
        } else {
            "${mascotId}_mascot.png"
        }
    }

    fun resolveMascot(
        context: Context,
        categoryId: Int,
        streak: Int,
        isActionToday: Boolean,
        isFrozen: Boolean,
        isLost: Boolean,
        hour: Int,
        languageIsArabic: Boolean
    ): ResolvedMascotResult {
        val config = getCachedConfig(context) ?: return ResolvedMascotResult()
        
        val targetStatusKey = when {
            isActionToday -> "active"
            isFrozen -> "frozen"
            isLost -> "warning"
            else -> "active"
        }

        // Find the category matching the status
        val category = config.widgetConfig?.categories?.find { it.statusKey == targetStatusKey }
        if (category == null || category.mascots.isEmpty()) {
            return ResolvedMascotResult()
        }
        
        // Formulate hour string HH:mm for simple comparison
        val hourStr = String.format("%02d:00", hour)
        // Day of week (1=Mon..7=Sun). Calendar.DAY_OF_WEEK returns Sun=1..Sat=7.
        // For our remote JSON: Mon=1, Tue=2, Wed=3, Thu=4, Fri=5, Sat=6, Sun=7
        val calendar = java.util.Calendar.getInstance()
        var currentDay = calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1
        if (currentDay == 0) currentDay = 7 // Map Sunday to 7
        
        val matchingMascots = category.mascots.filter { item ->
            val inTimeRange = hourStr >= item.startTime && hourStr <= item.endTime
            val inDays = item.daysOfWeek.isEmpty() || item.daysOfWeek.contains(currentDay)
            inTimeRange && inDays
        }

        if (matchingMascots.isEmpty()) {
            return ResolvedMascotResult()
        }

        // Pick first or random based on streak seed
        val selectedMascot = matchingMascots.first()
        val bitmap = getLocalMascotBitmap(context, selectedMascot.imageUrl, selectedMascot.id)

        val statements = category.statements
        val selectedPhrase = if (statements.isNotEmpty()) {
            val seed = (streak + hour + categoryId).hashCode()
            val idx = Math.abs(seed) % statements.size
            statements[idx]
        } else {
            null
        }

        return ResolvedMascotResult(
            bitmap = bitmap,
            phrase = selectedPhrase
        )
    }
}
