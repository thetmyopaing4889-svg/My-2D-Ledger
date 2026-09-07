package com.thetmyopaing.ledger.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thetmyopaing.ledger.LedgerApplication
import com.thetmyopaing.ledger.data.AgentEntity
import com.thetmyopaing.ledger.data.BettingEntryEntity
import com.thetmyopaing.ledger.data.BettingRecordEntity
import com.thetmyopaing.ledger.data.ClosedDayEntity
import com.thetmyopaing.ledger.data.ClosedNumberEntity
import com.thetmyopaing.ledger.data.CustomerEntity
import com.thetmyopaing.ledger.data.CustomerSettingsEntity
import com.thetmyopaing.ledger.data.LedgerSnapshot
import com.thetmyopaing.ledger.data.SpecialLimitEntity
import com.thetmyopaing.ledger.data.WinningNumberEntity
import com.thetmyopaing.ledger.domain.PreviewRow
import com.thetmyopaing.ledger.domain.buildPreview
import com.thetmyopaing.ledger.domain.normalizeDigit
import com.thetmyopaing.ledger.domain.parseManual
import com.thetmyopaing.ledger.domain.parseQuickFormat
import com.thetmyopaing.ledger.domain.parseAmount
import com.thetmyopaing.ledger.domain.QuickFormat
import com.thetmyopaing.ledger.domain.parseAhKway
import com.thetmyopaing.ledger.domain.parsePatThi
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LedgerUiState(
    val snapshot: LedgerSnapshot = LedgerSnapshot(),
    val busy: Boolean = true,
    val message: String? = null,
)

class LedgerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as LedgerApplication).repository
    private val _uiState = MutableStateFlow(LedgerUiState())
    val uiState: StateFlow<LedgerUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true)
            _uiState.value = _uiState.value.copy(snapshot = repository.load(), busy = false)
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun saveAgent(
        id: String?,
        name: String,
        address: String,
        phone: String,
        rate: String,
        remark: String,
    ) {
        val cleanName = name.trim()
        val parsedRate = rate.trim().toIntOrNull()
        if (cleanName.isBlank()) return notify("Agent အမည် ထည့်ပါ")
        if (parsedRate == null || parsedRate <= 0) return notify("Agent Rate မှန်ကန်စွာ ထည့်ပါ")
        viewModelScope.launch {
            repository.saveAgent(
                AgentEntity(
                    id = id ?: UUID.randomUUID().toString(),
                    name = cleanName,
                    address = address.trim(),
                    phone = phone.trim(),
                    rate = parsedRate,
                    remark = remark.trim(),
                ),
            )
            refresh()
        }
    }

    fun saveCustomer(
        id: String?,
        agentId: String,
        name: String,
        address: String,
        phone: String,
        remark: String,
    ) {
        if (name.trim().isBlank()) return notify("Customer အမည် ထည့်ပါ")
        viewModelScope.launch {
            repository.saveCustomer(
                CustomerEntity(
                    id = id ?: UUID.randomUUID().toString(),
                    agentId = agentId,
                    name = name.trim(),
                    address = address.trim(),
                    phone = phone.trim(),
                    remark = remark.trim(),
                ),
            )
            refresh()
        }
    }

    fun saveBet(
        customerId: String,
        agentId: String,
        date: String,
        session: String,
        rawInput: String,
    ) {
        viewModelScope.launch {
            val state = _uiState.value.snapshot
            if (state.closedDays.any { it.date == date }) {
                return@launch notify("ဒီရက်သည် Closed Day ဖြစ်သောကြောင့် စာရင်းသွင်း၍မရပါ")
            }
            val parsed = parseManual(rawInput)
            if (!parsed.isValid) return@launch notify(parsed.errors.firstOrNull() ?: "Input မမှန်ပါ")
            val preview = buildPreview(
                parsed,
                customerId,
                agentId,
                state.entries,
                state.settings,
                state.specialLimits,
                state.closedNumbers,
            )
            val invalid = preview.firstOrNull { !it.allowed }
            if (invalid != null) {
                return@launch notify(
                    when {
                        invalid.closed -> "${invalid.digit} သည် Closed Number ဖြစ်ပါသည်"
                        else -> "${invalid.digit} အတွက် Limit ကျော်နေပါသည်"
                    },
                )
            }
            val recordId = UUID.randomUUID().toString()
            repository.saveBet(
                BettingRecordEntity(
                    id = recordId,
                    customerId = customerId,
                    drawDate = date,
                    session = session,
                    rawInput = rawInput,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
                parsed.bets.map {
                    BettingEntryEntity(
                        id = UUID.randomUUID().toString(),
                        recordId = recordId,
                        customerId = customerId,
                        digit = it.digit,
                        amount = it.amount,
                    )
                },
            )
            refresh()
            notify("စာရင်းသွင်းပြီးပါပြီ")
        }
    }

    fun preview(
        customerId: String,
        agentId: String,
        rawInput: String,
    ): List<PreviewRow> {
        val state = _uiState.value.snapshot
        val parsed = parseManual(rawInput)
        if (!parsed.isValid) return emptyList()
        return buildPreview(
            parsed,
            customerId,
            agentId,
            state.entries,
            state.settings,
            state.specialLimits,
            state.closedNumbers,
        )
    }

    fun quickPreview(
        customerId: String,
        agentId: String,
        format: QuickFormat,
        amount: String,
    ): List<PreviewRow> {
        val state = _uiState.value.snapshot
        return buildPreview(
            parseQuickFormat(format, amount),
            customerId,
            agentId,
            state.entries,
            state.settings,
            state.specialLimits,
            state.closedNumbers,
        )
    }

    fun saveClosedDay(date: String) {
        if (date.isBlank()) return notify("ရက်စွဲရွေးပါ")
        viewModelScope.launch {
            repository.saveClosedDay(ClosedDayEntity(UUID.randomUUID().toString(), date))
            refresh()
        }
    }

    fun deleteClosedDay(id: String) {
        viewModelScope.launch {
            repository.deleteClosedDay(id)
            refresh()
        }
    }

    fun saveWinningNumber(date: String, session: String, digit: String) {
        val normalized = normalizeDigit(digit)
            ?: return notify("Winning Number သည် 00–99 ဖြစ်ရပါမည်")
        viewModelScope.launch {
            repository.saveWinningNumber(
                WinningNumberEntity(
                    id = _uiState.value.snapshot.winningNumbers.firstOrNull {
                        it.date == date && it.session == session
                    }?.id ?: UUID.randomUUID().toString(),
                    date = date,
                    session = session,
                    digit = normalized,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            refresh()
        }
    }

    fun deleteWinningNumber(id: String) {
        viewModelScope.launch {
            repository.deleteWinningNumber(id)
            refresh()
        }
    }

    fun saveClosedNumber(agentId: String, digit: String) {
        val normalized = normalizeDigit(digit)
            ?: return notify("Closed Number သည် 00–99 ဖြစ်ရပါမည်")
        viewModelScope.launch {
            repository.saveClosedNumber(
                ClosedNumberEntity(UUID.randomUUID().toString(), agentId, normalized),
            )
            refresh()
        }
    }

    fun deleteClosedNumber(id: String) {
        viewModelScope.launch {
            repository.deleteClosedNumber(id)
            refresh()
        }
    }

    fun saveSettings(customerId: String, commissionRate: String, allLimit: String) {
        val commission = commissionRate.toIntOrNull() ?: 0
        if (commission !in 0..100) return notify("Commission Rate သည် 0–100 ဖြစ်ရပါမည်")
        val limit = if (allLimit.isBlank()) null else parseAmount(allLimit)
        if (allLimit.isNotBlank() && limit == null) return notify("Limit မှန်ကန်စွာ ထည့်ပါ")
        viewModelScope.launch {
            repository.saveSettings(CustomerSettingsEntity(customerId, commission, limit))
            refresh()
        }
    }

    fun saveSpecialLimit(customerId: String, digit: String, amount: String) {
        val normalized = normalizeDigit(digit)
            ?: return notify("ဂဏန်းသည် 00–99 ဖြစ်ရပါမည်")
        val parsed = parseAmount(amount) ?: return notify("Limit ပမာဏမှန်ကန်စွာ ထည့်ပါ")
        viewModelScope.launch {
            repository.saveSpecialLimit(
                SpecialLimitEntity(UUID.randomUUID().toString(), customerId, normalized, parsed),
            )
            refresh()
        }
    }

    fun deleteSpecialLimit(id: String) {
        viewModelScope.launch {
            repository.deleteSpecialLimit(id)
            refresh()
        }
    }

    fun deleteBet(recordId: String) {
        viewModelScope.launch {
            repository.deleteBet(recordId)
            refresh()
        }
    }

    private fun notify(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }
}