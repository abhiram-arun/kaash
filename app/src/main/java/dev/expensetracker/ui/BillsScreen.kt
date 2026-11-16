@file:OptIn(ExperimentalMaterial3Api::class)

package dev.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.expensetracker.data.Period
import dev.expensetracker.data.RecurringBill
import dev.expensetracker.data.TxnRepository
import dev.expensetracker.parse.merchantKey
import dev.expensetracker.rupees
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val dueFmt = DateTimeFormatter.ofPattern("d MMM yyyy")
private val monFmt = DateTimeFormatter.ofPattern("MMM")
private const val DAY_MS = 86_400_000L

@Composable
fun BillsScreen(repo: TxnRepository) {
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    val bills by remember { repo.dao.bills() }.collectAsStateWithLifecycle(emptyList())
    val unsettled by remember { repo.dao.unsettled() }.collectAsStateWithLifecycle(emptyList())
    var editing by remember { mutableStateOf<RecurringBill?>(null) }
    val today = LocalDate.now()
    val sorted = bills.sortedBy { it.dueDate() }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
            item { ScreenHeader("Bills") }
            item {
                val soon = sorted.filter { ChronoUnit.DAYS.between(today, it.dueDate()) <= 30 }
                val overdue = sorted.count { it.dueDate() < today }
                Box(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp))
                        .background(HeroSurface).padding(20.dp),
                ) {
                    Column {
                        Text("Due in the next 30 days", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
                        Text(rupees(soon.sumOf { it.amountPaise }), color = Color.White, style = MaterialTheme.typography.displaySmall)
                        Text(
                            "${soon.size} bill${if (soon.size == 1) "" else "s"}" + if (overdue > 0) " · $overdue overdue" else "",
                            color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
            splitsToSettle(unsettled, System.currentTimeMillis()) { s ->
                scope.launch { repo.dao.setSettled(s.split.id, true, System.currentTimeMillis()) }
            }
            if (sorted.isEmpty()) item {
                Text(
                    "No bills yet. Add rent, EMIs, subscriptions and get reminded before they're due.",
                    Modifier.padding(20.dp), color = scheme.onSurfaceVariant,
                )
            } else item { SectionLabel("Upcoming") }
            itemsIndexed(sorted, key = { _, b -> b.id }) { i, b ->
                val due = b.dueDate()
                val days = ChronoUnit.DAYS.between(today, due)
                val (status, color) = when {
                    days < 0 -> "Overdue by ${-days} day${if (days == -1L) "" else "s"}" to scheme.error
                    days == 0L -> "Due today" to scheme.warning
                    days == 1L -> "Due tomorrow" to scheme.warning
                    days <= b.remindDaysBefore -> "Due in $days days" to scheme.warning
                    else -> "Due in $days days" to scheme.primary
                }
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 1.dp).clip(groupShape(i, sorted.size))
                        .background(scheme.surfaceContainer).clickable { editing = b }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.15f)),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Text("${due.dayOfMonth}", style = MaterialTheme.typography.titleMedium, color = color)
                        Text(due.format(monFmt).uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(b.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(rupees(b.amountPaise) + " · " + b.period.name.lowercase(), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        Text(status, style = MaterialTheme.typography.labelMedium, color = color)
                    }
                    FilledTonalIconButton({ scope.launch { repo.dao.upsertBill(b.copy(paidCount = b.paidCount + 1)) } }) {
                        Icon(Icons.Rounded.Check, "Mark ${b.name} paid")
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { editing = RecurringBill(name = "", amountPaise = 0, period = Period.MONTHLY, anchorEpochDay = today.toEpochDay()) },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Add bill") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = scheme.primary, contentColor = scheme.onPrimary,
        )
    }

    editing?.let { b ->
        BillDialog(
            b,
            onSave = { scope.launch { repo.dao.upsertBill(it) }; editing = null },
            onDelete = if (b.id != 0L) ({ scope.launch { repo.dao.deleteBill(b) }; editing = null }) else null,
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun BillDialog(bill: RecurringBill, onSave: (RecurringBill) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(bill.name) }
    var amount by remember { mutableStateOf(paiseToInput(bill.amountPaise.takeIf { it > 0 })) }
    var period by remember { mutableStateOf(bill.period) }
    var due by remember { mutableStateOf(bill.dueDate()) }
    var remind by remember { mutableStateOf(bill.remindDaysBefore.toString()) }
    var payee by remember { mutableStateOf(bill.merchantKey.orEmpty()) }
    var picking by remember { mutableStateOf(false) }

    val paise = parseRupees(amount)
    val remindDays = remind.toIntOrNull()?.takeIf { it in 0..60 }
    val valid = name.isNotBlank() && paise != null && remindDays != null
    val field = RoundedCornerShape(16.dp)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Event, contentDescription = null) },
        title = { Text(if (bill.id == 0L) "New bill" else "Edit bill") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, shape = field)
                OutlinedTextField(
                    amount, { amount = it }, label = { Text("Amount") }, prefix = { Text("₹ ") }, singleLine = true,
                    isError = amount.isNotBlank() && paise == null, shape = field,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Period.entries.forEachIndexed { i, p ->
                        SegmentedButton(
                            selected = period == p, onClick = { period = p },
                            shape = SegmentedButtonDefaults.itemShape(i, Period.entries.size),
                        ) { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    }
                }
                OutlinedButton({ picking = true }, Modifier.fillMaxWidth(), shape = field) {
                    Icon(Icons.Rounded.Event, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Next due: ${due.format(dueFmt)}")
                }
                OutlinedTextField(
                    remind, { remind = it }, label = { Text("Remind days before") }, singleLine = true,
                    isError = remindDays == null, shape = field,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    payee, { payee = it }, singleLine = true, shape = field,
                    label = { Text("Payee to auto-match (optional)") },
                    supportingText = { Text("e.g. AIRTEL. Blank matches by amount ±5%.") },
                )
                onDelete?.let { TextButton(it) { Text("Delete bill", color = MaterialTheme.colorScheme.error) } }
            }
        },
        confirmButton = {
            TextButton({
                onSave(
                    // Re-anchor on the chosen due date so the schedule restarts from there.
                    bill.copy(
                        name = name.trim(), amountPaise = paise!!, period = period, anchorEpochDay = due.toEpochDay(),
                        paidCount = 0, remindDaysBefore = remindDays!!,
                        merchantKey = merchantKey(payee).ifBlank { null },
                    )
                )
            }, enabled = valid) { Text("Save") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )

    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = due.toEpochDay() * DAY_MS)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton({
                    state.selectedDateMillis?.let { due = LocalDate.ofEpochDay(it / DAY_MS) } // picker returns UTC midnight
                    picking = false
                }) { Text("OK") }
            },
        ) { DatePicker(state) }
    }
}
