@file:OptIn(ExperimentalMaterial3Api::class)

package dev.expensetracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.text.input.KeyboardType
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
private const val DAY_MS = 86_400_000L

@Composable
fun BillsScreen(repo: TxnRepository) {
    val scope = rememberCoroutineScope()
    val bills by remember { repo.dao.bills() }.collectAsStateWithLifecycle(emptyList())
    var editing by remember { mutableStateOf<RecurringBill?>(null) }
    val today = LocalDate.now()

    Box(Modifier.fillMaxSize()) {
        LazyColumn {
            item { Text("Recurring bills", Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge) }
            if (bills.isEmpty()) item {
                Text(
                    "No bills yet. Tap + to add rent, EMIs, subscriptions…",
                    Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(bills.sortedBy { it.dueDate() }, key = { it.id }) { b ->
                val due = b.dueDate()
                val days = ChronoUnit.DAYS.between(today, due)
                val status = when {
                    days < 0 -> "Overdue by ${-days} day${if (days == -1L) "" else "s"}"
                    days == 0L -> "Due today"
                    days == 1L -> "Due tomorrow"
                    else -> "Due in $days days"
                }
                ListItem(
                    modifier = Modifier.clickable { editing = b },
                    headlineContent = { Text(b.name) },
                    supportingContent = {
                        Text(
                            "${rupees(b.amountPaise)} · ${b.period.name.lowercase()} · ${due.format(dueFmt)}\n$status",
                            color = if (due < today) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        TextButton({ scope.launch { repo.dao.upsertBill(b.copy(paidCount = b.paidCount + 1)) } }) {
                            Text("Mark paid")
                        }
                    },
                )
            }
        }
        FloatingActionButton(
            { editing = RecurringBill(name = "", amountPaise = 0, period = Period.MONTHLY, anchorEpochDay = today.toEpochDay()) },
            Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Filled.Add, "Add bill") }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (bill.id == 0L) "New bill" else "Edit bill") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(
                    amount, { amount = it }, label = { Text("Amount (₹)") }, singleLine = true,
                    isError = amount.isNotBlank() && paise == null,
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
                OutlinedButton({ picking = true }, Modifier.fillMaxWidth()) { Text("Next due: ${due.format(dueFmt)}") }
                OutlinedTextField(
                    remind, { remind = it }, label = { Text("Remind days before") }, singleLine = true,
                    isError = remindDays == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    payee, { payee = it }, singleLine = true,
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
