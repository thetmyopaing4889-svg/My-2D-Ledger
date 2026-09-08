package com.thetmyopaing.ledger.domain

import com.thetmyopaing.ledger.data.BettingEntryEntity
import com.thetmyopaing.ledger.data.ClosedNumberEntity
import com.thetmyopaing.ledger.data.CustomerSettingsEntity
import com.thetmyopaing.ledger.data.SpecialLimitEntity

data class PreviewRow(
    val digit: String,
    val current: Long,
    val input: Long,
    val limit: Long?,
    val closed: Boolean,
) {
    val after: Long get() = current + input
    val allowed: Boolean get() = !closed && (limit == null || after <= limit)
}

fun buildPreview(
    parsed: ParseResult,
    customerId: String,
    agentId: String,
    entries: List<BettingEntryEntity>,
    settings: List<CustomerSettingsEntity>,
    specialLimits: List<SpecialLimitEntity>,
    closedNumbers: List<ClosedNumberEntity>,
): List<PreviewRow> {
    val current = entries.filter { it.customerId == customerId }
        .groupingBy { it.digit }
        .fold(0L) { total, entry -> total + entry.amount }
    val customerSettings = settings.firstOrNull { it.customerId == customerId }
    val special = specialLimits.filter { it.customerId == customerId }.associate { it.digit to it.amount }
    val closed = closedNumbers.filter { it.agentId == agentId }.map { it.digit }.toSet()
    val incoming = parsed.bets.groupingBy { it.digit }
        .fold(0L) { total, bet -> total + bet.amount }

    return incoming.map { (digit, amount) ->
        PreviewRow(
            digit = digit,
            current = current[digit] ?: 0L,
            input = amount,
            limit = special[digit] ?: customerSettings?.allLimit,
            closed = digit in closed,
        )
    }.sortedBy { it.digit }
}

fun commission(totalBet: Long, rate: Int): Long = totalBet * rate / 100

fun payout(winningBet: Long, agentRate: Int): Long = winningBet * agentRate

fun profitLoss(totalBet: Long, winningBet: Long, agentRate: Int): Long =
    totalBet - payout(winningBet, agentRate)