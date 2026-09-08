package com.thetmyopaing.ledger.data

import androidx.room.withTransaction

class LedgerRepository(private val db: LedgerDatabase) {
    suspend fun load(): LedgerSnapshot = LedgerSnapshot(
        agents = db.agentDao().all(),
        customers = db.customerDao().all(),
        records = db.bettingDao().records(),
        entries = db.bettingDao().entries(),
        settings = db.settingsDao().settings(),
        specialLimits = db.settingsDao().specialLimits(),
        closedNumbers = db.closedNumberDao().all(),
        closedDays = db.globalDao().closedDays(),
        winningNumbers = db.globalDao().winningNumbers(),
    )

    suspend fun saveBet(record: BettingRecordEntity, entries: List<BettingEntryEntity>) {
        db.withTransaction {
            db.bettingDao().saveRecord(record)
            db.bettingDao().saveEntries(entries)
        }
    }

    suspend fun saveAgent(agent: AgentEntity) = db.agentDao().save(agent)

    suspend fun saveCustomer(customer: CustomerEntity) = db.customerDao().save(customer)

    suspend fun saveClosedDay(day: ClosedDayEntity) = db.globalDao().saveClosedDay(day)

    suspend fun deleteClosedDay(id: String) = db.globalDao().deleteClosedDay(id)

    suspend fun saveWinningNumber(number: WinningNumberEntity) =
        db.globalDao().saveWinningNumber(number)

    suspend fun deleteWinningNumber(id: String) = db.globalDao().deleteWinningNumber(id)

    suspend fun saveClosedNumber(number: ClosedNumberEntity) =
        db.closedNumberDao().save(number)

    suspend fun deleteClosedNumber(id: String) = db.closedNumberDao().delete(id)

    suspend fun saveSettings(settings: CustomerSettingsEntity) =
        db.settingsDao().saveSettings(settings)

    suspend fun saveSpecialLimit(limit: SpecialLimitEntity) =
        db.settingsDao().saveSpecialLimit(limit)

    suspend fun deleteSpecialLimit(id: String) = db.settingsDao().deleteSpecialLimit(id)

    suspend fun deleteBet(recordId: String) {
        db.withTransaction {
            db.bettingDao().deleteEntries(recordId)
            db.bettingDao().deleteRecord(recordId)
        }
    }
}

data class LedgerSnapshot(
    val agents: List<AgentEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val records: List<BettingRecordEntity> = emptyList(),
    val entries: List<BettingEntryEntity> = emptyList(),
    val settings: List<CustomerSettingsEntity> = emptyList(),
    val specialLimits: List<SpecialLimitEntity> = emptyList(),
    val closedNumbers: List<ClosedNumberEntity> = emptyList(),
    val closedDays: List<ClosedDayEntity> = emptyList(),
    val winningNumbers: List<WinningNumberEntity> = emptyList(),
)