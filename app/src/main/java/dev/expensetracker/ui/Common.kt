package dev.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.expensetracker.data.Category
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun MonthBar(month: YearMonth, onMonth: (YearMonth) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton({ onMonth(month.minusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month")
        }
        Text(month.format(DateTimeFormatter.ofPattern("MMM yyyy")), style = MaterialTheme.typography.titleMedium)
        IconButton({ onMonth(month.plusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month")
        }
    }
}

@Composable
fun Dot(color: Color, size: Dp = 10.dp) = Box(Modifier.size(size).background(color, CircleShape))

@Composable
fun TagDialog(categories: List<Category>, current: Long?, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onDismiss) { Text("Cancel") } },
        title = { Text("Category") },
        text = {
            LazyColumn {
                items(categories, key = { it.id }) { c ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(c.id) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = c.id == current, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Dot(Color(c.color))
                        Spacer(Modifier.width(8.dp))
                        Text(c.name)
                    }
                }
            }
        },
    )
}

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
                singleLine = true,
                isError = !valid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = { TextButton({ onSave(parseRupees(text)) }, enabled = valid) { Text("Save") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}
