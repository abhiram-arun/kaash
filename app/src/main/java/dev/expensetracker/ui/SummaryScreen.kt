package dev.expensetracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CreditCard
import dev.expensetracker.data.AccountInfo
import dev.expensetracker.data.AccountTotal
import dev.expensetracker.data.Txn
import dev.expensetracker.data.TxnRepository
import dev.expensetracker.data.accountInfo
import dev.expensetracker.monthRange
import dev.expensetracker.rupees
import dev.expensetracker.zone
import java.time.Instant
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun SummaryScreen(repo: TxnRepository, month: YearMonth, onMonth: (YearMonth) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val (from, to) = monthRange(month)
    val byTopic by remember(from, to) { repo.dao.spendByTopic(from, to) }.collectAsStateWithLifecycle(emptyList())
    val byCat by remember(from, to) { repo.dao.spendByCategory(from, to) }.collectAsStateWithLifecycle(emptyList())
    val spent by remember(from, to) { repo.dao.total("DEBIT", from, to) }.collectAsStateWithLifecycle(0L)
    val received by remember(from, to) { repo.dao.total("CREDIT", from, to) }.collectAsStateWithLifecycle(0L)
    val cats by remember { repo.dao.categories() }.collectAsStateWithLifecycle(emptyList())
    val byId = remember(cats) { cats.associateBy { it.id } }
    var expanded by rememberSaveable { mutableStateOf<Long?>(null) }
    val accountTotals by remember(from, to) { repo.dao.accountTotals(from, to) }.collectAsStateWithLifecycle(emptyList())
    val balances by remember { repo.dao.latestBalances() }.collectAsStateWithLifecycle(emptyList())
    val accounts = remember(accountTotals, balances) { accountRows(accountTotals, balances) }

    val untaggedColor = scheme.outline
    val slices = byTopic.map { t -> (t.categoryId?.let(byId::get)?.let { scheme.accent(it.color) } ?: untaggedColor) to t.total }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ScreenHeader("Insights") { MonthSwitcher(month, onMonth); Spacer(Modifier.width(8.dp)) } }
        item {
            Column(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(scheme.surfaceContainer).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Donut(slices, scheme.surfaceContainerHighest, Modifier.size(200.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Spent", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                        Text(rupees(spent), style = MaterialTheme.typography.titleLarge)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniStat("Received", rupees(received), scheme.positive, Modifier.weight(1f))
                    val net = received - spent
                    MiniStat(
                        "Net", (if (net >= 0) "+" else "−") + rupees(kotlin.math.abs(net)),
                        if (net >= 0) scheme.positive else scheme.error, Modifier.weight(1f),
                    )
                }
            }
        }
        item { SectionLabel("By category") }
        if (byTopic.isEmpty()) item {
            Text("No spending this month", Modifier.padding(horizontal = 20.dp), color = scheme.onSurfaceVariant)
        }
        itemsIndexed(byTopic, key = { _, t -> t.categoryId ?: -1 }) { i, t ->
            val c = t.categoryId?.let(byId::get)
            val color = c?.let { scheme.accent(it.color) } ?: untaggedColor
            val pct = if (spent > 0) t.total * 100f / spent else 0f
            val isOpen = expanded == t.categoryId && c != null
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 1.dp).clip(groupShape(i, byTopic.size))
                    .background(scheme.surfaceContainer)
                    .clickable(enabled = c != null) { expanded = if (isOpen) null else t.categoryId }
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryAvatar(c, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c?.name ?: "Untagged", style = MaterialTheme.typography.titleSmall)
                        Text("%.1f%% of spend".format(pct), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    }
                    Text(rupees(t.total), style = MaterialTheme.typography.titleSmall)
                    if (c != null) Icon(
                        if (isOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        if (isOpen) "Collapse" else "Show subcategories",
                        tint = scheme.onSurfaceVariant,
                    )
                }
                Bar(pct / 100f, color, Modifier.padding(top = 10.dp))
                AnimatedVisibility(isOpen) {
                    Column(Modifier.padding(top = 8.dp)) {
                        byCat.filter { it.categoryId != null && (it.categoryId == c?.id || byId[it.categoryId]?.parentId == c?.id) }
                            .forEach { s ->
                                val sub = byId[s.categoryId]
                                Row(Modifier.padding(start = 52.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(iconFor(sub), null, Modifier.size(18.dp), tint = color)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (sub?.id == c?.id) "General" else sub?.name.orEmpty(),
                                        Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(rupees(s.total), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                                }
                            }
                    }
                }
            }
        }

        if (accounts.isNotEmpty()) item {
            val inBank = accounts.filter { !it.info.isCard }.sumOf { it.balance?.balancePaise ?: 0 }
            Row(Modifier.padding(end = 20.dp), verticalAlignment = Alignment.Bottom) {
                SectionLabel("By account", Modifier.weight(1f))
                Text(
                    "${rupees(inBank)} in bank", Modifier.padding(bottom = 8.dp),
                    style = MaterialTheme.typography.labelLarge, color = scheme.secondary,
                )
            }
        }
        itemsIndexed(accounts, key = { _, a -> "acct-${a.account}" }) { i, a ->
            AccountRow(a, spent, Modifier.padding(horizontal = 16.dp, vertical = 1.dp).clip(groupShape(i, accounts.size)))
        }
    }
}

