package com.oryno.piggy_ledger.service

import android.content.Context
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import java.util.UUID

object DuplicateDetector {

    private const val WINDOW_MS = 3 * 60_000L
    private const val FLAG_WINDOW_MS = 24 * 60 * 60_000L
    private const val FEE_ABS = 1.0
    private const val FEE_PCT = 0.005

    data class IncomingRow(
        val amount: Double,
        val isIncome: Boolean,
        val merchant: String,
        val timestamp: Long,
        val source: String,
        val sourceKey: String?,
        val rawText: String,
        val reference: String? = null
    )

    data class Decision(
        val action: Action,
        val matchedRowId: String? = null,
        val matchedTable: String? = null,
        val groupId: String = UUID.randomUUID().toString()
    )

    enum class Action { INSERT, DROP_EXACT, INSERT_FLAGGED, MERGE }

    suspend fun evaluate(context: Context, incoming: IncomingRow): Decision {
        val dao = PiggyLedgerDatabase.getInstance(context).piggyLedgerDao()
        val since = incoming.timestamp - FLAG_WINDOW_MS
        val recent = dao.getRecentRowsForDedup(since)
        return evaluatePure(incoming, recent)
    }

    internal fun evaluatePure(incoming: IncomingRow, recent: List<com.oryno.piggy_ledger.data.RecentRow>): Decision {
        if (incoming.sourceKey != null &&
            recent.any { it.source == incoming.source && it.sourceKey == incoming.sourceKey }
        ) {
            return Decision(Action.DROP_EXACT)
        }

        val cross = recent.filter {
            it.source != incoming.source &&
                it.isIncome == incoming.isIncome &&
                kotlin.math.abs(it.timestamp - incoming.timestamp) <= FLAG_WINDOW_MS &&
                amountMatches(it.amount, incoming.amount)
        }

        if (cross.isEmpty()) return Decision(Action.INSERT)

        val best = cross.minByOrNull {
            kotlin.math.abs(it.timestamp - incoming.timestamp)
        } ?: return Decision(Action.INSERT)

        val gap = kotlin.math.abs(best.timestamp - incoming.timestamp)
        val corroborated = hasCorroboratingToken(best, incoming)

        return when {
            gap <= WINDOW_MS && corroborated -> Decision(
                action = Action.MERGE,
                matchedRowId = best.id,
                matchedTable = best.table
            )
            else -> Decision(
                action = Action.INSERT_FLAGGED,
                matchedRowId = best.id,
                matchedTable = best.table
            )
        }
    }

    private fun amountMatches(a: Double, b: Double): Boolean {
        val tolerance = maxOf(FEE_ABS, FEE_PCT * maxOf(kotlin.math.abs(a), kotlin.math.abs(b)))
        return kotlin.math.abs(kotlin.math.abs(a) - kotlin.math.abs(b)) <= tolerance
    }

    private fun hasCorroboratingToken(existing: com.oryno.piggy_ledger.data.RecentRow, incoming: IncomingRow): Boolean {
        if (existing.sourceKey != null && incoming.sourceKey != null &&
            existing.sourceKey == incoming.sourceKey) return true
        if (existing.reference != null && incoming.reference != null &&
            existing.reference.equals(incoming.reference, ignoreCase = true)) return true
        return false
    }
}
