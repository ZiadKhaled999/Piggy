package com.oryno.piggy_ledger.service

import android.content.Context

object SmsProcessor {
    suspend fun process(context: Context, sender: String, rawBody: String) {
        TransactionIngestor.ingest(
            context = context,
            senderLabel = sender,
            rawBody = rawBody,
            source = TransactionIngestor.SOURCE_SMS
        )
    }
}
