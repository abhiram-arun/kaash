package dev.expensetracker.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmsFailed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.expensetracker.BuildConfig
import dev.expensetracker.data.Category
import dev.expensetracker.data.Txn
import dev.expensetracker.data.TxnRepository
import dev.expensetracker.ingest.backfillInbox
import dev.expensetracker.monthRange
import dev.expensetracker.parse.Direction
import dev.expensetracker.parse.SAMPLE_SMS
import dev.expensetracker.rupees
import dev.expensetracker.toLocalDate
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.format.DateTimeFormatter

val CreditGreen = Color(0xFF43A047)
private val dayFmt = DateTimeFormatter.ofPattern("EEE, d MMM")
private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")

private fun hasSms(ctx: Context) = listOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
    .all { ctx.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(repo: TxnRepository, month: YearMonth, onMonth: (YearMonth) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var q by rememberSaveable { mutableStateOf("") }
    var dir by rememberSaveable { mutableStateOf<String?>(null) }
    var acct by rememberSaveable { mutableStateOf<String?>(null) }
    var cat by rememberSaveable { mutableStateOf<Long?>(null) }
    var untagged by rememberSaveable { mutableStateOf(false) }

    val (from, to) = monthRange(month)
    val txns by remember(from, to, q, dir, acct, cat, untagged) {
        repo.dao.txns(from, to, q.trim(), cat, untagged, dir, acct)
    }.collectAsStateWithLifecycle(emptyList())
    val cats by remember { repo.dao.categories() }.collectAsStateWithLifecycle(emptyList())
    val catById = remember(cats) { cats.associateBy { it.id } }
    val accounts by remember { repo.dao.accounts() }.collectAsStateWithLifecycle(emptyList())

    var tagging by remember { mutableStateOf<Txn?>(null) }
    var showUnparsed by remember { mutableStateOf(false) }

    var smsGranted by remember { mutableStateOf(hasSms(ctx)) }
    val rescan: () -> Unit = {
        scope.launch {
            val n = backfillInbox(ctx, repo)
            Toast.makeText(ctx, "Scanned $n SMS", Toast.LENGTH_SHORT).show()
        }
    }
    val askPerms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        smsGranted = hasSms(ctx)
        if (smsGranted) rescan()
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonthBar(month, onMonth, Modifier.weight(1f))
            IconButton(rescan, enabled = smsGranted) { Icon(Icons.Filled.Refresh, "Rescan SMS inbox") }
            IconButton({ showUnparsed = true }) { Icon(Icons.Filled.SmsFailed, "Unrecognised bank SMS") }
            if (BuildConfig.DEBUG) IconButton({
                scope.launch {
                    val now = System.currentTimeMillis()
                    SAMPLE_SMS.forEachIndexed { i, (s, b) -> repo.ingest(s, b, now - i * 3_600_000L) }
                }
            }) { Icon(Icons.Filled.BugReport, "Inject sample SMS") }
        }

        if (!smsGranted) Card(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Allow SMS access so bank alerts are read automatically.")
                Text(
                    "If Android says the setting is restricted: App info → ⋮ → Allow restricted settings, then try again.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button({
                    val perms = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                    if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
                    askPerms.launch(perms.toTypedArray())
                }) { Text("Grant access") }
            }
        }

        OutlinedTextField(
            q, { q = it },
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search payee") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(dir == "DEBIT", { dir = if (dir == "DEBIT") null else "DEBIT" }, { Text("Debits") })
            FilterChip(dir == "CREDIT", { dir = if (dir == "CREDIT") null else "CREDIT" }, { Text("Credits") })
            FilterChip(untagged, { untagged = !untagged; if (untagged) cat = null }, { Text("Untagged") })
            accounts.forEach { a -> FilterChip(acct == a, { acct = if (acct == a) null else a }, { Text("··$a") }) }
            cats.forEach { c ->
                FilterChip(cat == c.id, { cat = if (cat == c.id) null else c.id; untagged = false }, { Text(c.name) })
            }
        }

        val grouped = remember(txns) { txns.groupBy { it.timestamp.toLocalDate() } }
        LazyColumn(Modifier.weight(1f)) {
            if (txns.isEmpty()) item {
                Text("No transactions", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            grouped.forEach { (day, list) ->
                stickyHeader(key = day.toEpochDay()) {
                    Text(
                        day.format(dayFmt),
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                items(list, key = { it.id }) { t -> TxnRow(t, catById[t.categoryId]) { tagging = t } }
            }
        }
    }

    tagging?.let { t ->
        TagDialog(cats, t.categoryId, onPick = { c -> scope.launch { repo.tag(t, c) }; tagging = null }) { tagging = null }
    }
    if (showUnparsed) UnparsedDialog(repo) { showUnparsed = false }
}

@Composable
private fun TxnRow(t: Txn, cat: Category?, onClick: () -> Unit) {
    val credit = t.direction == Direction.CREDIT
    val time = Instant.ofEpochMilli(t.timestamp).atZone(dev.expensetracker.zone).format(timeFmt)
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Dot(cat?.let { Color(it.color) } ?: MaterialTheme.colorScheme.outlineVariant) },
        headlineContent = { Text(t.counterparty.ifBlank { if (credit) "Credit" else "Debit" }, maxLines = 1) },
        supportingContent = { Text("${cat?.name ?: "Untagged"} · ${t.channel} ··${t.account} · $time", maxLines = 1) },
        trailingContent = {
            Text(
                (if (credit) "+" else "−") + rupees(t.amountPaise),
                color = if (credit) CreditGreen else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
    )
}

@Composable
private fun UnparsedDialog(repo: TxnRepository, onDismiss: () -> Unit) {
    val list by remember { repo.dao.unparsed() }.collectAsStateWithLifecycle(emptyList())
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onDismiss) { Text("Close") } },
        title = { Text("Unrecognised bank SMS") },
        text = {
            if (list.isEmpty()) Text("None. Every bank SMS with an amount was parsed.")
            else SelectionContainer {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(list, key = { it.id }) { u ->
                        Column(Modifier.padding(vertical = 8.dp)) {
                            Text(u.sender, style = MaterialTheme.typography.labelMedium)
                            Text(u.body, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.padding(2.dp))
                        }
                    }
                }
            }
        },
    )
}
