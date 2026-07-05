package com.oryno.piggy_ledger.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Goal::class, Transaction::class, Loan::class], version = 2, exportSchema = false)
abstract class PiggyLedgerDatabase : RoomDatabase() {
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE loans ADD COLUMN deadline INTEGER")
            }
        }
    }
    abstract fun piggyLedgerDao(): PiggyLedgerDao
}
