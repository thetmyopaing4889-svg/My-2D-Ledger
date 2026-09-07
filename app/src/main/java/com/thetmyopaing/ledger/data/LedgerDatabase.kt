package com.thetmyopaing.ledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AgentEntity::class,
        CustomerEntity::class,
        BettingRecordEntity::class,
        BettingEntryEntity::class,
        CustomerSettingsEntity::class,
        SpecialLimitEntity::class,
        ClosedNumberEntity::class,
        ClosedDayEntity::class,
        WinningNumberEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun agentDao(): AgentDao
    abstract fun customerDao(): CustomerDao
    abstract fun bettingDao(): BettingDao
    abstract fun settingsDao(): SettingsDao
    abstract fun globalDao(): GlobalDao
    abstract fun closedNumberDao(): ClosedNumberDao

    companion object {
        @Volatile
        private var instance: LedgerDatabase? = null

        fun get(context: Context): LedgerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LedgerDatabase::class.java,
                    "my_2d_ledger.db",
                ).build().also { instance = it }
            }
    }
}