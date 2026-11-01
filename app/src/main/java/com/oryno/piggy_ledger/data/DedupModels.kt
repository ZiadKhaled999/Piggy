package com.oryno.piggy_ledger.data

import androidx.room.ColumnInfo

data class RecentRow(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "table_name") val table: String,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "is_income") val isIncome: Boolean,
    @ColumnInfo(name = "merchant") val merchant: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "sourceKey") val sourceKey: String?,
    @ColumnInfo(name = "reference") val reference: String?,
    @ColumnInfo(name = "rawText") val rawText: String
)
