package com.thetmyopaing.ledger.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "agents")
data class AgentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String = "",
    val phone: String = "",
    val rate: Int = 80,
    val remark: String = "",
)

@Entity(
    tableName = "customers",
    indices = [Index("agentId")],
)
data class CustomerEntity(
    @PrimaryKey val id: String,
    val agentId: String,
    val name: String,
    val address: String = "",
    val phone: String = "",
    val remark: String = "",
)

@Entity(
    tableName = "betting_records",
    indices = [Index(value = ["customerId", "drawDate", "session"])],
)
data class BettingRecordEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val drawDate: String,
    val session: String,
    val rawInput: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "betting_entries",
    indices = [Index("recordId"), Index(value = ["customerId", "digit"])],
)
data class BettingEntryEntity(
    @PrimaryKey val id: String,
    val recordId: String,
    val customerId: String,
    val digit: String,
    val amount: Long,
)

@Entity(
    tableName = "customer_settings",
)
data class CustomerSettingsEntity(
    @PrimaryKey val customerId: String,
    val commissionRate: Int = 0,
    val allLimit: Long? = null,
)

@Entity(
    tableName = "special_limits",
    indices = [Index(value = ["customerId", "digit"], unique = true)],
)
data class SpecialLimitEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val digit: String,
    val amount: Long,
)

@Entity(
    tableName = "closed_numbers",
    indices = [Index(value = ["agentId", "digit"], unique = true)],
)
data class ClosedNumberEntity(
    @PrimaryKey val id: String,
    val agentId: String,
    val digit: String,
)

@Entity(
    tableName = "closed_days",
    indices = [Index(value = ["date"], unique = true)],
)
data class ClosedDayEntity(
    @PrimaryKey val id: String,
    val date: String,
)

@Entity(
    tableName = "winning_numbers",
    indices = [Index(value = ["date", "session"], unique = true)],
)
data class WinningNumberEntity(
    @PrimaryKey val id: String,
    val date: String,
    val session: String,
    val digit: String,
    val createdAt: Long,
    val updatedAt: Long,
)