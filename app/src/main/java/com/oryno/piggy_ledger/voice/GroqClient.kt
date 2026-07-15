package com.oryno.piggy_ledger.voice

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import com.squareup.moshi.JsonClass
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GroqResponse(val text: String)

@JsonClass(generateAdapter = true)
data class GroqChatRequest(
    val model: String,
    val messages: List<GroqChatMessage>,
    val response_format: GroqResponseFormat? = null
)

@JsonClass(generateAdapter = true)
data class GroqChatMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class GroqResponseFormat(
    val type: String
)

@JsonClass(generateAdapter = true)
data class GroqChatResponse(
    val choices: List<GroqChoice>
)

@JsonClass(generateAdapter = true)
data class GroqChoice(
    val message: GroqChatMessage
)

interface GroqApi {
    @Multipart
    @POST("v1/audio/transcriptions")
    suspend fun transcribe(
        @Header("Authorization") auth: String,
        @Part file: MultipartBody.Part,
        @Part("model") model: okhttp3.RequestBody,
        @Part("language") language: okhttp3.RequestBody,
        @Part("prompt") prompt: okhttp3.RequestBody,
        @Part("response_format") responseFormat: okhttp3.RequestBody
    ): GroqResponse

    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") auth: String,
        @retrofit2.http.Body request: GroqChatRequest
    ): GroqChatResponse
}

object GroqClient {
    private const val BASE_URL = "https://api.groq.com/openai/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: GroqApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GroqApi::class.java)
    }
}
