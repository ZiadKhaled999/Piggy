package com.oryno.piggy_ledger.service

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oryno.piggy_ledger.data.PiggyLedgerDao
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncWorkerTest {

    private lateinit var context: Context
    private lateinit var db: PiggyLedgerDatabase
    private lateinit var dao: PiggyLedgerDao

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PiggyLedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.piggyLedgerDao()
        
        // Clear any existing sync state
        val prefs = context.getSharedPreferences("sync_state", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun needsFullResyncFlagWorksCorrectly() = runBlocking {
        // Initially false
        assertFalse(SyncWorker.needsFullResync(context))
        
        // Set flag via shared prefs
        val prefs = context.getSharedPreferences("sync_state", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("needs_full_resync", true).apply()
        
        assertTrue(SyncWorker.needsFullResync(context))
        
        // Clear flag via static method
        SyncWorker.clearFullResyncFlag(context)
        assertFalse(SyncWorker.needsFullResync(context))
    }

    @Test
    fun scheduleOneTimeUsesUniqueNameConstant() {
        assertEquals("SyncWork", SyncWorker.UNIQUE_ONE_TIME)
    }

    @Test
    fun schedulePeriodicUsesUniqueNameConstant() {
        assertEquals("SyncPeriodic", SyncWorker.UNIQUE_PERIODIC)
    }
}