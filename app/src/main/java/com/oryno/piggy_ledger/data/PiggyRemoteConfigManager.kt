package com.oryno.piggy_ledger.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit

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
    private const val FALLBACK_GITHUB_RAW_URL = "https://raw.githubusercontent.com/ZiadKhaled999/piggy-assets/main/piggy_remote_config.json"
    private const val CACHE_FILE_NAME = "piggy_remote_config_cache.json"

    private val jsonFormatter = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
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
        val urlsToTry = listOf(
            getRemoteConfigUrl(context),
            FALLBACK_GITHUB_RAW_URL,
            DEFAULT_REMOTE_URL
        ).distinct()

        for (configUrl in urlsToTry) {
            try {
                Log.d(TAG, "Fetching remote config from: $configUrl")
                val request = Request.Builder()
                    .url(configUrl)
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val jsonBody = response.body?.string()
                    if (!jsonBody.isNullOrBlank()) {
                        val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
                        cacheFile.writeText(jsonBody)

                        val config = jsonFormatter.decodeFromString<PiggyRemoteConfig>(jsonBody)
                        val allMascots = config.widgetConfig?.categories?.flatMap { it.mascots } ?: emptyList()
                        downloadMascotImages(context, allMascots, config.widgetConfig?.defaultImageUrl)
                        Log.d(TAG, "Successfully fetched and synced remote config with ${allMascots.size} mascots")
                        return@withContext true
                    }
                } else {
                    Log.w(TAG, "Fetch from $configUrl returned code ${response.code}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching remote config from $configUrl", e)
            }
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

    private fun downloadMascotImages(context: Context, mascots: List<RemoteMascotItem>, defaultImageUrl: String? = null) {
        val mascotDir = File(context.filesDir, "mascots").apply { if (!exists()) mkdirs() }
        val allUrls = mascots.mapNotNull { if (!it.imageUrl.isNullOrBlank()) it.imageUrl to it.id else null }.toMutableList()
        if (!defaultImageUrl.isNullOrBlank()) {
            allUrls.add(defaultImageUrl to "default_mascot")
        }

        for ((url, id) in allUrls) {
            val fileName = getFileNameFromUrl(url, id)
            val targetFile = File(mascotDir, fileName)
            if (!targetFile.exists() || targetFile.length() == 0L) {
                downloadSingleImage(context, targetFile, url)
            }
        }
    }

    private fun downloadSingleImage(context: Context?, targetFile: File, url: String) {
        val urlsToTry = mutableListOf<String>()
        val filename = url.substringAfterLast('/').substringBefore('?')
        if (filename.isNotBlank()) {
            urlsToTry.add("https://piggy-assets.vercel.app/mascots/$filename")
        }
        urlsToTry.add(url)
        if (url.contains("raw.githubusercontent.com") && url.contains("/main/")) {
            val path = url.substringAfter("/main/")
            urlsToTry.add("https://piggy-assets.vercel.app/$path")
        }

        for (tryUrl in urlsToTry.distinct()) {
            try {
                val req = Request.Builder().url(tryUrl).build()
                val resp = httpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    resp.body?.use { body ->
                        targetFile.writeBytes(body.bytes())
                        Log.d(TAG, "Downloaded image to ${targetFile.name} from $tryUrl")
                        if (context != null) {
                            com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
                        }
                    }
                    return // Success, stop trying other URLs
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed downloading image from $tryUrl", e)
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
                // Trigger asynchronous background download for missing image
                CoroutineScope(Dispatchers.IO).launch {
                    downloadSingleImage(context, targetFile, urlOrFileName)
                }
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

    private fun parseTimeToMinutes(timeStr: String?, defaultMinutes: Int): Int {
        if (timeStr.isNullOrBlank()) return defaultMinutes
        return try {
            val parts = timeStr.trim().split(":")
            if (parts.size >= 2) {
                val h = parts[0].toInt()
                val m = parts[1].toInt()
                h * 60 + m
            } else {
                defaultMinutes
            }
        } catch (e: Exception) {
            defaultMinutes
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

        val categories = config.widgetConfig?.categories ?: emptyList()
        if (categories.isEmpty()) return ResolvedMascotResult()

        // Match category by statusKey or fall back to first category
        val category = categories.find { it.statusKey == targetStatusKey } 
            ?: categories.find { it.statusKey == "active" } 
            ?: categories.first()

        val mascots = category.mascots
        if (mascots.isEmpty()) {
            return ResolvedMascotResult()
        }

        val currentMinutes = hour * 60
        val calendar = Calendar.getInstance()
        var currentDay = calendar.get(Calendar.DAY_OF_WEEK) - 1
        if (currentDay == 0) currentDay = 7 // Sunday = 7

        // Filter mascots by time and days of week
        val matchingMascots = mascots.filter { item ->
            val startMin = parseTimeToMinutes(item.startTime, 0)
            val endMin = parseTimeToMinutes(item.endTime, 1439)
            val inTimeRange = currentMinutes in startMin..endMin
            val inDays = item.daysOfWeek.isEmpty() || item.daysOfWeek.contains(currentDay)
            inTimeRange && inDays
        }

        // Selected mascot: filtered match or fallback to first mascot in category
        val selectedMascot = matchingMascots.firstOrNull() ?: mascots.first()
        
        var bitmap = getLocalMascotBitmap(context, selectedMascot.imageUrl, selectedMascot.id)
        
        // Fallback to default image URL if available
        if (bitmap == null && !config.widgetConfig?.defaultImageUrl.isNullOrBlank()) {
            bitmap = getLocalMascotBitmap(context, config.widgetConfig?.defaultImageUrl, "default_mascot")
        }

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
