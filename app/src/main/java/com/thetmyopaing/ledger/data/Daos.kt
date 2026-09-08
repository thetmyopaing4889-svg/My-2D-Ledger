package com.thetmyopaing.ledger.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AgentDao {
    @Query("SELECT * FROM agents ORDER BY name")
    suspend fun all(): List<AgentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(agent: AgentEntity)

    @Query("DELETE FROM agents WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name")
    suspend fun all(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE agentId = :agentId ORDER BY name")
    suspend fun forAgent(agentId: String): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface BettingDao {
    @Query("SELECT * FROM betting_records ORDER BY drawDate DESC, createdAt DESC")
    suspend fun records(): List<BettingRecordEntity>

    @Query("SELECT * FROM betting_entries")
    suspend fun entries(): List<BettingEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRecord(record: BettingRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEntries(entries: List<BettingEntryEntity>)

    @Query("DELETE FROM betting_records WHERE id = :id")
    suspend fun deleteRecord(id: String)

    @Query("DELETE FROM betting_entries WHERE recordId = :recordId")
    suspend fun deleteEntries(recordId: String)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM customer_settings")
    suspend fun settings(): List<CustomerSettingsEntity>

    @Query("SELECT * FROM special_limits")
    suspend fun specialLimits(): List<SpecialLimitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: CustomerSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSpecialLimit(limit: SpecialLimitEntity)

    @Query("DELETE FROM special_limits WHERE id = :id")
    suspend fun deleteSpecialLimit(id: String)
}

@Dao
interface GlobalDao {
    @Query("SELECT * FROM closed_days ORDER BY date")
    suspend fun closedDays(): List<ClosedDayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveClosedDay(day: ClosedDayEntity)

    @Query("DELETE FROM closed_days WHERE id = :id")
    suspend fun deleteClosedDay(id: String)

    @Query("SELECT * FROM winning_numbers ORDER BY date DESC, session")
    suspend fun winningNumbers(): List<WinningNumberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWinningNumber(number: WinningNumberEntity)

    @Query("DELETE FROM winning_numbers WHERE id = :id")
    suspend fun deleteWinningNumber(id: String)
}

@Dao
interface ClosedNumberDao {
    @Query("SELECT * FROM closed_numbers")
    suspend fun all(): List<ClosedNumberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(number: ClosedNumberEntity)

    @Query("DELETE FROM closed_numbers WHERE id = :id")
    suspend fun delete(id: String)
}