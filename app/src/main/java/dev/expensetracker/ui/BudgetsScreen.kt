package dev.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.expensetracker.data.Budget
import dev.expensetracker.data.Category
import dev.expensetracker.data.TOPIC_ORDER
import dev.expensetracker.data.TxnRepository
import dev.expensetracker.monthRange
import dev.expensetracker.rupees
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** Topic-level budgets for the current month (what alerts fire on); subtopic spend rolls up. */
@Composable
fun BudgetsScreen(repo: TxnRepository) {
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    val month = remember { YearMonth.now() }
    val (from, to) = monthRange(month)
    val cats by remember { repo.dao.categories() }.collectAsStateWithLifecycle(emptyList())
    val budgets by remember { repo.dao.budgets() }.collectAsStateWithLifecycle(emptyList())
    val spentList by remember { repo.dao.spendByTopic(from, to) }.collectAsStateWithLifecycle(emptyList())
    val budgetBy = remember(budgets) { budgets.associateBy { it.categoryId } }
    val spentBy = remember(spentList) { spentList.associate { it.categoryId to it.total } }
    val topics = remember(cats) { cats.filter { it.parentId == null }.sortedBy { TOPIC_ORDER[it.name] ?: 99 } }
    val withBudget = topics.filter { it.id in budgetBy }
    val without = topics.filter { it.id !in budgetBy }
    var editing by remember { mutableStateOf<Category?>(null) }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ScreenHeader("Budgets", subtitle = month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))) }
        item {
            val limit = withBudget.sumOf { budgetBy.getValue(it.id).monthlyLimitPaise }
            val used = withBudget.sumOf { spentBy[it.id] ?: 0 }
            Box(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(HeroSurface).padding(20.dp),
            ) {
                if (limit == 0L) Column {
                    Text("Set monthly limits", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Pick a category below. You'll get a nudge at 80% and an alert when you cross it.",
                        color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium,
                    )
                } else Column {
                    val left = limit - used
                    Text(if (left >= 0) "Left to spend" else "Over budget", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
                    Text(rupees(kotlin.math.abs(left)), color = Color.White, style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(12.dp))
                    Bar(used.toFloat() / limit, MaterialTheme.colorScheme.primary)
                    Text(
                        "${rupees(used)} of ${rupees(limit)} across ${withBudget.size} budgets",
                        Modifier.padding(top = 8.dp), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        if (withBudget.isNotEmpty()) item { SectionLabel("Your budgets") }
        itemsIndexed(withBudget, key = { _, c -> c.id }) { i, c ->
            val b = budgetBy.getValue(c.id)
            val s = spentBy[c.id] ?: 0
            val frac = s.toFloat() / b.monthlyLimitPaise
            val state = when { frac >= 1f -> scheme.error; frac >= 0.8f -> scheme.warning; else -> scheme.positive }
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 1.dp).clip(groupShape(i, withBudget.size))
                    .background(scheme.surfaceContainer).clickable { editing = c }.padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryAvatar(c, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, style = MaterialTheme.typography.titleSmall)
                        Text("${rupees(s)} of ${rupees(b.monthlyLimitPaise)}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                    val left = b.monthlyLimitPaise - s
                    Column(horizontalAlignment = Alignment.End) {
                        Text(rupees(kotlin.math.abs(left)), style = MaterialTheme.typography.titleSmall, color = state)
                        Text(if (left >= 0) "left" else "over", style = MaterialTheme.typography.labelSmall, color = state)
                    }
                }
                Bar(frac, state, Modifier.padding(top = 10.dp))
            }
        }
        item { SectionLabel(if (withBudget.isEmpty()) "Categories" else "Add a budget") }
        itemsIndexed(without, key = { _, c -> c.id }) { i, c ->
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 1.dp).clip(groupShape(i, without.size))
                    .background(scheme.surfaceContainer).clickable { editing = c }.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryAvatar(c, 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.name, style = MaterialTheme.typography.bodyLarge)
                    spentBy[c.id]?.let { Text("${rupees(it)} spent", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant) }
                }
                Box(Modifier.clip(CircleShape).background(scheme.primaryContainer).padding(6.dp)) {
                    Icon(Icons.Rounded.Add, "Set budget for ${c.name}", tint = scheme.onPrimaryContainer)
                }
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
