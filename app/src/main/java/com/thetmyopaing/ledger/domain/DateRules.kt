package com.thetmyopaing.ledger.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val businessZone: ZoneId = ZoneId.of("Asia/Rangoon")
private val businessDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun businessToday(): String = LocalDate.now(businessZone).format(businessDateFormatter)

fun parseBusinessDate(value: String): LocalDate? = try {
    LocalDate.parse(value.trim(), businessDateFormatter)
} catch (_: DateTimeParseException) {
    null
}

fun isValidSession(value: String): Boolean = value == "မနက်" || value == "ညနေ"