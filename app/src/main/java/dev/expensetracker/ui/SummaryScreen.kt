package dev.expensetracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.expensetracker.data.TxnRepository
import dev.expensetracker.monthRange
import dev.expensetracker.rupees
import java.time.YearMonth

@Composable
fun SummaryScreen(repo: TxnRepository, month: YearMonth, onMonth: (YearMonth) -> Unit) {
    val (from, to) = monthRange(month)
    val totals by remember(from, to) { repo.dao.spendByCategory(from, to) }.collectAsStateWithLifecycle(emptyList())
    val spent by remember(from, to) { repo.dao.total("DEBIT", from, to) }.collectAsStateWithLifecycle(0L)
    val received by remember(from, to) { repo.dao.total("CREDIT", from, to) }.collectAsStateWithLifecycle(0L)
    val cats by remember { repo.dao.categories() }.collectAsStateWithLifecycle(emptyList())
    val catById = remember(cats) { cats.associateBy { it.id } }

    Column(Modifier.padding(horizontal = 16.dp)) {
        MonthBar(month, onMonth)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Spent", spent, Modifier.weight(1f))
            Stat("Received", received, Modifier.weight(1f))
        }
        val max = totals.maxOfOrNull { it.total }?.coerceAtLeast(1) ?: 1
        LazyColumn(Modifier.padding(top = 12.dp)) {
            if (totals.isEmpty()) item { Text("No spending this month", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(totals, key = { it.categoryId ?: -1 }) { t ->
                val c = catById[t.categoryId]
                val pct = if (spent > 0) t.total * 100 / spent else 0
                Column(Modifier.padding(vertical = 8.dp)) {
                    Row {
                        Text(c?.name ?: "Untagged", Modifier.weight(1f))
                        Text("${rupees(t.total)}  ·  $pct%", style = MaterialTheme.typography.bodyMedium)
                    }
                    LinearProgressIndicator(
                        progress = { t.total.toFloat() / max },
                        modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 4.dp),
                        color = c?.let { Color(it.color) } ?: MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, paise: Long, modifier: Modifier) = Card(modifier) {
    Column(Modifier.padding(16.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(rupees(paise), style = MaterialTheme.typography.titleLarge)
    }
}
