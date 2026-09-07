package com.thetmyopaing.ledger.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BettingParserTest {
    @Test
    fun basicSeparatorsProduceTheSameBet() {
        listOf("10.100", "10-100", "10 100").forEach { input ->
            val result = parseManual(input)
            assertTrue(result.isValid, input)
            assertEquals("10", result.bets.single().digit)
            assertEquals(100, result.bets.single().amount)
        }
    }

    @Test
    fun multipleNumbersAreNormalizedAndSummed() {
        val result = parseManual("10.13.14 100")
        assertEquals(listOf("10", "13", "14"), result.bets.map { it.digit })
        assertEquals(300, result.total)
    }

    @Test
    fun reversePreservesLeadingZero() {
        val result = parseManual("10R100")
        assertEquals(listOf("01", "10"), result.bets.map { it.digit }.sorted())
        assertEquals(200, result.total)
    }

    @Test
    fun powerHasExactlyTenEntries() {
        val result = parseQuickFormat(QuickFormat.POWER, "100")
        assertTrue(result.isValid)
        assertEquals(10, result.bets.size)
        assertEquals(1000, result.total)
    }

    @Test
    fun ahKwayDeduplicatesRepeatedDigits() {
        val result = parseAhKway("345", "100", includeDoubles = false)
        assertEquals(listOf("34", "35", "43", "45", "53", "54"), result.bets.map { it.digit }.sorted())
    }

    @Test
    fun patThiContains99Once() {
        val result = parsePatThi("9", "100")
        assertEquals(19, result.bets.size)
        assertEquals(1, result.bets.count { it.digit == "99" })
    }
}