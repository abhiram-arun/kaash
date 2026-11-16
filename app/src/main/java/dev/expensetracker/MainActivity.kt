package dev.expensetracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.expensetracker.data.TxnRepository
import dev.expensetracker.ui.BillsScreen
import dev.expensetracker.ui.BudgetsScreen
import dev.expensetracker.ui.CalendarScreen
import dev.expensetracker.ui.KaashTheme
import dev.expensetracker.ui.SummaryScreen
import dev.expensetracker.ui.TransactionsScreen
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    /** Txn id from a tapped "new transaction" notification, consumed once its tag picker opens. */
    private val openTxn = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark-only app: light status/nav icons regardless of the system theme.
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        if (savedInstanceState == null) openTxn.value = intent.txnExtra()
        val repo = (application as App).repo
        setContent { KaashTheme { AppUi(repo, openTxn.value) { openTxn.value = null } } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openTxn.value = intent.txnExtra()
    }

    private fun Intent.txnExtra() = getLongExtra(Notifier.EXTRA_TXN, -1L).takeIf { it > 0 }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Txns("Activity", Icons.AutoMirrored.Rounded.ReceiptLong),
    Summary("Insights", Icons.Rounded.DonutLarge),
    Budgets("Budgets", Icons.Rounded.Savings),
    Bills("Bills", Icons.Rounded.EventRepeat),
    Calendar("Calendar", Icons.Rounded.CalendarMonth),
}

@Composable
private fun AppUi(repo: TxnRepository, openTxn: Long?, onOpened: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Tab.Txns) }
    LaunchedEffect(openTxn) { if (openTxn != null) tab = Tab.Txns }
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) } // shared so tabs stay on the same month
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = scheme.background,
        bottomBar = {
            NavigationBar(containerColor = scheme.surfaceContainer) {
                Tab.entries.forEach {
                    NavigationBarItem(
                        selected = tab == it,
                        onClick = { tab = it },
                        icon = { Icon(it.icon, contentDescription = null) },
                        label = { Text(it.label, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = scheme.primaryContainer,
                            selectedIconColor = scheme.primary,
                            selectedTextColor = scheme.primary,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                Tab.Txns -> TransactionsScreen(repo, month, { month = it }, openTxn, onOpened)
                Tab.Summary -> SummaryScreen(repo, month) { month = it }
                Tab.Budgets -> BudgetsScreen(repo)
                Tab.Bills -> BillsScreen(repo)
                Tab.Calendar -> CalendarScreen(repo, month) { month = it }
            }
        }
    }
}
