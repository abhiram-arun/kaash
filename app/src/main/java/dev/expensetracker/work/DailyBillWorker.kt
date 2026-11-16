package dev.expensetracker.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.expensetracker.App
import dev.expensetracker.data.RecurringBill
import dev.expensetracker.rupees
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class DailyBillWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as App
        val today = LocalDate.now()
        for (b in app.repo.dao.allBills()) {
            val title = reminderTitle(b, today) ?: continue
            app.notifier.show(1000 + b.id.toInt(), title, rupees(b.amountPaise))
        }
        return Result.success()
    }
}

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
