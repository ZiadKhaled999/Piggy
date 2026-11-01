package com.oryno.piggy_ledger.data

import android.content.Context

class SyncCheckpointStore(context: Context) {
    private val prefs = context.getSharedPreferences("sync_checkpoints", Context.MODE_PRIVATE)

    fun get(userId: String): Long = prefs.getLong("ck_$userId", 0L)

    fun put(userId: String, seq: Long) {
        prefs.edit().putLong("ck_$userId", seq).apply()
    }

    fun clear(userId: String) {
        prefs.edit().remove("ck_$userId").apply()
    }
}
