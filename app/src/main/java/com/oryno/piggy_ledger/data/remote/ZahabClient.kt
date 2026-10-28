package com.oryno.piggy_ledger.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class MetalPriceSnapshot(
    val country: String,
    val countryName: String?,
    val countryNameLocal: String?,
    val currency: String,
    val gold: Map<Int, Double>,
    val silver: Map<Int, Double>,
    val spotUsdPerOzGold: Double?,
    val spotUsdPerOzSilver: Double?,
    val market: String?,
    val marketNote: String?,
    val source: String?,
    val fetchedAt: Long,
) {
    fun goldPerGram(karat: Int): Double? = gold[karat]
    fun silverPerGram(purity: Int): Double? = silver[purity]
}

sealed class ZahabError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Network(message: String, cause: Throwable?) : ZahabError(message, cause)
    class Http(val code: Int, message: String) : ZahabError("HTTP $code: $message")
    class Parse(message: String) : ZahabError(message)
    class UpstreamTool(message: String) : ZahabError(message)
}

class ZahabClient(
    private val mode: Mode = Mode.DIRECT_MCP,
    private val baseUrl: String = "http://10.0.2.2:8080",
    httpClient: OkHttpClient? = null,
) {
    enum class Mode { DIRECT_MCP, REST_SERVER }

    private val http: OkHttpClient = httpClient ?: OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private data class CacheEntry(val value: MetalPriceSnapshot, val storedAt: Long)
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val cacheTtlMs = 30_000L

    @Throws(ZahabError::class)
    suspend fun fetchPrices(
        country: String,
        forceRefresh: Boolean = false,
    ): MetalPriceSnapshot {
        val code = country.lowercase().trim()
        if (!forceRefresh) {
            cache[code]?.let { hit ->
                if (System.currentTimeMillis() - hit.storedAt < cacheTtlMs) return hit.value
            }
        }
        val snapshot = when (mode) {
            Mode.DIRECT_MCP -> fetchFromMcp(code)
            Mode.REST_SERVER -> fetchFromServer(code)
        }
        cache[code] = CacheEntry(snapshot, System.currentTimeMillis())
        return snapshot
    }

    fun clearCache() = cache.clear()

    private suspend fun fetchFromMcp(country: String): MetalPriceSnapshot =
        withContext(Dispatchers.IO) {
            val (gText, sText) = coroutineScope {
                val g = async { callMcpTool("get_gold_price", mapOf("country" to country, "karat" to 24)) }
                val s = async { callMcpTool("get_silver_price", mapOf("country" to country, "purity" to 999)) }
                g.await() to s.await()
            }
            parseProse(country, gText, sText)
        }

    private fun callMcpTool(toolName: String, args: Map<String, Any>): String {
        val argsJson = JSONObject(args as Map<*, *>).toString()
        val body = """
            {"jsonrpc":"2.0","id":1,"method":"tools/call",
             "params":{"name":"$toolName","arguments":$argsJson}}
        """.trimIndent()

        val request = Request.Builder()
            .url("https://www.zahabprice.com/mcp")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .header("MCP-Protocol-Version", "2024-11-05")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:158.0) Gecko/20100101 Firefox/158.0")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        http.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw ZahabError.Http(resp.code, raw.take(200))

            val root = try { JSONObject(raw) } catch (e: Exception) {
                throw ZahabError.Parse("bad JSON from MCP: ${raw.take(200)}")
            }
            if (root.has("error")) throw ZahabError.Parse("rpc error: ${root.get("error")}")
            val result = root.optJSONObject("result")
                ?: throw ZahabError.Parse("missing result")
            if (result.optBoolean("isError")) {
                throw ZahabError.UpstreamTool(result.optJSONArray("content")?.let { arr ->
                    (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("text") }
                        .joinToString("\n")
                } ?: "unknown tool error")
            }
            val content = result.optJSONArray("content")
                ?: throw ZahabError.Parse("missing content")
            return buildString {
                for (i in 0 until content.length()) {
                    val part = content.optJSONObject(i) ?: continue
                    if (part.optString("type") == "text") {
                        if (isNotEmpty()) append('\n')
                        append(part.optString("text"))
                    }
                }
            }
        }
    }

    private suspend fun fetchFromServer(country: String): MetalPriceSnapshot =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/prices/$country")
                .get()
                .build()

            http.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw ZahabError.Http(resp.code, raw.take(200))
                parseServerJson(raw)
            }
        }

    private fun parseServerJson(raw: String): MetalPriceSnapshot {
        val root = try { JSONObject(raw) } catch (e: Exception) {
            throw ZahabError.Parse("bad JSON from server: ${raw.take(200)}")
        }
        val goldObj = root.optJSONObject("gold") ?: JSONObject()
        val silverObj = root.optJSONObject("silver") ?: JSONObject()

        val gold = buildMap {
            goldObj.keys().forEach { k -> put(k.toIntOrNull() ?: return@forEach, goldObj.optDouble(k)) }
        }
        val silver = buildMap {
            silverObj.keys().forEach { k -> put(k.toIntOrNull() ?: return@forEach, silverObj.optDouble(k)) }
        }

        return MetalPriceSnapshot(
            country = root.optString("country", "??"),
            countryName = root.optStringOrNull("country_name"),
            countryNameLocal = root.optStringOrNull("country_name_local"),
            currency = root.optString("currency", "???"),
            gold = gold,
            silver = silver,
            spotUsdPerOzGold = root.optDoubleOrNull("spot_usd_per_oz_gold"),
            spotUsdPerOzSilver = root.optDoubleOrNull("spot_usd_per_oz_silver"),
            market = root.optStringOrNull("market"),
            marketNote = root.optStringOrNull("market_note"),
            source = root.optStringOrNull("source"),
            fetchedAt = System.currentTimeMillis(),
        )
    }

    private val goldLine = Regex("""(\d+)K\s+gold\s*:\s*([\d,]+(?:\.\d+)?)\s*([A-Z]{3})/gram""", RegexOption.IGNORE_CASE)
    private val silverLine = Regex("""(\d{3})\s+(?:purity|fine)?\s*silver\s*:\s*([\d,]+(?:\.\d+)?)\s*([A-Z]{3})/gram""", RegexOption.IGNORE_CASE)
    private val spotLine = Regex("""Global spot:\s*\$([\d,]+(?:\.\d+)?)\s*USD/oz""", RegexOption.IGNORE_CASE)
    private val marketLine = Regex("""Market:\s*([^\n(]+?)\s*(?:\(([^)]*)\))?(?:\n|$)""", RegexOption.IGNORE_CASE)
    private val countryLine = Regex("""(?:Gold|Silver) price in\s+([^\n(—]+?)\s*(?:\(([^)]+)\))?\s*—""", RegexOption.IGNORE_CASE)
    private val sourceLine = Regex("""Source:\s*(\S+)""", RegexOption.IGNORE_CASE)

    private fun parseProse(country: String, goldText: String, silverText: String): MetalPriceSnapshot {
        val goldRaw = LinkedHashMap<Int, Double>()
        val silverRaw = LinkedHashMap<Int, Double>()
        var currency: String? = null

        goldLine.findAll(goldText).forEach { m ->
            val karat = m.groupValues[1].toIntOrNull() ?: return@forEach
            if (karat !in goldRaw) {
                goldRaw[karat] = m.groupValues[2].replace(",", "").toDoubleOrNull() ?: return@forEach
                currency = currency ?: m.groupValues[3]
            }
        }
        silverLine.findAll(silverText).forEach { m ->
            val purity = m.groupValues[1].toIntOrNull() ?: return@forEach
            if (purity !in silverRaw) {
                silverRaw[purity] = m.groupValues[2].replace(",", "").toDoubleOrNull() ?: return@forEach
                currency = currency ?: m.groupValues[3]
            }
        }
        if (goldRaw.isEmpty() && silverRaw.isEmpty()) {
            throw ZahabError.Parse("no prices in prose:\n${goldText.take(200)}\n---\n${silverText.take(200)}")
        }

        val gold = LinkedHashMap(goldRaw)
        goldRaw[24]?.let { base ->
            listOf(24, 22, 21, 18, 14, 9).forEach { k ->
                gold.getOrPut(k) { round4(base * k / 24.0) }
            }
        }
        val silver = LinkedHashMap(silverRaw)
        silverRaw[999]?.let { base ->
            listOf(999, 925, 900, 800).forEach { p ->
                silver.getOrPut(p) { round4(base * p / 999.0) }
            }
        }

        val countryInfo = countryLine.find(goldText) ?: countryLine.find(silverText)
        val marketInfo = marketLine.find(goldText)
        val sourceInfo = sourceLine.find(goldText) ?: sourceLine.find(silverText)
        val gSpot = spotLine.find(goldText)
        val sSpot = spotLine.find(silverText)

        return MetalPriceSnapshot(
            country = country,
            countryName = countryInfo?.groupValues?.getOrNull(1)?.trim()?.ifEmpty { null },
            countryNameLocal = countryInfo?.groupValues?.getOrNull(2)?.trim()?.ifEmpty { null },
            currency = currency ?: "???",
            gold = gold,
            silver = silver,
            spotUsdPerOzGold = gSpot?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull(),
            spotUsdPerOzSilver = sSpot?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull(),
            market = marketInfo?.groupValues?.getOrNull(1)?.trim()?.ifEmpty { null },
            marketNote = marketInfo?.groupValues?.getOrNull(2)?.ifEmpty { null },
            source = sourceInfo?.groupValues?.getOrNull(1),
            fetchedAt = System.currentTimeMillis(),
        )
    }

    private fun round4(v: Double) = Math.round(v * 10_000.0) / 10_000.0
}

private fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key) || !has(key)) null else optString(key).ifEmpty { null }

private fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (isNull(key) || !has(key)) null else optDouble(key)
