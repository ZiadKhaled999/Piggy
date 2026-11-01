package com.oryno.piggy_ledger.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SyncChange(
    val table: String,
    val data: JsonObject
)

@Serializable
data class PushRequest(val changes: List<SyncChange>)

@Serializable
data class ChangeId(val table: String, val id: String)

@Serializable
data class PushResponse(
    val accepted: List<ChangeId> = emptyList(),
    val conflicts: List<SyncChange> = emptyList(),
    val skipped: List<ChangeId> = emptyList()
)

@Serializable
data class PullResponse(
    val changes: List<SyncChange> = emptyList(),
    val checkpoint: Long = 0L,
    val hasMore: Boolean = false
)
