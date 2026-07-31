package com.oryno.piggy_ledger.data

import com.oryno.piggy_ledger.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseManager {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            // You can install Auth later if needed, but since we are using Clerk for auth,
            // we will pass the JWT token in Postgrest headers manually or through Auth module's custom token.
        }
    }
}
