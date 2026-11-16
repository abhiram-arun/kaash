@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package dev.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.expensetracker.data.Category
import dev.expensetracker.data.TOPIC_ORDER
import dev.expensetracker.data.Txn
import dev.expensetracker.data.TxnRepository
import dev.expensetracker.parse.Direction
import dev.expensetracker.rupees
import dev.expensetracker.zone
import java.time.Instant
import java.time.format.DateTimeFormatter

private val whenFmt = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a")

/** Two-step picker: topic grid → subtopic chips, plus search across every topic and subtopic. */
@Composable
fun TagSheet(t: Txn, cats: List<Category>, repo: TxnRepository, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val byId = remember(cats) { cats.associateBy { it.id } }
    val topics = remember(cats) { cats.filter { it.parentId == null }.sortedBy { TOPIC_ORDER[it.name] ?: 99 } }
    val subs = remember(cats) { cats.filter { it.parentId != null }.groupBy { it.parentId!! } }
    val current = t.categoryId?.let(byId::get)
    val currentTopicId = current?.let { it.parentId ?: it.id }
    var open by remember { mutableStateOf(currentTopicId?.let(byId::get)) }
    var q by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        // Scrolls as a whole now that the split block can make it taller than the screen; inner lists are height-bounded.
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryAvatar(current, 52.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(displayName(t), style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        Instant.ofEpochMilli(t.timestamp).atZone(zone).format(whenFmt) + " · ${t.channel} ··${t.account}",
                        style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
                    )
                }
                val credit = t.direction == Direction.CREDIT
                Text(
                    (if (credit) "+" else "−") + rupees(t.amountPaise),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (credit) scheme.positive else scheme.onSurface,
                )
            }
            Text(
                t.raw,
                Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(scheme.surfaceContainerHigh).padding(12.dp),
                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
                maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
            if (t.direction == Direction.DEBIT) SplitSection(t, repo)
            OutlinedTextField(
                q, { q = it },
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                placeholder = { Text("Search ${cats.size} categories") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
            )

            Box(Modifier.heightIn(min = 240.dp, max = 460.dp)) {
                val topic = open
                when {
                    q.isNotBlank() -> {
                        val ql = q.trim().lowercase()
                        val hits = cats.filter {
                            it.name.lowercase().contains(ql) ||
                                it.parentId?.let(byId::get)?.name?.lowercase()?.contains(ql) == true
                        }.sortedBy { it.parentId != null }
                        LazyColumn {
                            if (hits.isEmpty()) item { Text("No category matches \"$q\"", color = scheme.onSurfaceVariant) }
                            items(hits, key = { it.id }) { c ->
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                        .background(if (c.id == t.categoryId) scheme.accent(c.color).copy(alpha = 0.14f) else scheme.surface)
                                        .clickable { onPick(c.id) }.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CategoryAvatar(c, 38.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(c.name, style = MaterialTheme.typography.titleSmall)
                                        c.parentId?.let(byId::get)?.let {
                                            Text(it.name, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    topic == null -> LazyVerticalGrid(
                        GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(topics, key = { it.id }) { c ->
                            val selected = c.id == currentTopicId
                            Column(
                                Modifier.clip(RoundedCornerShape(18.dp))
                                    .background(if (selected) scheme.accent(c.color).copy(alpha = 0.18f) else scheme.surfaceContainerHigh)
                                    .clickable { open = c }.padding(vertical = 14.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CategoryAvatar(c, 44.dp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    c.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                                    maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    else -> Column(Modifier.verticalScroll(rememberScrollState())) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton({ open = null }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "All topics") }
                            CategoryAvatar(topic, 36.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(topic.name, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SubChip(topic, "General", t.categoryId == topic.id) { onPick(topic.id) }
                            subs[topic.id].orEmpty().forEach { c ->
                                SubChip(c, c.name, t.categoryId == c.id) { onPick(c.id) }
                            }
                        }
                    }
                }
            }
            if (t.merchantKey.isNotBlank()) Text(
                "All transactions from ${displayName(t)} will get this tag.",
                Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SubChip(c: Category, label: String, selected: Boolean, onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.accent(c.color)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(iconFor(c), contentDescription = null, tint = tint, modifier = Modifier.size(18.dp)) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = tint.copy(alpha = 0.2f)),
    )
}
