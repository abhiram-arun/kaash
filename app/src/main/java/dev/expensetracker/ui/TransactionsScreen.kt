package dev.expensetracker.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.SmsFailed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.expensetracker.App
import dev.expensetracker.data.Category
import dev.expensetracker.data.TOPIC_ORDER
import dev.expensetracker.data.Txn
import dev.expensetracker.data.TxnRepository
import dev.expensetracker.ingest.backfillInbox
import dev.expensetracker.monthRange
import dev.expensetracker.parse.Direction
import dev.expensetracker.rupees
import dev.expensetracker.toLocalDate
import dev.expensetracker.zone
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val dayFmt = DateTimeFormatter.ofPattern("EEEE, d MMM")
private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")

private fun hasSms(ctx: Context) = listOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
    .all { ctx.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

@Composable
fun TransactionsScreen(
    repo: TxnRepository, month: YearMonth, onMonth: (YearMonth) -> Unit,
    openTxnId: Long? = null, onOpened: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    var q by rememberSaveable { mutableStateOf("") }
    var dir by rememberSaveable { mutableStateOf<String?>(null) }
    var acct by rememberSaveable { mutableStateOf<String?>(null) }
    var cat by rememberSaveable { mutableStateOf<Long?>(null) }
    var untagged by rememberSaveable { mutableStateOf(false) }

    val (from, to) = monthRange(month)
    val txns by remember(from, to, q, dir, acct, cat, untagged) {
        repo.dao.txns(from, to, q.trim(), cat, untagged, dir, acct)
    }.collectAsStateWithLifecycle(emptyList())
    val spent by remember(from, to) { repo.dao.total("DEBIT", from, to) }.collectAsStateWithLifecycle(0L)
    val received by remember(from, to) { repo.dao.total("CREDIT", from, to) }.collectAsStateWithLifecycle(0L)
    val untaggedCount by remember(from, to) { repo.dao.untaggedCount(from, to) }.collectAsStateWithLifecycle(0)
    val cats by remember { repo.dao.categories() }.collectAsStateWithLifecycle(emptyList())
    val byId = remember(cats) { cats.associateBy { it.id } }
    val topics = remember(cats) { cats.filter { it.parentId == null }.sortedBy { TOPIC_ORDER[it.name] ?: 99 } }
    val accounts by remember { repo.dao.accounts() }.collectAsStateWithLifecycle(emptyList())
    val unparsed by remember { repo.dao.unparsed() }.collectAsStateWithLifecycle(emptyList())

    var tagging by remember { mutableStateOf<Txn?>(null) }
    var showUnparsed by remember { mutableStateOf(false) }
    // Tapped "new transaction" notification → straight into its tag picker.
    LaunchedEffect(openTxnId) {
        openTxnId?.let { tagging = repo.dao.txn(it); onOpened() }
    }
    var smsGranted by remember { mutableStateOf(hasSms(ctx)) }
    // Re-check on return: on Android 13+ SMS access is granted in system Settings, not via our dialog.
    LifecycleResumeEffect(Unit) {
        smsGranted = hasSms(ctx)
        onPauseOrDispose {}
    }
    val rescan: () -> Unit = {
        val app = ctx.applicationContext as App
        app.scope.launch {
            val n = backfillInbox(app, repo)
            withContext(Dispatchers.Main) { Toast.makeText(app, "Scanned $n SMS", Toast.LENGTH_SHORT).show() }
        }
    }
    val askPerms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        smsGranted = hasSms(ctx)
        if (smsGranted) rescan()
    }

    val grouped = remember(txns) { txns.groupBy { it.timestamp.toLocalDate() } }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            ScreenHeader("Kaash") {
                IconButton(rescan, enabled = smsGranted) { Icon(Icons.Rounded.Refresh, "Rescan SMS inbox") }
                IconButton({ showUnparsed = true }) {
                    BadgedBox(badge = { if (unparsed.isNotEmpty()) Badge { Text("${unparsed.size}") } }) {
                        Icon(Icons.Rounded.SmsFailed, "Unrecognised bank SMS")
                    }
                }
            }
        }
        item {
            HeroCard(month, onMonth, spent, received, untaggedCount) { untagged = true; cat = null }
        }
        if (!smsGranted) item { PermissionCard(askPerms::launch) }
        item {
            OutlinedTextField(
                q, { q = it },
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
                placeholder = { Text("Search payee") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = scheme.surfaceContainer, focusedContainerColor = scheme.surfaceContainer,
                    unfocusedBorderColor = Color.Transparent,
                ),
            )
        }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip("Spent", dir == "DEBIT") { dir = if (dir == "DEBIT") null else "DEBIT" }
                Chip("Received", dir == "CREDIT") { dir = if (dir == "CREDIT") null else "CREDIT" }
                Chip("Untagged", untagged) { untagged = !untagged; if (untagged) cat = null }
                accounts.forEach { a -> Chip("··$a", acct == a) { acct = if (acct == a) null else a } }
                topics.forEach { c ->
                    Chip(c.name, cat == c.id, c) { cat = if (cat == c.id) null else c.id; untagged = false }
                }
            }
        }
        if (txns.isEmpty()) item {
            Text(
                "No transactions match.", Modifier.padding(24.dp), color = scheme.onSurfaceVariant,
            )
        }
        grouped.forEach { (day, list) ->
            item(key = "h${day.toEpochDay()}") { DayHeader(day, list) }
            itemsIndexed(list, key = { _, t -> t.id }) { i, t ->
                TxnRow(
                    t, t.categoryId?.let(byId::get), byId,
                    Modifier.padding(horizontal = 16.dp, vertical = 1.dp).clip(groupShape(i, list.size)),
                ) { tagging = t }
            }
        }
    }

    tagging?.let { t ->
        TagSheet(t, cats, repo, onPick = { c -> scope.launch { repo.tag(t, c) }; tagging = null }) { tagging = null }
    }
    if (showUnparsed) UnparsedDialog(unparsed.map { it.sender to it.body }) { showUnparsed = false }
}

