package com.oryno.piggy_ledger

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient

fun test() {
    val client: SupabaseClient = createSupabaseClient("", "") { }
    // client.postgrest["a"].upsert(listOf("a")) { headers { append("Authorization", "Bearer token") } }
}
