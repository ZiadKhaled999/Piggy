package com.oryno.piggy_ledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Goal::class, Transaction::class, Loan::class], version = 2, exportSchema = false)
abstract class PiggyLedgerDatabase : RoomDatabase() {
    companion object {
        @Volatile
        private var INSTANCE: PiggyLedgerDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE loans ADD COLUMN deadline INTEGER")
            }
        }

        fun getInstance(context: Context): PiggyLedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PiggyLedgerDatabase::class.java,
                    "piggy_ledger_db"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
    abstract fun piggyLedgerDao(): PiggyLedgerDao
}
