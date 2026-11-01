package com.oryno.piggy_ledger.service

import com.oryno.piggy_ledger.data.RecentRow
import org.junit.Assert.assertEquals
import org.junit.Test

class DuplicateDetectorTest {

    private fun row(
        amount: Double,
        isIncome: Boolean,
        source: String,
        sourceKey: String? = null,
        rawText: String = "text",
        timestamp: Long = 1_000_000L,
        table: String = "account_transactions",
        id: String = "id-$source-$timestamp",
        reference: String? = null
    ) = RecentRow(id, table, amount, isIncome, "merchant", timestamp, source, sourceKey, reference, rawText)

    @Test
    fun sameSourceKey_dropsExact() {
        val recent = listOf(row(100.0, false, "AUTOMATIC_SMS", sourceKey = "k1"))
        val incoming = DuplicateDetector.IncomingRow(100.0, false, "m", 1_000_000L, "AUTOMATIC_SMS", "k1", "text")
        assertEquals(DuplicateDetector.Action.DROP_EXACT, DuplicateDetector.evaluatePure(incoming, recent).action)
    }

    @Test
    fun crossChannelWithin3MinWithInstaPayToken_merges() {
        val recent = listOf(row(100.0, false, "AUTOMATIC_SMS", sourceKey = "k1", rawText = "instapay transfer", timestamp = 1_000_000L))
        val incoming = DuplicateDetector.IncomingRow(100.0, false, "m", 1_000_000L + 60_000, "AUTOMATIC_NOTIFICATION", "k1", "instapay alert")
        assertEquals(DuplicateDetector.Action.MERGE, DuplicateDetector.evaluatePure(incoming, recent).action)
    }

    @Test
    fun crossChannelWithin3MinNoToken_flagsBoth() {
        val recent = listOf(row(100.0, false, "AUTOMATIC_SMS", rawText = "text", timestamp = 1_000_000L))
        val incoming = DuplicateDetector.IncomingRow(100.0, false, "m", 1_000_000L + 60_000, "AUTOMATIC_NOTIFICATION", null, "other")
        assertEquals(DuplicateDetector.Action.INSERT_FLAGGED, DuplicateDetector.evaluatePure(incoming, recent).action)
    }

    @Test
    fun crossChannel5MinGap_flagsBoth() {
        val recent = listOf(row(100.0, false, "AUTOMATIC_SMS", rawText = "instapay", timestamp = 1_000_000L))
        val incoming = DuplicateDetector.IncomingRow(100.0, false, "m", 1_000_000L + 300_000, "AUTOMATIC_NOTIFICATION", null, "instapay")
        assertEquals(DuplicateDetector.Action.INSERT_FLAGGED, DuplicateDetector.evaluatePure(incoming, recent).action)
    }

    @Test
    fun crossChannelBeyond24H_inserts() {
        val recent = listOf(row(100.0, false, "AUTOMATIC_SMS", rawText = "instapay", timestamp = 1_000_000L))
        val incoming = DuplicateDetector.IncomingRow(100.0, false, "m", 1_000_000L + 25 * 60 * 60 * 1000L, "AUTOMATIC_NOTIFICATION", null, "instapay")
        assertEquals(DuplicateDetector.Action.INSERT, DuplicateDetector.evaluatePure(incoming, recent).action)
    }

    @Test
    fun crossChannelDirectionMismatch_inserts() {
        val recent = listOf(row(100.0, true, "AUTOMATIC_SMS", rawText = "instapay", timestamp = 1_000_000L))
        val incoming = DuplicateDetector.IncomingRow(100.0, false, "m", 1_000_000L + 60_000, "AUTOMATIC_NOTIFICATION", null, "instapay")
        assertEquals(DuplicateDetector.Action.INSERT, DuplicateDetector.evaluatePure(incoming, recent).action)
    }

    @Test
    fun amountWithinFeeTolerance_merges() {
        val recent = listOf(row(100.0, false, "AUTOMATIC_SMS", sourceKey = "k1", rawText = "instapay", timestamp = 1_000_000L))
        val incoming = DuplicateDetector.IncomingRow(100.10, false, "m", 1_000_000L + 60_000, "AUTOMATIC_NOTIFICATION", "k1", "instapay")
        assertEquals(DuplicateDetector.Action.MERGE, DuplicateDetector.evaluatePure(incoming, recent).action)
    }

    @Test
    fun amountOutsideFeeTolerance_inserts() {
        val recent = listOf(row(100.0, false, "AUTOMATIC_SMS", rawText = "instapay", timestamp = 1_000_000L))
        val incoming = DuplicateDetector.IncomingRow(105.0, false, "m", 1_000_000L + 60_000, "AUTOMATIC_NOTIFICATION", null, "instapay")
        assertEquals(DuplicateDetector.Action.INSERT, DuplicateDetector.evaluatePure(incoming, recent).action)
    }

    @Test
    fun last4DigitsMatch_merges() {
        val recent = listOf(row(100.0, false, "AUTOMATIC_SMS", rawText = "card ...4521", timestamp = 1_000_000L))
        val incoming = DuplicateDetector.IncomingRow(100.0, false, "m", 1_000_000L + 60_000, "AUTOMATIC_NOTIFICATION", null, "acct 4521")
        assertEquals(DuplicateDetector.Action.INSERT_FLAGGED, DuplicateDetector.evaluatePure(incoming, recent).action)
    }

    @Test
    fun referenceTokenOverlap_merges() {
        val recent = listOf(row(100.0, false, "AUTOMATIC_SMS", rawText = "REF123456 payment", timestamp = 1_000_000L, reference = "REF123456"))
        val incoming = DuplicateDetector.IncomingRow(100.0, false, "m", 1_000_000L + 60_000, "AUTOMATIC_NOTIFICATION", null, "ref REF123456", reference = "REF123456")
        assertEquals(DuplicateDetector.Action.MERGE, DuplicateDetector.evaluatePure(incoming, recent).action)
    }
}
