package com.thetmyopaing.ledger.domain

data class ParsedBet(
    val digit: String,
    val amount: Long,
    val source: String,
)

data class ParseResult(
    val bets: List<ParsedBet> = emptyList(),
    val errors: List<String> = emptyList(),
) {
    val total: Long get() = bets.sumOf { it.amount }
    val isValid: Boolean get() = errors.isEmpty() && bets.isNotEmpty()
}

enum class QuickFormat(
    val title: String,
    val description: String,
) {
    POWER("ပါဝါ", "05 50 16 61 27 72 38 83 49 94"),
    NAKHAT("နက္ခတ်", "07 70 18 81 24 42 35 53 69 96"),
    DOUBLE("အပူး", "00 11 22 33 44 55 66 77 88 99"),
    BROTHER("ညီအကို", "01 10 12 21 23 32 34 43 45 54 56 65 67 76 78 87 89 98 09 90"),
    TOP("ထိပ်စည်း", "90 91 92 93 94 95 96 97 98 99"),
    LAST("နောက်ပိတ်", "09 19 29 39 49 59 69 79 89 99"),
)

private val burmeseDigits = mapOf(
    '၀' to '0', '၁' to '1', '၂' to '2', '၃' to '3', '၄' to '4',
    '၅' to '5', '၆' to '6', '၇' to '7', '၈' to '8', '၉' to '9',
)

fun normalizeDigits(value: String): String =
    value.map { burmeseDigits[it] ?: it }.joinToString("")

fun normalizeDigit(value: String): String? {
    val digits = normalizeDigits(value).filter(Char::isDigit)
    if (digits.isEmpty() || digits.length > 2) return null
    return digits.padStart(2, '0')
}

fun parseQuickFormat(format: QuickFormat, amountText: String): ParseResult {
    val amount = parseAmount(amountText) ?: return ParseResult(errors = listOf("ပမာဏမှန်ကန်စွာ ထည့်ပါ"))
    val digits = format.description.split(" ")
    return ParseResult(digits.map { ParsedBet(it, amount, format.title) })
}

fun parseAhKway(source: String, amountText: String, includeDoubles: Boolean): ParseResult {
    val normalized = normalizeDigits(source).filter(Char::isDigit)
    val amount = parseAmount(amountText) ?: return ParseResult(errors = listOf("ပမာဏမှန်ကန်စွာ ထည့်ပါ"))
    if (normalized.length < 3) return ParseResult(errors = listOf("အခွေအတွက် အနည်းဆုံး ဂဏန်း ၃ လုံးလိုပါသည်"))

    val values = linkedSetOf<String>()
    for (i in normalized.indices) {
        for (j in i + 1 until normalized.length) {
            val a = normalized[i]
            val b = normalized[j]
            values += "$a$b"
            values += "$b$a"
        }
        if (includeDoubles) values += "$normalized[i]${normalized[i]}"
    }
    return ParseResult(values.map { ParsedBet(it, amount, if (includeDoubles) "အခွေပူး" else "အခွေ") })
}

fun parsePatThi(source: String, amountText: String): ParseResult {
    val sourceDigit = normalizeDigits(source).filter(Char::isDigit)
    val amount = parseAmount(amountText) ?: return ParseResult(errors = listOf("ပမာဏမှန်ကန်စွာ ထည့်ပါ"))
    if (sourceDigit.length != 1) return ParseResult(errors = listOf("ပတ်သီးအတွက် ဂဏန်းတစ်လုံးပဲ ထည့်ပါ"))
    val digit = sourceDigit[0]
    val values = linkedSetOf<String>()
    for (prefix in '0'..'9') {
        values += "$prefix$digit"
        values += "$digit$prefix"
    }
    return ParseResult(values.map { ParsedBet(it, amount, "ပတ်သီး") })
}

fun parseManual(raw: String): ParseResult {
    val input = normalizeDigits(raw).replace(",", "").trim()
    if (input.isBlank()) return ParseResult()

    val reverseMatch = Regex("""^(.+?)\s*[rR]\s*(\d+)$""").matchEntire(input)
    if (reverseMatch != null) {
        val amount = parseAmount(reverseMatch.groupValues[2])
            ?: return ParseResult(errors = listOf("ပမာဏမှန်ကန်စွာ ထည့်ပါ"))
        val numbers = splitNumberPart(reverseMatch.groupValues[1])
        if (numbers.isEmpty()) return ParseResult(errors = listOf("ဂဏန်းမတွေ့ပါ"))
        val output = linkedMapOf<String, ParsedBet>()
        numbers.forEach { rawDigit ->
            val digit = normalizeDigit(rawDigit)
                ?: return ParseResult(errors = listOf("$rawDigit သည် 00–99 မဟုတ်ပါ"))
            listOf(digit, digit.reversed()).forEach { value ->
                output[value] = ParsedBet(value, amount, "$digit R")
            }
        }
        return ParseResult(output.values.toList())
    }

    val match = Regex("""^(.+?)[./\-\s]+(\d+)$""").matchEntire(input)
        ?: return ParseResult(errors = listOf("ဥပမာ 10.100 သို့မဟုတ် 10R100 ပုံစံသုံးပါ"))
    val amount = parseAmount(match.groupValues[2])
        ?: return ParseResult(errors = listOf("ပမာဏမှန်ကန်စွာ ထည့်ပါ"))
    val numbers = splitNumberPart(match.groupValues[1])
    if (numbers.isEmpty()) return ParseResult(errors = listOf("ဂဏန်းမတွေ့ပါ"))

    val output = linkedMapOf<String, ParsedBet>()
    numbers.forEach { rawDigit ->
        val digit = normalizeDigit(rawDigit)
            ?: return ParseResult(errors = listOf("$rawDigit သည် 00–99 မဟုတ်ပါ"))
        val previous = output[digit]
        output[digit] = ParsedBet(digit, (previous?.amount ?: 0L) + amount, rawDigit)
    }
    return ParseResult(output.values.toList())
}

private fun splitNumberPart(value: String): List<String> =
    value.split(Regex("""[./\-\s]+""")).filter { it.isNotBlank() }

fun parseAmount(value: String): Long? =
    normalizeDigits(value).replace(",", "").trim().toLongOrNull()?.takeIf { it > 0 }

fun allDigits(): List<String> = (0..99).map { it.toString().padStart(2, '0') }