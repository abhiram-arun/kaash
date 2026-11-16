package dev.expensetracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.expensetracker.data.Budget
import dev.expensetracker.data.Category
import dev.expensetracker.data.TxnRepository
import dev.expensetracker.monthRange
import dev.expensetracker.rupees
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** Budgets always track the current month, which is what the alerts fire on. */
@Composable
fun BudgetsScreen(repo: TxnRepository) {
    val scope = rememberCoroutineScope()
    val month = remember { YearMonth.now() }
    val (from, to) = monthRange(month)
    val cats by remember { repo.dao.categories() }.collectAsStateWithLifecycle(emptyList())
    val budgets by remember { repo.dao.budgets() }.collectAsStateWithLifecycle(emptyList())
    val spentList by remember { repo.dao.spendByCategory(from, to) }.collectAsStateWithLifecycle(emptyList())
    val budgetBy = remember(budgets) { budgets.associateBy { it.categoryId } }
    val spentBy = remember(spentList) { spentList.associate { it.categoryId to it.total } }
    var editing by remember { mutableStateOf<Category?>(null) }

    Column {
        Text(
            "Budgets · ${month.format(DateTimeFormatter.ofPattern("MMMM"))}",
            Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        LazyColumn {
            items(cats, key = { it.id }) { c ->
                val b = budgetBy[c.id]
                val s = spentBy[c.id] ?: 0
                ListItem(
                    modifier = Modifier.clickable { editing = c },
                    leadingContent = { Dot(Color(c.color)) },
                    headlineContent = { Text(c.name) },
                    supportingContent = {
                        if (b == null) Text("No limit · ${rupees(s)} spent · tap to set")
                        else Column {
                            Text("${rupees(s)} of ${rupees(b.monthlyLimitPaise)}")
                            LinearProgressIndicator(
                                progress = { (s.toFloat() / b.monthlyLimitPaise).coerceAtMost(1f) },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                color = if (s >= b.monthlyLimitPaise) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
        }
    }

    editing?.let { c ->
        AmountDialog("${c.name} monthly limit", budgetBy[c.id]?.monthlyLimitPaise, onSave = { paise ->
            scope.launch {
                if (paise == null) repo.dao.deleteBudget(c.id) else repo.dao.upsertBudget(Budget(c.id, paise))
            }
            editing = null
        }) { editing = null }
    }
}
