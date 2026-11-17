package dev.expensetracker.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.expensetracker.App
import dev.expensetracker.Notifier
import dev.expensetracker.data.RecurringBill
import dev.expensetracker.data.SplitDetail
import dev.expensetracker.data.displayPayee
import dev.expensetracker.parse.Direction
import dev.expensetracker.rupees
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Daily: bill reminders, plus weekly nudges for splits still unsettled a week after they were added. */
class DailyBillWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as App
        val dao = app.repo.dao
        val today = LocalDate.now()
        for (b in dao.allBills()) {
            val title = reminderTitle(b, today) ?: continue
            app.notifier.show(1000 + b.id.toInt(), title, rupees(b.amountPaise))
        }

        val now = System.currentTimeMillis()
        for (rows in splitsDueForReminder(dao.unsettledList(), now)) {
            val first = rows.minBy { it.split.createdAt }
            app.notifier.show(
                id = first.split.personId.toInt(),
                title = "${first.personName} owes you ${rupees(rows.sumOf { it.split.amountPaise })}",
                text = if (rows.size == 1) "Split for ${displayPayee(first.payee, Direction.DEBIT)} isn't settled yet."
                else "${rows.size} splits aren't settled yet.",
                channel = Notifier.SPLITS,
                txnId = first.split.txnId,
            )
            dao.markReminded(rows.map { it.split.id }, now)
        }
        return Result.success()
    }
}

const val WEEK_MS = 7 * 24 * 60 * 60 * 1000L

/** Unsettled splits older than a week and not reminded in the last week, grouped one notification per person. */
fun splitsDueForReminder(splits: List<SplitDetail>, now: Long): Collection<List<SplitDetail>> =
    splits.filter { d ->
        val s = d.split
        !s.settled && now - s.createdAt >= WEEK_MS && (s.lastReminded == null || now - s.lastReminded >= WEEK_MS)
    }.groupBy { it.split.personId }.values

/** Null when the next unpaid occurrence is further away than the bill's reminder window. */
fun reminderTitle(b: RecurringBill, today: LocalDate): String? {
    val days = ChronoUnit.DAYS.between(today, b.dueDate())
    return when {
        days > b.remindDaysBefore -> null
        days < 0 -> "Overdue: ${b.name}"
        days == 0L -> "${b.name} due today"
        days == 1L -> "${b.name} due tomorrow"
        else -> "${b.name} due in $days days"
    }
}
