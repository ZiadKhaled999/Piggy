# Supabase to NeonDB Migration Guide

This document serves as a comprehensive, deep-dive reference for how Supabase is currently implemented in the Piggy Ledger application, and outlines the exact files, strategies, logging, and architectural details required to replace it with NeonDB.

As requested, no code has been changed. This file strictly identifies the current state of the application regarding its Supabase integration and provides the blueprint for swapping to Neon without headaches.

---

## 1. Architectural Strategy & Usage of Supabase

### Why We Use Supabase Currently
Supabase is currently serving as the **Cloud Synchronization Backend** for local Room Database data. The app is designed to be "offline-first".
1. Local changes (inserts, updates) are written to the local SQLite database via Room with an `isSynced = false` flag.
2. A WorkManager (`SyncWorker`) runs periodically (or is triggered manually) to call the `SyncManager`.
3. The `SyncManager` pushes unsynced local data to Supabase (upsert) and pulls remote data from Supabase down to the local database, achieving a two-way sync.
4. Supabase’s `postgrest-kt` SDK allows the app to communicate directly with the database using an auto-generated REST API (PostgREST), bypassing the need to write a custom backend server.

### Auth Strategy (Clerk + Supabase)
We do not use Supabase's built-in Auth. Instead, we use **Clerk**.
When making requests to Supabase, we fetch a Clerk JWT token using the `"supabase"` template (`session.fetchToken(GetTokenOptions(template = "supabase"))`). This token is attached as a `Bearer` token to the `Authorization` header. If that fails, it falls back to the `SUPABASE_ANON_KEY`. Supabase evaluates this token via Row Level Security (RLS) policies.

---

## 2. File-by-File Breakdown

Here is every single file where Supabase is referenced, what it does, and how it must change for Neon.

### A. Initialization & Client Configuration
**File:** `app/src/main/java/com/oryno/piggy_ledger/data/SupabaseClient.kt`

**What it does:** 
Contains the `SupabaseManager` singleton. It instantiates the `io.github.jan.supabase.SupabaseClient` using the `SUPABASE_URL` and `SUPABASE_ANON_KEY` from `BuildConfig`. It also installs the `Postgrest` plugin for database queries.

**Neon Migration Need:**
- Delete this file entirely, or replace it with a `NeonClient.kt` / `ApiClient.kt`.
- Since Neon relies on a standard Postgres connection (and optionally a Serverless HTTP driver), you will likely need to initialize an HTTP Client (like `Ktor`) pointing to a custom backend or a Neon Edge function API. Android apps should *never* connect to a Postgres database directly via TCP/JDBC for security reasons.

### B. The Core Sync Engine
**File:** `app/src/main/java/com/oryno/piggy_ledger/service/SyncManager.kt`

**What it does:** 
This is the heaviest Supabase-coupled file. It handles pushing and pulling data for 11 tables:
`user_preferences`, `streak_dates`, `goals`, `transactions`, `loans`, `loan_payments`, `accounts`, `account_transactions`, `pending_transactions`, `ai_conversations`, `ai_chat_messages`.

**How it works (The `syncTable` function):**
1. **Push:** Uses `supabase.postgrest[tableName].upsert(toPush)` to send local rows where `isSynced = false`.
2. **Pull:** Uses `supabase.postgrest[tableName].select { filter { eq("userId", userId) } }` to fetch all rows belonging to the user.
3. **Delete:** The `deleteFromCloud(tableName, id)` function runs `supabase.postgrest[tableName].delete { filter { eq("id", id); eq("userId", user.id) } }`.
4. **Auth Headers:** The `getAuthHeader()` function extracts the Clerk JWT and manually appends it, along with `apikey` (Supabase Anon Key), to every request.

**Logs:**
It relies heavily on logging for debugging syncs:
- `Log.i("SyncManager", "Starting syncAll for userId=$userId")`
- `Log.d("SyncManager", "Pushing ${toPush.size} items to $tableName")`
- `Log.i("SyncManager", "Successfully pushed ...")`
- `Log.e("SyncManager", "Sync error on table $tableName", e)` (This is where the `401 Unauthorized` API errors were caught).

