package dev.expensetracker

import android.app.Application
import androidx.room.Room
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.expensetracker.data.AppDatabase
import dev.expensetracker.data.MIGRATION_1_2
import dev.expensetracker.data.MIGRATION_2_3
import dev.expensetracker.data.MIGRATION_3_4
import dev.expensetracker.data.MIGRATION_4_5
import dev.expensetracker.data.TxnRepository
import dev.expensetracker.work.DailyBillWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class App : Application() {
    val db by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "expenses.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
    }
    val notifier by lazy { Notifier(this) }
    val repo by lazy { TxnRepository(db, notifier) }

    /** Outlives screens, so work like the inbox scan isn't cancelled when the user switches tabs. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        scope.launch { repo.seedCategories() }
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "bill-reminders",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DailyBillWorker>(1, TimeUnit.DAYS).build(),
        )
    }
}
