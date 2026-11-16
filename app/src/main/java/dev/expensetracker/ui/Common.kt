package dev.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.expensetracker.data.Category
import dev.expensetracker.data.Txn
import dev.expensetracker.data.displayPayee
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        actions()
    }
}

/** Pill-shaped ‹ Nov 2026 › switcher; [content] colour lets it sit on the hero gradient. */
@Composable
fun MonthSwitcher(month: YearMonth, onMonth: (YearMonth) -> Unit, content: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        Modifier.clip(CircleShape).background(content.copy(alpha = 0.10f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton({ onMonth(month.minusMonths(1)) }, Modifier.size(36.dp)) {
            Icon(Icons.Rounded.ChevronLeft, "Previous month", tint = content)
        }
        Text(month.format(DateTimeFormatter.ofPattern("MMM yyyy")), style = MaterialTheme.typography.labelLarge, color = content)
        IconButton({ onMonth(month.plusMonths(1)) }, Modifier.size(36.dp)) {
            Icon(Icons.Rounded.ChevronRight, "Next month", tint = content)
        }
    }
}

/** Corner shape for a row inside a grouped card: big radius on the outer corners only. */
fun groupShape(index: Int, count: Int): Shape {
    val big = 20.dp
    val small = 6.dp
    val top = if (index == 0) big else small
    val bottom = if (index == count - 1) big else small
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

fun displayName(t: Txn): String = displayPayee(t.counterparty, t.direction)

/** "Food & Dining › Cafes & Coffee", "Food & Dining", or "Untagged". */
fun categoryLabel(c: Category?, byId: Map<Long, Category>): String {
    if (c == null) return "Untagged"
    val parent = c.parentId?.let(byId::get) ?: return c.name
    return "${parent.name} › ${c.name}"
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) = Text(
    text.uppercase(),
    modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

/** Parses a rupee amount typed by the user into paise; null if blank, invalid or not positive. */
fun parseRupees(s: String): Long? =
    s.trim().replace(",", "").toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }?.movePointRight(2)?.toLong()

fun paiseToInput(p: Long?): String = p?.let { BigDecimal.valueOf(it, 2).stripTrailingZeros().toPlainString() }.orEmpty()

/** Blank input saves null (clears the limit). */
@Composable
fun AmountDialog(title: String, initial: Long?, onSave: (Long?) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(paiseToInput(initial)) }
    val valid = text.isBlank() || parseRupees(text) != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                text, { text = it },
                label = { Text("Amount (₹), blank to remove") },
                prefix = { Text("₹ ") },
                singleLine = true,
                isError = !valid,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = { TextButton({ onSave(parseRupees(text)) }, enabled = valid) { Text("Save") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}
