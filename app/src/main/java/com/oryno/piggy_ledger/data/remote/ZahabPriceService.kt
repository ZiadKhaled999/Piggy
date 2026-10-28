package com.oryno.piggy_ledger.data.remote

import android.util.Log
import com.oryno.piggy_ledger.data.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
data class ZahabMcpRequest(
    val name: String,
    val arguments: Map<String, String> = emptyMap()
)

@Serializable
data class ZahabMcpContent(
    val type: String = "text",
    val text: String = ""
)

@Serializable
data class ZahabMcpResponse(
    val content: List<ZahabMcpContent> = emptyList()
)

data class LiveGoldPrices(
    val price24k: Double,
    val price21k: Double,
    val price18k: Double,
    val currency: String = "EGP",
    val timestamp: Long = System.currentTimeMillis(),
    val isLive: Boolean = true,
    val rawText: String = ""
)

data class LiveSilverPrices(
    val pricePerGram: Double,
    val currency: String = "EGP",
    val timestamp: Long = System.currentTimeMillis(),
    val isLive: Boolean = true,
    val rawText: String = ""
)

object ZahabPriceService {
    private const val BASE_URL = "https://zahabprice.com"
    private const val TAG = "MarketRateService"
    private const val CACHE_DURATION_MS = 50_000L // 50 seconds cache requirement

    @Volatile
    private var cachedGoldPrices: LiveGoldPrices? = null

    @Volatile
    private var lastGoldFetchTime: Long = 0L

    @Volatile
    private var cachedSilverPrices: LiveSilverPrices? = null

    @Volatile
    private var lastSilverFetchTime: Long = 0L

    // Default fallback rates (EGP) in case device is offline or endpoint fails
    val FALLBACK_GOLD = LiveGoldPrices(
        price24k = 4120.0,
        price21k = 3605.0,
        price18k = 3090.0,
        currency = "EGP",
        isLive = false,
        rawText = "Market fallback rates"
    )

    val FALLBACK_SILVER = LiveSilverPrices(
        pricePerGram = 52.0,
        currency = "EGP",
        isLive = false,
        rawText = "Market fallback rates"
    )

    suspend fun getGoldPrice(countryCode: String = "EG", forceRefresh: Boolean = false): LiveGoldPrices {
        val now = System.currentTimeMillis()
        val cached = cachedGoldPrices
        if (!forceRefresh && cached != null && (now - lastGoldFetchTime) < CACHE_DURATION_MS) {
            return cached
        }

        return try {
            val response: ZahabMcpResponse = ApiClient.ktorClient.post(BASE_URL) {
                contentType(ContentType.Application.Json)
                setBody(
                    ZahabMcpRequest(
                        name = "get_gold_price",
                        arguments = mapOf("country" to countryCode.uppercase())
                    )
                )
            }.body()

            val textContent = response.content.firstOrNull()?.text.orEmpty()
            val parsed = if (textContent.isNotBlank()) {
                parseGoldPrices(textContent)
            } else {
                FALLBACK_GOLD
            }
            cachedGoldPrices = parsed
            lastGoldFetchTime = System.currentTimeMillis()
            parsed
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch market gold price", e)
            cachedGoldPrices ?: FALLBACK_GOLD
        }
    }

    suspend fun getSilverPrice(countryCode: String = "EG", forceRefresh: Boolean = false): LiveSilverPrices {
        val now = System.currentTimeMillis()
        val cached = cachedSilverPrices
        if (!forceRefresh && cached != null && (now - lastSilverFetchTime) < CACHE_DURATION_MS) {
            return cached
        }

        return try {
            val response: ZahabMcpResponse = ApiClient.ktorClient.post(BASE_URL) {
                contentType(ContentType.Application.Json)
                setBody(
                    ZahabMcpRequest(
                        name = "get_silver_price",
                        arguments = mapOf("country" to countryCode.uppercase())
                    )
                )
            }.body()

            val textContent = response.content.firstOrNull()?.text.orEmpty()
            val parsed = if (textContent.isNotBlank()) {
                parseSilverPrices(textContent)
            } else {
                FALLBACK_SILVER
            }
            cachedSilverPrices = parsed
            lastSilverFetchTime = System.currentTimeMillis()
            parsed
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch market silver price", e)
            cachedSilverPrices ?: FALLBACK_SILVER
        }
    }

    private fun parseGoldPrices(text: String): LiveGoldPrices {
        var p24 = 0.0
        var p21 = 0.0
        var p18 = 0.0

        // Match 24K / 24k pattern
        val regex24 = Regex("""24\s*(?:k|karat)?\s*[:=\-]?\s*([0-9,]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
        val regex21 = Regex("""21\s*(?:k|karat)?\s*[:=\-]?\s*([0-9,]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
        val regex18 = Regex("""18\s*(?:k|karat)?\s*[:=\-]?\s*([0-9,]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)

        regex24.find(text)?.groupValues?.get(1)?.let {
            p24 = it.replace(",", "").toDoubleOrNull() ?: 0.0
        }
        regex21.find(text)?.groupValues?.get(1)?.let {
            p21 = it.replace(",", "").toDoubleOrNull() ?: 0.0
        }
        regex18.find(text)?.groupValues?.get(1)?.let {
            p18 = it.replace(",", "").toDoubleOrNull() ?: 0.0
        }

        // If specific Karats were found
        if (p24 > 0 || p21 > 0 || p18 > 0) {
            if (p21 > 0 && p24 == 0.0) p24 = p21 * (24.0 / 21.0)
            if (p24 > 0 && p21 == 0.0) p21 = p24 * (21.0 / 24.0)
            if (p21 > 0 && p18 == 0.0) p18 = p21 * (18.0 / 21.0)

            return LiveGoldPrices(
                price24k = p24,
                price21k = p21,
                price18k = p18,
                isLive = true,
                rawText = text
            )
        }

        // Alternative generic numbers extractor
        val numbers = Regex("""([0-9,]+(?:\.[0-9]+)?)""")
            .findAll(text)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .filter { it > 1000.0 } // Gold price per gram in EGP is > 1000
            .toList()

        return if (numbers.isNotEmpty()) {
            val primary = numbers.first()
            LiveGoldPrices(
                price24k = primary * (24.0 / 21.0),
                price21k = primary,
                price18k = primary * (18.0 / 21.0),
                isLive = true,
                rawText = text
            )
        } else {
            FALLBACK_GOLD
        }
    }

    private fun parseSilverPrices(text: String): LiveSilverPrices {
        val numbers = Regex("""([0-9,]+(?:\.[0-9]+)?)""")
            .findAll(text)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .filter { it in 10.0..500.0 } // Silver per gram reasonable range
            .toList()

        val price = numbers.firstOrNull() ?: 52.0
        return LiveSilverPrices(
            pricePerGram = price,
            isLive = numbers.isNotEmpty(),
            rawText = text
        )
    }
}