**Neon Migration Need:**
- Replace `supabase.postgrest` calls with standard HTTP calls using Ktor or Retrofit.
- You will need a REST API in front of Neon (e.g., Node.js/Express, Next.js API routes, or a Neon Serverless function) to accept the `upsert`, `select`, and `delete` payloads.
- The `getAuthHeader()` function will only need to supply the Clerk JWT (no more `apikey` required for Neon).

### C. Data Repositories
**Files:** 
- `app/src/main/java/com/oryno/piggy_ledger/data/PiggyLedgerRepository.kt`
- `app/src/main/java/com/oryno/piggy_ledger/ai/AiChatRepository.kt`

**What they do:**
They contain helper methods to insert/delete local Room data.
Crucially, when a record is deleted locally, we don't just mark it `isDeleted = true`. The repositories launch a background coroutine to call `SyncManager(context).deleteFromCloud(tableName, id)` immediately, hard-deleting the row from Supabase.

**Neon Migration Need:**
- You will update the `deleteFromCloud` call inside `SyncManager` to point to the new Neon API. The repositories themselves won't need much structural change, just ensuring they call the updated SyncManager HTTP method.

### D. Dependencies & Gradle
**Files:**
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`

**What they do:**
Define the Supabase KMP (Kotlin Multiplatform) SDK.
- `toml`: `supabase = "3.0.0"`, `supabase-postgrest = { group = "io.github.jan-tennert.supabase", name = "postgrest-kt" }`
- `gradle`: `implementation(libs.supabase.postgrest)`

**Neon Migration Need:**
- Remove the `supabase` and `supabase.postgrest` dependencies.
- You already have `ktor.client.android` and `ktor.client.core` installed, which you can use directly to communicate with your new Neon backend.

### E. Environment Variables
**Files:**
- `.env` and `.env.example`

**What they do:**
Provide compile-time secrets to `BuildConfig` via the Secrets Gradle Plugin.
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`

**Neon Migration Need:**
- Replace these with `NEON_API_URL` (pointing to the backend service you will build to proxy requests to Neon).
- You will no longer need an Anon Key.

---

## 3. Migration Strategy to NeonDB

Because Neon is a managed PostgreSQL provider (database-as-a-service) and not a full backend-as-a-service (BaaS) like Supabase, **Neon does not provide a PostgREST API out of the box.**

To migrate seamlessly:

### Step 1: Create an API Layer
You cannot securely connect an Android app directly to Neon using a JDBC connection string. You must create an API.
- Deploy a simple API (e.g., using Node.js + Express, Hono, or Cloudflare Workers) that exposes `/sync/pull`, `/sync/push`, and `/sync/delete` endpoints.
- This backend will securely hold your Neon connection string (`postgresql://user:pass@ep-rest-of-host.neon.tech/neondb`).
- This backend will verify the Clerk JWT (using the `@clerk/backend` SDK) to ensure `userId` matches.

### Step 2: Rewrite `SyncManager.kt`
Instead of calling `supabase.postgrest`, you will use Android's Ktor HTTP client.

*Old Supabase Way:*
```kotlin
supabase.postgrest["transactions"].upsert(toPush)
```

*New Neon/API Way:*
```kotlin
val response = ktorClient.post("${BuildConfig.NEON_API_URL}/transactions/upsert") {
    headers { append("Authorization", "Bearer $clerkJwt") }
    contentType(ContentType.Application.Json)
    setBody(toPush)
}
```

### Step 3: Database Schema Migration
- You will connect to NeonDB and run the exact same `CREATE TABLE` scripts found in your original `supabase_schema.sql`.
- You will drop the Supabase-specific RLS policies (`CREATE POLICY ...`) because your new custom Node.js API will handle authorization and row-level checks directly in its logic (e.g., `WHERE "userId" = clerk_user_id`).

---

## 4. Summary of Required Actions When You Are Ready

1. Ask me to remove the Supabase dependencies from Gradle.
2. Ask me to remove `SupabaseClient.kt`.
3. Give me the base URL of the new custom API you set up to sit in front of Neon.
4. Ask me to rewrite `SyncManager.kt` to use standard HTTP (Ktor) to hit your API instead of using the PostgREST SDK.
5. Update your `.env` variables to reflect the new API url.

Until you instruct me to begin this process, zero code has been modified.