@Composable
private fun HeroCard(
    month: YearMonth, onMonth: (YearMonth) -> Unit, spent: Long, received: Long, untagged: Int, onReview: () -> Unit,
) {
    Box(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(HeroSurface).padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Spent this month", Modifier.weight(1f), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
                MonthSwitcher(month, onMonth, Color.White)
            }
            Text(rupees(spent), color = Color.White, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(16.dp))
            Row {
                HeroStat("Received", rupees(received), Modifier.weight(1f))
                val net = received - spent
                HeroStat("Net", (if (net >= 0) "+" else "−") + rupees(kotlin.math.abs(net)), Modifier.weight(1f))
            }
            if (untagged > 0) Surface(
                onClick = onReview, shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(
                    "Tag $untagged untagged", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier) = Column(modifier) {
    Text(label, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
    Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun PermissionCard(launch: (Array<String>) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(scheme.tertiaryContainer).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconAvatar(Icons.Rounded.Sms, scheme.onTertiaryContainer, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Read bank SMS automatically", style = MaterialTheme.typography.titleSmall, color = scheme.onTertiaryContainer)
            Text(
                "Blocked? App info → ⋮ → Allow restricted settings.",
                style = MaterialTheme.typography.bodySmall, color = scheme.onTertiaryContainer,
            )
        }
        Button({
            val perms = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
            if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
            launch(perms.toTypedArray())
        }) { Text("Allow") }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, cat: Category? = null, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = cat?.let { { Icon(iconFor(it), null, Modifier.size(18.dp), tint = scheme.accent(it.color)) } },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = scheme.surfaceContainer,
            selectedContainerColor = scheme.primaryContainer,
            selectedLabelColor = scheme.onPrimaryContainer,
        ),
        border = null,
    )
}

@Composable
private fun DayHeader(day: LocalDate, list: List<Txn>) {
    val debit = list.filter { it.direction == Direction.DEBIT }.sumOf { it.amountPaise }
    val label = when (day) {
        LocalDate.now() -> "Today"
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> day.format(dayFmt)
    }
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        if (debit > 0) Text("−${rupees(debit)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TxnRow(t: Txn, cat: Category?, byId: Map<Long, Category>, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val credit = t.direction == Direction.CREDIT
    val time = Instant.ofEpochMilli(t.timestamp).atZone(zone).format(timeFmt)
    Row(
        modifier.fillMaxWidth().background(scheme.surfaceContainer).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(cat, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(displayName(t), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                categoryLabel(cat, byId),
                style = MaterialTheme.typography.bodySmall,
                color = cat?.let { scheme.accent(it.color) } ?: scheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text("${t.channel} ··${t.account} · $time", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            (if (credit) "+" else "−") + rupees(t.amountPaise),
            style = MaterialTheme.typography.titleSmall,
            color = if (credit) scheme.positive else scheme.onSurface,
        )
    }
}

@Composable
private fun UnparsedDialog(list: List<Pair<String, String>>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onDismiss) { Text("Close") } },
        title = { Text("Unrecognised bank SMS") },
        text = {
            if (list.isEmpty()) Text("None. Every bank SMS with an amount was parsed.")
            else SelectionContainer {
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(list) { (sender, body) ->
                        Column {
                            Text(sender, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(body, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
    )
}
