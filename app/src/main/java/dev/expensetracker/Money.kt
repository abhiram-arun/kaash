package dev.expensetracker

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

private val inr = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

fun rupees(paise: Long): String = inr.format(BigDecimal.valueOf(paise, 2))

/** "₹950", "₹1.2k", "₹27.5k" for tight calendar cells. */
fun compactRupees(paise: Long): String {
    val r = paise / 100.0
    return if (r < 1000) "₹${r.toLong()}" else "₹%.1fk".format(r / 1000)
}

val zone: ZoneId get() = ZoneId.systemDefault()

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

/** Inclusive epoch-millis range covering the whole month in the device time zone. */
fun monthRange(ym: YearMonth): Pair<Long, Long> =
    ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() to
        ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
