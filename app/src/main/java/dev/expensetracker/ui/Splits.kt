@file:OptIn(ExperimentalMaterial3Api::class)

package dev.expensetracker.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.expensetracker.data.Person
import dev.expensetracker.data.SplitDetail
import dev.expensetracker.data.Txn
import dev.expensetracker.data.TxnRepository
import dev.expensetracker.data.displayPayee
import dev.expensetracker.parse.Direction
import dev.expensetracker.rupees
import dev.expensetracker.toLocalDate
import dev.expensetracker.work.WEEK_MS
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@Composable
fun PersonAvatar(name: String, size: Dp = 36.dp) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.size(size).background(scheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) {
        Text(
            name.trim().take(1).uppercase(),
            style = if (size >= 36.dp) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
            color = scheme.onSecondaryContainer,
        )
    }
}

/** Split block inside the transaction sheet: who owes what, settle toggles, add person. Debits only. */
@Composable
fun SplitSection(t: Txn, repo: TxnRepository) {
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    val splits by remember(t.id) { repo.dao.splitsFor(t.id) }.collectAsStateWithLifecycle(emptyList())
    val people by remember { repo.dao.people() }.collectAsStateWithLifecycle(emptyList())
    var adding by remember { mutableStateOf(false) }
    val othersTotal = splits.sumOf { it.split.amountPaise }

    Column(
        Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(scheme.surfaceContainerHigh).padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Split", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (splits.isEmpty()) "Share this expense with others"
                    else "Your share ${rupees(t.amountPaise - othersTotal)}",
                    style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
                )
            }
            TextButton({ adding = true }, enabled = othersTotal < t.amountPaise) {
                Icon(Icons.Rounded.PersonAdd, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add person")
            }
        }
        splits.forEach { s ->
            val settled = s.split.settled
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PersonAvatar(s.personName, 32.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.personName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (settled) "Settled" else "Owes you",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (settled) scheme.positive else scheme.warning,
                    )
                }
                Text(
                    rupees(s.split.amountPaise), style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (settled) TextDecoration.LineThrough else null,
                    color = if (settled) scheme.onSurfaceVariant else scheme.onSurface,
                )
                Checkbox(
                    checked = settled,
                    onCheckedChange = { c ->
                        scope.launch { repo.dao.setSettled(s.split.id, c, if (c) System.currentTimeMillis() else null) }
                    },
                    modifier = Modifier.semantics { contentDescription = "Settled with ${s.personName}" },
                )
                IconButton({ scope.launch { repo.dao.deleteSplit(s.split) } }) {
                    Icon(Icons.Rounded.Close, "Remove ${s.personName} from split", tint = scheme.onSurfaceVariant)
                }
            }
        }
    }

    if (adding) SplitDialog(t, people, splits, onSave = { name, paise ->
        scope.launch { repo.addSplit(t, name, paise) }
        adding = false
    }) { adding = false }
}

/** Pick a known person from the dropdown or type a new name; share defaults to an equal split including the owner. */
@Composable
private fun SplitDialog(
    t: Txn, people: List<Person>, existing: List<SplitDetail>, onSave: (String, Long) -> Unit, onDismiss: () -> Unit,
) {
    val remaining = t.amountPaise - existing.sumOf { it.split.amountPaise }
    val taken = remember(existing) { existing.map { it.personName.lowercase() }.toSet() }
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf(paiseToInput(minOf(remaining, t.amountPaise / (existing.size + 2)))) }
    var expanded by remember { mutableStateOf(false) }

    val q = name.trim()
    val matches = people.filter { it.name.lowercase() !in taken && it.name.contains(q, ignoreCase = true) }
    val known = people.any { it.name.equals(q, ignoreCase = true) }
    val duplicate = q.lowercase() in taken
    val canAddNew = q.isNotEmpty() && !known && !duplicate
    val showMenu = expanded && (matches.isNotEmpty() || canAddNew)
    val paise = parseRupees(amount)
    val amountOk = paise != null && paise <= remaining
    val field = RoundedCornerShape(16.dp)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.PersonAdd, contentDescription = null) },
        title = { Text("Split with") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = showMenu, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        name, { name = it; expanded = true },
                        Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
                        label = { Text("Person") },
                        placeholder = { Text(if (people.isEmpty()) "Type a name" else "Pick or type a name") },
                        singleLine = true, shape = field, isError = duplicate,
                        supportingText = if (duplicate) ({ Text("Already in this split") }) else null,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showMenu) },
                    )
                    ExposedDropdownMenu(expanded = showMenu, onDismissRequest = { expanded = false }) {
                        matches.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                leadingIcon = { PersonAvatar(p.name, 28.dp) },
                                onClick = { name = p.name; expanded = false },
                            )
                        }
                        if (canAddNew) DropdownMenuItem(
                            text = { Text("Add “$q”") },
                            leadingIcon = { Icon(Icons.Rounded.PersonAdd, contentDescription = null) },
                            onClick = { expanded = false },
                        )
                    }
                }
                OutlinedTextField(
                    amount, { amount = it },
                    label = { Text("Their share") }, prefix = { Text("₹ ") }, singleLine = true, shape = field,
                    isError = amount.isNotBlank() && !amountOk,
                    supportingText = { Text("Up to ${rupees(remaining)} of ${rupees(t.amountPaise)}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            TextButton({ onSave(q, paise!!) }, enabled = q.isNotEmpty() && !duplicate && amountOk) { Text("Add") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}

private val splitDateFmt = DateTimeFormatter.ofPattern("d MMM")

/** "Splits to settle" list for the Bills tab: one group per person with their total and a settle button per split. */
fun LazyListScope.splitsToSettle(list: List<SplitDetail>, now: Long, onSettle: (SplitDetail) -> Unit) {
    if (list.isEmpty()) return
    item { SectionLabel("Splits to settle") }
    list.groupBy { it.split.personId }.values.forEach { rows ->
        val count = rows.size + 1
        item(key = "person-${rows.first().split.personId}") {
            val scheme = MaterialTheme.colorScheme
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 1.dp).fillMaxWidth().clip(groupShape(0, count))
                    .background(scheme.surfaceContainer).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PersonAvatar(rows.first().personName, 40.dp)
                Spacer(Modifier.width(12.dp))
                Text(rows.first().personName, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Column(horizontalAlignment = Alignment.End) {
                    Text(rupees(rows.sumOf { it.split.amountPaise }), style = MaterialTheme.typography.titleSmall, color = scheme.warning)
                    Text("owes you", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                }
            }
        }
        itemsIndexed(rows, key = { _, s -> "split-${s.split.id}" }) { i, s ->
            val scheme = MaterialTheme.colorScheme
            val overdue = now - s.split.createdAt >= WEEK_MS
            val days = ((now - s.split.createdAt) / (24 * 60 * 60 * 1000L)).toInt()
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 1.dp).fillMaxWidth().clip(groupShape(i + 1, count))
                    .background(scheme.surfaceContainer).padding(start = 64.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(displayPayee(s.payee, Direction.DEBIT), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        s.txnTime.toLocalDate().format(splitDateFmt) + " · " +
                            when (days) { 0 -> "split today"; 1 -> "split yesterday"; else -> "split $days days ago" },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (overdue) scheme.warning else scheme.onSurfaceVariant,
                    )
                }
                Text(rupees(s.split.amountPaise), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(4.dp))
                FilledTonalIconButton({ onSettle(s) }) { Icon(Icons.Rounded.Check, "Mark ${s.personName}'s split settled") }
            }
        }
        item(key = "gap-${rows.first().split.personId}") { Spacer(Modifier.height(8.dp)) }
    }
}