/** One account's month split plus its latest SMS-reported balance (available limit for credit cards). */
private class AccountRowData(val account: String, val info: AccountInfo, val month: AccountTotal?, val balance: Txn?)

private fun accountRows(totals: List<AccountTotal>, balances: List<Txn>): List<AccountRowData> {
    val byAcct = totals.associateBy { it.account }
    val balBy = balances.groupBy { it.account }.mapValues { (_, l) -> l.maxBy { it.timestamp } }
    return (byAcct.keys + balBy.keys).distinct().map { acct ->
        val t = byAcct[acct]
        val b = balBy[acct]
        AccountRowData(acct, accountInfo(b?.raw ?: t!!.sample), t, b)
    }.sortedWith(compareByDescending<AccountRowData> { it.month?.spent ?: 0 }.thenBy { it.account })
}

private val asOfFmt = DateTimeFormatter.ofPattern("d MMM, h:mm a")

@Composable
private fun AccountRow(a: AccountRowData, monthSpent: Long, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val tint = if (a.info.isCard) scheme.tertiary else scheme.secondary
    Column(modifier.fillMaxWidth().background(scheme.surfaceContainer).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAvatar(if (a.info.isCard) Icons.Rounded.CreditCard else Icons.Rounded.AccountBalance, tint, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${a.info.bank} ··${a.account}", style = MaterialTheme.typography.titleSmall)
                Text(
                    (if (a.info.isCard) "Credit card" else "Bank account") +
                        (a.balance?.let { " · as of " + Instant.ofEpochMilli(it.timestamp).atZone(zone).format(asOfFmt) } ?: ""),
                    style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(a.balance?.balancePaise?.let(::rupees) ?: "—", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (a.balance == null) "no balance in SMS" else if (a.info.isCard) "available" else "balance",
                    style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant,
                )
            }
        }
        a.month?.let { m ->
            Row(Modifier.padding(top = 12.dp)) {
                Text("Spent ${rupees(m.spent)}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                if (m.received > 0) Text("Received ${rupees(m.received)}", style = MaterialTheme.typography.bodySmall, color = scheme.positive)
            }
            Bar(if (monthSpent > 0) m.spent.toFloat() / monthSpent else 0f, tint, Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun Donut(slices: List<Pair<Color, Long>>, track: Color, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = 26.dp.toPx()
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(stroke / 2, stroke / 2)
        val total = slices.sumOf { it.second }.toFloat()
        drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        if (total <= 0f) return@Canvas
        val gap = if (slices.size > 1) 2.5f else 0f
        var start = -90f
        slices.forEach { (color, v) ->
            val sweep = 360f * v / total
            drawArc(color, start + gap / 2, (sweep - gap).coerceAtLeast(0.6f), false, topLeft, arcSize, style = Stroke(stroke))
            start += sweep
        }
    }
}

@Composable
fun Bar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(color.copy(alpha = 0.16f))) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp).clip(CircleShape).background(color))
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color, modifier: Modifier) = Column(
    modifier.clip(RoundedCornerShape(18.dp)).background(color.copy(alpha = 0.12f)).padding(14.dp),
) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.titleMedium, color = color)
}
