package dev.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    val scheme = MaterialTheme.colorScheme
    val (from, to) = monthRange(month)
    val txns by remember(from, to) { repo.dao.txns(from, to) }.collectAsStateWithLifecycle(emptyList())
    val bills by remember { repo.dao.bills() }.collectAsStateWithLifecycle(emptyList())
    val cats by remember { repo.dao.categories() }.collectAsStateWithLifecycle(emptyList())
    val byId = remember(cats) { cats.associateBy { it.id } }
    var selected by rememberSaveable { mutableStateOf<LocalDate?>(LocalDate.now()) }
    val today = LocalDate.now()

    val txnsByDay = remember(txns) { txns.groupBy { it.timestamp.toLocalDate() } }
    val spendByDay = remember(txnsByDay) {
        txnsByDay.mapValues { (_, l) -> l.filter { it.direction == Direction.DEBIT }.sumOf { it.amountPaise } }
    }
    val maxSpend = spendByDay.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val billsByDay = remember(bills, month) { billsInMonth(bills, month) }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        ScreenHeader("Calendar") { MonthSwitcher(month, onMonth); Spacer(Modifier.width(8.dp)) }
        Column(
            Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(28.dp)).background(scheme.surfaceContainer).padding(10.dp),
        ) {
            Row {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                    Text(
                        it, Modifier.weight(1f).padding(vertical = 6.dp), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant,
                    )
                }
            }
            val lead = month.atDay(1).dayOfWeek.value - 1
            val cells = List<LocalDate?>(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
            cells.chunked(7).forEach { week ->
                Row {
                    week.forEach { d ->
                        if (d == null) Spacer(Modifier.weight(1f))
                        else DayCell(
                            d, spendByDay[d] ?: 0, maxSpend, billsByDay.containsKey(d), d == selected, d == today,
                            Modifier.weight(1f),
                        ) { selected = d }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Row(Modifier.padding(start = 8.dp, top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Less", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                listOf(0.10f, 0.18f, 0.28f, 0.38f).forEach {
                    Box(Modifier.padding(horizontal = 2.dp).size(12.dp).clip(RoundedCornerShape(3.dp)).background(scheme.primary.copy(alpha = it)))
                }
                Text("More", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Box(Modifier.size(8.dp).background(scheme.tertiary, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text("Bill due", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
        }

        val day = selected?.takeIf { YearMonth.from(it) == month } ?: return@Column
        val dayBills = billsByDay[day].orEmpty()
        val dayTxns = txnsByDay[day].orEmpty()
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)) {
            Text(day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            spendByDay[day]?.takeIf { it > 0 }?.let { Text("−${rupees(it)}", style = MaterialTheme.typography.titleSmall, color = scheme.onSurfaceVariant) }
        }
        val rows = dayBills.size + dayTxns.size
        if (rows == 0) Text("Nothing on this day", Modifier.padding(horizontal = 20.dp), color = scheme.onSurfaceVariant)
        dayBills.forEachIndexed { i, b ->
            DetailRow(i, rows, { IconAvatar(Icons.Rounded.Event, scheme.tertiary, 40.dp) }, b.name, "Bill due", rupees(b.amountPaise), scheme.tertiary)
        }
        dayTxns.forEachIndexed { i, t ->
            val c = t.categoryId?.let(byId::get)
            val credit = t.direction == Direction.CREDIT
            DetailRow(
                dayBills.size + i, rows, { CategoryAvatar(c, 40.dp) }, displayName(t), categoryLabel(c, byId),
                (if (credit) "+" else "−") + rupees(t.amountPaise), if (credit) scheme.positive else scheme.onSurface,
            )
        }
    }
}

@Composable
private fun DetailRow(
    i: Int, count: Int, avatar: @Composable () -> Unit, title: String, subtitle: String, amount: String, amountColor: Color,
) {
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 1.dp).fillMaxWidth().clip(groupShape(i, count))
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        avatar()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Text(amount, style = MaterialTheme.typography.titleSmall, color = amountColor)
    }
}

@Composable
private fun DayCell(
    d: LocalDate, spent: Long, maxSpend: Long, billDue: Boolean, selected: Boolean, today: Boolean,
    modifier: Modifier, onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val bg = when {
        selected -> scheme.primary
        spent > 0 -> scheme.primary.copy(alpha = 0.08f + 0.30f * spent / maxSpend) // capped so day numbers keep ≥4.5:1
        else -> Color.Transparent
    }
    val fg = if (selected) scheme.onPrimary else scheme.onSurface
    Column(
        modifier.height(58.dp).padding(2.dp).clip(RoundedCornerShape(14.dp)).background(bg)
            .then(if (today && !selected) Modifier.border(1.5.dp, scheme.primary, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("${d.dayOfMonth}", style = MaterialTheme.typography.titleSmall, color = fg)
        if (spent > 0) Text(compactRupees(spent), style = MaterialTheme.typography.labelSmall, maxLines = 1, color = fg.copy(alpha = 0.8f))
        if (billDue) Box(Modifier.padding(top = 2.dp).size(6.dp).background(if (selected) scheme.onPrimary else scheme.tertiary, CircleShape))
    }
}
