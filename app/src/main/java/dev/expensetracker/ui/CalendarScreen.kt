package dev.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.expensetracker.compactRupees
import dev.expensetracker.data.RecurringBill
import dev.expensetracker.data.TxnRepository
import dev.expensetracker.monthRange
import dev.expensetracker.parse.Direction
import dev.expensetracker.rupees
import dev.expensetracker.toLocalDate
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** Every unpaid occurrence of each bill that falls inside [month]. */
fun billsInMonth(bills: List<RecurringBill>, month: YearMonth): Map<LocalDate, List<RecurringBill>> {
    val out = mutableMapOf<LocalDate, MutableList<RecurringBill>>()
    val start = month.atDay(1)
    val end = month.atEndOfMonth()
    for (b in bills) {
        var k = b.paidCount
        while (k < b.paidCount + 1000) { // guard: weekly bills anchored years ago
            val d = b.dueDate(k++)
            if (d > end) break
            if (d >= start) out.getOrPut(d) { mutableListOf() } += b
        }
    }
    return out
}

@Composable
fun CalendarScreen(repo: TxnRepository, month: YearMonth, onMonth: (YearMonth) -> Unit) {
    val (from, to) = monthRange(month)
    val txns by remember(from, to) { repo.dao.txns(from, to) }.collectAsStateWithLifecycle(emptyList())
    val bills by remember { repo.dao.bills() }.collectAsStateWithLifecycle(emptyList())
    var selected by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val today = LocalDate.now()

    val txnsByDay = remember(txns) { txns.groupBy { it.timestamp.toLocalDate() } }
    val spendByDay = remember(txnsByDay) {
        txnsByDay.mapValues { (_, l) -> l.filter { it.direction == Direction.DEBIT }.sumOf { it.amountPaise } }
    }
    val billsByDay = remember(bills, month) { billsInMonth(bills, month) }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
        MonthBar(month, onMonth)
        Row {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
        }
        val lead = month.atDay(1).dayOfWeek.value - 1
        val cells = List<LocalDate?>(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row {
                week.forEach { d ->
                    if (d == null) Spacer(Modifier.weight(1f))
                    else DayCell(
                        d, spendByDay[d] ?: 0, billsByDay.containsKey(d), d == selected, d == today,
                        Modifier.weight(1f),
                    ) { selected = d }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        val day = selected?.takeIf { YearMonth.from(it) == month } ?: return@Column
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text(day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), style = MaterialTheme.typography.titleMedium)
        billsByDay[day].orEmpty().forEach { b ->
            ListItem(
                headlineContent = { Text("Bill: ${b.name}") },
                trailingContent = { Text(rupees(b.amountPaise), color = MaterialTheme.colorScheme.error) },
            )
        }
        txnsByDay[day].orEmpty().forEach { t ->
            val credit = t.direction == Direction.CREDIT
            ListItem(
                headlineContent = { Text(t.counterparty.ifBlank { if (credit) "Credit" else "Debit" }) },
                trailingContent = {
                    Text((if (credit) "+" else "−") + rupees(t.amountPaise), color = if (credit) CreditGreen else MaterialTheme.colorScheme.onSurface)
                },
            )
        }
        if (billsByDay[day].isNullOrEmpty() && txnsByDay[day].isNullOrEmpty()) {
            Text("Nothing on this day", Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DayCell(
    d: LocalDate, spent: Long, billDue: Boolean, selected: Boolean, today: Boolean,
    modifier: Modifier, onClick: () -> Unit,
) {
    val bg = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        today -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    Column(
        modifier.height(60.dp).padding(2.dp).clip(RoundedCornerShape(8.dp)).background(bg).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("${d.dayOfMonth}", style = MaterialTheme.typography.bodyMedium)
        if (spent > 0) Text(compactRupees(spent), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        if (billDue) Dot(MaterialTheme.colorScheme.error, 6.dp)
    }
}
