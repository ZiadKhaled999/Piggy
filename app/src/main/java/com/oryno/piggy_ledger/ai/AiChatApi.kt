package com.oryno.piggy_ledger.ai

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface GroqApi {
    @POST("openai/v1/chat/completions")
    @Headers("Content-Type: application/json")
    suspend fun getCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: GroqRequest
    ): GroqResponse
}
