package com.oryno.piggy_ledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Goal::class, Transaction::class, Loan::class, LoanPayment::class, Account::class, AccountTransaction::class, PendingTransaction::class, AiChatMessage::class, AiConversation::class, UserPreferencesEntity::class, StreakDateEntity::class, OnboardingAnswer::class], version = 15, exportSchema = false)
abstract class PiggyLedgerDatabase : RoomDatabase() {

    companion object {
        @Volatile
        private var INSTANCE: PiggyLedgerDatabase? = null

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `onboarding_answers` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`key`))")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ai_conversations ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `ai_conversations` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("ALTER TABLE ai_chat_messages ADD COLUMN conversationId TEXT NOT NULL DEFAULT 'default'")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `ai_chat_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE loans ADD COLUMN deadline INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE loans ADD COLUMN email TEXT")
                db.execSQL("ALTER TABLE loans ADD COLUMN photoUri TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `icon_color` TEXT NOT NULL, `currency` TEXT NOT NULL, `starting_balance` REAL NOT NULL, `current_balance` REAL NOT NULL, `exclude_from_all` INTEGER NOT NULL, `credit_limit` REAL, `available_credit` REAL, `payment_due_day` INTEGER, `card_numbers` TEXT, `bank_account_no` TEXT, `provider` TEXT, `insta_pay_fee` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `account_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `account_id` INTEGER NOT NULL, `amount` REAL NOT NULL, `merchant` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `source` TEXT NOT NULL, FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_account_transactions_account_id` ON `account_transactions` (`account_id`)")
            }
        }
        
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN icon_name TEXT NOT NULL DEFAULT 'AccountBalance'")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN logo_url TEXT")
                db.execSQL("ALTER TABLE accounts ADD COLUMN local_logo_path TEXT")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pending_transactions` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`amount` REAL NOT NULL, " +
                    "`merchant` TEXT NOT NULL, " +
                    "`raw_sms_body` TEXT NOT NULL, " +
                    "`sender` TEXT NOT NULL, " +
                    "`timestamp` INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `loan_payments` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`loanId` TEXT NOT NULL, " +
                    "`amount` REAL NOT NULL, " +
                    "`timestamp` INTEGER NOT NULL, " +
                    "`note` TEXT, " +
                    "FOREIGN KEY(`loanId`) REFERENCES `loans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_loan_payments_loanId` ON `loan_payments` (`loanId`)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN label TEXT")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val tables = listOf("goals", "transactions", "loans", "loan_payments", "accounts", "account_transactions", "pending_transactions", "ai_conversations", "ai_chat_messages")
                for (table in tables) {
                    try { db.execSQL("ALTER TABLE $table ADD COLUMN userId TEXT NOT NULL DEFAULT ''") } catch(e: Exception) {}
                    try { db.execSQL("ALTER TABLE $table ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0") } catch(e: Exception) {}
                    try { db.execSQL("ALTER TABLE $table ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0") } catch(e: Exception) {}
                    try { db.execSQL("ALTER TABLE $table ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0") } catch(e: Exception) {}
                }
                db.execSQL("CREATE TABLE IF NOT EXISTS `user_preferences` (`userId` TEXT NOT NULL, `hasOnboarded` INTEGER NOT NULL, `hasLanguageSelected` INTEGER NOT NULL, `hasHeardAboutUs` INTEGER NOT NULL, `personalizedIntent` INTEGER NOT NULL, `personalizedIntensity` INTEGER NOT NULL, `savingMode` TEXT NOT NULL, `customIdentifiersJson` TEXT NOT NULL, `isBiometricLockEnabled` INTEGER NOT NULL, `isScreenshotProtectionEnabled` INTEGER NOT NULL, `isPremium` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `isSynced` INTEGER NOT NULL, PRIMARY KEY(`userId`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `streak_dates` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `dateStr` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `isSynced` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        fun getInstance(context: Context): PiggyLedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PiggyLedgerDatabase::class.java,
                    "piggy_ledger_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
    abstract fun piggyLedgerDao(): PiggyLedgerDao
}
