package com.oryno.piggy_ledger.data

import android.util.Log
import com.oryno.piggy_ledger.BuildConfig
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiClient {
    // FIX(sync): timeouts so a stalled radio can't hang syncAll() (24 sequential
    // calls) forever; log level gated so release builds never dump JWT/PII bodies.
    val ktorClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
        install(Logging) {
            level = if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("KtorHttp", message)
                }
            }
        }
    }
}
