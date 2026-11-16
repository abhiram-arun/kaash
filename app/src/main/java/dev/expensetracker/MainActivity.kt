package dev.expensetracker

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import dev.expensetracker.data.TxnRepository
import dev.expensetracker.ui.BillsScreen
import dev.expensetracker.ui.BudgetsScreen
import dev.expensetracker.ui.CalendarScreen
import dev.expensetracker.ui.SummaryScreen
import dev.expensetracker.ui.TransactionsScreen
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as App).repo
        setContent {
            val ctx = LocalContext.current
            val dark = isSystemInDarkTheme()
            val scheme = when {
                Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
                dark -> darkColorScheme()
                else -> lightColorScheme()
            }
            MaterialTheme(colorScheme = scheme) { AppUi(repo) }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Txns("Txns", Icons.AutoMirrored.Filled.ReceiptLong),
    Summary("Summary", Icons.Filled.PieChart),
    Budgets("Budgets", Icons.Filled.Savings),
    Bills("Bills", Icons.Filled.Event),
    Calendar("Calendar", Icons.Filled.CalendarMonth),
}

@Composable
private fun AppUi(repo: TxnRepository) {
    var tab by rememberSaveable { mutableStateOf(Tab.Txns) }
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) } // shared so tabs stay on the same month
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach {
                    NavigationBarItem(
                        selected = tab == it,
                        onClick = { tab = it },
                        icon = { Icon(it.icon, contentDescription = null) },
                        label = { Text(it.label, maxLines = 1) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                Tab.Txns -> TransactionsScreen(repo, month) { month = it }
                Tab.Summary -> SummaryScreen(repo, month) { month = it }
                Tab.Budgets -> BudgetsScreen(repo)
                Tab.Bills -> BillsScreen(repo)
                Tab.Calendar -> CalendarScreen(repo, month) { month = it }
            }
        }
    }
}
